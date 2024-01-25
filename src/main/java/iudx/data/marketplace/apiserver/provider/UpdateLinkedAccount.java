package iudx.data.marketplace.apiserver.provider;


import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.auditing.AuditingService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;

public class UpdateLinkedAccount {
    PostgresService postgresService;
    Api api;
    AuditingService auditingService;
    public UpdateLinkedAccount(PostgresService postgresService, Api api, AuditingService auditingService) {
        this.postgresService = postgresService;
        this.api = api;
        this.auditingService = auditingService;
    }

    public Future<JsonObject> initiateUpdatingLinkedAccount(JsonObject request, User provider)
    {
        return null;
    }
}
