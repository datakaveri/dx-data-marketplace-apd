CREATE VIEW resource_from_product_variant AS
(SELECT _id, resource_info-> 0 ->> 'id' AS resource_id
FROM product_variant);


CREATE VIEW product_variant_view AS
(

SELECT P._id AS "productVariantId", P.product_variant_name AS "productVariantName", P.product_id AS "productId",
P.provider_id AS "providerId", P.resource_info AS "resources",
P.price AS "price", P.validity AS "expiryInMonths"
, P.modified_at AS "updatedAt"  , P.created_at AS "createdAt" , resource_entity.resource_server AS "resourceServerUrl"
, P.status AS "productVariantStatus"
FROM product_variant P
INNER JOIN resource_from_product_variant  R
ON R._id = P._id
INNER JOIN resource_entity
ON resource_entity._id::uuid = R.resource_id::uuid
);

ALTER VIEW product_variant_view OWNER TO dmp_user;
ALTER VIEW resource_from_product_variant OWNER TO dmp_user;


GRANT INSERT, SELECT, UPDATE, DELETE, REFERENCES, TRIGGER ON product_variant_view TO dmp_user;
GRANT INSERT, SELECT, UPDATE, DELETE, REFERENCES, TRIGGER ON resource_from_product_variant TO dmp_user;
