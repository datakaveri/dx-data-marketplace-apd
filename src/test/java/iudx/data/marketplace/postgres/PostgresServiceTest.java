package iudx.data.marketplace.postgres;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import iudx.data.marketplace.configuration.Configuration;
import iudx.data.marketplace.product.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(VertxExtension.class)
public class PostgresServiceTest {

  public static final Logger LOGGER = LogManager.getLogger(PostgresServiceTest.class);
  private static PostgresServiceImpl pgService;
  public static String CONTAINER = "postgres:12.11";

  @Container
  private static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(CONTAINER).withInitScript("pg_test_schema.sql");
  static String table, pdTable;
  private static Configuration config;
  private static JsonObject dbConfig;
  private static Supplier<String> supplier;

  @BeforeAll
  static void setup(Vertx vertx, VertxTestContext testContext) {

    config = new Configuration();
    dbConfig = config.configLoader(3, vertx);

    table = dbConfig.getJsonArray(Constants.TABLES).getString(0);
    pdTable = dbConfig.getJsonArray(Constants.TABLES).getString(2);

    postgresContainer.start();
    supplier = () -> UUID.randomUUID().toString();

    Integer port = postgresContainer.getFirstMappedPort();
    String host = postgresContainer.getHost();
    String db = postgresContainer.getDatabaseName();
    String user = postgresContainer.getUsername();
    String password = postgresContainer.getPassword();
    postgresContainer.withUsername(user);
    postgresContainer.withPassword(password);
    postgresContainer.withDatabaseName(db);
    postgresContainer.withExposedPorts(port);

    if (postgresContainer.isRunning()) {
      dbConfig.put("databasePort", port);

      PgConnectOptions connectOptions =
              new PgConnectOptions()
                      .setPort(port)
                      .setHost(host)
                      .setDatabase(db)
                      .setUser(user)
                      .setPassword(password)
                      .setReconnectAttempts(2)
                      .setReconnectInterval(1000);

      PoolOptions poolOptions = new PoolOptions().setMaxSize(dbConfig.getInteger("poolSize"));
      PgPool pool = PgPool.pool(vertx, connectOptions, poolOptions);

      pgService = new PostgresServiceImpl(pool);
      testContext.completeNow();
    } else {
      testContext.failNow("setup failed");
    }
  }

  @Test
  @Order(1)
  @DisplayName("Test execute query - success")
  public void testExecuteQuerySuccess(VertxTestContext testContext) {
    StringBuilder stringBuilder =
            new StringBuilder(
                    Constants.INSERT_PRODUCT_QUERY
                            .replace("$0", table)
                            .replace("$1", "product-id-new")
                            .replace("$2", "b2c27f3f-2524-4a84-816e-91f9ab23f837")
                            .replace("$3", "provider-name")
                            .replace("$4", "ACTIVE"));

    String expected = "{\"type\":\"urn:dx:dmp:success\",\"title\":\"Success\",\"results\":[]}";
    pgService.executeQuery(
            stringBuilder.toString(),
            handler -> {
              if (handler.succeeded()) {
                assertEquals(expected, handler.result().toString());
                assertTrue(handler.result().containsKey("type"));
                assertTrue(handler.result().containsKey("title"));
                assertTrue(handler.result().containsKey("results"));
                assertEquals("Success", handler.result().getString("title"));
                assertEquals("urn:dx:dmp:success", handler.result().getString("type"));
                testContext.completeNow();
              } else {
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @Order(2)
  @DisplayName("test execute query - failure")
  public void testExecuteQueryFailure(VertxTestContext testContext) {

    StringBuilder stringBuilder =
            new StringBuilder(
                    Constants.INSERT_PRODUCT_QUERY
                            .replace("$0", table)
                            .replace("$1", "product-id-new")
                            .replace("$2", "b2c27f3f-2524-4a84-816e-91f9ab23f837")
                            .replace("$3", "provider-name")
                            .replace("$4", "ACTIVE"));

    String expected = "{\"type\":\"urn:dx:dmp:DatabaseError\",\"title\":\"Database error\",\"detail\":\"ERROR: duplicate key value violates unique constraint \\\"product_pk\\\" (23505)\"}";
    pgService.executeQuery(stringBuilder.toString(), handler -> {
      if(handler.failed()) {
        assertEquals(expected,handler.cause().getMessage());
        testContext.completeNow();
      } else {
        testContext.failNow("test execute query unexpectedly failed");
      }
    });
  }

  @Test
  @Order(3)
  @DisplayName("Test execute count query - success")
  public void testExecuteCountQuery(VertxTestContext testContext) {
    StringBuilder stringBuilder = new StringBuilder(Constants.SELECT_PRODUCT_QUERY.replace("$0", table).replace("$1", "b2c27f3f-2524-4a84-816e-91f9ab23f837").replace("$2", "urn:datakaveri.org:b2c27f3f-2524-4a84-816e-91f9ab23f837:testProduct1"));

    pgService.executeCountQuery(stringBuilder.toString(), handler -> {
      if(handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info(result.encodePrettily());
        int hits = result.getInteger("totalHits");
        assertEquals(1, hits);
        testContext.completeNow();
      } else {
        testContext.failNow("execute count test failed");
      }
    });
  }

  @Test
  @Order(4)
  @DisplayName("Test execute count query - failure")
  public void testExecuteCountQueryFailure(VertxTestContext testContext) {
    String query = "select count(*) from nosuchtable";

    String expected = "{\"type\":\"urn:dx:dmp:DatabaseError\",\"title\":\"Database error\",\"detail\":\"ERROR: relation \\\"nosuchtable\\\" does not exist (42P01)\"}";
    pgService.executeCountQuery(query, handler -> {
      if(handler.failed()) {
        assertEquals(expected,handler.cause().getMessage());
        testContext.completeNow();
      } else {
        testContext.failNow("execute count unexpectedly failed");
      }
    });
  }

  @Test
  @Order(5)
  @DisplayName("Test execute prepapred query - success")
  public void testExecutePreparedQuery(VertxTestContext testContext) {
    JsonObject params = new JsonObject().put(Constants.STATUS, "INACTIVE").put(Constants.PRODUCT_ID, "product-id-alter");

    String expected = "{\"type\":\"urn:dx:dmp:success\",\"title\":\"Success\",\"results\":[]}";
    pgService.executePreparedQuery(Constants.DELETE_PRODUCT_QUERY.replace("$0", table), params, handler -> {
      if(handler.succeeded()) {
        assertEquals(expected, handler.result().toString());
        assertTrue(handler.result().containsKey("type"));
        assertTrue(handler.result().containsKey("title"));
        assertTrue(handler.result().containsKey("results"));
        assertEquals("Success", handler.result().getString("title"));
        assertEquals("urn:dx:dmp:success", handler.result().getString("type"));
        testContext.completeNow();
      } else {
        testContext.failNow("execute prepared query test failed");
      }
    });
  }

  @Test
  @Order(6)
  @DisplayName("test execute prepared query - failure")
  public void testExecutePrepQueryFailure(VertxTestContext testContext) {

    pgService.executePreparedQuery(Constants.DELETE_PRODUCT_QUERY.replace("$0", table), new JsonObject(), handler -> {
      if(handler.failed()) {
        testContext.completeNow();
      } else {
        testContext.failNow("test prepared query failed unexpectedly");
      }
    });
  }
  @Test
  @Order(7)
  @DisplayName("test execute transaction - success")
  public void testExecuteTransaction(VertxTestContext testContext) {
    List<String> queries = new ArrayList<>();
    queries.add(Constants.INSERT_P_R_REL_QUERY.replace("$0", pdTable).replace("$1", "urn:datakaveri.org:b2c27f3f-2524-4a84-816e-91f9ab23f837:testProduct1").replace("$2", "a347c5b6-5281-4749-9eab-89784d8f8f9a"));
    queries.add(Constants.INSERT_P_R_REL_QUERY.replace("$0", pdTable).replace("$1", "urn:datakaveri.org:b2c27f3f-2524-4a84-816e-91f9ab23f837:testProduct1").replace("$2", "a347c5b6-5281-4749-9eab-89784d8f8f9a"));

    String expected = "{\"type\":\"urn:dx:dmp:success\",\"title\":\"Success\"}";
    pgService.executeTransaction(queries, handler -> {
      LOGGER.info(handler);
      if(handler.succeeded()) {
        assertEquals(expected, handler.result().toString());
        testContext.completeNow();
      } else {
        testContext.failNow("execute transaction test failed");
      }
    });
  }

  @Test
  @Order(8)
  @DisplayName("test execute transaction - failure")
  public void testExecuteTransactionFailure(VertxTestContext testContext) {
    List<String> queries = new ArrayList<>();
    queries.add(Constants.INSERT_P_R_REL_QUERY.replace("$0", pdTable).replace("$1", "product-id-alter").replace("$2", "resource-id-some"));
    queries.add(Constants.INSERT_P_R_REL_QUERY.replace("$0", pdTable).replace("$1", "product-id-alter").replace("$2", "resource-id-thing"));

    pgService.executeTransaction(queries, handler -> {
      if(handler.failed()) {
        testContext.completeNow();
      } else {
        testContext.failNow("execute transaction test failed unexpectedly");
      }
    });
  }
}
