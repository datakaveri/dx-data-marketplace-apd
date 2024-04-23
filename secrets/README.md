<table class="tg">
<thead>
  <tr>
    <th class="tg-f41v"><span style="font-weight:700;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Key</span></th>
    <th class="tg-f41v"><span style="font-weight:700;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Purpose</span></th>
    <th class="tg-f41v"><span style="font-weight:700;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Details</span></th>
  </tr>
</thead>
<tbody>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">dxApiBasePath</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">API base path for DX DMP-APD server</span><br><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Default : /dx/apd/acl/v1</span></td>
    <td class="tg-0lax" rowspan="3"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Reference : </span><a href="https://swagger.io/docs/specification/2-0/api-host-and-base-path/"><span style="font-weight:400;font-style:normal;text-decoration:underline;color:#15C;background-color:transparent">https://swagger.io/docs/specification/2-0/api-host-and-base-path/</span></a><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent"> </span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">dxCatalogueBasePath</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">API base path for DX Catalogue server</span><br><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Default : /iudx/cat/v1</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">dxAuthBasePath</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">API base path for DX AAA server</span><br><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Default : /auth/v1</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">catServerPort</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Host name of DX Catalogue server for fetching the information of resources, resource groups</span></td>
    <td class="tg-0lax" rowspan="4"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">In order to connect to the DX Catalogue server, required information such as catServerHost, catServerPort, dxCatalogueBasePath etc., should be updated in commonConfig module</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">catServerHost</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Port number to access HTTPS APIs of Catalogue Server</span><br><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent"> </span><br><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Default : 443</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">catItemPath</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">DX Catalogue Item endpoint path : [Link to API](https://api.cat-test.iudx.io/apis#tag/Entity/operation/get%20item)</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">catRelPath</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">DX Catalogue Relationship path : [Link to API] (https://api.cat-test.iudx.io/apis#tag/Relationship/operation/get%20related%20entity)</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">authPort</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Port number to access HTTPS APIs of Auth server</span><br><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Default : 443</span></td>
    <td class="tg-0lax" rowspan="2"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">In order to connect with DX Authentication server, required information like authServerHost, authPort, dxAuthBasePath etc., should be updated in commonConfig, AuthenticationVerticle module</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">authHost</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Host name of Auth Server for fetching information about user, delegate, authentication of user</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">tables</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Tables listed in postgres for DX DMP APD server</span></td>
    <td class="tg-0lax"></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">clientId</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">APD trustee client ID</span></td>
    <td class="tg-0lax" rowspan="2"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">DX DMP-APD as trustee user credentials</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">clientSecret</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">APD trustee client secret</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">enableLogging</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">To enable Razorpay logs</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Set as false in production instance of DX DMP APD Server</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">id</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Address of the verticle</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Example : iudx.apd.acl.server.apiserver.ApiServerVerticle , iudx.apd.acl.server.policy.PolicyVerticle</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">isWorkerVerticle</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Checks if worker verticles are to be created for a given verticle to perform blocking operations</span></td>
    <td class="tg-0lax"></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">verticleInstances</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Number of instances required for verticles </span></td>
    <td class="tg-0lax"></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">ssl</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">To create a encrypted link between the browser and server to keep the information private and secure</span></td>
    <td class="tg-0lax"></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">httpPort</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Port for running the instance DX DMP-APD Server</span></td>
    <td class="tg-0lax"></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">ssl</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">To create a encrypted link between the browser and server to keep the information private and secure</span></td>
    <td class="tg-0lax"></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">authServerHost</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Host name of Auth Server for fetching information about user, delegate, authentication of user</span></td>
    <td class="tg-0lax"></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">jwtIgnoreExpiry</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Set to while using the server locally to allow expired tokens</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Set as false in Production and Development instance of DX DMP-APD server</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">issuer</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">To authenticate the issuer in the token</span></td>
    <td class="tg-0lax"></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">apdURL</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">DX DMP-APD URL to validate audience field</span></td>
    <td class="tg-0lax"></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">databaseIP</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Database IP address</span></td>
    <td class="tg-0lax" rowspan="6"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">In order to connect to the appropriate Postgres database, required information like databaseIP, databasePort, etc., could be</span><br><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">updated in commonConfig module</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">databasePort</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Port number</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">databaseName</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Database name</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">databaseUserName</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Database user name</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">databasePassword</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Database password</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">poolSize</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Pg pool size for Postgres Client</span><br><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Reference : </span><br><a href="https://vertx.io/docs/vertx-pg-client/java/"><span style="font-weight:400;font-style:normal;text-decoration:underline;color:#15C;background-color:transparent">https://vertx.io/docs/vertx-pg-client/java/</span></a><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent"> </span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">isAccountActivationCheckBeingDone</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Enabled to true if account activation check from Razorpay is being done</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Set as true in Production and Development instance of DX DMP-APD server</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">dataBrokerIP</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">RMQ IP address</span></td>
    <td class="tg-0lax" rowspan="12"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">For sending Audit information to the auditing server. In order to connect to the appropriate RabbitMQ instance, required information such as dataBrokerIP, dataBrokerPort etc.</span><br><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">should be updated in AuditingVerticle module.</span><br><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">[Link to DX Auditing Server] (</span><a href="https://github.com/datakaveri/auditing-server"><span style="font-weight:400;font-style:normal;text-decoration:underline;color:#15C;background-color:transparent">https://github.com/datakaveri/auditing-server</span></a><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">)</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">dataBrokerPort</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">RMQ port number</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">dataBrokerVhost</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Vhost being used to send Audit information</span><br><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Default : IUDX-INTERNAL</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">dataBrokerUserName</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">User name for RMQ</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">dataBrokerPassword</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Password for RMQ</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">dataBrokerManagementPort</span></td>
    <td class="tg-0lax"></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">connectionTimeout</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Setting connection timeout as part of RabbitMQ config options to set up webclient</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">requestedHeartbeat</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Defines after what period of time the peer TCP connection should be considered unreachable by RabbitMQ</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">handshakeTimeout</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">To increase or decrease the default connection time out</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">requestedChannelMax</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Tells no more that 5 (or given number) could be opened up on a connection at the same time</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">networkRecoveryInterval</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Interval to restart the connection between rabbitmq node and clients</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">automaticRecoveryEnabled</span></td>
    <td class="tg-0lax"></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">razorPayKey</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Razorpay generated key to access Razorpay APIs</span></td>
    <td class="tg-0lax" rowspan="3"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Razorpay configuration to connect with Razorpay for order creation, payment verification etc.,</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">razorPaySecret</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Razorpay generated secret to access Razorpay APIs</span></td>
  </tr>
  <tr>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">webhook_secret</span></td>
    <td class="tg-0lax"><span style="font-weight:400;font-style:normal;text-decoration:none;color:#000;background-color:transparent">Secret used while creating webhook on Razorpay dashboard</span></td>
  </tr>
</tbody>
</table>