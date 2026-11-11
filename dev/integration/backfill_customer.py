"""Backfill ownership for legacy quote notifications already in DynamoDB."""
import argparse
import json
import os
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from uuid import UUID

import boto3


def lookup(base_url, path, token, field):
    request = Request(base_url.rstrip("/") + path, headers={"Authorization": "Bearer " + token})
    try:
        with urlopen(request, timeout=10) as response:
            value = json.load(response).get(field)
            return str(UUID(value)) if value else None
    except HTTPError as error:
        if error.code == 404:
            return None
        raise


def backfill(args):
    core_url = os.environ["CORE_BASE_URL"]
    billing_url = os.environ["BILLING_BASE_URL"]
    token = os.environ["CORE_SERVICE_TOKEN"]
    billing_token = os.environ.get("BILLING_SERVICE_TOKEN", token)
    ddb = boto3.client("dynamodb", endpoint_url=os.environ.get("DYNAMODB_ENDPOINT"))
    counts = dict(resolved=0, unresolved=0, updated=0, concurrent=0)
    pages = ddb.get_paginator("scan").paginate(
        TableName=args.table,
        FilterExpression="#type = :quote AND attribute_not_exists(customerId)",
        ExpressionAttributeNames={"#type": "type"},
        ExpressionAttributeValues={":quote": {"S": "QUOTE_GENERATED"}},
        ProjectionExpression="id, referenceId, serviceOrderId",
    )
    for page in pages:
        for item in page["Items"]:
            ref = str(UUID(item["referenceId"]["S"]))
            order_id = item.get("serviceOrderId", {}).get("S")
            if not order_id:
                order_id = lookup(billing_url, "/api/v1/quotes/" + ref, billing_token, "serviceOrderId")
            owner = lookup(core_url, "/api/v1/service-orders/" + str(UUID(order_id)) + "/customer",
                           token, "customerId") if order_id else None
            if not owner:
                counts["unresolved"] += 1
                continue
            counts["resolved"] += 1
            if not args.apply:
                continue
            try:
                ddb.update_item(
                    TableName=args.table, Key={"id": item["id"]},
                    UpdateExpression="SET customerId = :owner, serviceOrderId = :order",
                    ConditionExpression="attribute_not_exists(customerId) AND #type = :quote AND referenceId = :ref "
                                        "AND (attribute_not_exists(serviceOrderId) OR serviceOrderId = :order)",
                    ExpressionAttributeNames={"#type": "type"},
                    ExpressionAttributeValues={
                        ":owner": {"S": owner}, ":order": {"S": order_id},
                        ":quote": {"S": "QUOTE_GENERATED"}, ":ref": {"S": ref},
                    },
                )
                counts["updated"] += 1
            except ddb.exceptions.ConditionalCheckFailedException:
                counts["concurrent"] += 1
    return counts


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--table", default="Notifications")
    parser.add_argument("--apply", action="store_true", help="Perform the updates instead of a dry run")
    arguments = parser.parse_args()
    print(json.dumps({"mode": "apply" if arguments.apply else "dry-run", **backfill(arguments)}))
