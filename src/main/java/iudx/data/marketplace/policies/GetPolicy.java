package iudx.data.marketplace.policies;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import iudx.data.marketplace.apiserver.util.Role;

import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.postgres.PostgresServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetPolicy {
    private static final Logger LOG = LoggerFactory.getLogger(GetPolicy.class);
    private static final String FAILURE_MESSAGE = "Policy could not be fetched";
    private final PostgresService postgresService;

    public GetPolicy(PostgresService postgresService) {
        this.postgresService = postgresService;
    }

    public Future<JsonObject> initiateGetPolicy(User user) {
      return null;
    }

}
