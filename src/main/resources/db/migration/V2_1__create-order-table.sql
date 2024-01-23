CREATE TYPE currency AS ENUM (
    'INR'
);

CREATE TABLE IF NOT EXISTS order_table (
    order_id uuid NOT NULL,
    amount integer NOT NULL,
    currency currency NOT NULL,
    account_id varchar NOT NULL,
    notes json NOT NULL,
    CONSTRAINT order_id_pk PRIMARY KEY (order_id)
);