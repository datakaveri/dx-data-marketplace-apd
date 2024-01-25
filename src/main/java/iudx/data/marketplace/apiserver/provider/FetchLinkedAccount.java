package iudx.data.marketplace.apiserver.provider;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;

public class FetchLinkedAccount {
    PostgresService postgresService;
    Api api;
    public FetchLinkedAccount(PostgresService postgresService, Api api) {
        this.postgresService = postgresService;
        this.api = api;
    }

    public Future<JsonObject> initiateFetchingLinkedAccount(User provider)
    {
        return null;
    }
}
