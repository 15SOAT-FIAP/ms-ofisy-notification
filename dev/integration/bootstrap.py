"""Local-only topology: producer topics -> filtered FIFO queue -> FIFO DLQ."""
import json
import time
import boto3
from botocore.config import Config

TABLE = "Notifications"
QUEUE = "ofisy-notification.fifo"
ENDPOINT = "http://localstack:4566"
CONFIG = Config(connect_timeout=2, read_timeout=3, retries={"max_attempts": 1})


def wait_for(operation, label):
    for _ in range(90):
        try:
            return operation()
        except Exception:
            time.sleep(1)
    raise RuntimeError("Service unavailable: " + label)


def main():
    ddb = boto3.client("dynamodb", endpoint_url=ENDPOINT, config=CONFIG)
    sqs = boto3.client("sqs", endpoint_url=ENDPOINT, config=CONFIG)
    sns = boto3.client("sns", endpoint_url=ENDPOINT, config=CONFIG)
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
    dlq = sqs.create_queue(QueueName="ofisy-notification-dlq.fifo", Attributes={"FifoQueue": "true"})["QueueUrl"]
    dlq_arn = sqs.get_queue_attributes(QueueUrl=dlq, AttributeNames=["QueueArn"])["Attributes"]["QueueArn"]
    queue = sqs.create_queue(QueueName=QUEUE, Attributes={
        "FifoQueue": "true", "VisibilityTimeout": "3", "ReceiveMessageWaitTimeSeconds": "1",
        "RedrivePolicy": json.dumps({"deadLetterTargetArn": dlq_arn, "maxReceiveCount": "3"})
    })["QueueUrl"]
    queue_arn = sqs.get_queue_attributes(QueueUrl=queue, AttributeNames=["QueueArn"])["Attributes"]["QueueArn"]
    topics = [
        (sns.create_topic(Name="ofisy-stock.fifo", Attributes={"FifoTopic": "true"})["TopicArn"], "LOW_STOCK"),
        (sns.create_topic(Name="ofisy-billing.fifo", Attributes={"FifoTopic": "true"})["TopicArn"], "QUOTE_GENERATED")
    ]
    sqs.set_queue_attributes(QueueUrl=queue, Attributes={"Policy": json.dumps({
        "Version": "2012-10-17", "Statement": [{
            "Effect": "Allow", "Principal": {"Service": "sns.amazonaws.com"}, "Action": "sqs:SendMessage",
            "Resource": queue_arn, "Condition": {"ArnEquals": {"aws:SourceArn": [arn for arn, _ in topics]}}
        }]
    })})
    for arn, event_type in topics:
        sns.subscribe(TopicArn=arn, Protocol="sqs", Endpoint=queue_arn, Attributes={
            "RawMessageDelivery": "true", "FilterPolicyScope": "MessageAttributes",
            "FilterPolicy": json.dumps({"type": [event_type]})
        })
    print("READY: DynamoDB, SNS FIFO producer topics, filtered SQS FIFO and DLQ", flush=True)


if __name__ == "__main__":
    main()