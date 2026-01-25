-- Author: TTG
-- Description: Create schema product
------------------------------------------------------------------------------------------------------------------------

-- Create product schema
CREATE SCHEMA IF NOT EXISTS product;

-- Grant privileges on the schema
GRANT ALL ON SCHEMA product TO postgres;
GRANT USAGE ON SCHEMA product TO postgres;

-- Set default privileges for future tables in the schema
ALTER DEFAULT PRIVILEGES IN SCHEMA product GRANT ALL ON TABLES TO postgres;
ALTER DEFAULT PRIVILEGES IN SCHEMA product GRANT ALL ON SEQUENCES TO postgres;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
