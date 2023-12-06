CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

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
	provider_id uuid NOT NULL,
	resource_server_url varchar NOT NULL,
	accessPolicy varchar NOT NULL,
	created_at timestamp without time zone NOT NULL,
	modified_at timestamp without time zone NOT NULL,
	CONSTRAINT resource_pk PRIMARY KEY (_id),
	CONSTRAINT provider_id_fk FOREIGN KEY (provider_id) REFERENCES user_table(_id)
);


CREATE TABLE IF NOT EXISTS product
(
    product_id uuid UNIQUE NOT NULL,
    resource_id uuid NOT NULL,
    status status_type NOT NULL,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
    CONSTRAINT product_pk PRIMARY KEY (product_id)
);



CREATE TABLE IF NOT EXISTS policy
(
    _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
    resource_id uuid NOT NULL,
    constraints json NOT NULL,
    provider_id uuid NOT NULL,
    consumer_email_id varchar NOT NULL,
    expiry_at timestamp without time zone,
    expiry_amount varchar,
    status policy_status NOT NULL,
    product_id uuid NOT NULL,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
    CONSTRAINT resource_id_fk FOREIGN KEY(resource_id) REFERENCES resource_entity(_id),
    CONSTRAINT provider_id_fk FOREIGN KEY (provider_id) REFERENCES user_table (_id),
    CONSTRAINT product_id_fk FOREIGN KEY (product_id) REFERENCES product (product_id)
);



CREATE TABLE IF NOT EXISTS product_resource_relation
(
    product_id uuid REFERENCES product (product_id),
    resource_id uuid REFERENCES resource_entity (_id),
    _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
    CONSTRAINT _id_pkey PRIMARY KEY (_id)
);


CREATE TABLE IF NOT EXISTS product_variant
(
    _id uuid NOT NULL PRIMARY KEY,
    product_variant_name varchar NOT NULL,
    product_id uuid NOT NULL,
    provider_id uuid NOT NULL,
    resource_name varchar[] NOT NULL,
    resource_ids uuid[] NOT NULL,
    resource_capabilities varchar[][] NOT NULL,
    price varchar NOT NULL,
    validity numeric NOT NULL,
    status status_type NOT NULL,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
    CONSTRAINT product_id_fk FOREIGN KEY (product_id)
    REFERENCES product (product_id),
    CONSTRAINT provider_id_fk FOREIGN KEY (provider_id)
    REFERENCES user_table (_id)
);



CREATE TABLE IF NOT EXISTS purchase
(
    _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
    consumer_id uuid NOT NULL,
    product_id uuid NOT NULL,
    payment_status payment_status_type NOT NULL,
    payment_time timestamp without time zone NOT NULL,
    expiry numeric NOT NULL,
    product_variant json NOT NULL,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
    CONSTRAINT purchase_id_pk PRIMARY KEY (_id),
    CONSTRAINT consumer_id_fk FOREIGN KEY (consumer_id)
    REFERENCES user_table (_id),
    CONSTRAINT product_id_fk FOREIGN KEY (product_id)
    REFERENCES product (product_id)
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

-- purchase
CREATE TRIGGER update_purchase_created BEFORE INSERT ON purchase FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_purchase_modified BEFORE INSERT
OR UPDATE ON
   purchase FOR EACH ROW EXECUTE PROCEDURE update_modified ();



