package iudx.data.marketplace.postgresql;

import static iudx.data.marketplace.common.Constants.POSTGRESQL_SERVICE_ADDRESS;
import static iudx.data.marketplace.common.Constants.POSTGRES_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PostgresqlVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(PostgresqlVerticle.class);
  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;

  private PgConnectOptions connectOptions;
  private PoolOptions poolOptions;
  private PgPool pool;

  private String databaseIP;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int poolSize;

  private PostgresqlService pgService;

  @Override
  public void start() throws Exception {

    databaseIP = config().getString("databaseIP");
    databasePort = config().getInteger("databasePort");
    databaseName = config().getString("databaseName");
    databaseUserName = config().getString("databaseUserName");
    databasePassword = config().getString("databasePassword");
    poolSize = config().getInteger("poolSize");

    this.connectOptions =
        new PgConnectOptions()
            .setPort(databasePort)
            .setHost(databaseIP)
            .setDatabase(databaseName)
            .setUser(databaseUserName)
            .setPassword(databasePassword)
            .setReconnectAttempts(2)
            .setReconnectInterval(1000L);

    this.poolOptions = new PoolOptions().setMaxSize(poolSize);
    this.pool = PgPool.pool(vertx, connectOptions, poolOptions);

    pgService = new PostgresqlServiceImpl(this.pool);

    binder = new ServiceBinder(vertx);
    consumer =
        binder.setAddress(POSTGRESQL_SERVICE_ADDRESS).register(PostgresqlService.class, pgService);
    LOGGER.info("Postgres verticle started.");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
