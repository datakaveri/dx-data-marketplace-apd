CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE status_type AS ENUM
(
    'ACTIVE',
    'INACTIVE',
    'EXPIRED'
);

CREATE TYPE payment_status as ENUM
(
    'SUCCEEDED',
    'FAILED'
);

CREATE TABLE IF NOT EXISTS product
(
    productID varchar UNIQUE NOT NULL,
    providerID varchar NOT NULL,
    providerName varchar NOT NULL,
    status status_type NOT NULL,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
    CONSTRAINT product_pk PRIMARY KEY (productID)
);

CREATE TABLE IF NOT EXISTS resource
(
    resourceID varchar UNIQUE NOT NULL,
    resourceName varchar UNIQUE NOT NULL,
    accessPolicy varchar NOT NULL,
    providerID varchar NOT NULL,
    providerName varchar NOT NULL,
    totalResources integer NOT NULL,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
    CONSTRAINT resource_pk PRIMARY KEY (resourceID)
);

CREATE TABLE IF NOT EXISTS product_resource_relation
(
    productID varchar REFERENCES product (productID),
    resourceID varchar REFERENCES resource (resourceID),
    _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
    CONSTRAINT pkey PRIMARY KEY (_id)
);

CREATE TABLE IF NOT EXISTS product_variant
(
    _id uuid NOT NULL,
    productVariantName varchar NOT NULL,
    productID varchar NOT NULL,
    providerID varchar NOT NULL,
    resourceNames varchar[] NOT NULL,
    resourceIDs varchar[] NOT NULL,
    resourceCapabilities varchar[][] NOT NULL,
    price numeric NOT NULL,
    validity numeric NOT NULL,
    status status_type NOT NULL,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
    CONSTRAINT pv_pk PRIMARY KEY
    (
      _id
    ),
    CONSTRAINT product_fk
        FOREIGN KEY(productID)
            REFERENCES product(productID)
);

CREATE TABLE IF NOT EXISTS purchase
(
    consumerID varchar NOT NULL,
    productID varchar NOT NULL,
    paymentStatus payment_status NOT NULL,
    paymentTime timestamp without time zone NOT NULL,
    expiry numeric NOT NULL,
    product_variant json NOT NULL,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
    _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
    CONSTRAINT purchase_pk PRIMARY KEY (_id),
    CONSTRAINT product_fk
        FOREIGN KEY(productID)
            REFERENCES product(productID)
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

-- product table
CREATE TRIGGER update_product_created BEFORE INSERT ON product FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_product_modified BEFORE INSERT
OR UPDATE ON
   product FOR EACH ROW EXECUTE PROCEDURE update_modified ();

-- resource table
CREATE TRIGGER update_resource_created BEFORE INSERT ON resource FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_resource_modified BEFORE INSERT
OR UPDATE ON
   resource FOR EACH ROW EXECUTE PROCEDURE update_modified ();

-- product variant table
CREATE TRIGGER update_pv_created BEFORE INSERT ON product_variant FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_pv_modified BEFORE INSERT
OR UPDATE ON
   product_variant FOR EACH ROW EXECUTE PROCEDURE update_modified ();

-- purchase table
CREATE TRIGGER update_purchase_created BEFORE INSERT ON purchase FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_purchase_modified BEFORE INSERT
OR UPDATE ON
   purchase FOR EACH ROW EXECUTE PROCEDURE update_modified ();
