import time
import boto3
from botocore.config import Config

TABLE = "Notifications"
QUEUE = "techchallenge-ofisy-notifications-queue.fifo"
CONFIG = Config(connect_timeout=2, read_timeout=3, retries={"max_attempts": 1})


def wait_for(operation, label):
    for _ in range(90):
        try:
            return operation()
        except Exception:
            time.sleep(1)
    raise RuntimeError("Service unavailable: " + label)


def main():
    ddb = boto3.client("dynamodb", endpoint_url="http://dynamodb:8000", config=CONFIG)
    sqs = boto3.client("sqs", endpoint_url="http://sqs:9324", config=CONFIG)
    tables = wait_for(ddb.list_tables, "DynamoDB")["TableNames"]
    if TABLE not in tables:
        ddb.create_table(
            TableName=TABLE, BillingMode="PAY_PER_REQUEST",
            AttributeDefinitions=[{"AttributeName": n, "AttributeType": "S"}
                                  for n in ("id", "type", "createdAt", "referenceId")],
            KeySchema=[{"AttributeName": "id", "KeyType": "HASH"}],
            GlobalSecondaryIndexes=[
                {"IndexName": "TypeIndex", "KeySchema": [
                    {"AttributeName": "type", "KeyType": "HASH"},
                    {"AttributeName": "createdAt", "KeyType": "RANGE"}], "Projection": {"ProjectionType": "ALL"}},
                {"IndexName": "ReferenceIdIndex", "KeySchema": [
                    {"AttributeName": "referenceId", "KeyType": "HASH"}], "Projection": {"ProjectionType": "ALL"}},
            ])
    wait_for(lambda: ddb.describe_table(TableName=TABLE), "Notifications table")
    wait_for(lambda: sqs.get_queue_url(QueueName=QUEUE), "SQS FIFO queue")
    print("READY: Notifications table, TypeIndex, ReferenceIdIndex and FIFO queue", flush=True)


if __name__ == "__main__":
    main()
