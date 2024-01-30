ALTER TABLE order_table OWNER TO dmp_user;

 GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE order_table TO ${dmp_user};