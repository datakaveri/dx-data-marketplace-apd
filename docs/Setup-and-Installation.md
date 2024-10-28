<p align="center">
<img src="./cdpg.png" width="300">
</p>

# Setup and Installation Guide
This document contains the installation and configuration information required to deploy the Data Exchange (DX) DMP APD Server.

## Configuration
In order to connect the DX DMP APD Server with PostgreSQL, RabbitMQ, Razorpay Service, DX Catalogue Server, DX Auth Server, etc please refer [Configurations](./Configurations.md). It contains appropriate information which shall be updated as per the deployment.

## Dependencies
In this section we explain about the dependencies and their scope. It is expected that the dependencies are met before starting the deployment of DX DMP APD Server.

### External Dependencies
| Software Name        | Purpose                                                                                             | 
|:---------------------|:----------------------------------------------------------------------------------------------------|
| PostgreSQL           | For storing information related to policy, linked account, orders, resources, users, payment status |
| RabbitMQ             | To publish auditing related data to auditing server via RabbitMQ exchange                           |
| Java Razorpay Client | To onboard providers, create order, checkout page for consumers and process the payment             |


### Internal Dependencies
| Software Name                                               | Purpose                                                                  | 
|:------------------------------------------------------------|:-------------------------------------------------------------------------|
| DX Authentication Authorization and Accounting (AAA) Server | Used to download certificate for JWT token decoding and to get user info |
| DX Catalogue Server                                         | Used to fetch the list of resource and provider related information      |

## Prerequisites
### Keycloak registration for DX DMP-APD as trustee and APD
- The trustee user must be registered on Keycloak as a user
    - This can be done via the keycloak admin panel, or by using Data Exchange (DX) UI
    - The trustee user need not have any roles beforehand
- The COS admin user must call the create APD API : [Link to the API](https://authorization.iudx.org.in/apis#tag/Access-Policy-Domain-(APD)-APIs/operation/post-auth-v1-apd)
  with the name as the name of the APD, owner as email address of trustee (as registered on Keycloak)
  and URL as the domain of the APD
- Once the APD has been successfully registered, the trustee user will gain the trustee role
  scoped to that particular APD.
    - They can verify this by calling the list user roles API : [Link to the API](https://authorization.iudx.org.in/apis#tag/User-APIs/operation/get-auth-v1-user-roles)
- The trustee can get client credentials to be used in APD Operations by calling the
  get default client credentials API : [Link to the API](https://authorization.iudx.org.in/apis#tag/User-APIs/operation/get-auth-v1-user-clientcredentials)

### DMP-APD Server registering with Razorpay
- DMP APD Server could acts a sub merchant to onboard providers (merchants) for creating-linked account by creating an account in Razorpay : [link to the dashboard](https://dashboard.razorpay.com/?screen=sign_in)
- The live account mode needs to be activated in Razorpay by completing the KYC, filling other details
- Live account needs to be activated in order to allow the real-time transactions
- To use Linked account APIs through DMP, a dashboard needs to be created and a initial linked account should be created from the dashboard : [Link to pre-requisite]( https://razorpay.com/docs/payments/route/#prerequisites)
  Otherwise, Route feature not enabled for the merchant / Internal Server Error could be one the errors

### Provider's registration with Razorpay 
- Provider acts a merchant to the DMP-APD Server
- They need to create linked account in order to route the payments to their accounts
- Linked account creation at DMP involves :
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
- 2 Webhooks are to be created on Razorpay dashboard to capture information about 2 events from Razorpay asynchronously
    - order.paid
    - payment.failed
- A secret should be added while creating the webhook and could be stored in config with field name : webhook_secret
- baseUrl could be replaced with appropriate Data Marketplace APD URL

| Name           | Endpoint on DMP                     | Event on Razorpay |
|----------------|-------------------------------------|-------------------|
| Order paid     | https://baseUrl/order-paid-webhooks | order-paid        |
| Payment failed | https://baseUrl/payments-failed     | payment-failed    |


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


#### RabbitMQ
- To setup RabbitMQ refer the setup and installation instructions available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/databroker)
- After deployment of RabbitMQ, we need to ensure that there are certain prerequisites met. In case if it is not met, please login to RabbitMQ management portal using the RabbitMQ management UI and create a the following

##### Create vHost

| Type  | Name          | Details                    |   
|-------|---------------|----------------------------|
| vHost | IUDX-INTERNAL | Create a vHost in RabbitMQ |


##### Create Exchange

| Exchange Name | Type of exchange | features | Details                                                                              |   
|---------------|------------------|----------|--------------------------------------------------------------------------------------|
| auditing      | direct           | durable  | Create an exchange in vHost IUDX-INTERNAL to allow audit information to be published |  


##### Create Queue and Bind to Exchange
| Exchange Name | Queue Name | vHost   | routing key | Details                                                                                                                                    |  
|---------------|------------|---------|-------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| auditing      | direct     | durable | #           | Create a queue in vHost IUDX-INTERNAL to allow audit information to be consumed. Ensure that the queue is bound to the auditing exchange   |

##### User and Permissions
Create a DX DMP APD user using the RabbitMQ management UI and set write permission. This user will be used by DX DMP APD server to publish audit data

| API                         | Body           | Details                                                |   
|-----------------------------|----------------|--------------------------------------------------------|
| /api/users/user/permissions | As shown below | Set permission for a user to publish audit information | 


Body for the API request

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

#### PostgresQL
- To setup PostgreSQL refer setup and installation instructions available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/postgres)
- **Note** : PostgreSQL database should be configured with a RBAC user having CRUD privileges

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

#### Auditing
- Auditing is done using the DX Auditing Server which uses Immudb and Postgres for storing the audit logs
- To setup immuclient for immudb please refer [immudb setup guide](https://github.com/datakaveri/iudx-deployment/tree/master/docs/immudb-setup)
- The schema for auditing table in PostgreSQL is present here - [postgres auditing table schema](https://github.com/datakaveri/iudx-resource-server/blob/master/src/main/resources/db/migration/V5_7__create_audit_table_for_dmp.sql)
- The schema for Immudb table, index for the table is present here - [immudb schema in DX Auditing Server](https://github.com/datakaveri/auditing-server/tree/main/src/main/resources/immudb/migration)
- Auditing is currently done for Create Linked account, Update Linked account and Create Policy 

| Table Name    | Purpose                                                                                                                                             | DB                 | 
|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|--------------------|
| auditing_dmp  | To store logged information about endpoint, caller of the endpoint, timestamp when the POST, DELETE, PUT requests respond with 200 success response | Immudb, PostgreSQL |

- **Note** : User ID, endpoint and epoch time are indexed in Immudb to retrieve the logs faster

### Database Migration using Flyway
- Database flyway migrations help in updating the schema, permissions, grants, triggers etc., with the latest version
- Each flyway schema file is versioned with the format `V<majorVersion>_<minorVersion>__<description>.sql`, ex : `V1_1__init-tables.sql`
- Schemas for PostgreSQL tables are present here - [Flyway schema](https://github.com/datakaveri/iudx-data-marketplace-apd/tree/main/src/main/resources/db/migration)
- Values like DB URL, database user credentials, user and schema name should be populated in flyway.conf
- The following commands shall be executed
    - ``` mvn flyway:info -Dflyway.configFiles=flyway.conf``` To get the flyway schema history table
    - ``` mvn clean flyway:migrate -Dflyway.configFiles=flyway.conf ``` To migrate flyway schema
    - ``` mvn flyway:repair ``` To resolve some migration errors during flyway migration
- Please find the reference to Flyway migration [here](https://documentation.red-gate.com/fd/migrations-184127470.html)



## Installation Steps
### Maven
1. Install java 11 and maven
2. Use the maven exec plugin based starter to start the server. Goto the root folder where the pom.xml file is present and run the below command.
   `mvn clean compile exec:java@rs-apd-server`

### JAR
1. Install java 11 and maven
2. Set Environment variables
```
export DMP_APD_URL=https://<dmp-apd-domain-name>
export LOG_LEVEL=INFO
```
3. Use maven to package the application as a JAR. Goto the root folder where the pom.xml file is present and run the below command.
   `mvn clean package -Dmaven.test.skip=true`
4. 2 JAR files would be generated in the `target/` directory
    - `iudx.data.marketplace-cluster-1.0.0-SNAPSHOT-fat.jar` - clustered vert.x containing micrometer metrics
    - `iudx.data.marketplace-dev-1.0.0-SNAPSHOT-fat.jar` - non-clustered vert.x and does not contain micrometer metrics

#### Running the clustered JAR
**Note**: The clustered JAR requires Zookeeper to be installed. Refer [here](https://zookeeper.apache.org/doc/current/zookeeperStarted.html) to learn more about how to set up Zookeeper. Additionally, the `zookeepers` key in the config being used needs to be updated with the IP address/domain of the system running Zookeeper.
The JAR requires 3 runtime arguments when running:

* --config/-c : path to the config file
* --host/-i : the hostname for clustering
* --isClustered/-C : true for clustering and false for non-clustered mode
* --modules/-m : comma separated list of module names to deploy

e.g. ```java -jar target/iudx.iudx.apd.acl.server-cluster-0.0.1-SNAPSHOT-fat.jar --host $(hostname) -C true
-c secrets/all-verticles-configs/config.json -m iudx.data.marketplace.authenticator.AuthenticationVerticle, iudx.data.marketplace.apiserver.ApiServerVerticle,
iudx.data.marketplace.consumer.ConsumerVerticle, iudx.data.marketplace.product.ProductVerticle, iudx.data.marketplace.auditing.AuditingVerticle```

e.g. ```java -jar target/iudx.data.marketplace-cluster-1.0.0-SNAPSHOT-fat.jar -c secrets/all-verticles-configs/config.json --isClustered true --host localhost --modules all```

Use the `--help/-h` argument for more information. You may additionally append an `ACL_APD_JAVA_OPTS` environment
variable containing any Java options to pass to the application.

e.g.
```
$ export DMP_APD_JAVA_OPTS="Xmx40496m"
$ java $DMP_APD_JAVA_OPTS -jar target/iudx.data.marketplace-cluster-1.0.0-SNAPSHOT-fat.jar ...

```

#### Running the non-clustered JAR
The JAR requires 1 runtime argument when running

* --config/-c : path to the config file

e.g. ```java -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4j2LogDelegateFactory -jar -target/iudx.data.marketplace-dev-1.0.0-SNAPSHOT-fat.jar -c secrets/all-verticles-configs/config.json -C false```

Use the `--help/-h` argument for more information. You may additionally append an `RS_JAVA_OPTS` environment variable containing any Java options to pass to the application.

e.g.
```
$ export DMP_APD_JAVA_OPTS="-Xmx1024m"
$ java DMP_APD_JAVA_OPTS -jar target/iudx.data.marketplace-dev-1.0.0-SNAPSHOT-fat.jar ...
```

### Docker
1. Install docker and docker-compose
2. Clone this repo
3. Build the images
   ` ./docker/build.sh`
4. Modify the `docker-compose.yml` file to map the config file
5. Start the server in production (prod) or development (dev) mode using docker-compose
   ` docker-compose up prod `

## Logging and Monitoring
### Log4j 2
- For asynchronous logging, logging messages to the console in a specific format, Apache's log4j 2 is used
- For log formatting, adding appenders, adding custom logs, setting log levels, log4j2.xml could be updated : [link](https://github.com/datakaveri/dx-acl-apd/blob/main/src/main/resources/log4j2.xml)
- Please find the reference to log4j 2 : [here](https://logging.apache.org/log4j/2.x/manual/index.html)

### Micrometer
- Micrometer is used for observability of the application
- Reference link: [vertx-micrometer-metrics](https://vertx.io/docs/vertx-micrometer-metrics/java/)
- The metrics from micrometer is stored in Prometheus which can be used to alert, observe,
  take steps towards the current state of the application
- The data sent to Prometheus can then be visualised in Grafana
- Reference link: [vertx-prometheus-grafana](https://how-to.vertx.io/metrics-prometheus-grafana-howto/)
- DX Deployment repository references for [Prometheus](https://github.com/datakaveri/iudx-deployment/tree/master/K8s-deployment/K8s-cluster/addons/mon-stack/prometheus), [Loki](https://github.com/datakaveri/iudx-deployment/tree/master/K8s-deployment/K8s-cluster/addons/mon-stack/loki), [Grafana](https://github.com/datakaveri/iudx-deployment/tree/master/K8s-deployment/K8s-cluster/addons/mon-stack/grafana)

## Testing
### Unit Testing
1. Run the server through either docker, maven or redeployer
2. Run the unit tests and generate a surefire report
   `mvn clean test-compile surefire:test surefire-report:report`
3. Jacoco reports are stored in `./target/`


### Integration Testing

Integration tests are through Postman/Newman whose script can be found from [here](https://github.com/datakaveri/iudx-data-marketplace-apd/tree/main/src/test/resources).
1. Install prerequisites
- [postman](https://www.postman.com/) + [newman](https://www.npmjs.com/package/newman)
- [newman reporter-htmlextra](https://www.npmjs.com/package/newman-reporter-htmlextra)
2. Example Postman environment can be found [here](https://github.com/datakaveri/iudx-data-marketplace-apd/blob/main/src/test/resources/postman-environment.json)
- Please find the README to setup postman environment file [here](https://github.com/datakaveri/iudx-data-marketplace-apd/blob/main/src/test/resources/README.md)
3. Run the server through either docker, maven or redeployer
4. Run the integration tests and generate the newman report
   `newman run <postman-collection-path> -e <postman-environment> --insecure -r htmlextra --reporter-htmlextra-export .`
5. Command to store report in `target/newman`:  `newman run <postman-collection-path> -e <postman-environment> --insecure -r htmlextra --reporter-htmlextra-export ./target/newman/report.html`


### Performance Testing
- JMeter is for used performance testing, load testing of the application
- Please find the reference to JMeter : [here](https://jmeter.apache.org/usermanual/get-started.html)
- Command to generate HTML report at `target/jmeter`
```
rm -r -f target/jmeter && jmeter -n -t jmeter/<file-name>.jmx -l target/jmeter/sample-reports.csv -e -o target/jmeter/
```

### Security Testing
- For security testing, Zed Attack Proxy(ZAP) Scanning is done to discover security risks, vulnerabilities to help us address them
- A report is generated to show vulnerabilities as high risk, medium risk, low risk and false positive
- Please find the reference to ZAP : [here](https://www.zaproxy.org/getting-started/)