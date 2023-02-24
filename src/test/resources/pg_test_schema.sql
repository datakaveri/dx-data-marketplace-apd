CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS product
(
    productID varchar UNIQUE NOT NULL,
    providerID varchar NOT NULL,
    providerName varchar NOT NULL,
    status varchar NOT NULL,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
    CONSTRAINT product_pk PRIMARY KEY (productID)
);

CREATE TABLE IF NOT EXISTS dataset
(
    datasetID varchar UNIQUE NOT NULL,
    datasetName varchar UNIQUE NOT NULL,
    accessPolicy varchar NOT NULL,
    providerID varchar NOT NULL,
    providerName varchar NOT NULL,
    totalResources integer NOT NULL,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
    CONSTRAINT dataset_pk PRIMARY KEY (datasetID)
);

CREATE TABLE IF NOT EXISTS product_dataset_relation
(
    productID varchar REFERENCES product (productID),
    datasetID varchar REFERENCES dataset (datasetID),
    _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
    CONSTRAINT pkey PRIMARY KEY (_id)
);

-- modified_at column function
CREATE
OR REPLACE
   FUNCTION update_modified () RETURNS TRIGGER AS '
BEGIN NEW.modified_at = now ();
RETURN NEW;
END;
' language 'plpgsql';

-- created_at column function
CREATE
OR REPLACE
   FUNCTION update_created () RETURNS TRIGGER AS '
BEGIN NEW.created_at = now ();
RETURN NEW;
END;
' language 'plpgsql';

---
-- Triggers
---

-- product table
CREATE TRIGGER update_product_created BEFORE INSERT ON product FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_product_modified BEFORE INSERT
OR UPDATE ON
   product FOR EACH ROW EXECUTE PROCEDURE update_modified ();

-- dataset table
CREATE TRIGGER update_dataset_created BEFORE INSERT ON dataset FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_dataset_modified BEFORE INSERT
OR UPDATE ON
   dataset FOR EACH ROW EXECUTE PROCEDURE update_modified ();

INSERT INTO product (productID, providerID, providerName, status) VALUES ('product-id-alter', 'provider-id', 'provider-name', 'ACTIVE');

INSERT INTO dataset (datasetID, datasetName, accessPolicy, providerID, providerName, totalResources) VALUES ('dataset-id-1', 'dat-name-1', 'OPEN', 'provider-id', 'provider-name', 10);
INSERT INTO dataset (datasetID, datasetName, accessPolicy, providerID, providerName, totalResources) VALUES ('dataset-id-2', 'dat-name-2', 'SECURE', 'provider-id', 'provider-name', 8);
