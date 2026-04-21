#!/bin/bash
# PostgreSQL setup script for XTDB external source via pgoutput
# Requirements: PostgreSQL 15+, running on localhost:5432

set -e

# Configuration
PG_HOST="${PG_HOST:-localhost}"
PG_PORT="${PG_PORT:-5432}"
PG_USER="${PG_USER:-postgres}"
PG_DB="${PG_DB:-xtdb_dev}"
SLOT_NAME="${SLOT_NAME:-xtdb_slot}"
PUBLICATION_NAME="${PUBLICATION_NAME:-xtdb_publication}"

echo "🔧 Setting up PostgreSQL for XTDB external source..."
echo "Host: $PG_HOST, Port: $PG_PORT, DB: $PG_DB"

# Set PostgreSQL password
export PGPASSWORD="${PG_PASSWORD:-password}"

# Create sample table if not exists
echo "📋 Creating sample table..."
psql -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" -d "$PG_DB" <<EOF
-- Create sample documents table
CREATE TABLE IF NOT EXISTS public.documents (
    _id TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    amount DECIMAL(15, 2),
    supplier TEXT,
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Enable REPLICA IDENTITY (required for DELETE operations)
ALTER TABLE public.documents REPLICA IDENTITY FULL;

-- Create replication slot if not exists
DO \$\$
BEGIN
    PERFORM * FROM pg_create_logical_replication_slot('$SLOT_NAME', 'pgoutput');
EXCEPTION WHEN OTHERS THEN
    NULL;
END \$\$;

-- Create publication (drop if exists to recreate)
DROP PUBLICATION IF EXISTS $PUBLICATION_NAME;
CREATE PUBLICATION $PUBLICATION_NAME FOR TABLE public.documents;

-- Check slot status
SELECT slot_name, plugin, slot_type FROM pg_replication_slots WHERE slot_name = '$SLOT_NAME';

-- Check publication status
SELECT * FROM pg_publication WHERE pubname = '$PUBLICATION_NAME';
EOF

echo "✅ PostgreSQL setup complete!"
echo ""
echo "Replication slot: $SLOT_NAME"
echo "Publication: $PUBLICATION_NAME"
echo "Table: public.documents"
echo ""
echo "Ready for XTDB external source ingestion!"
