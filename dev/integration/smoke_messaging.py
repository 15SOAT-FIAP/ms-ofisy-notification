"""Exercises the PR #21 consumer through real LocalStack SNS/SQS/DynamoDB."""
import json
import time
import uuid
from events import client, event, send, assert_quote_subscription


def eventually(operation, label):
    for _ in range(90):
        result = operation()
        if result:
            return result
        time.sleep(1)
    raise AssertionError("Timeout: " + label)


def main():
    ddb, sqs = client("dynamodb"), client("sqs")
    assert_quote_subscription()

    def item(body):
        return ddb.get_item(TableName="Notifications", Key={"id": {"S": body["eventId"]}}, ConsistentRead=True).get("Item")

    for kind, internal in [("StockLow", "LOW_STOCK"), ("QuoteSentForApproval", "QUOTE_GENERATED")]:
        body = event(kind)
        send(body)
        stored = eventually(lambda: item(body), kind + " consumption")
        assert stored["type"]["S"] == internal
        ddb.update_item(TableName="Notifications", Key={"id": {"S": body["eventId"]}},
                        UpdateExpression="SET #r = :yes", ExpressionAttributeNames={"#r": "read"},
                        ExpressionAttributeValues={":yes": {"BOOL": True}})
        send(body)
        time.sleep(3)
        assert item(body)["read"]["BOOL"] is True
        assert item(body)["updatedAt"] == stored["updatedAt"]

    premature = event("QuoteCreated")
    send(premature)
    failure = {"eventId": str(uuid.uuid4()), "type": "StockConsumptionFailed",
               "serviceOrderId": premature["serviceOrderId"], "occurredAt": premature["occurredAt"], "payload": {}}
    send(failure)
    approval = event("QuoteSentForApproval")
    send(approval)
    eventually(lambda: item(approval), "approval positive control")
    assert item(premature) is None and item(failure) is None
    assert "disponível para aprovação" in item(approval)["message"]["S"]

    ignored = event("StockLow")
    ignored["type"] = "Unsubscribed"
    send(ignored, "ofisy-stock-events.fifo")
    control = event("StockLow")
    send(control)
    eventually(lambda: item(control), "filter positive control")
    assert item(ignored) is None

    invalid = event("StockLow")
    del invalid["payload"]["currentQuantity"]
    send(invalid)
    healthy = event("StockLow")
    send(healthy)
    eventually(lambda: item(healthy), "independent group")
    dlq = sqs.get_queue_url(QueueName="ofisy-notification-dlq.fifo")["QueueUrl"]
    failed = eventually(lambda: sqs.receive_message(QueueUrl=dlq, MaxNumberOfMessages=1, WaitTimeSeconds=1).get("Messages"), "retry and DLQ")[0]
    assert json.loads(failed["Body"])["eventId"] == invalid["eventId"]
    assert item(invalid) is None
    invalid["payload"]["currentQuantity"] = 2
    send(invalid)
    eventually(lambda: item(invalid), "repair and replay")
    sqs.delete_message(QueueUrl=dlq, ReceiptHandle=failed["ReceiptHandle"])
    print(json.dumps({"status": "PASS", "checks": ["canonical events", "no premature quote or stock failure notification", "approval message", "internal types preserved", "body filters", "idempotence", "retry and DLQ", "independent group", "replay"]}))


if __name__ == "__main__":
    main()
