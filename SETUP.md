SETUP GUIDE
----

This document contains the installation and configuration processes
of the external modules for each verticle in DX DMP APD Server.


The external dependencies used by DX DMP APD Server are:
- `PostgreSQL` :  used to store and query data related to
    - Policies
    - Merchants 
    - Orders
    - Resources
    - Providers, consumers
    - Payment status
- `RabbitMQ` : used to
    - publish auditing data for auditing-server
- `Razorpay` : uses Razorpay Java SDK
  - to create linked account and onboard merchants
  - to create orders
  - to verify payment and webhook signature

  

The DX DMP APD also connects with various DX dependencies namely
- Authorization Server : used to download the certificate for token decoding
- Catalogue Server : used to download the list of resources, access policies and resource related information.
- Auditing Server : used to store metering related data.
## Setting up Data marketplace APD server with dependencies

**Note** : Access to HTTP APIs for search functionality should be configured with TLS and RBAC privileges

In order to connect to the appropriate DB required information such as databaseIP,databasePort etc. should be updated in the PostgresVerticle modules available in [config-example.json](configs/config-example.json).


**ApiServerVerticle**
```
    {
      "id": "iudx.data.marketplace.apiserver.ApiServerVerticle",
      "verticleInstances": <number-of-verticle-instances>,
      "isWorkerVerticle": false,
      "keystore": "<path/to/keystore.jks>",
      "keystorePassword": ",keystore-password>",
      "ip": "localhost",
      "httpPort": <port-number>,
      "ssl": false
    }
```


----

## Setting up PostgreSQL for DX DMP APD Server
-  Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/postgres) to setup PostgreSQL

**Note** : PostgresQL database should be configured with a RBAC user having CRUD privileges

In order to connect to the appropriate Postgres database, required information such as databaseIP,databasePort etc. should be updated in the PostgresVerticle module available in [config-example.json](secrets/all-verticles-configs/config-example.json).

**PostgresVerticle**
```
{
    "id": "iudx.resource.server.database.postgres.PostgresVerticle",
    "isWorkerVerticle":false,
    "verticleInstances": <num-of-verticle-instances>,
    "databaseIp": "localhost",
    "databasePort": <port-number>,
    "databaseName": <database-name>,
    "databaseUserName": <username-for-psql>,
    "databasePassword": <password-for-psql>,
    "poolSize": <pool-size>
}
```


## Setting up RazorPayVerticle for DX DMP APD Server

**RazorPayVerticle**
```
{
      "id": "iudx.data.marketplace.razorpay.RazorPayVerticle",
      "verticleInstances": <num-of-verticle-instances>,
      "isWorkerVerticle": false,
      "razorPayKey": "<razorpay-generated-key>",
      "razorPaySecret": "<razorpay-generated-password>"
    }
```


#### Schemas for PostgreSQL tables in DX DMP APD Server
- Refer to Flyway Schemas [here](https://github.com/datakaveri/iudx-data-marketplace-apd/tree/main/src/main/resources/db/migration).
1. User Table Schema to store user information
```
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
```

2. Resource entity schema to store information fetch from DX Catalogue server
```

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
```
3. Product Table Schema 
```
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
```
4. Product Resource Relation Table Schema
```
CREATE TABLE IF NOT EXISTS product_resource_relation
(
    product_id varchar REFERENCES product (product_id),
    resource_id uuid REFERENCES resource_entity (_id),
    _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
    CONSTRAINT _id_pkey PRIMARY KEY (_id)
);
```
5. Merchant Table Schema to store information about provider during payment gateway onboarding
```
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
```

6. Product Variant Table Schema
```
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
```

7. Order table schema 
```
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
```
8. Payment Table Schema
```
CREATE TABLE IF NOT EXISTS payment_table (
    order_id varchar NOT NULL,
    payment_id varchar NOT NULL,
    payment_signature varchar NOT NULL,
    created_at timestamp without time zone NOT NULL,
    CONSTRAINT payment_pk PRIMARY KEY (payment_id),
    CONSTRAINT order_id_fk FOREIGN KEY (order_id)
        REFERENCES order_table (order_id)
);

```
9. Invoice Table schema to store information related to purchase
```
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
```
10. Policy Table schema 
```
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
```
11. Auditing Server Auditing Table for DMP
```
CREATE TABLE IF NOT EXISTS auditing_dmp
(
   _id uuid DEFAULT uuid_generate_v4 () NOT NULL PRIMARY KEY,
   user_id uuid NOT NULL,
   api varchar NOT NULL,
   method varchar NOT NULL,
   info JSON NOT NULL,
   time timestamp without time zone NOT NULL,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL
);
```

----

## Setting up RabbitMQ for DX DMP APD Server
- Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/databroker) to setup RMQ.


In order to connect to the appropriate RabbitMQ instance, required information such as dataBrokerIP,dataBrokerPort etc. should be updated in the AuditingVerticle module available in [config-example.json](configs/config-example.json).

**AuditingVerticle**
```
    {
      "id": "iudx.data.marketplace.auditing.AuditingVerticle",
      "isWorkerVerticle": false,
      "verticleInstances": <num-of-verticle-instances>,
      "dataBrokerIP": "localhost",
      "dataBrokerPort": <port-number>,
      "dataBrokerVhost": "<vHost-name>",
      "dataBrokerUserName": "<username-for-rmq>",
      "dataBrokerPassword": "<password-for-rmq>",
      "dataBrokerManagementPort": <management-port-number>,
      "connectionTimeout": <time-in-milliseconds>,
      "requestedHeartbeat": <time-in-seconds>,
      "handshakeTimeout": <time-in-milliseconds>,
      "requestedChannelMax": <num-of-max-channels>,
      "networkRecoveryInterval": <time-in-milliseconds>,
      "automaticRecoveryEnabled": "true",
      "prodVhost": "<vHost-name-for-production-server>",
      "internalVhost": "<VHost-name>",
      "externalVhost": "<vHost-name>"
    }
```

----

## Setting up all the other verticles


In order to use the consumer and provider APIs in DMP, we need to input required information in PolicyVerticle, ProductVerticle, ProductVariantVerticle, ConsumerVerticle, LinkedAccountVerticle modules available in [config-example.json](configs/config-example.json).

**ProductVerticle**
```
    {
      "id": "iudx.data.marketplace.product.ProductVerticle",
      "verticleInstances": <num-of-verticle-instances>,
      "isWorkerVerticle": false,
      "isAccountActivationCheckBeingDone" : <true|false>
    }
```

**ProductVariantVerticle**
```
    {
      "id": "iudx.data.marketplace.product.variant.ProductVariantVerticle",
      "verticleInstances": <num-of-verticle-instances>,
      "isWorkerVerticle": false
    }
```
**ConsumerVerticle**
```
    {
      "id": "iudx.data.marketplace.consumer.ConsumerVerticle",
      "verticleInstances": <num-of-verticle-instances>,
      "isWorkerVerticle": false
    }
```
**LinkedAccountVerticle**
```
    {
      "id": "iudx.data.marketplace.apiserver.provider.linkedAccount.LinkedAccountVerticle",
      "verticleInstances": <num-of-verticle-instances>,
      "isWorkerVerticle": false
    }
```

**PolicyVerticle**
```
    {
      "id": "iudx.data.marketplace.policies.PolicyVerticle",
      "isWorkerVerticle": false,
      "verticleInstances": <num-of-verticle-instances>
    }

```


## Connecting with DX Catalogue Server and having base path

In order to connect to the DX catalogue server, required information such as catServerHost,catServerPort ,server specific base path should be updated in the common config module availabe in [config-example.json](configs/config-example.json).

**CommonConfig**
```
  "commonConfig": {
    "dxApiBasePath": "<dmp-apd-api-base-path>",
    "dxCatalogueBasePath": "<base-path-for-catalogue-server>",
    "dxAuthBasePath": "<base-path-for-authentication-server>",
    "catServerPort":<catalogue-server-port-number>,
    "catServerHost": "<catalogue-server-host>",
    "catItemPath": "<catalogue-item-api-path>",
    "catRelPath": "<catalgoue-relationship-api-path>",
    "tables": [
      "product",
      "resource_entity",
      "product_resource_relation",
      "product_variant",
      "user_table",
      "policy",
      "invoice",
      "order_table",
      "merchant_table",
      "payment_table"
    ],
    "authPort": <auth-server-port-number>,
    "authHost": "<authentication-server-host>",
    "clientId": "<dmp-apd-client-id>",
    "clientSecret": "<dmp-apd-client-secret>",
    "enableLogging": false
  }

```

-------


## Connecting with DX Authorization Server

In order to connect to the DX authentication server, required information such as issuer, auth server host, DMP APD URL should be updated in the AuthenticationVerticle module availabe in [config-example.json](configs/config-example.json).

**AuthenticationVerticle**
```
    {
      "id": "iudx.data.marketplace.authenticator.AuthenticationVerticle",
      "verticleInstances": <number-of-verticle-instances>,
      "isWorkerVerticle": false,
      "authServerHost": "<auth-host-url>",
      "jwtIgnoreExpiry": <true | false>,
      "issuer": "<token-issuer-url>",
      "apdURL": "<dmp-apd-url>"
    }
```