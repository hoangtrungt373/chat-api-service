-- Schema Initialization
-- This script creates the product schema and sets up privileges

-- Create custom schema (equivalent to SQL Server's dbo)
CREATE SCHEMA IF NOT EXISTS product;

-- Grant privileges on the schema
GRANT ALL ON SCHEMA product TO postgres;
GRANT USAGE ON SCHEMA product TO postgres;

-- Set the search path to use the product schema
SET search_path TO product, public;

-- Set default privileges for future tables in the schema
ALTER DEFAULT PRIVILEGES IN SCHEMA product GRANT ALL ON TABLES TO postgres;

