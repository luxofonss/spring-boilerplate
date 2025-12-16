CREATE TABLE booking_sessions (
    id bigserial PRIMARY KEY,
    token varchar(255) NOT NULL,
    user_id bigint,
    event_id varchar(255),
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP
);
