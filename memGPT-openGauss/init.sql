-- Title: Init Letta Database for OpenGauss

-- Set up our schema and extensions in our new database.
\c letta

-- Ensure the gaussdb user can login (password already set during user creation)
ALTER USER gaussdb WITH LOGIN;

-- Grant necessary privileges to gaussdb user in public schema
GRANT CREATE ON SCHEMA public TO gaussdb;
GRANT USAGE ON SCHEMA public TO gaussdb;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO gaussdb;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO gaussdb;

-- Also create letta schema and grant permissions
CREATE SCHEMA IF NOT EXISTS letta;
GRANT ALL ON SCHEMA letta TO gaussdb;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA letta TO gaussdb;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA letta TO gaussdb;

-- Set default search path
ALTER DATABASE letta SET search_path TO public, letta;

-- OpenGauss has built-in vector support, no extension needed
-- Just create a comment to indicate vector functionality is available
COMMENT ON SCHEMA letta IS 'Letta schema with OpenGauss vector support';
