 bsanj## Functional Requirements
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


## detail flow
- receive booking request via api gateway.
- check for duplicate session (redis):
    * key: `session:user:{user_id}:event:{event_id}`
    * type: string
    * value: token
    * ttl: 1 day (based on use case)
    * purpose: prevents double-clicks/multiple active attempts.
    * if duplicate: return "already in queue" with existing token.
- calculate estimated queue position (redis):
    - perform: `incr total:ingestion_count:{event_id}`
    - purpose: provides instant, non-binding queue number for better ux. 
    - for more accuracy, optionally query zcard(`waiting_list:{event_id}`) + get(`counter:active_room:{event_id}`) atomically.
- log message to topic a: `initial_booking_requests` (kafka).
- return token with `status=queued` and queue number to the client.
- consumer triage reads messages from topic a.
- perform atomic slot check and increment (redis lua script):
    * key: `counter:active_room:{event_id}`
    * type: string (integer)
    * purpose: tracks the total number of users currently allowed in the 10-minute room (n limit).
    * lua: get count, if count < n then incr and return success, else return fail.
- if successful (incr done), push message to topic b: `active_room_topic`.
- if failed, store user in redis zset: `waiting_list:{event_id}`.
    * member: `{user_id}`
    * score: `timestamp` (ensures fifo fairness).
- commit offset for topic a.
- consumer booking reads message from topic b.
- set up delayed queue: add to redis zset `delayed_timeouts:{event_id}` with member `{user_id:request_id}` and score = current_timestamp + 600 (10 minutes).
- notify client via websocket: status=active, user can now select seat.
- separate poller consumer periodically checks zset: use zrangebyscore `delayed_timeouts:{event_id}` -inf current_timestamp to get expired tasks, process (send to topic c: `slot_released`), then zrem them atomically (e.g., via lua script).
- user selects seat via api: establish distributed lock (redis setnx) on the seat id:
    * key: `lock:seat:{seat_id}`
    * type: string
    * ttl: 600 seconds (10 minutes)
    * purpose: prevents simultaneous purchase of the same seat.
- process payment via payment gateway.
- upon success:
    - seat has number: perform atomic db update (`listingstatus = sold`). delete the seat lock key from redis.
    - seat doesn't have number but group, decrease quantity.
    - zpopmin waiting_list (atomic get + remove)
    - if user exists:
       - not decr counter (keep slot reserved)
       - push to topic B directly (bypass topic A)
       - notify user: status=active
    - else (no waiting user):
       - decr counter (release slot)
- upon payment failure: delete the seat lock key from redis (release early), send message to topic c: `slot_released`. also zrem from `delayed_timeouts:{event_id}`.
- if user abandons (no action in 10 min): poller triggers topic c.
- handle topic `slot_released`: perform redis atomic decr `counter:active_room:{event_id}` (lua script for decr if >0). also decr `total:ingestion_count:{event_id}`.
- consumer invitation reads message from topic c (or triggered by timeout).
- retrieve the longest waiting user (lowest score) from redis zset: `waiting_list:{event_id}` (use zpopmin for atomic remove).
- push the invited user's message back to topic a: `initial_booking_requests` (to re-check atomic slot).
- send websocket notification to the invited user: status=invited, wait for active.
- optimize seat selection with bloom filter
  - Tránh query DB cho seats đã sold
    - Cache seats available trong Redis Set
      - redis.sadd(available_seats:{event_id}, *seat_ids)
    
    - Khi user chọn ghế
      - if redis.sismember(available_seats:{event_id}, seat_id):
    - Try lock
      - if redis.set(lock:seat:{seat_id}, user_id, nx=True, ex=600):
      - redis.srem(available_seats:{event_id}, seat_id)
    - Process payment