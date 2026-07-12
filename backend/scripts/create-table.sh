#!/usr/bin/env bash
# DynamoDB Local にテーブル + GSI を作成する(冪等: 既存ならスキップ)
# 設計: .claude/01_development_docs/02_database_design.md
set -euo pipefail

ENDPOINT="${DYNAMODB_ENDPOINT:-http://localhost:8000}"
TABLE_NAME="${TABLE_NAME:-swiss-stage}"
REGION="ap-northeast-1"

# DynamoDB Local はダミー認証情報でよいが、-sharedDb でも固定値に統一しておく
export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-local}"
export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-local}"

if aws dynamodb describe-table --table-name "$TABLE_NAME" \
    --endpoint-url "$ENDPOINT" --region "$REGION" >/dev/null 2>&1; then
  echo "Table '$TABLE_NAME' already exists. Skipping."
  exit 0
fi

aws dynamodb create-table \
  --table-name "$TABLE_NAME" \
  --endpoint-url "$ENDPOINT" \
  --region "$REGION" \
  --billing-mode PAY_PER_REQUEST \
  --attribute-definitions \
    AttributeName=PK,AttributeType=S \
    AttributeName=SK,AttributeType=S \
    AttributeName=GSI1PK,AttributeType=S \
    AttributeName=GSI1SK,AttributeType=S \
    AttributeName=GSI2PK,AttributeType=S \
  --key-schema \
    AttributeName=PK,KeyType=HASH \
    AttributeName=SK,KeyType=RANGE \
  --global-secondary-indexes '[
    {
      "IndexName": "GSI1",
      "KeySchema": [
        {"AttributeName": "GSI1PK", "KeyType": "HASH"},
        {"AttributeName": "GSI1SK", "KeyType": "RANGE"}
      ],
      "Projection": {"ProjectionType": "ALL"}
    },
    {
      "IndexName": "GSI2",
      "KeySchema": [
        {"AttributeName": "GSI2PK", "KeyType": "HASH"}
      ],
      "Projection": {"ProjectionType": "ALL"}
    }
  ]' >/dev/null

echo "Table '$TABLE_NAME' created at $ENDPOINT."
