CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
alter SCHEMA ${flyway:defaultSchema} OWNER TO dmp_user;

CREATE TYPE policy_status AS ENUM
(
'ACTIVE',
'DELETED'
);

CREATE TYPE payment_status_type as ENUM
(
    'SUCCEEDED',
    'FAILED'
);

CREATE TYPE status_type AS ENUM
(
    'ACTIVE',
    'INACTIVE',
    'EXPIRED'
);

CREATE TYPE currency AS ENUM (
    'INR'
);

CREATE type linked_account_status_type AS ENUM
	(
		'CREATED',
		'ACTIVATED'
	);

CREATE TABLE IF NOT EXISTS user_table
(
 _id uuid NOT NULL,
  email_id varchar NOT NULL,
	first_name varchar NOT NULL,
	last_name varchar NOT NULL,
	created_at timestamp without time zone NOT NULL,
	modified_at timestamp without time zone NOT NULL,
	CONSTRAINT user_pk PRIMARY KEY (_id)

);


CREATE TABLE IF NOT EXISTS resource_entity
(
    _id uuid NOT NULL,
    resource_name varchar UNIQUE NOT NULL,
    provider_name varchar NOT NULL,
	provider_id uuid NOT NULL,
	resource_server varchar NOT NULL,
	accessPolicy varchar NOT NULL,
	created_at timestamp without time zone NOT NULL,
	modified_at timestamp without time zone NOT NULL,
	CONSTRAINT resource_pk PRIMARY KEY (_id),
	CONSTRAINT provider_id_fk FOREIGN KEY (provider_id) REFERENCES user_table(_id)
);


CREATE TABLE IF NOT EXISTS product
(
    product_id varchar UNIQUE NOT NULL,
    status status_type NOT NULL,
    provider_id UUID NOT NULL,
    provider_name varchar NOT NULL,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
    CONSTRAINT product_pk PRIMARY KEY (product_id),
    CONSTRAINT provider_id_fk FOREIGN KEY (provider_id)
    REFERENCES user_table(_id)
);

CREATE TABLE IF NOT EXISTS merchant_table
	(
		reference_id varchar(20) NOT NULL PRIMARY KEY,
		phone_number numeric NOT NULL,
		email varchar NOT NULL UNIQUE,
		legal_business_name varchar NOT NULL,
		customer_facing_business_name varchar NOT NULL,
		account_id varchar NOT NULL UNIQUE,
		provider_id UUID NOT NULL UNIQUE,
		status linked_account_status_type NOT NULL,
		rzp_account_product_id varchar NOT NULL UNIQUE,
		created_at timestamp without time zone NOT NULL,
		modified_at timestamp without time zone NOT NULL,
		CONSTRAINT provider_id_fk FOREIGN KEY (provider_id) REFERENCES user_table(_id)
	);

CREATE TABLE IF NOT EXISTS product_resource_relation
(
    product_id varchar REFERENCES product (product_id),
    resource_id uuid REFERENCES resource_entity (_id),
    _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
    CONSTRAINT _id_pkey PRIMARY KEY (_id)
);

CREATE TABLE IF NOT EXISTS product_variant
(
    _id uuid NOT NULL PRIMARY KEY,
    product_variant_name varchar NOT NULL,
    product_id varchar NOT NULL,
    provider_id uuid NOT NULL,
    resource_name varchar[] NOT NULL,
    resource_ids_and_capabilities jsonb NOT NULL,
    price numeric NOT NULL,
    validity numeric NOT NULL,
    status status_type NOT NULL,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
    CONSTRAINT product_id_fk FOREIGN KEY (product_id)
    REFERENCES product (product_id),
    CONSTRAINT provider_id_fk FOREIGN KEY (provider_id)
    REFERENCES user_table (_id)
);

CREATE TABLE IF NOT EXISTS order_table (
    order_id varchar NOT NULL,
    amount integer NOT NULL,
    currency currency NOT NULL,
    account_id varchar NOT NULL,
    notes json NOT NULL,
    created_at timestamp without time zone NOT NULL,
    CONSTRAINT order_id_pk PRIMARY KEY (order_id),
	CONSTRAINT account_id_fk FOREIGN KEY (account_id)
	REFERENCES merchant_table(account_id)

);


CREATE TABLE IF NOT EXISTS payment_table (
    order_id varchar NOT NULL,
    payment_id varchar NOT NULL,
    payment_signature varchar NOT NULL,
    created_at timestamp without time zone NOT NULL,
    CONSTRAINT payment_pk PRIMARY KEY (payment_id),
    CONSTRAINT order_id_fk FOREIGN KEY (order_id)
        REFERENCES order_table (order_id)
);



CREATE TABLE IF NOT EXISTS invoice
(
    _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
    consumer_id uuid NOT NULL,
    order_id varchar NOT NULL,
    product_variant_id uuid NOT NULL,
    payment_status payment_status_type NOT NULL,
    payment_time timestamp without time zone NOT NULL,
    expiry numeric NOT NULL,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
    CONSTRAINT invoice_id_pk PRIMARY KEY (_id),
    CONSTRAINT consumer_id_fk FOREIGN KEY (consumer_id)
    REFERENCES user_table (_id),
    CONSTRAINT invoice_product_variant_id_fkey FOREIGN KEY (product_variant_id)
    REFERENCES product_variant (_id),
    CONSTRAINT fk_invoice_order FOREIGN KEY (order_id)
    REFERENCES order_table (order_id)


);

CREATE TABLE IF NOT EXISTS policy
(
    _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
    resource_id uuid NOT NULL,
    invoice_id uuid NOT NULL,
    constraints json NOT NULL,
    provider_id uuid NOT NULL,
    consumer_email_id varchar NOT NULL,
    expiry_at timestamp without time zone NOT NULL,
    status policy_status NOT NULL,
    product_variant_id uuid NOT NULL,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
    CONSTRAINT resource_id_fk FOREIGN KEY(resource_id) REFERENCES resource_entity(_id),
    CONSTRAINT provider_id_fk FOREIGN KEY (provider_id) REFERENCES user_table (_id),
    CONSTRAINT policy_product_variant_id_fkey FOREIGN KEY (product_variant_id) REFERENCES product_variant (_id),
    CONSTRAINT policy_invoice_id_fkey FOREIGN KEY (invoice_id) REFERENCES invoice (_id)
);


-- modified_at column function
CREATE
OR REPLACE
   FUNCTION update_modified () RETURNS TRIGGER AS $$
BEGIN NEW.modified_at = now ();
RETURN NEW;
END;
$$ language 'plpgsql';

-- created_at column function
CREATE
OR REPLACE
   FUNCTION update_created () RETURNS TRIGGER AS $$
BEGIN NEW.created_at = now ();
RETURN NEW;
END;
$$ language 'plpgsql';


---
-- add owner
---
ALTER TABLE user_table OWNER TO dmp_user;
ALTER TABLE resource_entity OWNER TO dmp_user;
ALTER TABLE product OWNER TO dmp_user;
ALTER TABLE merchant_table OWNER TO dmp_user;
ALTER TABLE order_table OWNER TO dmp_user;
ALTER TABLE product_resource_relation OWNER TO dmp_user;
ALTER TABLE product_variant OWNER TO dmp_user;
ALTER TABLE invoice OWNER TO dmp_user;
ALTER TABLE policy OWNER TO dmp_user;

GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO dmp_user;


---
-- Triggers
---

-- user_table
CREATE trigger update_user_table_created before insert on user_table for each row EXECUTE procedure update_created();
CREATE trigger update_user_table_modified before insert or update on user_table for each row EXECUTE procedure update_modified ();

-- resource_entity
CREATE trigger update_resource_entity_created before insert on resource_entity for each row EXECUTE procedure update_created();
CREATE trigger update_resource_entity_modified before insert or update on resource_entity for each row EXECUTE procedure update_modified();

-- product
CREATE TRIGGER update_product_created BEFORE INSERT ON product FOR EACH ROW EXECUTE PROCEDURE update_created();
CREATE TRIGGER update_product_modified BEFORE INSERT OR UPDATE ON product FOR EACH ROW EXECUTE PROCEDURE update_modified();

-- policy
CREATE TRIGGER update_policy_created BEFORE INSERT ON policy FOR EACH ROW EXECUTE PROCEDURE update_created();
CREATE TRIGGER update_policy_modified BEFORE INSERT OR UPDATE ON policy FOR EACH ROW EXECUTE PROCEDURE update_modified();

-- product_variant
CREATE TRIGGER update_pv_created BEFORE INSERT ON product_variant FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_pv_modified BEFORE INSERT
OR UPDATE ON
   product_variant FOR EACH ROW EXECUTE PROCEDURE update_modified ();

-- invoice
CREATE TRIGGER update_invoice_created BEFORE INSERT ON invoice FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_invoice_modified BEFORE INSERT
OR UPDATE ON
   invoice FOR EACH ROW EXECUTE PROCEDURE update_modified ();

-- merchant_table
CREATE TRIGGER update_merchant_table_created BEFORE INSERT ON merchant_table FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_merchant_table_modified BEFORE INSERT
OR UPDATE ON
   merchant_table FOR EACH ROW EXECUTE PROCEDURE update_modified ();

-- order_table
CREATE TRIGGER update_order_table_created BEFORE INSERT ON order_table FOR EACH ROW EXECUTE PROCEDURE update_created ();


-- payment_table
CREATE TRIGGER update_payment_table_created BEFORE INSERT ON payment_table FOR EACH ROW EXECUTE PROCEDURE update_created ();


---
--   grant privileges
---

GRANT INSERT, SELECT, UPDATE, DELETE, REFERENCES, TRIGGER ON TABLE policy TO dmp_user;
GRANT INSERT, SELECT, UPDATE, DELETE, REFERENCES, TRIGGER ON TABLE product TO dmp_user;
GRANT INSERT, SELECT, UPDATE, DELETE, REFERENCES, TRIGGER ON TABLE resource_entity TO dmp_user;
GRANT INSERT, SELECT, UPDATE, DELETE, REFERENCES, TRIGGER ON TABLE user_table TO dmp_user;
GRANT INSERT, SELECT, UPDATE, DELETE, REFERENCES, TRIGGER ON TABLE invoice TO dmp_user;
GRANT INSERT, SELECT, UPDATE, DELETE, REFERENCES, TRIGGER ON TABLE product_resource_relation TO dmp_user;
GRANT INSERT, SELECT, UPDATE, DELETE, REFERENCES, TRIGGER ON TABLE product_variant TO dmp_user;
GRANT INSERT, SELECT, UPDATE, DELETE, REFERENCES, TRIGGER ON TABLE merchant_table TO dmp_user;
GRANT INSERT, SELECT, UPDATE, DELETE, REFERENCES, TRIGGER ON TABLE order_table TO dmp_user;

---
--   constraint
---
ALTER TABLE merchant_table ADD CONSTRAINT check_min_length CHECK (length(reference_id) = 20);