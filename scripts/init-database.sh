#!/bin/bash
set -e

# =============================================================================
# Database Initialization Script
# =============================================================================
# This script runs when the PostgreSQL Docker container starts for the first time.
# 
# IMPORTANT: This is for Docker/infrastructure setup ONLY.
# - Schema and table creation is handled by Flyway (in chat-api-common module)
# - Flyway migrations are in: chat-api-common/src/main/resources/db/migration/
#
# Execution Order:
#   1. Docker creates database from POSTGRES_DB env variable
#   2. This script runs (grants privileges)
#   3. Spring Boot app starts
#   4. Flyway runs migrations (creates schema, tables, etc.)
# =============================================================================

echo "============================================="
echo "Initializing PostgreSQL for Chat API Service"
echo "============================================="

# Grant privileges to the postgres user for the main database
# Note: Database name must be quoted if it contains hyphens or special characters
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Grant full privileges on the database
    GRANT ALL PRIVILEGES ON DATABASE "$POSTGRES_DB" TO $POSTGRES_USER;
EOSQL

echo "============================================="
echo "PostgreSQL initialization completed!"
echo "Flyway will handle schema/table creation when the app starts."
echo "============================================="
