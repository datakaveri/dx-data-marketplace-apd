CREATE type linked_account_status_type AS ENUM
	(
		'CREATED',
		'ACTIVATED'
	);

	CREATE TABLE IF NOT EXISTS merchant_table
	(
		reference_id varchar(20) NOT NULL PRIMARY KEY,
		phone_number numeric NOT NULL,
		email varchar NOT NULL,
		legal_business_name varchar NOT NULL,
		customer_facing_business_name varchar NOT NULL,
		account_id varchar NOT NULL,
		provider_id UUID NOT NULL,
		status linked_account_status_type NOT NULL,
		created_at timestamp without time zone NOT NULL,
		modified_at timestamp without time zone NOT NULL,
		CONSTRAINT provider_id_fk FOREIGN KEY (provider_id) REFERENCES user_table(_id)
	);

	ALTER TABLE merchant_table OWNER TO dmp_user;

---
-- Triggers
---
CREATE TRIGGER update_merchant_table_created BEFORE INSERT ON merchant_table FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_merchant_table_modified BEFORE INSERT
OR UPDATE ON
   merchant_table FOR EACH ROW EXECUTE PROCEDURE update_modified ();


---
--   grant privileges
---
GRANT INSERT, SELECT, UPDATE, DELETE, REFERENCES, TRIGGER ON TABLE merchant_table TO dmp_user;

---
--   constraint
---
ALTER TABLE merchant_table ADD CONSTRAINT check_min_length CHECK (length(reference_id) = 20);
