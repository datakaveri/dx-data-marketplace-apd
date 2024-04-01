[![Build Status](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fiudx%2520DMP%2520APD%2520(master)%2520pipeline%2F)](https://jenkins.iudx.io/job/iudx%20DMP%20APD%20(master)%20pipeline/lastBuild/)
[![Jenkins Coverage](https://img.shields.io/jenkins/coverage/jacoco?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fiudx%2520DMP%2520APD%2520(master)%2520pipeline%2F)](https://jenkins.iudx.io/job/iudx%20DMP%20APD%20(master)%20pipeline/lastBuild/jacoco/)
[![Unit Tests](https://img.shields.io/jenkins/tests?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fiudx%2520DMP%2520APD%2520(master)%2520pipeline%2F&label=unit%20tests)](https://jenkins.iudx.io/job/iudx%20DMP%20APD%20(master)%20pipeline/lastBuild/testReport/)
[![Security Tests](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fiudx%2520DMP%2520APD%2520(master)%2520pipeline%2F&label=security%20tests)](https://jenkins.iudx.io/job/iudx%20DMP%20APD%20(master)%20pipeline/lastBuild/zap/)
[![Integration Tests](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fiudx%2520DMP%2520APD%2520(master)%2520pipeline%2F&label=integration%20tests)](https://jenkins.iudx.io/job/iudx%20DMP%20APD%20(master)%20pipeline/lastBuild/Integration_20Test_20Report/)

![IUDX](./docs/iudx.png)

# iudx-data-marketplace-apd

The data marketplace is IUDX's platform that enables data Providers to host their resources bundled as a product.
Likewise, data consumers can buy the products listed by various providers. The consumers can make purchases against a
product variant of the any product. Providers can receive payments through [Razorpay](https://razorpay.com/docs/) which
is implemented as the payment gateway.
All users can interact with the data marketplace API server using HTTPs requests.

<p align="center">
<img src="./docs/img.png">
</p>

## Features

- Provider can onboard to Razorpay as merchant using the marketplace's Linked Account Creation flow.
- Provider can create a product by bundling their resources and then create product-variants by adding various
  capabilities / constraints to access those resources.
- Consumers can fetch latest resources, products, product-variants and filter them accordingly.
- Consumers can place orders and make payments against an order via Razorpay.
- Consumers can list all the purchases they have made.
- Providers can also list the purchases made against all their resources or products and filter them accordingly.
- When a payment is successful and verified, resource access policies are created for the given consumer.
- Razorpay interacts with the server using webhooks to feed real-time information related to transactions and payments
- Secure data access over TLS
- The data marketplace is scalable and uses open source components like Vert.x toolkit for asynchronous operation,
  RabbitMQ as a databroker for auditing requests, PostgreSQL as a database
- Integration with DX Catalogue Server for understanding resource metadata, DX Auth Server for token introspection and
  DX Auditing Server for metering

## API Docs

Click here to access API docs : [link](https://dmp-apd.iudx.io/)

## Prerequisites

### External Dependencies Installation

The data marketplace Server uses the following external dependencies

- PostgreSQL
- RabbitMQ

Find the installations of the above along with the configurations to modify the database url, port and associated
credentials in the appropriate sections
[here](SETUP.md)

## Getting Started

### Docker Based

1. Install docker and docker-compose
2. Clone this repo
3. Build the images
   ` ./docker/build.sh`
4. Modify the `docker-compose.yml` file to map the config file you just created
5. Start the server in production (prod) or development (dev) mode using docker-compose
   ` docker-compose up prod `

### Maven Based

1. Install java 11 and maven
2. Use the maven exec plugin based starter to start the server
   `mvn clean compile exec:java@rs-apd-server`

### JAR based

1. Install java 11 and maven
2. Set Environment variables

```
export DMP_APD_URL=https://<dmp-apd-domain-name>
export LOG_LEVEL=INFO
```

3. Use maven to package the application as a JAR
   `mvn clean package -Dmaven.test.skip=true`
4. 2 JAR files would be generated in the `target/` directory
    - `iudx.data.marketplace-cluster-0.0.1-SNAPSHOT-fat.jar` - clustered vert.x containing micrometer metrics
    - `iudx.data.marketplace-dev-0.0.1-SNAPSHOT-fat.jar` - non-clustered vert.x and does not contain micrometer metrics

#### Running the clustered JAR

**Note**: The clustered JAR requires Zookeeper to be installed.
Refer [here](https://zookeeper.apache.org/doc/r3.3.3/zookeeperStarted.html) to learn more about how to set up Zookeeper.
Additionally, the `zookeepers` key in the config being used needs to be updated with the IP address/domain of the system
running Zookeeper.

The JAR requires 3 runtime arguments when running:

* --config/-c : path to the config file
* --hostname/-i : the hostname for clustering
* --modules/-m : comma separated list of module names to deploy

e.g. `java -jar target/iudx.data.marketplace-cluster-0.0.1-SNAPSHOT-fat.jar --host $(hostname) -c secrets/all-verticles-configs/config-dev.json -m iudx.data.marketplace.postgres.PostgresVerticle,iudx.data.marketplace.product.ProductVerticle
,iudx.data.marketplace.authenticator.AuthenticationVerticle ,iudx.data.marketplace.consumer.ConsumerVerticle,iudx.data.marketplace.auditing.AuditingVerticle`

Use the `--help/-h` argument for more information. You may additionally append an `DMP_APD_JAVA_OPTS` environment
variable containing any Java options to pass to the application.

e.g.

```
$ export DMP_APD_JAVA_OPTS="-Xmx4096m"
$ java $DMP_APD_JAVA_OPTS -jar target/iudx.data.marketplace-cluster-0.0.1-SNAPSHOT-fat.jar ...
```

#### Running the non-clustered JAR

The JAR requires 1 runtime argument when running:

* --config/-c : path to the config file

e.g. `java -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4j2LogDelegateFactory -jar target/iudx.data.marketplace-dev-0.0.1-SNAPSHOT-fat.jar -c secrets/all-verticles-configs/config-dev.json`

Use the `--help/-h` argument for more information. You may additionally append an `DMP_APD_JAVA_OPTS` environment
variable containing any Java options to pass to the application.

e.g.

```
$ export DMP_APD_JAVA_OPTS="-Xmx1024m"
$ java $DMP_APD_JAVA_OPTS -jar target/iudx.data.marketplace-dev-0.0.1-SNAPSHOT-fat.jar ...
```

### Testing

### Unit tests
1. Run the tests using `mvn clean test checkstyle:checkstyle pmd:pmd`  
2. Reports are stored in `./target/`


### Integration tests
Integration tests are through Rest Assured 
1. Run the server through either docker, maven or redeployer
2. Run the integration tests `mvn test-compile failsafe:integration-test -DskipUnitTests=true -DintTestHost=localhost -DintTestPort=8080`
3. Reports are stored in `./target/`

## Contributing

We follow Git Merge based workflow

1. Fork this repo
2. Create a new feature branch in your fork. Multiple features must have a hyphen separated name
3. Commit to your fork and raise a Pull Request with upstream

## License

[View License](./LICENSE)
