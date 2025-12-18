# TICKETMASTER SEQUENCE DIAGRAMS

## 1. User Request Booking

```mermaid
sequenceDiagram
participant U as User
participant TM as TicketMaster
participant R as Redis
participant K as Kafka

    U->>TM: POST /api/booking/request
    TM->>R: EVAL Lua Script (check duplicate + add to waiting_list)
    R-->>TM: {success: 1, sequence: 1234}
    TM->>K: Publish booking_request (async)
    TM-->>U: {success: true, position: 1234}
```

*Kafka Messages:*

Topic: booking_request
```json
{
"user_id": "user_123",
"event_id": "event_456",
"sequence": 1234,
"timestamp": 1234567890
}
```

*Redis Lua Scripts:*
``` lua
-- Enqueue to waiting_list
local event_id = ARGV[1]
local user_id = ARGV[2]

-- Check duplicate
local score = redis.call('zscore', 'waiting_list:'..event_id, user_id)
if score then return {0, score} end

-- Add to waiting list
local sequence = redis.call('incr', 'queue_sequence:'..event_id)
redis.call('zadd', 'waiting_list:'..event_id, sequence, user_id)

return {1, sequence}
```
---

## 2. Get Queue Position

```mermaid
sequenceDiagram
participant U as User
participant TM as TicketMaster
participant R as Redis

    U->>TM: GET /api/booking/position
    TM->>R: EVAL Lua Script (check waiting_list + active_users)
    R-->>TM: {status, position, state, expire_time}
    TM-->>U: {status: 'queued', position: 100}
```
*Redis Lua Scripts:*
``` lua
-- Get Queue Position
local user_id = ARGV[1]
local event_id = ARGV[2]

-- Check waiting_list
local score = redis.call('zscore', 'waiting_list:'..event_id, user_id)
if score then
local rank = redis.call('zrank', 'waiting_list:'..event_id, user_id)
return {'queued', rank + 1, score}
end

-- Check active_users
if redis.call('zscore', 'active_users:'..event_id, user_id) then
local state = redis.call('hget', 'user_state:'..user_id..':'..event_id, 'state')
local seat_id = redis.call('hget', 'user_state:'..user_id..':'..event_id, 'seat_id')
local expire_time = redis.call('hget', 'user_state:'..user_id..':'..event_id, 'expire_time')
return {'active', state, seat_id, expire_time}
end

return {'not_found', nil, nil}
```
---

## 3. Auto Promote User (Background Process)

```mermaid
sequenceDiagram
participant SRC as SlotReleasedConsumer
participant RMQ as RabbitMQ
participant R as Redis
participant WS as WebSocket

    Note over RMQ: Slot released event triggered
    RMQ->>SRC: slot-released message
    SRC->>R: EVAL PromoteUserToBookingRoom
    R-->>SRC: {success: 1, next_user_id, expire_time}
    SRC->>RMQ: Publish timeout message (delay 10min)
    SRC->>WS: Notify user "Your turn!"
```
*RabbitMQ Messages:*

- Exchange: slot-released-exchange 
- Queue: slot-released-queue 
- Routing Key: slot-released
```json
{
  "event_id": "event_456",
  "reason": "timeout|payment_success|payment_failure|cancellation",
  "timestamp": 1234567890
}
```
- Exchange: timeout-exchange (delayed)
- Queue: timeout-queue
- Routing Key: timeout
- Delay: 600000ms (10 minutes)
```json
{
  "user_id": "user_123",
  "event_id": "event_456",
  "expire_time": 1234567890,
  "scheduled_at": 1234567890
}
```
**Redis Lua Scripts:**

```lua
-- PromoteUserToBookingRoom
local event_id = ARGV[1]
local current_time = tonumber(ARGV[2])
local timeout_seconds = 600

-- Check capacity
local limit = 1000
local count = redis.call('zcard', 'active_users:'..event_id)
if count >= limit then
    return {0, 'room_full'}
end

-- Pop next user from waiting_list
local next = redis.call('zpopmin', 'waiting_list:'..event_id, 1)
if not next[1] then
    return {0, 'no_waiting'}
end

local next_user_id = next[1]

-- Add to active_users
local expire_time = current_time + timeout_seconds
redis.call('zadd', 'active_users:'..event_id, expire_time, next_user_id)

-- Create user_state
redis.call('hset', 'user_state:'..next_user_id..':'..event_id,
    'state', 'active',
    'seat_id', '',
    'expire_time', expire_time)
redis.call('expire', 'user_state:'..next_user_id..':'..event_id, timeout_seconds + 100)

return {1, next_user_id, expire_time}
```
---

## 4. Select Seat

```mermaid
sequenceDiagram
    participant U as User
    participant TM as TicketMaster
    participant R as Redis
    
    U->>TM: POST /api/booking/select-seat
    TM->>R: EVAL Lua Script (verify state + lock seat)
    alt Seat Available
        R-->>TM: {success: 1}
        TM-->>U: {success: true, seat_id: 'A12'}
    else Seat Taken
        R-->>TM: {success: 0, error: 'seat_unavailable'}
        TM-->>U: {success: false, error: 'seat_unavailable'}
    end
```
*Redis Lua Scripts:*
```lua
-- Lock Seat
local user_id = ARGV[1]
local event_id = ARGV[2]
local seat_id = ARGV[3]

-- Verify user is active
local state = redis.call('hget', 'user_state:'..user_id..':'..event_id, 'state')
if state ~= 'active' then
    return {0, 'invalid_state'}
end

-- Lock seat (atomic)
if redis.call('set', 'lock:seat:'..seat_id, user_id, 'nx', 'ex', 600) then
    redis.call('hset', 'user_state:'..user_id..':'..event_id,
        'state', 'seat_selected',
        'seat_id', seat_id)
    return {1, 'success'}
end

return {0, 'seat_unavailable'}
```
---

## 5. Complete Payment Flow (Happy Path)

```mermaid
sequenceDiagram
    participant U as User
    participant TM as TicketMaster
    participant R as Redis
    participant PG as Payment Gateway
    participant K as Kafka
    participant RMQ as RabbitMQ
    participant SRC as SlotReleasedConsumer
    
    U->>TM: POST /api/booking/payment/initiate
    TM->>R: EVAL Lua (update state to payment_pending)
    R-->>TM: {success: 1}
    TM->>PG: Create payment
    PG-->>TM: payment_url
    TM->>K: Publish payment_initiated
    TM-->>U: {payment_url}
    
    Note over U,PG: User completes payment
    
    PG->>TM: POST /api/booking/payment/success (webhook)
    TM->>R: EVAL Lua (release seat + complete booking)
    R-->>TM: {success: 1}
    TM->>RMQ: Publish slot-released (immediate)
    TM->>K: Publish booking_completed
    TM-->>PG: 200 OK
    
    RMQ->>SRC: slot-released message
    SRC->>R: EVAL PromoteUserToBookingRoom
    Note over SRC: Promote next user from queue
```
**Kafka Messages:**

- Topic: payment_initiated
```json
{
  "user_id": "user_123",
  "event_id": "event_456",
  "payment_id": "pay_789",
  "seat_id": "A12",
  "timestamp": 1234567890
}
```
- Topic: booking_completed

```json
{
  "user_id": "user_123",
  "event_id": "event_456",
  "seat_id": "A12",
  "payment_id": "pay_789",
  "timestamp": 1234567890
}
```
*RabbitMQ Messages:*

- Exchange: slot-released-exchange
- Routing Key: slot-released
- Delay: NO (immediate)
```json
{
  "event_id": "event_456",
  "reason": "payment_success",
  "timestamp": 1234567890
}
```

*Redis Lua Scripts:*
```lua
-- Initiate Payment
local user_id = ARGV[1]
local event_id = ARGV[2]
local payment_id = ARGV[3]

local state = redis.call('hget', 'user_state:'..user_id..':'..event_id, 'state')
if state ~= 'seat_selected' then
    return {0, 'invalid_state'}
end

-- CHECK existing payment_id
local existing_payment = redis.call('hget', 'user_state:'..user_id..':'..event_id,'payment_id')
if existing_payment and existing_payment ~= '' then
    return {0, 'payment_existed', existing_payment}
end
redis.call('hset', 'user_state:'..user_id..':'..event_id,
    'state', 'payment_pending',
    'payment_id', payment_id)

return {1, 'success'}
```

```lua
-- Payment Success
local event_id = ARGV[1]
local user_id = ARGV[2]
local seat_id = ARGV[3]
local payment_id = ARGV[4]

redis.call('del', 'lock:seat:'..seat_id)
redis.call('zrem', 'active_users:'..event_id, user_id)
redis.call('hset', 'user_state:'..user_id..':'..event_id,
    'state', 'completed',
    'payment_id', payment_id)
redis.call('expire', 'user_state:'..user_id..':'..event_id, 3600)

return {1, 'success'}
```
```lua
-- PromoteUserToBookingRoom (called by SlotReleasedConsumer)
local event_id = ARGV[1]
local current_time = tonumber(ARGV[2])
local timeout_seconds = 600

local limit = 1000
local count = redis.call('zcard', 'active_users:'..event_id)
if count >= limit then
    return {0, 'room_full'}
end

local next = redis.call('zpopmin', 'waiting_list:'..event_id, 1)
if not next[1] then
    return {0, 'no_waiting'}
end

local next_user_id = next[1]
local expire_time = current_time + timeout_seconds
redis.call('zadd', 'active_users:'..event_id, expire_time, next_user_id)
redis.call('hset', 'user_state:'..next_user_id..':'..event_id,
    'state', 'active',
    'seat_id', '',
    'expire_time', expire_time)
redis.call('expire', 'user_state:'..next_user_id..':'..event_id, timeout_seconds + 100)

return {1, next_user_id, expire_time}
```
---

## 6. Payment Failure Flow

```mermaid
sequenceDiagram
    participant PG as Payment Gateway
    participant TM as TicketMaster
    participant R as Redis
    participant RMQ as RabbitMQ
    participant K as Kafka
    participant SRC as SlotReleasedConsumer
    
    PG->>TM: POST /api/booking/payment/failure (webhook)
    TM->>R: EVAL CleanupUserSession
    R-->>TM: {success: 1, seat_id}
    TM->>RMQ: Publish slot-released
    TM->>K: Publish payment_failed
    TM-->>PG: 200 OK
    
    RMQ->>SRC: slot-released message
    SRC->>R: EVAL PromoteUserToBookingRoom
    Note over SRC: Promote next user
```
*Kafka Messages:*

- Topic: payment_failed

```json
{
  "user_id": "user_123",
  "event_id": "event_456",
  "payment_id": "pay_789",
  "reason": "insufficient_funds|timeout|cancelled",
  "timestamp": 1234567890
}
```
*RabbitMQ Messages:*

- Exchange: slot-released-exchange
- Routing Key: slot-released

```json
{
  "event_id": "event_456",
  "reason": "payment_failure",
  "timestamp": 1234567890
}
```
*Redis Lua Scripts:*
-- CleanupUserSession

```lua
local event_id = ARGV[1]
local user_id = ARGV[2]

local seat_id = redis.call('hget', 'user_state:'..user_id..':'..event_id, 'seat_id')

if seat_id and seat_id ~= '' then
    redis.call('del', 'lock:seat:'..seat_id)
end

redis.call('zrem', 'active_users:'..event_id, user_id)
redis.call('del', 'user_state:'..user_id..':'..event_id)

return {1, seat_id}
```
---

## 7. Timeout Handler Flow

```mermaid
sequenceDiagram
    participant RMQ as RabbitMQ Delayed Queue
    participant TC as TimeoutConsumer
    participant R as Redis
    participant SRC as SlotReleasedConsumer
    
    Note over RMQ: 10 minutes elapsed
    RMQ->>TC: timeout message {user_id, event_id}
    TC->>R: EVAL Lua (check state)
    
    alt Already Completed
        R-->>TC: {success: 0, 'already_completed'}
        Note over TC: Skip processing
    else Timeout
        R-->>TC: {success: 1, seat_id}
        TC->>RMQ: Publish slot-released
        
        RMQ->>SRC: slot-released message
        SRC->>R: EVAL PromoteUserToBookingRoom
        Note over SRC: Promote next user
    end
```
*RabbitMQ Messages:*

- Exchange: timeout-exchange (delayed)
- Queue: timeout-queue
- Routing Key: timeout
- Received after 10 minutes delay
```json
{
  "user_id": "user_123",
  "event_id": "event_456",
  "expire_time": 1234567890,
  "scheduled_at": 1234567890
}
```
- Exchange: slot-released-exchange
- Routing Key: slot-released
- Published by TimeoutConsumer
```json
{
  "event_id": "event_456",
  "reason": "timeout",
  "timestamp": 1234567890
}
```
*Redis Lua Scripts:*

```lua
-- Handle Timeout
local event_id = ARGV[1]
local user_id = ARGV[2]

local state = redis.call('hget', 'user_state:'..user_id..':'..event_id, 'state')

if state == 'completed' or not state then
    redis.call('zrem', 'active_users:'..event_id, user_id)
    return {0, 'already_completed'}
end

local seat_id = redis.call('hget', 'user_state:'..user_id..':'..event_id, 'seat_id')

if seat_id and seat_id ~= '' then
    redis.call('del', 'lock:seat:'..seat_id)
end

redis.call('zrem', 'active_users:'..event_id, user_id)
redis.call('del', 'user_state:'..user_id..':'..event_id)

return {1, 'timeout_processed', seat_id}
```
---

## 8. User Cancellation Flow

```mermaid
sequenceDiagram
    participant U as User
    participant TM as TicketMaster
    participant R as Redis
    participant RMQ as RabbitMQ
    participant SRC as SlotReleasedConsumer
    
    U->>TM: POST /api/booking/cancel
    TM->>R: EVAL Lua Script
    
    alt From Waiting List
        R-->>TM: {success: 1, 'cancelled_from_queue'}
        TM-->>U: {success: true, from: 'queue'}
    else From Active Users
        R-->>TM: {success: 1, 'cancelled_from_active', seat_id}
        TM->>RMQ: Publish slot-released
        TM-->>U: {success: true, from: 'active'}
        
        RMQ->>SRC: slot-released message
        SRC->>R: EVAL PromoteUserToBookingRoom
    else Payment Pending
        R-->>TM: {success: 0, 'cannot_cancel_during_payment'}
        TM-->>U: {success: false, error: 'cannot_cancel'}
    end
```
*RabbitMQ Messages:*

- Exchange: slot-released-exchange
- Routing Key: slot-released
- Only published if cancelled from active_users
```json
{
  "event_id": "event_456",
  "reason": "cancellation",
  "timestamp": 1234567890
}
```
*Redis Lua Scripts:*

```lua
-- Cancel Booking
local event_id = ARGV[1]
local user_id = ARGV[2]

if redis.call('zscore', 'waiting_list:'..event_id, user_id) then
    redis.call('zrem', 'waiting_list:'..event_id, user_id)
    return {1, 'cancelled_from_queue', nil}
end

if redis.call('zscore', 'active_users:'..event_id, user_id) then
    local state = redis.call('hget', 'user_state:'..user_id..':'..event_id, 'state')
    
    if state == 'payment_pending' then
        return {0, 'cannot_cancel_during_payment', nil}
    end
    
    local seat_id = redis.call('hget', 'user_state:'..user_id..':'..event_id, 'seat_id')
    if seat_id and seat_id ~= '' then
        redis.call('del', 'lock:seat:'..seat_id)
    end
    
    redis.call('zrem', 'active_users:'..event_id, user_id)
    redis.call('del', 'user_state:'..user_id..':'..event_id)
    
    return {1, 'cancelled_from_active', seat_id}
end

return {0, 'not_found', nil}
```
---

## 9. Complete User Journey (End-to-End)

```mermaid
sequenceDiagram
    participant U as User
    participant TM as TicketMaster
    participant R as Redis
    participant RMQ as RabbitMQ
    participant SRC as SlotReleasedConsumer
    participant PG as Payment Gateway
    participant K as Kafka
     Note over U: Step 1: Join Queue
    U->>TM: POST /api/booking/request
    TM->>R: Add to waiting_list
    TM->>K: Publish booking_request
    TM-->>U: position: 500
    
    Note over U: Wait in queue...
    
    Note over SRC: Step 2: Auto Promote
    SRC->>R: EVAL PromoteUserToBookingRoom
    SRC->>RMQ: Schedule timeout (10min)
    SRC-->>U: WebSocket: "Your turn!"
    
    Note over U: Step 3: Select Seat
    U->>TM: POST /api/booking/select-seat
    TM->>R: Lock seat A12
    TM-->>U: success
    
    Note over U: Step 4: Payment
    U->>TM: POST /api/booking/payment/initiate
    TM->>R: Update state to payment_pending
    TM->>PG: Create payment
    PG-->>U: Redirect to payment page
    
    U->>PG: Complete payment
    PG->>TM: Webhook: payment success
    
    Note over TM: Step 5: Complete
    TM->>R: Complete booking + release seat
    TM->>RMQ: Publish slot-released
    TM->>K: Publish booking_completed
    TM-->>U: Confirmation email
    
    Note over SRC: Step 6: Promote Next
    RMQ->>SRC: slot-released
    SRC->>R: Promote next user from queue
```
**Redis Lua Scripts Used:**

```lua
-- Step 1: Enqueue (see diagram #1)
-- Step 2: PromoteUserToBookingRoom (see diagram #3)
-- Step 3: Lock Seat (see diagram #4)
-- Step 4: Initiate Payment (see diagram #5)
-- Step 5: Payment Success (see diagram #5)
-- Step 6: PromoteUserToBookingRoom (see diagram #3)
```
---

## 10. Race Condition Prevention

```mermaid
sequenceDiagram
    participant U1 as User 1
    participant U2 as User 2
    participant TM as TicketMaster
    participant R as Redis (Lua Atomic)
    
    Note over U1,U2: Both try to book seat A12
    
    par Concurrent Requests
        U1->>TM: Select seat A12
        U2->>TM: Select seat A12
    end
    
    TM->>R: EVAL Lua (SET NX seat:A12)
    Note over R: Atomic operation
    
    alt User 1 wins
        R-->>TM: OK (User 1)
        TM-->>U1: Success
        R-->>TM: FAIL (User 2)
        TM-->>U2: Seat unavailable
    end
```
*Redis Lua Scripts:*

```lua
-- Lock Seat (Atomic)
local user_id = ARGV[1]
local event_id = ARGV[2]
local seat_id = ARGV[3]

local state = redis.call('hget', 'user_state:'..user_id..':'..event_id, 'state')
if state ~= 'active' then
    return {0, 'invalid_state'}
end

-- Atomic SET NX operation prevents race condition
if redis.call('set', 'lock:seat:'..seat_id, user_id, 'nx', 'ex', 600) then
    redis.call('hset', 'user_state:'..user_id..':'..event_id,
        'state', 'seat_selected',
        'seat_id', seat_id)
    return {1, 'success'}
end

return {0, 'seat_unavailable'}
```
---

## Key Design Principles

1. *Atomicity*: All Redis operations use Lua scripts (atomic)
2. *No Polling*: RabbitMQ delayed messages instead of scheduled jobs
3. *Event-Driven*: Slot released → Auto promote next user
4. *Idempotency*: Timeout handler checks state before cleanup
5. *Race Condition Free*: Redis NX operations + Lua scripts'

## TODO
- Payment success -> handle delete redis data of user
- Add api /api/seats/available?event_id=xxx
- 