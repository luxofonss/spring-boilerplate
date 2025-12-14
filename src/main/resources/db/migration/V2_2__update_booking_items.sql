ALTER TABLE booking_items ADD COLUMN seat_group_id uuid;
ALTER TABLE booking_items ADD COLUMN quantity int;

ALTER TABLE booking_items
ADD CONSTRAINT check_seat_reference
CHECK (
    (seat_inventory_id IS NOT NULL AND seat_group_id IS NULL AND quantity IS NULL)
    OR
    (seat_inventory_id IS NULL AND seat_group_id IS NOT NULL AND quantity > 0)
);

CREATE INDEX ON booking_items (seat_group_id);