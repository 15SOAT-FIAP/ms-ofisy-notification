#!/bin/sh
set -eu

awslocal() {
  aws --endpoint-url "${LOCALSTACK_ENDPOINT:-http://localhost:4566}" "$@"
}

if ! awslocal dynamodb describe-table --table-name Notifications >/dev/null 2>&1; then
  awslocal dynamodb create-table --table-name Notifications --billing-mode PAY_PER_REQUEST \
    --attribute-definitions AttributeName=id,AttributeType=S AttributeName=type,AttributeType=S AttributeName=createdAt,AttributeType=S AttributeName=referenceId,AttributeType=S AttributeName=customerId,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --global-secondary-indexes '[
      {"IndexName":"TypeIndex","KeySchema":[{"AttributeName":"type","KeyType":"HASH"},{"AttributeName":"createdAt","KeyType":"RANGE"}],"Projection":{"ProjectionType":"ALL"}},
      {"IndexName":"ReferenceIdIndex","KeySchema":[{"AttributeName":"referenceId","KeyType":"HASH"}],"Projection":{"ProjectionType":"ALL"}},
      {"IndexName":"CustomerIdIndex","KeySchema":[{"AttributeName":"customerId","KeyType":"HASH"},{"AttributeName":"createdAt","KeyType":"RANGE"}],"Projection":{"ProjectionType":"ALL"}}
    ]' >/dev/null
fi

dlq=$(awslocal sqs create-queue --queue-name ofisy-notification-dlq.fifo --attributes FifoQueue=true --query QueueUrl --output text)
dlq_arn=$(awslocal sqs get-queue-attributes --queue-url "$dlq" --attribute-names QueueArn --query Attributes.QueueArn --output text)
queue=$(awslocal sqs create-queue --queue-name ofisy-notification.fifo \
  --attributes "{\"FifoQueue\":\"true\",\"VisibilityTimeout\":\"3\",\"ReceiveMessageWaitTimeSeconds\":\"1\",\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"$dlq_arn\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}" --query QueueUrl --output text)
queue_arn=$(awslocal sqs get-queue-attributes --queue-url "$queue" --attribute-names QueueArn --query Attributes.QueueArn --output text)
stock=$(awslocal sns create-topic --name ofisy-stock-events.fifo --attributes FifoTopic=true --query TopicArn --output text)
billing=$(awslocal sns create-topic --name ofisy-billing-events.fifo --attributes FifoTopic=true --query TopicArn --output text)
awslocal sqs set-queue-attributes --queue-url "$queue" --attributes \
  "{\"Policy\":\"{\\\"Version\\\":\\\"2012-10-17\\\",\\\"Statement\\\":[{\\\"Effect\\\":\\\"Allow\\\",\\\"Principal\\\":{\\\"Service\\\":\\\"sns.amazonaws.com\\\"},\\\"Action\\\":\\\"sqs:SendMessage\\\",\\\"Resource\\\":\\\"$queue_arn\\\",\\\"Condition\\\":{\\\"ArnEquals\\\":{\\\"aws:SourceArn\\\":[\\\"$stock\\\",\\\"$billing\\\"]}}}]}\"}"

subscribe() {
  awslocal sns subscribe --topic-arn "$1" --protocol sqs --notification-endpoint "$queue_arn" \
    --attributes "{\"RawMessageDelivery\":\"true\",\"FilterPolicyScope\":\"MessageBody\",\"FilterPolicy\":\"{\\\"type\\\":[$2]}\"}" >/dev/null
}

subscribe "$stock" '\"StockLow\"'
subscribe "$billing" '\"QuoteSentForApproval\",\"PaymentCheckoutCreated\",\"PaymentConfirmed\",\"PaymentFailed\"'
