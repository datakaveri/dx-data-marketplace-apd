ALTER TABLE order_table ALTER COLUMN order_id TYPE varchar;

ALTER TABLE invoice
    ADD CONSTRAINT fk_invoice_order FOREIGN KEY (order_id) REFERENCES order_table (order_id);

