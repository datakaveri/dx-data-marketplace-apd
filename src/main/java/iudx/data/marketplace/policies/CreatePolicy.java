package iudx.data.marketplace.policies;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.common.CatalogueService;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.postgres.PostgresServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreatePolicy {
    private static final Logger LOGGER = LogManager.getLogger(CreatePolicy.class);
    private final PostgresServiceImpl postgresService;
    private final CatalogueService catalogueService;

    public CreatePolicy(PostgresServiceImpl postgresService, CatalogueService catalogueService) {
        this.postgresService = postgresService;
        this.catalogueService = catalogueService;
    }
    public Future<JsonObject> initiateCreatePolicy(JsonObject request, User user) {
        Promise<JsonObject> promise = Promise.promise();
       return null;

    }


}
