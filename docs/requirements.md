## Functional Requirements
- Create an event
- View an event
- Search for events
- Book tickets

## Non-functional Requirements
- strong consistency for booking tickets & high availability for searching and viewing events
- read >> write
- scalability to handle surges from popular events
- handle payment 
- peak load test: 2.5m request in 10 first minutes (fifa wc 2022)
  - 2.500.000 / (10 * 60) ~ 4200 tps
  - 4200 tps for viewing event api
  - 4200 tps for booking api
- 500ms latency under normal load

## Apis
- Post: /events
- Get: /events/:id
- GET: /events/search  (search event)
- POST: /events/reserve
- POST: /purchases

## Database 
### events

| Column             |      Type | Description |
|--------------------|----------:|------------:|
| id                 |      uuid | primary key |
| name               |      text |             | 
| code               |      text |             |
| description        |      text |             | 
| start_date         | timestamp |             | 
| end_date           | timestamp |             | 
| start_booking_time | timestamp |             | 
| end_booking_time   | timestamp |             | 
| location           |      text |             | 
| additional_info    |     jsonb |             |


### seat_types
| Column              |    Type |                    Description |
|---------------------|--------:|-------------------------------:|
| id                  |    uuid |                    primary key |
| name                |    text |                                | 
| description         |    text |                                | 
| price               | numeric |                                | 
| event_id            |    uuid |          reference table event | 
| total_quantity      |    int4 |              quantity of seats | 
| is_reversed_seating | boolean | true if seat has number or key | 

### seat_inventory
| Column       |    Type |            Description |
|--------------|--------:|-----------------------:|
| id           |    uuid |            primary key |
| event_id     |    uuid |                        | 
| seat_type_id |    uuid |                        | 
| seat_num     | varchar |                        | 
| status       | varchar |  AVAILABLE, HELD, SOLD |
| version      |     int | for Optimistic Locking |

## bookings
| Column     |          Type |                               Description |
|------------|--------------:|------------------------------------------:|
| id         |          uuid |                               primary key |
| user_id    |          uuid |                                           | 
| event_id   |          uuid |                                           | 
| amount     | decimal(10,2) |                                           | 
| status     |  varchar(255) | PENDING_PAYMENT, PAID, CANCELLED, TIMEOUT |
| created_at |     timestamp |                                           |
| expires_at |     timestamp |                                           | 

## booking_items
| Column            |          Type |   Description |
|-------------------|--------------:|--------------:|
| id                |          uuid |   primary key |
| booking_id        |          uuid |               | 
| seat_inventory_id |          uuid |               | 

## System design
### Read data
- Master DB for: INSERT, UPDATE, DELETE
- Slave DB: replication of master, for GET
- Elasticsearch: Searching
- Use Debezium and Kafka for data union
- Redis for caching
- Delay queue for purchase expiration: Redis ZSET 
- Cron job Reconciliation to keep consistency
- Soft Lock Extension when user purchase
- Cache stampede prevention 