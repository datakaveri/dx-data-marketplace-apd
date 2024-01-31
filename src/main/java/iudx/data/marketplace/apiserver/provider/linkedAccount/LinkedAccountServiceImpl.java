package iudx.data.marketplace.apiserver.provider.linkedAccount;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.policies.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkedAccountServiceImpl implements LinkedAccountService {
  private static final Logger LOG = LoggerFactory.getLogger(LinkedAccountServiceImpl.class);

  private final CreateLinkedAccount createLinkedAccount;
  private final FetchLinkedAccount fetchLinkedAccount;
  private final UpdateLinkedAccount updateLinkedAccount;

  public LinkedAccountServiceImpl(
      CreateLinkedAccount createLinkedAccount,
      FetchLinkedAccount fetchLinkedAccount,
      UpdateLinkedAccount updateLinkedAccount) {
    this.createLinkedAccount = createLinkedAccount;
    this.fetchLinkedAccount = fetchLinkedAccount;
    this.updateLinkedAccount = updateLinkedAccount;
  }

  @Override
  public Future<JsonObject> createLinkedAccount(JsonObject request, User user) {
    Promise<JsonObject> promise = Promise.promise();

    this.createLinkedAccount
        .initiateCreatingLinkedAccount(request, user)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOG.info("Success ---->");
                promise.complete(handler.result());
              } else {
                LOG.error("Failure --->");
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
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
