CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

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

CREATE TABLE IF NOT EXISTS product_resource_relation
(
    product_id varchar REFERENCES product (product_id),
    resource_id uuid REFERENCES resource_entity (_id),
    _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
    CONSTRAINT _id_pkey PRIMARY KEY (_id)
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


-- product table
CREATE TRIGGER update_product_created BEFORE INSERT ON product FOR EACH ROW EXECUTE PROCEDURE update_created();
CREATE TRIGGER update_product_modified BEFORE INSERT OR UPDATE ON product FOR EACH ROW EXECUTE PROCEDURE update_modified();

-- resource table
CREATE trigger update_resource_entity_created before insert on resource_entity for each row EXECUTE procedure update_created();
CREATE trigger update_resource_entity_modified before insert or update on resource_entity for each row EXECUTE procedure update_modified();


INSERT INTO public.user_table (_id, email_id, first_name, last_name, created_at, modified_at) VALUES ('b2c27f3f-2524-4a84-816e-91f9ab23f837', 'bryanrobert@iisc.ac.in', 'B', 'Robert A B C D', '2024-02-14 10:38:39.394019', '2024-02-14 10:38:39.394019') ON CONFLICT DO NOTHING;

INSERT INTO public.resource_entity (_id, resource_name, provider_name, provider_id, resource_server, accesspolicy, created_at, modified_at) VALUES ('a347c5b6-5281-4749-9eab-89784d8f8f9a', 'Chandigarh Paid Parking Locations', 'Administrator of the IUDX platform', 'b2c27f3f-2524-4a84-816e-91f9ab23f837', '49cf5c76-09de-11ee-be56-0242ac120002', 'OPEN', '2024-02-14 10:39:33.957266', '2024-02-14 10:39:33.957266') ON CONFLICT DO NOTHING;
INSERT INTO public.resource_entity (_id, resource_name, provider_name, provider_id, resource_server, accesspolicy, created_at, modified_at) VALUES ('2d043bdb-fc62-4650-8426-dc72492cd621', 'TEST ITEM FOR APD /verify TESTS', 'Administrator of the IUDX platform', 'b2c27f3f-2524-4a84-816e-91f9ab23f837', '49cf5c76-09de-11ee-be56-0242ac120002', 'SECURE', '2024-02-14 10:39:33.957266', '2024-02-14 10:39:33.957266') ON CONFLICT DO NOTHING;
INSERT INTO public.resource_entity (_id, resource_name, provider_name, provider_id, resource_server, accesspolicy, created_at, modified_at) VALUES ('b58da193-23d9-43eb-b98a-a103d4b6103c', 'FW055 Environment-UUID', 'Administrator of the IUDX platform', 'b2c27f3f-2524-4a84-816e-91f9ab23f837', '49cf5c76-09de-11ee-be56-0242ac120002', 'OPEN', '2024-02-14 10:39:33.957266', '2024-02-14 10:39:33.957266') ON CONFLICT DO NOTHING;
INSERT INTO public.resource_entity (_id, resource_name, provider_name, provider_id, resource_server, accesspolicy, created_at, modified_at) VALUES ('ae2b8b01-f642-411a-babb-cbd1b75fa2a1', 'Flood level data from FWR055', 'Administrator of the IUDX platform', 'b2c27f3f-2524-4a84-816e-91f9ab23f837', '49cf5c76-09de-11ee-be56-0242ac120002', 'SECURE', '2024-02-14 10:39:33.957266', '2024-02-14 10:39:33.957266') ON CONFLICT DO NOTHING;
INSERT INTO public.resource_entity (_id, resource_name, provider_name, provider_id, resource_server, accesspolicy, created_at, modified_at) VALUES ('2a4ac538-ac5d-4c8f-963a-cf0b5cc62520', 'Bus Position and ETA of Public Transit Buses in Surat City', 'Administrator of the IUDX platform', 'b2c27f3f-2524-4a84-816e-91f9ab23f837', '49cf5c76-09de-11ee-be56-0242ac120002', 'SECURE', '2024-02-14 10:39:33.957266', '2024-02-14 10:39:33.957266') ON CONFLICT DO NOTHING;
INSERT INTO public.resource_entity (_id, resource_name, provider_name, provider_id, resource_server, accesspolicy, created_at, modified_at) VALUES ('cc231b1d-6011-430b-918a-2fc4eff6a9d4', 'Public Addressing Systems Locations in Pune City', 'Administrator of the IUDX platform', 'b2c27f3f-2524-4a84-816e-91f9ab23f837', '49cf5c76-09de-11ee-be56-0242ac120002', 'SECURE', '2024-02-14 10:39:33.957266', '2024-02-14 10:39:33.957266') ON CONFLICT DO NOTHING;
INSERT INTO public.resource_entity (_id, resource_name, provider_name, provider_id, resource_server, accesspolicy, created_at, modified_at) VALUES ('695e222b-3fae-4325-8db0-3e29d01c4fc0', 'RS Adaptor Test Resource', 'provider for the postman collection', 'b2c27f3f-2524-4a84-816e-91f9ab23f837', 'rs.iudx.io', 'SECURE', '2024-02-15 09:58:33.197723', '2024-02-15 09:58:33.197723') ON CONFLICT DO NOTHING;
INSERT INTO public.resource_entity (_id, resource_name, provider_name, provider_id, resource_server, accesspolicy, created_at, modified_at) VALUES ('83c2e5c2-3574-4e11-9530-2b1fbdfce832', 'Surat Transit Realtime Position-UUID', 'provider for the postman collection', 'b2c27f3f-2524-4a84-816e-91f9ab23f837', 'rs.iudx.io', 'SECURE', '2024-02-15 09:58:33.197723', '2024-02-15 09:58:33.197723') ON CONFLICT DO NOTHING;

INSERT INTO public.product (product_id, status, provider_id, provider_name, created_at, modified_at) VALUES ('urn:datakaveri.org:b2c27f3f-2524-4a84-816e-91f9ab23f837:testProduct1', 'ACTIVE', 'b2c27f3f-2524-4a84-816e-91f9ab23f837', 'Administrator of the IUDX platform', '2024-02-14 10:42:30.91675', '2024-02-14 10:42:30.91675') ON CONFLICT DO NOTHING;
INSERT INTO public.product (product_id, status, provider_id, provider_name, created_at, modified_at) VALUES ('urn:datakaveri.org:b2c27f3f-2524-4a84-816e-91f9ab23f837:newProductAbcd', 'ACTIVE', 'b2c27f3f-2524-4a84-816e-91f9ab23f837', 'provider for the postman collection', '2024-02-15 09:58:33.197723', '2024-02-15 09:58:33.197723') ON CONFLICT DO NOTHING;


INSERT INTO public.product_resource_relation (product_id, resource_id, _id) VALUES ('urn:datakaveri.org:b2c27f3f-2524-4a84-816e-91f9ab23f837:testProduct1', 'a347c5b6-5281-4749-9eab-89784d8f8f9a', '1c4a5655-aa72-4838-831e-a71fcf4446a3') ON CONFLICT DO NOTHING;
INSERT INTO public.product_resource_relation (product_id, resource_id, _id) VALUES ('urn:datakaveri.org:b2c27f3f-2524-4a84-816e-91f9ab23f837:testProduct1', '2d043bdb-fc62-4650-8426-dc72492cd621', '7bea59e5-70b5-4fb9-a17c-c94d01129c11') ON CONFLICT DO NOTHING;
INSERT INTO public.product_resource_relation (product_id, resource_id, _id) VALUES ('urn:datakaveri.org:b2c27f3f-2524-4a84-816e-91f9ab23f837:newProductAbcd', '695e222b-3fae-4325-8db0-3e29d01c4fc0', '580ebaa2-eabb-449c-947b-b4b44de05ab9') ON CONFLICT DO NOTHING;
INSERT INTO public.product_resource_relation (product_id, resource_id, _id) VALUES ('urn:datakaveri.org:b2c27f3f-2524-4a84-816e-91f9ab23f837:newProductAbcd', '83c2e5c2-3574-4e11-9530-2b1fbdfce832', 'bf6fe192-14ee-42d9-b31c-7ec630d820cf') ON CONFLICT DO NOTHING;

