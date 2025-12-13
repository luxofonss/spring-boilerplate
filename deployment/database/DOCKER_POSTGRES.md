# ============================================
# PostgreSQL Primary-Replica Cluster - README
# ============================================

> Based on [Peter Eremeykin's Single-Primary PostgreSQL Replication](https://medium.com/@eremeykin/how-to-setup-single-primary-postgresql-replication-with-docker-compose-74c1d7f0656f)

## 📂 File Structure

```
deployment/database/
├── 00_init.sql           # Creates replication user and slot
├── docker-compose.yml    # Primary + Replica configuration
└── DOCKER_POSTGRES.md    # This documentation
```

## 📋 Quick Start

### 1. Start the PostgreSQL Cluster
```bash
docker-compose up -d postgres_primary postgres_replica
```

### 2. Check Status
```bash
# View running containers
docker-compose ps

# View Primary logs
docker-compose logs -f postgres_primary

# View Replica logs
docker-compose logs -f postgres_replica
```

### 3. Verify Replication
```bash
# Check replication slot status on Primary
psql postgres://ticketmaster:ticketmaster_secret@localhost:5431/ticketmaster_db -xc \
'SELECT * FROM pg_replication_slots;'

# Verify Replica is in recovery mode
psql postgres://ticketmaster:ticketmaster_secret@localhost:5433/ticketmaster_db -xc \
'SELECT pg_is_in_recovery();'

# Check replication status on Primary
docker exec -it postgres_primary psql -U ticketmaster -d ticketmaster_db -c "SELECT * FROM pg_stat_replication;"
```

## 🔌 Connection Details

| DataSource    | Host      | Port | Purpose                               |
|---------------|-----------|------|---------------------------------------|
| **Primary**   | localhost | 5431 | WRITE operations (INSERT, UPDATE, DELETE) |
| **Replica**   | localhost | 5433 | READ operations (SELECT)              |

### Application Credentials
- **Database:** `ticketmaster_db`
- **Username:** `ticketmaster`
- **Password:** `ticketmaster_secret`

### Replication User (Internal)
- **Username:** `replicator`
- **Password:** `replicator_password`

## 🏗️ How It Works

### Physical Replication
This setup uses **Physical Replication** which:
- Operates at binary level using WAL (Write-Ahead Log)
- Demands binary compatibility (same PostgreSQL version)
- Replica is read-only; all writes go to Primary
- Uses **replication slots** to track progress and prevent WAL deletion

### Initialization Flow
1. **Primary starts** → runs `00_init.sql` to create:
   - `replicator` user with REPLICATION privilege
   - `replication_slot` physical replication slot

2. **Replica starts** → executes `pg_basebackup`:
   - Connects to Primary using `replicator` credentials
   - Creates base backup with `-R` flag (auto-generates `standby.signal` and `postgresql.auto.conf`)
   - Starts PostgreSQL in standby/recovery mode

### Key Configuration
```yaml
# Primary settings (command args)
-c wal_level=replica           # Enable WAL for replication
-c hot_standby=on              # Allow read queries on standby
-c max_wal_senders=10          # Max concurrent replication connections
-c max_replication_slots=10    # Max replication slots
-c hot_standby_feedback=on     # Replica sends feedback to Primary
```

## 🚀 Spring Boot Integration

The application automatically routes queries based on `@Transactional` annotations:

```java
// Uses REPLICA (port 5433) - Read operations
@Transactional(readOnly = true)
public List<Event> findAllEvents() {
    return eventRepository.findAll();
}

// Uses PRIMARY (port 5431) - Write operations
@Transactional
public Event createEvent(Event event) {
    return eventRepository.save(event);
}
```

## 🔧 Useful Commands

```bash
# Stop cluster
docker-compose down

# Stop and remove volumes (DELETES ALL DATA!)
docker-compose down -v

# Restart a specific service
docker-compose restart postgres_primary

# View real-time logs
docker-compose logs -f

# Execute SQL on Primary
docker exec -it postgres_primary psql -U ticketmaster -d ticketmaster_db

# Execute SQL on Replica
docker exec -it postgres_replica psql -U ticketmaster -d ticketmaster_db
```

## ⚠️ Troubleshooting

### Replica not syncing
1. Check if Primary is running: `docker-compose ps`
2. Check Primary logs: `docker-compose logs postgres_primary`
3. Verify replication slot exists:
   ```bash
   docker exec -it postgres_primary psql -U ticketmaster -d ticketmaster_db -c "SELECT * FROM pg_replication_slots;"
   ```
   Expected output should show:
   - `active = t` (slot is active)
   - `restart_lsn` is not null

### Connection refused
1. Ensure ports are not in use:
   - Windows: `netstat -an | findstr "5431 5433"`
   - Linux/Mac: `lsof -i :5431 -i :5433`
2. Check Docker network: `docker network ls`

### Replication lag
```sql
-- On Primary: Check replication lag
SELECT 
    client_addr,
    state,
    sent_lsn,
    write_lsn,
    flush_lsn,
    replay_lsn
FROM pg_stat_replication;
```

### Fresh start (reset everything)
```bash
docker-compose down -v
docker-compose up -d postgres_primary postgres_replica
```

## 📝 Notes

- **Replication Delay**: Replica may have slight data delay compared to Primary (asynchronous replication)
- **Read-Only Replica**: Attempting to write on Replica will fail with `ERROR: cannot execute ... in read-only transaction`
- **pg_basebackup**: Runs on every Replica container restart (suitable for dev/test, consider persistent volumes for production)

## 🔗 References

- [Peter Eremeykin - How to Setup Single-Primary PostgreSQL Replication](https://medium.com/@eremeykin/how-to-setup-single-primary-postgresql-replication-with-docker-compose-74c1d7f0656f)
- [GitHub - eremeykin/pg-primary-replica](https://github.com/eremeykin/pg-primary-replica)
- [PostgreSQL Documentation - Streaming Replication](https://www.postgresql.org/docs/current/warm-standby.html#STREAMING-REPLICATION)
