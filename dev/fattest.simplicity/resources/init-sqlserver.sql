-- Create database
CREATE DATABASE TEST;

-- Enable XA connections
EXEC sp_sqljdbc_xa_install;

-- Allow snapshot isolation
ALTER DATABASE TEST SET ALLOW_SNAPSHOT_ISOLATION ON