package iudx.data.marketplace.apiserver.provider;


import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.auditing.AuditingService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CreateLinkedAccount {
    private static final Logger LOGGER = LogManager.getLogger(CreateLinkedAccount.class);
    PostgresService postgresService;
    Api api;
    AuditingService auditingService;
    public CreateLinkedAccount(PostgresService postgresService, Api api, AuditingService auditingService) {
        this.postgresService = postgresService;
        this.api = api;
        this.auditingService = auditingService;
    }


    public Future<JsonObject> initiateCreatingLinkedAccount(JsonObject request, User provider)
    {
        return null;
    }
}
