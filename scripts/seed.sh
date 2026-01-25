#!/bin/bash
# =============================================================================
# Concert Reservation Service - Test Data Seed Runner
# =============================================================================
# 사용법: ./scripts/seed.sh
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# .env 파일 로드
if [ -f "$PROJECT_DIR/.env" ]; then
    export $(grep -v '^#' "$PROJECT_DIR/.env" | xargs)
fi

# 기본값 설정
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-$MYSQL_ROOT_PASSWORD}"
MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DATABASE="${MYSQL_DATABASE:-concert}"

echo "🌱 Seeding test data..."
echo "   Host: $MYSQL_HOST:$MYSQL_PORT"
echo "   Database: $MYSQL_DATABASE"
echo ""

# Docker 컨테이너 내에서 실행하는 경우
if [ "$1" = "--docker" ]; then
    docker exec -i concert-mysql mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" < "$SCRIPT_DIR/seed_data.sql"
else
    mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" < "$SCRIPT_DIR/seed_data.sql"
fi

echo ""
echo "✅ Test data seeded successfully!"
