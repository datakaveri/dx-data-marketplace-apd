CREATE TABLE IF NOT EXISTS merchant_table (
    reference_id varchar NOT NULL,
    phone_numer numeric NOT NULL,
    email varchar NOT NULL,
    business_name varchar NOT NULL,
    customer_facing_business_name varchar NOT NULL,
    customer_business_name varchar NOT NULL,
    account_id varchar NOT NULL,
    provider_id uuid NOT NULL,
    CONSTRAINT merchant_pk PRIMARY KEY (reference_id),
    CONSTRAINT provider_user_fk FOREIGN KEY (provider_id) REFERENCES user_table (_id)
);