package iudx.data.marketplace.apiserver.provider;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.policies.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkedAccountServiceImpl implements LinkedAccountService{
    private static final Logger LOG = LoggerFactory.getLogger(LinkedAccountServiceImpl.class);

    public LinkedAccountServiceImpl(CreateLinkedAccount createLinkedAccount, FetchLinkedAccount fetchLinkedAccount, UpdateLinkedAccount updateLinkedAccount, JsonObject config) {
    }

    @Override
    public Future<JsonObject> createLinkedAccount(JsonObject request, User user) {
        return null;
    }

    @Override
    public Future<JsonObject> fetchLinkedAccount(User user) {
        return null;
    }

    @Override
    public Future<JsonObject> updateLinkedAccount(JsonObject request, User user) {
        return null;
    }
}
