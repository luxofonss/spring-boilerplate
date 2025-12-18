**API:** `GET /api/booking/select-seat`

```mermaid
sequenceDiagram
    participant U as User
    participant TM as TicketMaster 
    participant R as Redis

    U->>TM: request
    TM->>R: EVAL Lua Script (verify state + lock seat)
    alt Seat Available
        R -->> TM: {success: 1}
        TM -->> U: {success: true, seat_id: 'A12'}
    else Seat Taken
        TM -->>U: {success: false, error: 'seat_unavailable'}
    end
```

```
local rank = redis.call('zrank', 'waiting_list:'..event_id, user_id)
ir rank then
    return {queued, rank + 1}
end
if redis.call('zscore', 'active_users:'..event_id, user_id) then
    local state = redis.call('hget','user_state:'..user_id..':'..event_id, 'state')
       
```