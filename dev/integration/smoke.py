"""End-to-end SNS -> filtered SQS FIFO -> application -> DynamoDB -> secured REST API."""
import base64
import hashlib
import hmac
import json
import os
import time
import urllib.error
import urllib.request
import urllib.parse
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
    sqs = boto3.client("sqs", endpoint_url="http://localstack:4566", config=config)
    ddb = boto3.client("dynamodb", endpoint_url="http://localstack:4566", config=config)
    queue = sqs.get_queue_url(QueueName="ofisy-notification.fifo")["QueueUrl"]
    sns = boto3.client("sns", endpoint_url="http://localstack:4566", config=config)
    topic_arns = {topic["TopicArn"].split(":")[-1]: topic["TopicArn"] for topic in sns.list_topics()["Topics"]}
    event_id = str(uuid.uuid4())
    event = {"eventId": event_id, "type": "LOW_STOCK", "serviceOrderId": None, "occurredAt": "2026-10-13T00:00:00Z",
             "payload": {"stockId": str(uuid.uuid4()), "productName": "Radiador smoke test", "currentQuantity": 2, "minThreshold": 10}}

    def send(body):
        sns.publish(TopicArn=topic_arns["ofisy-stock.fifo" if body["type"] == "LOW_STOCK" else "ofisy-billing.fifo"],
                    Message=json.dumps(body), MessageGroupId=body.get("serviceOrderId") or body["payload"]["stockId"],
                    MessageAttributes={"type": {"DataType": "String", "StringValue": body["type"]}},
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
    quote = {"eventId": str(uuid.uuid4()), "type": "QUOTE_GENERATED", "occurredAt": event["occurredAt"],
             "payload": {"quoteId": str(uuid.uuid4()), "serviceOrderId": str(uuid.uuid4()), "totalPrice": "123.45"}}
    quote["serviceOrderId"] = quote["payload"]["serviceOrderId"]
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
    # Traverse all four HTTP list routes with real continuation tokens.
    expected_ids = {"stock": {event_id}, "service-orders": {quote["eventId"]}}
    read_ids = {"stock": {event_id}, "service-orders": {quote["eventId"]}}
    for category, template in (("stock", event), ("service-orders", quote)):
        for i in range(3):
            body = {**template, "eventId": str(uuid.uuid4())}
            expected_ids[category].add(body["eventId"])
            send(body)
            if i == 1:
                read_ids[category].add(body["eventId"])
        eventually(lambda: expected_ids[category].issubset({n["id"] for n in request("/" + category, jwt=admin)[1]["items"]}),
                   "pagination dataset consumed: " + category)
        for notification_id in read_ids[category]:
            assert request("/" + category + "/" + notification_id + "/read", method="PATCH", jwt=admin)[0] == 200
        eventually(lambda: not any(n["id"] in read_ids[category] for n in request("/" + category + "/unread", jwt=admin)[1]["items"]),
                   "unread index converged: " + category)
        for unread in (False, True):
            route = "/" + category + ("/unread" if unread else "")
            cursor = None
            seen = []
            for _ in range(12):
                query = "?limit=1" + ("&nextToken=" + urllib.parse.quote(cursor, safe="") if cursor else "")
                status, page = request(route + query, jwt=admin)
                assert status == 200
                seen.extend(n["id"] for n in page["items"])
                cursor = page["nextToken"]
                if cursor is None:
                    break
            else:
                raise AssertionError("Pagination did not terminate: " + route)
            wanted = expected_ids[category] - read_ids[category] if unread else expected_ids[category]
            assert len(seen) == len(set(seen)) and set(seen) == wanted, (route, seen, wanted)

    # A valid payload with an unsubscribed routing attribute must be filtered by SNS.
    ignored = {**event, "eventId": str(uuid.uuid4())}
    sns.publish(TopicArn=topic_arns["ofisy-stock.fifo"], Message=json.dumps(ignored),
                MessageGroupId=ignored["payload"]["stockId"], MessageDeduplicationId=ignored["eventId"],
                MessageAttributes={"type": {"DataType": "String", "StringValue": "UNSUBSCRIBED"}})
    control = {**event, "eventId": str(uuid.uuid4())}
    send(control)
    eventually(lambda: request("/stock/" + control["eventId"], jwt=admin)[0] == 200, "SNS filter positive control")
    eventually(queue_drained, "SNS filter queue drained")
    assert "Item" not in ddb.get_item(TableName="Notifications", Key={"id": {"S": ignored["eventId"]}})
    assert queue_drained()

    # Invalid recognized payload must fail and reach DLQ; another group continues.
    invalid = {**event, "eventId": str(uuid.uuid4()), "payload": {**event["payload"], "stockId": str(uuid.uuid4())}}
    del invalid["payload"]["currentQuantity"]
    send(invalid)
    healthy = {**event, "eventId": str(uuid.uuid4()), "payload": {**event["payload"], "stockId": str(uuid.uuid4())}}
    send(healthy)
    eventually(lambda: request("/stock/" + healthy["eventId"], jwt=admin)[0] == 200, "independent FIFO group")

    dlq = sqs.get_queue_url(QueueName="ofisy-notification-dlq.fifo")["QueueUrl"]
    def receive_failed():
        messages = sqs.receive_message(QueueUrl=dlq, MaxNumberOfMessages=1, WaitTimeSeconds=1).get("Messages", [])
        return messages[0] if messages else None

    failed = eventually(receive_failed, "invalid payload retried and moved to DLQ")
    assert json.loads(failed["Body"])["eventId"] == invalid["eventId"]
    assert request("/stock/" + invalid["eventId"], jwt=admin)[0] == 404
    repaired = json.loads(failed["Body"])
    repaired["payload"]["currentQuantity"] = 2
    send(repaired)
    eventually(lambda: request("/stock/" + repaired["eventId"], jwt=admin)[0] == 200, "manual DLQ repair and replay")
    sqs.delete_message(QueueUrl=dlq, ReceiptHandle=failed["ReceiptHandle"])
    print(json.dumps({"status": "PASS", "checks": ["SNS FIFO to SQS FIFO with raw envelope", "SNS type filter", "invalid payload retry and DLQ", "independent group progress", "manual DLQ repair and replay", "JWT issuer", "JWT signature", "anonymous stock denied",
        "LOW_STOCK consumption", "PATCH read", "redelivery preserves read and updatedAt", "DynamoDB persistence",
        "TypeIndex listing", "QUOTE_GENERATED consumption", "core message and DTO fields", "wrong-type GET/PATCH 404 both directions",
        "wrong-type PATCH preserves state", "missing-id GET/PATCH 404 both categories", "core public quote read route",
        "quote redelivery preserves read and updatedAt", "both unread lists reflect read state", "all four HTTP routes paginate without missing or duplicate records"], "stockNotificationId": event_id,
        "quoteNotificationId": quote["eventId"]}, indent=2), flush=True)


if __name__ == "__main__":
    main()
