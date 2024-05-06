DROP TABLE policy;

CREATE TABLE IF NOT EXISTS policy
(
    _id uuid DEFAULT uuid_generate_v4 () NOT NULL PRIMARY KEY,
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

ALTER TABLE resource_entity
DROP CONSTRAINT resource_entity_resource_name_key;
