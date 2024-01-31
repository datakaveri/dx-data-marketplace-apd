CREATE TABLE IF NOT EXISTS payment_table (
    order_id varchar NOT NULL,
    payment_id varchar NOT NULL,
    payment_signature varchar NOT NULL,
    CONSTRAINT payment_pk PRIMARY KEY (payment_id)
);