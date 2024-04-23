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
# Setting up Data marketplace APD server with dependencies

**Note** : Access to HTTP APIs for search functionality should be configured with TLS and RBAC privileges

## Setting up config
- In order to setup PostgreSQL, RabbitMQ, Razorpay Service, connect with DX Catalogue Server, DX AAA Server, DX Auditing server, 
appropriate information could be updated in configs available
- Please refer [README.md](secrets/README.md) to update config options


## Prerequisites
### Keycloak registration for DX DMP-APD as trustee and DMP
- The trustee user must be registered on Keycloak as a user
  - This can be done via the keycloak admin panel, or by using Data Exchange (DX) UI
  - The trustee user need not have any roles beforehand
- The COS admin user must call the create APD API : [Link to the API](https://authorization.iudx.org.in/apis#tag/Access-Policy-Domain-(APD)-APIs/operation/post-auth-v1-apd)
  with the name as the name of the APD, owner as email address of trustee (same as whatever is registered on Keycloak)
  and URL as the domain of the APD
- Once the APD has been successfully registered, the trustee user will gain the trustee role
  scoped to that particular APD.
  - They can verify this by calling the list user roles API : [Link to the API](https://authorization.iudx.org.in/apis#tag/User-APIs/operation/get-auth-v1-user-roles)
- The trustee can get client credentials to be used in APD Operations by calling the
  get default client credentials API : [Link to the API](https://authorization.iudx.org.in/apis#tag/User-APIs/operation/get-auth-v1-user-clientcredentials)

### Razorpay Registration
- DMP APD Server could acts a sub-merchant to onboard providers (merchants) for creating-linked account by creating an account in Razorpay :
 [Link to dashboard](https://dashboard.razorpay.com/?screen=sign_in)
- The live account mode needs to be activated in Razorpay by completing the KYC, filling other details
- Live account needs to be activated in order to allow the real-time transactions

### Razorpay Registration
Linked account creation at DMP involves :
- Registration as provider at Data Exchange (DX) Platform 
- Approval of provider
  - Link to the get provider registration API : [here](https://authorization.iudx.org.in/apis#tag/Admin-APIs/operation/get-auth-v1-admin-provider-registrations)
  - Link to the update provider registration API : [here](https://authorization.iudx.org.in/apis#tag/Admin-APIs/operation/put-auth-v1-admin-provider-registrations)
- Registration as merchant at Razorpay
  - Link to the dashboard : [here](https://dashboard.razorpay.com/)
- Creation of resource items at Catalogue Server
  - Link to create item : [here](https://cos.iudx.org.in/cat/apis#tag/Entity/operation/create%20item)
- Creation of Linked account at Data Marketplace server
- Creation of Product at Data Marketplace server


### Webhook Creation at Razorpay
- Two Webhooks are to be created on Razorpay dashboard to capture information about 2 events from Razorpay asynchronously
  - Order-paid
  - Payment failed
- A secret should be added while creating the webhook and could be stored in config-dev with field name : webhook_secret

| Name           | Endpoint on DMP                       | Event on Razorpay |
|----------------|---------------------------------------|-------------------|
| Order paid     | https://<baseUrl>/order-paid-webhooks | order-paid        |
| Payment failed | https://<baseUrl>/payments-failed     | payment-failed    |

### Testing Payment flow in Razorpay’s test mode
- When the mode in Razorpay dashboard is test mode, the payment flow could be simulated by consumer 
buying a product-variant from UI
- When consumer clicks on check-out and buys a 
product variant on UI, a payment page from Razorpay pops up
- After the consumer selects options like netbanking 
and clicks on pay now, the consumer could select Success or Failed button to simulate the success or failure flow
- In Success flow, the consumer’s purchase is successful 
and a policy is created for the resources listed under the given product variant
- During failure flow, the consumer’s purchase fails and
could be fetched in list failed purchase



## Setting up RabbitMQ for DX DMP-APD Server
- To setup RMQ refer the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/databroker)

### vHost table
| vHost         |  
|---------------|
| IUDX-INTERNAL |

### Exchange table
| Exchange Name | Type of exchange | features |   
|---------------|------------------|----------|
| auditing      | direct           | durable  | 

### Queue table
| Exchange Name | Queue Name | vHost   | routing key |
|---------------|------------|---------|-------------|
| auditing      | direct     | durable | #           |

### Permissions
DMP-APD user could have write permission as it publishes audit data
```
 "permissions": [
        {
          "vhost": "IUDX-INTERNAL",
          "permission": {
            "configure": "^$",
            "write": "^auditing$",
            "read": "^$"
          }
        }
]
```

## Setting up Postgres for DX DMP-APD Server
- To setup PostgreSQL refer the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/postgres)
- **Note** : PostgreSQL database should be configured with a RBAC user having CRUD privileges
- Schemas for PostgreSQL tables are present here - [Flyway schema](src/main/resources/db/migration)

| Table Name                | Purpose                                                                                                                              |
|---------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| user_table                | To store user related information like first name, last name, email etc, that is fetched from AAA Server                             |
| resource_entity           | To store resource information fetched from catalogue                                                                                 |
| policy                    | To store policy related information, resource info, consumer and provider info                                                       |
| product                   | For storing provider, product related information                                                                                    |
| merchant_table            | For storing linked account related information of the provider along with razorpay account_id, account product ID,  business details |
| product_resource_relation | For mapping the product ID with resource ID(s)                                                                                       |
| product_variant           | For storing product variants of a given product                                                                                      |
| order_table               | For storing order ID from Razorpay, amount of the product variant, currency used etc.,                                               |
| payment_table             | To store payment related information like paymentId, payment signature, orderId from Razorpay to verify a payment                    |
| invoice                   | To store purchase related information                                                                                                |

## Setting up Auditing for DX DMP-APD Server
- Auditing is done using Immudb and Postgres DB
- To Setup immuclient for immudb please refere [here](https://github.com/datakaveri/iudx-deployment/tree/master/docs/immudb-setup) 
- Schema for PostgreSQL table is present [here](https://github.com/datakaveri/iudx-resource-server/blob/master/src/main/resources/db/migration/V5_2__create-auditing-acl-apd-table.sql)
- Schema for Immudb table, index for the table is present [here](https://github.com/datakaveri/auditing-server/tree/main/src/main/resources/immudb/migration) 