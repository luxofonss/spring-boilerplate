**API:** `GET /api/booking/select-seat`

```mermaid
sequenceDiagram
    participant SRC as SlotReleasedConsumer
    participant RMQ as RabbitMQ 
    participant R as Redis
    participant WS as WebSocket

    Note over RMQ: Slot released event triggered
    RMQ-->>SRC: slot-released message
    SRC->>R: EVAL PromoteUserToBookingRoom
    R-->>SRC: {success:1, next_user_id, expire_time}
    SRC->>RMQ: Publish timeout message (delay 10 min)
    SRC->>WS: Notify user "Your turn"
```

```
local rank = redis.call('zrank', 'waiting_list:'..event_id, user_id)
ir rank then
    return {queued, rank + 1}
end
if redis.call('zscore', 'active_users:'..event_id, user_id) then
    local state = redis.call('hget','user_state:'..user_id..':'..event_id, 'state')
       
```