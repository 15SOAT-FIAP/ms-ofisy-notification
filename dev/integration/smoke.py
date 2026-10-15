"""End-to-end SQS -> application -> DynamoDB -> secured REST API."""
import base64
import hashlib
import hmac
import json
import os
import time
import urllib.error
import urllib.request
import uuid
import boto3
from botocore.config import Config

BASE = "http://app:8081/api/v1/notifications"
SECRET = os.environ["APP_JWT_SECRET"]
ISSUER = os.environ["APP_JWT_ISSUER"]


def encode(value):
    return base64.urlsafe_b64encode(json.dumps(value, separators=(",", ":")).encode()).rstrip(b"=").decode()


def token(issuer=ISSUER, secret=SECRET, roles=None):
    content = encode({"alg": "HS256", "typ": "JWT"}) + "." + encode({
        "sub": "local-tester", "iss": issuer, "roles": roles or ["ROLE_ADMIN"],
        "iat": int(time.time()), "exp": int(time.time()) + 3600})
    signature = hmac.new(secret.encode(), content.encode(), hashlib.sha256).digest()
    return content + "." + base64.urlsafe_b64encode(signature).rstrip(b"=").decode()


def request(path, method="GET", jwt=None):
    headers = {"Authorization": "Bearer " + jwt} if jwt else {}
    try:
        with urllib.request.urlopen(urllib.request.Request(BASE + path, headers=headers, method=method), timeout=4) as response:
            return response.status, json.loads(response.read())
    except urllib.error.HTTPError as error:
        return error.code, None


def eventually(operation, description):
    for _ in range(90):
        try:
            result = operation()
            if result:
                return result
        except (OSError, urllib.error.URLError):
            pass
        time.sleep(1)
    raise AssertionError("Timeout: " + description)


def main():
    admin = token()
    eventually(lambda: request("/stock", jwt=admin)[0] == 200, "API startup")
    assert request("/stock")[0] in (401, 403), "Anonymous stock access was allowed"
    assert request("/stock", jwt=token(issuer="wrong-issuer"))[0] == 401
    assert request("/stock", jwt=token(secret="another-local-secret-with-at-least-32-bytes"))[0] == 401
    config = Config(connect_timeout=3, read_timeout=5, retries={"max_attempts": 1})
    sqs = boto3.client("sqs", endpoint_url="http://sqs:9324", config=config)
    ddb = boto3.client("dynamodb", endpoint_url="http://dynamodb:8000", config=config)
    queue = sqs.get_queue_url(QueueName="techchallenge-ofisy-notifications-queue.fifo")["QueueUrl"]
    event_id = str(uuid.uuid4())
    event = {"eventId": event_id, "eventType": "LOW_STOCK", "occurredAt": "2026-10-13T00:00:00Z",
             "payload": {"stockId": str(uuid.uuid4()), "productName": "Radiador smoke test", "currentQuantity": 2, "minThreshold": 10}}

    def send(body):
        sqs.send_message(QueueUrl=queue, MessageBody=json.dumps(body), MessageGroupId="smoke",
                         MessageDeduplicationId=str(uuid.uuid4()))

    send(event)
    eventually(lambda: request("/stock/" + event_id, jwt=admin)[0] == 200, "LOW_STOCK consumption")
    stock_record = request("/stock/" + event_id, jwt=admin)[1]
    assert stock_record["stockId"] == event["payload"]["stockId"] and stock_record["quoteId"] is None
    assert stock_record["message"] == "Estoque baixo para Radiador smoke test. Quantidade atual: 2. Mínimo: 10"
    assert request("/service-orders/" + event_id)[0] == 404
    assert request("/service-orders/" + event_id + "/read", method="PATCH")[0] == 404
    assert request("/stock/" + event_id, jwt=admin)[1]["read"] is False
    status, marked = request("/stock/" + event_id + "/read", method="PATCH", jwt=admin)
    assert status == 200 and marked["read"] is True
    send(event)  # New SQS deduplication id forces a second delivery of the same domain event.

    def queue_drained():
        attributes = sqs.get_queue_attributes(QueueUrl=queue, AttributeNames=[
            "ApproximateNumberOfMessages", "ApproximateNumberOfMessagesNotVisible"])["Attributes"]
        return all(int(v) == 0 for v in attributes.values())

    time.sleep(2)
    eventually(queue_drained, "redelivery acknowledged")
    status, after = request("/stock/" + event_id, jwt=admin)
    assert status == 200 and after["read"] and after["updatedAt"] == marked["updatedAt"]
    item = ddb.get_item(TableName="Notifications", Key={"id": {"S": event_id}}, ConsistentRead=True)["Item"]
    assert item["read"]["BOOL"] is True
    stock = request("/stock?limit=10", jwt=admin)[1]
    assert any(n["id"] == event_id for n in stock["items"])
    quote = {"eventId": str(uuid.uuid4()), "eventType": "QUOTE_GENERATED", "occurredAt": event["occurredAt"],
             "payload": {"quoteId": str(uuid.uuid4()), "serviceOrderId": str(uuid.uuid4()), "totalPrice": "123.45"}}
    send(quote)
    eventually(lambda: any(n["id"] == quote["eventId"] for n in request("/service-orders")[1]["items"]), "QUOTE_GENERATED public listing")
    quote_path = "/service-orders/" + quote["eventId"]
    quote_record = request(quote_path)[1]
    assert quote_record["quoteId"] == quote["payload"]["quoteId"] and quote_record["stockId"] is None
    assert quote_record["message"] == "Orçamento #{} gerado para a ordem de serviço '{}'. Valor total: R$ 123.45".format(quote["payload"]["quoteId"], quote["payload"]["serviceOrderId"])
    assert request("/stock/" + quote["eventId"], jwt=admin)[0] == 404
    assert request("/stock/" + quote["eventId"] + "/read", method="PATCH", jwt=admin)[0] == 404
    assert request(quote_path)[1]["read"] is False
    assert request(quote_path)[1]["updatedAt"] == quote_record["updatedAt"]
    quote_status, quote_marked = request(quote_path + "/read", method="PATCH")
    assert quote_status == 200 and quote_marked["read"] is True
    send(quote)
    time.sleep(2)
    eventually(queue_drained, "quote redelivery acknowledged")
    assert request(quote_path)[1]["read"] is True
    assert request(quote_path)[1]["updatedAt"] == quote_marked["updatedAt"]
    for category, notification_id in (("stock", event_id), ("service-orders", quote["eventId"])):
        eventually(lambda: not any(n["id"] == notification_id for n in request("/" + category + "/unread", jwt=admin)[1]["items"]), "unread list reflects read state")
    missing = str(uuid.uuid4())
    for category in ("stock", "service-orders"):
        assert request("/" + category + "/" + missing, jwt=admin)[0] == 404
        assert request("/" + category + "/" + missing + "/read", method="PATCH", jwt=admin)[0] == 404
    print(json.dumps({"status": "PASS", "checks": ["JWT issuer", "JWT signature", "anonymous stock denied",
        "LOW_STOCK consumption", "PATCH read", "redelivery preserves read and updatedAt", "DynamoDB persistence",
        "TypeIndex listing", "QUOTE_GENERATED consumption", "core message and DTO fields", "wrong-type GET/PATCH 404 both directions",
        "wrong-type PATCH preserves state", "missing-id GET/PATCH 404 both categories", "core public quote read route",
        "quote redelivery preserves read and updatedAt", "both unread lists reflect read state"], "stockNotificationId": event_id,
        "quoteNotificationId": quote["eventId"]}, indent=2), flush=True)


if __name__ == "__main__":
    main()
