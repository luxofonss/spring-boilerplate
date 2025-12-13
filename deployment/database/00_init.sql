-- ============================================
-- PostgreSQL Primary Initialization Script
-- ============================================
-- This script creates the replication user and slot
-- Required for primary-replica physical replication

-- Create replication user with encrypted password
CREATE USER replicator WITH REPLICATION ENCRYPTED PASSWORD 'replicator_password';

-- Create physical replication slot
-- The slot tracks replication progress and prevents WAL deletion before replica consumes it
SELECT pg_create_physical_replication_slot('replication_slot');
