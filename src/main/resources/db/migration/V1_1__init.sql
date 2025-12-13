CREATE TABLE events (
    id uuid not null primary key default gen_random_uuid(),
    name text not null,
    code varchar(25) unique not null,
    description text,
    start_time timestamp not null,
    end_time timestamp,
    start_booking_time timestamp not null,
    end_booking_time timestamp,
    location text,
    additional_info jsonb,
    status varchar(50),
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);
CREATE INDEX ON events (code);

CREATE TABLE seat_groups (
    id uuid not null primary key default gen_random_uuid(),
    name text not null,
    description text,
    price numeric(19, 4) not null,
    event_id uuid not null, -- refer to table "events"
    total_quantity int4 not null,
    is_reserved_seating boolean default false,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

CREATE INDEX ON seat_groups (event_id);

CREATE TABLE seat_inventories (
    id uuid not null primary key default gen_random_uuid(),
    event_id uuid not null,
    seat_type_id uuid not null,
    seat_num varchar,
    status varchar(24),
    version int4 default 0,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);
ALTER TABLE seat_inventories ADD CONSTRAINT seat_inventor_unique unique(event_id, seat_type_id, seat_num);
CREATE INDEX ON seat_inventories (event_id, seat_type_id, seat_num);

CREATE TABLE bookings (
    id uuid not null primary key default gen_random_uuid(),
    user_id uuid not null,
    event_id uuid not null,
    amount numeric(19, 4) not null,
    status varchar(50) not null,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);
CREATE INDEX ON bookings (event_id, user_id);

CREATE TABLE booking_items (
    id uuid not null primary key default gen_random_uuid(),
    booking_id uuid not null,
    seat_inventory_id uuid not null,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);
ALTER TABLE booking_items ADD CONSTRAINT uq_bi_seat_inventory_id UNIQUE(seat_inventory_id);
CREATE INDEX ON booking_items (booking_id);