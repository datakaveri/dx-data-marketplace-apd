package iudx.data.marketplace.postgres;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import iudx.data.marketplace.product.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
@Testcontainers
@ExtendWith(VertxExtension.class)
public class PostgresServiceTest {
  static PostgresServiceImpl pgService;
  @Container
  static PostgreSQLContainer container = new PostgreSQLContainer<>("postgres:12.11")
      .withInitScript("pg_test_schema.sql");


  @BeforeAll
  public static void  setUp(VertxTestContext vertxTestContext) {
    // Now we have an address and port for Postgresql, no matter where it is running
    Integer port = container.getFirstMappedPort();
    String host = container.getHost();
    String db = container.getDatabaseName();
    String user = container.getUsername();
    String password = container.getPassword();

    PgConnectOptions connectOptions = new PgConnectOptions()
        .setPort(port)
        .setHost(host)
        .setDatabase(db)
        .setUser(user)
        .setPassword(password);

    PoolOptions poolOptions = new PoolOptions()
        .setMaxSize(10);

    Vertx vertxObj = Vertx.vertx();

    PgPool pool = PgPool.pool(vertxObj, connectOptions, poolOptions);

    pgService = new PostgresServiceImpl(pool);
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test executeQuery method : Success")
  public void testExecuteQuerySuccess(VertxTestContext vertxTestContext) {
    StringBuilder stringBuilder = new StringBuilder(Constants.INSERT_P_D_REL_QUERY.replace("$1", "product-id-new").replace("$2", "dataset-id-new"));

    String expected = "{\"type\":\"urn:dx:rs:success\",\"title\":\"Success\",\"result\":[]}";
    pgService.executeQuery(stringBuilder.toString(), handler -> {
      if (handler.succeeded()) {
        assertEquals(expected, handler.result().toString());
        assertTrue(handler.result().containsKey("type"));
        assertTrue(handler.result().containsKey("title"));
        assertTrue(handler.result().containsKey("result"));
        assertEquals("Success", handler.result().getString("title"));
        assertEquals("urn:dx:rs:success", handler.result().getString("type"));
        vertxTestContext.completeNow();
      } else {
        vertxTestContext.failNow(handler.cause());
      }
    });

  }

}
