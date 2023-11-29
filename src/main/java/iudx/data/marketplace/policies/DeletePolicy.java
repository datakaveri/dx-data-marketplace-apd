package iudx.data.marketplace.policies;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import iudx.data.marketplace.postgres.PostgresService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class DeletePolicy {
    private static final Logger LOG = LoggerFactory.getLogger(DeletePolicy.class);
    private static final String FAILURE_MESSAGE = "Policy could not be deleted";
    private final PostgresService postgresService;
    private PgPool pool;

    public DeletePolicy(PostgresService postgresService) {
        this.postgresService = postgresService;
    }

    public Future<JsonObject> initiateDeletePolicy(JsonObject policy, User user) {
        UUID policyUuid = UUID.fromString(policy.getString("id"));
       return null;
    }
}
