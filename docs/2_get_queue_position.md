**API:** `GET /api/booking/position?user_id=xxx&event_id=yyy`

```mermaid
sequenceDiagram
    participant U as User
    participant TM as TicketMaster
    participant R as Redis

    U->>TM: request
    TM->>R: execute lua script
    R-->>TM: return
    TM-->>U: return

```

```
local rank = redis.call('zrank', 'waiting_list:'..event_id, user_id)
ir rank then
    return {queued, rank + 1}
end
if redis.call('zscore', 'active_users:'..event_id, user_id) then
    local state = redis.call('hget','user_state:'..user_id..':'..event_id, 'state')
       
```