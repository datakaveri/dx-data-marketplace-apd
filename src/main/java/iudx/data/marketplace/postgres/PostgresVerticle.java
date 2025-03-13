package iudx.data.marketplace.postgres;

import static iudx.data.marketplace.common.Constants.POSTGRES_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import iudx.data.marketplace.postgres.service.PostgresService;
import iudx.data.marketplace.postgres.service.PostgresServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PostgresVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(PostgresVerticle.class);
  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;

  @Override
  public void start() throws Exception {

    String databaseIp = config().getString("databaseIP");
    int databasePort = config().getInteger("databasePort");
    String databaseName = config().getString("databaseName");
    String databaseUserName = config().getString("databaseUserName");
    String databasePassword = config().getString("databasePassword");
    int poolSize = config().getInteger("poolSize");

    PgConnectOptions connectOptions =
        new PgConnectOptions()
            .setPort(databasePort)
            .setHost(databaseIp)
            .setDatabase(databaseName)
            .setUser(databaseUserName)
            .setPassword(databasePassword)
            .setReconnectAttempts(2)
            .setReconnectInterval(1000L);

    PoolOptions poolOptions = new PoolOptions().setMaxSize(poolSize);
    Pool pool = Pool.pool(vertx, connectOptions, poolOptions);

    PostgresService pgService = new PostgresServiceImpl(pool);

    binder = new ServiceBinder(vertx);
    consumer =
        binder.setAddress(POSTGRES_SERVICE_ADDRESS).register(PostgresService.class, pgService);
    LOGGER.info("Postgres verticle started.");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
