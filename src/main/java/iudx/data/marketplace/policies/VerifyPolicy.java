package iudx.data.marketplace.policies;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.common.CatalogueService;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.postgres.PostgresServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VerifyPolicy {
    private static final Logger LOGGER = LogManager.getLogger(VerifyPolicy.class);
    private final PostgresService postgresService;
    private final CatalogueService catalogueService;

    public VerifyPolicy(PostgresService postgresService, CatalogueService catalogueService) {
        this.postgresService = postgresService;
        this.catalogueService = catalogueService;
    }
    public Future<JsonObject> initiateVerifyPolicy(JsonObject request) {
        Promise<JsonObject> promise = Promise.promise();

       return null;
    }
}
