<p align="center">
<img src="./cdpg.png" width="300">
</p>

# Modules
This document contains the information of the configurations to setup various services and dependencies in order to bring up the DX DMP APD Server. 
Please find the example configuration file [here](https://github.com/datakaveri/dx-dmp-apd/blob/main/example-config/config.json). While running the server, config.json file could
be added [secrets](https://github.com/datakaveri/dx-dmp-apd/tree/main/secrets/all-verticles-configs).


## Api Server Verticle

| Key Name          | Value Datatype | Value Example                              | Description                                                                                                                                     |
|:------------------|:--------------:|:-------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false                                      | To check if worker verticle needs to be deployed for blocking operations                                                                        |
| verticleInstances |    integer     | 1                                          | Number of instances required for verticles                                                                                                      |
| httpPort          |    integer     | 8080                                       | Port for running the instance DX DMP-APD Server                                                                                                 |
| keystore          |     String     | secrets/all-verticles-configs/keystore.jks | Path to where keystore is stored. It stores private key, SSL certificate for the DX DMP-APD Server to be deployed on clustered mode             |
| keystorePassword  |     String     | rNB4n4Z%np                                 | Password to access keystore file                                                                                                                |

## Other Configuration

| Key Name                                 | Value Datatype  | Value Example                        | Description                                                                                                             |
|:-----------------------------------------|:---------------:|:-------------------------------------|:------------------------------------------------------------------------------------------------------------------------|
| version                                  |      Float      | 1.0                                  | config version                                                                                                          |
| clusterId                                |     String      | iudx-marketplace-cluster             | cluster id to deploy clustered vert.x instance                                                                          |
| commonConfig.dxApiBasePath               |     String      | /dx/apd/dmp/v1                       | API base path for DX DMP-APD. Reference : [link](https://swagger.io/docs/specification/2-0/api-host-and-base-path/)     |
| commonConfig.dxAuthBasePath              |     String      | /auth/v1                             | API base path for DX Auth server. Reference : [link](https://swagger.io/docs/specification/2-0/api-host-and-base-path/) |
| commonConfig.catServerHost               |     String      | api.cat-test.iudx.io                 | Host name of DX Catalogue server for fetching the information of resources, resource groups                             |
| commonConfig.catServerPort               |     integer     | 443                                  | Port number to access HTTPS APIs of Catalogue Server                                                                    |
| commonConfig.authHost                    |     String      | authvertx.iudx.io                    | Host name of Auth Server for fetching information about user, delegate, authentication of user                          |
| commonConfig.authPort                    |     integer     | 443                                  | Port number to access HTTPS APIs of DX AAA server Default                                                               |
| commonConfig.clientId                    |      UUID       | b806432c-e510-4233-a4ff-316af67b6df8 | APD trustee client ID                                                                                                   |
| commonConfig.clientSecret                |      UUID       | 87d05695-1911-44f6-a1bc-d04422df6209 | APD trustee client secret                                                                                               |
| commonConfig.apdURL                      |     String      | dmp-apd.iudx.io                      | DX DMP-APD URL to validate audience field                                                                               |
| commonConfig.enableLogging               |     boolean     | false                                | To enable logs from Razorpay                                                                                            |
| commonConfig.catItemPath                 |     String      | /iudx/cat/v1/item                    | DX Catalogue Server's item API path                                                                                     |
| commonConfig.catRelPath                  |     String      | /iudx/cat/v1/relationship            | DX Catalogue Server's relationship API path                                                                             |
| commonConfig.tables                      | Array of String | [product, invoice]                   | Database tables used in DX DMP-APD Server                                                                               |

## Policy Verticle

| Key Name          | Value Datatype | Value Example | Description                                                              |
|:------------------|:--------------:|:--------------|:-------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false         | To check if worker verticle needs to be deployed for blocking operations |
| verticleInstances |    integer     | 1             | Number of instances required for verticles                               |


## Authentication Verticle

| Key Name          | Value Datatype | Value Example     | Description                                                                                     |
|:------------------|:--------------:|:------------------|:------------------------------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false             | To check if worker verticle needs to be deployed for blocking operations                        |
| verticleInstances |    integer     | 1                 | Number of instances required for verticles                                                      |
| issuer            |     String     | cos.iudx.io       | Data Exchange(DX) COS URL to authenticate the issuer in the token                               |
| jwtIgnoreExpiry   |    boolean     | false             | Set to true while using the server locally to allow expired tokens                              |

## Auditing Verticle

| Key Name                 | Value Datatype | Value Example | Description                                                                                            |
|:-------------------------|:--------------:|:--------------|:-------------------------------------------------------------------------------------------------------|
| isWorkerVerticle         |    boolean     | false         | To check if worker verticle needs to be deployed for blocking operations                               |
| verticleInstances        |    integer     | 1             | Number of instances required for verticles                                                             |
| dataBrokerIP             |     String     | localhost     | RMQ IP address                                                                                         |
| dataBrokerPort           |    integer     | 24568         | RMQ port number                                                                                        |
| dataBrokerVhost          |     String     | vHostName     | Vhost being used to send Audit information Default                                                     |
| dataBrokerUserName       |     String     | rmqUserName   | User name for RMQ                                                                                      |
| dataBrokerPassword       |     String     | rmqPassword   | Password for RMQ                                                                                       |
| dataBrokerManagementPort |    integer     | 28041         | Port on which RMQ Management plugin is running                                                         |
| connectionTimeout        |    integer     | 6000          | Setting connection timeout as part of RabbitMQ config options to set up webclient                      |
| requestedHeartbeat       |    integer     | 60            | Defines after what period of time the peer TCP connection should be considered unreachable by RabbitMQ |
| handshakeTimeout         |    integer     | 6000          | To increase or decrease the default connection time out                                                |
| requestedChannelMax      |    integer     | 5             | Tells no more that 5 (or given number) could be opened up on a connection at the same time             |
| networkRecoveryInterval  |    integer     | 500           | Interval to restart the connection between rabbitmq node and clients                                   |

## Postgres Verticle

| Key Name          | Value Datatype | Value Example  | Description                                                                                                                                                         |
|:------------------|:--------------:|:---------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false          | To check if worker verticle needs to be deployed for blocking operations                                                                                            |
| verticleInstances |    integer     | 1              | Number of instances required for verticles                                                                                                                          |
| databaseIP        |     String     | localhost      | Postgres Database IP address                                                                                                                                        |
| databasePort      |    integer     | 5433           | Postgres Port number                                                                                                                                                |
| databaseSchema    |     String     | acl_apd_schema | Postgres Database schema                                                                                                                                            |
| databaseName      |     String     | acl_apd        | Postgres Database name                                                                                                                                              |
| databaseUserName  |     String     | dbUserName     | Postgres Database user name                                                                                                                                         |
| databasePassword  |     String     | dbPassword     | Password for Postgres DB                                                                                                                                            |
| poolSize          |    integer     | 25             | The number of connections in a Postgres pool to execute a DB operation and release back into the pool. Reference :  https://vertx.io/docs/vertx-pg-client/java/     |

## Product Verticle

| Key Name                           | Value Datatype | Value Example | Description                                                                                                                                                                   |
|:-----------------------------------|:--------------:|:--------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| isWorkerVerticle                   |    boolean     | false         | To check if worker verticle needs to be deployed for blocking operations                                                                                                      |
| verticleInstances                  |    integer     | 1             | Number of instances required for verticles                                                                                                                                    |
| isAccountActivationCheckBeingDone  |    boolean     | true          | To allow or disallow a provider to create a product based on the activation of their linked account. Set to true to only allow the provider whose linked account is activated |

## ProductVariant Verticle

| Key Name          | Value Datatype | Value Example | Description                                                              |
|:------------------|:--------------:|:--------------|:-------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false         | To check if worker verticle needs to be deployed for blocking operations |
| verticleInstances |    integer     | 1             | Number of instances required for verticles                               |

## Consumer Verticle

| Key Name          | Value Datatype | Value Example | Description                                                              |
|:------------------|:--------------:|:--------------|:-------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false         | To check if worker verticle needs to be deployed for blocking operations |
| verticleInstances |    integer     | 1             | Number of instances required for verticles                               |

## LinkedAccount Verticle

| Key Name          | Value Datatype | Value Example | Description                                                              |
|:------------------|:--------------:|:--------------|:-------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false         | To check if worker verticle needs to be deployed for blocking operations |
| verticleInstances |    integer     | 1             | Number of instances required for verticles                               |

## Webhook Verticle

| Key Name          | Value Datatype | Value Example | Description                                                              |
|:------------------|:--------------:|:--------------|:-------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false         | To check if worker verticle needs to be deployed for blocking operations |
| verticleInstances |    integer     | 1             | Number of instances required for verticles                               |


## RazorPay Verticle

| Key Name          | Value Datatype | Value Example     | Description                                                                                              |
|:------------------|:--------------:|:------------------|:---------------------------------------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false             | To check if worker verticle needs to be deployed for blocking operations                                 |
| verticleInstances |    integer     | 1                 | Number of instances required for verticles                                                               |
| razorPayKey       |     String     | rzp_test_1D&eAxrZ | Razorpay key that is obtained from its dashboard to connect to the Java Razorpay client                  |
| razorPaySecret    |     String     | HbKv2nhBj3@J      | Password to connect to Razorpay client                                                                   |
| webhook_secret    |     String     | MMz6j$HAbzYz      | Password to confirm Razorpay webhook's identity. It is used while creating webhook on Razorpay dashboard |
