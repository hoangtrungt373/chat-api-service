#!/bin/bash
set -e

# Database initialization script for chat API service
# This script is executed when the PostgreSQL container starts
# The main database (chat_premier) is already created by POSTGRES_DB environment variable

echo "Initializing chat_premier database..."

# Grant privileges to the postgres user for the main database
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    GRANT ALL PRIVILEGES ON DATABASE $POSTGRES_DB TO $POSTGRES_USER;
EOSQL

# Note: All .sql files in /docker-entrypoint-initdb.d/ are automatically executed
# by PostgreSQL in alphabetical order (01-extensions.sql, 02-schema.sql, 03-user-table.sql)

echo "Database initialization completed successfully!"
