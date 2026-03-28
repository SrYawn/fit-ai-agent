#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CHANGE_ID="${1:-}"
CHANGE_DIR="$ROOT_DIR/openspec/changes/$CHANGE_ID"
TEMPLATE_DIR="$ROOT_DIR/openspec/templates"

usage() {
    echo "Usage: bash scripts/create_change.sh <change-id>"
    echo ""
    echo "Format:"
    echo "  YYYY-MM-short-topic"
    echo ""
    echo "Example:"
    echo "  bash scripts/create_change.sh 2025-03-example-change"
}

if [[ -z "$CHANGE_ID" ]]; then
    usage
    exit 1
fi

if [[ ! "$CHANGE_ID" =~ ^[0-9]{4}-[0-9]{2}-[a-z0-9-]+$ ]]; then
    echo "Invalid change-id: $CHANGE_ID"
    echo "Expected format: YYYY-MM-short-topic"
    exit 1
fi

if [[ -e "$CHANGE_DIR" ]]; then
    echo "Change directory already exists: $CHANGE_DIR"
    exit 1
fi

mkdir -p "$CHANGE_DIR"

cp "$TEMPLATE_DIR/change-request.md" "$CHANGE_DIR/request.md"
cp "$TEMPLATE_DIR/design.md" "$CHANGE_DIR/design.md"
cp "$TEMPLATE_DIR/tasks.md" "$CHANGE_DIR/tasks.md"
cp "$TEMPLATE_DIR/implementation.md" "$CHANGE_DIR/implementation.md"
cp "$TEMPLATE_DIR/test-report.md" "$CHANGE_DIR/test-report.md"
cp "$TEMPLATE_DIR/decision-log.md" "$CHANGE_DIR/decision-log.md"

echo "Created OpenSpec change scaffold:"
echo "  $CHANGE_DIR"
