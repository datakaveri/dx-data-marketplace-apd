package iudx.data.marketplace.policies;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;
import iudx.data.marketplace.apiserver.util.Role;

import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.postgres.PostgresServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.common.HttpStatusCode.BAD_REQUEST;
import static iudx.data.marketplace.policies.util.Constants.GET_POLICY_4_CONSUMER_QUERY;
import static iudx.data.marketplace.policies.util.Constants.GET_POLICY_4_PROVIDER_QUERY;

public class GetPolicy {
    private static final Logger LOG = LoggerFactory.getLogger(GetPolicy.class);
    public static final String FAILURE_MESSAGE = "Policy could not be fetched";

    private final PostgresService postgresService;

  public GetPolicy(PostgresService postgresService) {
    this.postgresService = postgresService;
}


  public Future<JsonObject> initiateGetPolicy(User user) {
    Role role = user.getUserRole();
    switch (role) {
      case CONSUMER_DELEGATE:
      case CONSUMER:
        return getConsumerPolicy(user, GET_POLICY_4_CONSUMER_QUERY);
      case PROVIDER_DELEGATE:
      case PROVIDER:
        return getProviderPolicy(user, GET_POLICY_4_PROVIDER_QUERY);
      default:
        JsonObject response =
            new JsonObject()
                .put(TYPE, BAD_REQUEST.getValue())
                .put(TITLE, BAD_REQUEST.getUrn())
                .put(DETAIL, "Invalid role");
        return Future.failedFuture(response.encode());
    }
  }

  /**
   * Fetch policy details of the provider based on the ownerId and gets the information about
   * consumer like consumer first name, last name, id based on the consumer email-Id
   *
   * @param provider Object of User type
   * @param query Query to be executed
   * @return Policy details
   */
  public Future<JsonObject> getProviderPolicy(User provider, String query) {
    Promise<JsonObject> promise = Promise.promise();
    String ownerIdValue = provider.getUserId();
    String resourceServerUrl = provider.getResourceServerUrl();

    LOG.trace(provider.toString());
    String finalQuery = query.replace("$1", "'" + ownerIdValue + "'")
            .replace("$2", "'" + resourceServerUrl + "'");
//    Tuple tuple = Tuple.of(ownerIdValue, resourceServerUrl);
    JsonObject jsonObject =
        new JsonObject()
            .put("email", provider.getEmailId())
            .put(
                "name",
                new JsonObject()
                    .put("firstName", provider.getFirstName())
                    .put("lastName", provider.getLastName()))
            .put("id", provider.getUserId());
    JsonObject providerInfo = new JsonObject().put("provider", jsonObject);
    this.executeGetPolicy(finalQuery, providerInfo, Role.PROVIDER)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOG.info("success while executing GET provider policy");
                promise.complete(handler.result());
              } else {
                LOG.error("failure while executing GET provider policy");
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
  }

  /**
   * Fetches policies related to the consumer based on the consumer's email-Id <br>
   * Also gets information related to the owner of the policy like first name, last name, email-Id
   * based on the ownerId
   *
   * @param consumer Object of User type
   * @param query Query to be executed
   * @return Policy details
   */
  public Future<JsonObject> getConsumerPolicy(User consumer, String query) {
    Promise<JsonObject> promise = Promise.promise();
    String emailId = consumer.getEmailId();
    String resourceServerUrl = consumer.getResourceServerUrl();
    LOG.trace(consumer.toString());
//    Tuple tuple = Tuple.of("consumer@gmail.com", resourceServerUrl);
    JsonObject jsonObject =
        new JsonObject()
            .put("email", emailId)
            .put(
                "name",
                new JsonObject()
                    .put("firstName", consumer.getFirstName())
                    .put("lastName", consumer.getLastName()))
            .put("id", consumer.getUserId());
    JsonObject consumerInfo = new JsonObject().put("consumer", jsonObject);

    String finalQuery = query
            .replace("$1", "'" + consumer.getEmailId() + "'")
            .replace("$2", "'" + resourceServerUrl + "'");
    this.executeGetPolicy(finalQuery, consumerInfo, Role.CONSUMER)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOG.info("success while executing GET consumer policy");
                promise.complete(handler.result());
              } else {
                LOG.error("Failure while executing GET consumer policy");
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
  }

  /**
   * Executes the respective queries by using the vertx PgPool instance
   *
   * @param query String query to be executed
   * @param information Information to be added in the response
   * @return the response as Future JsonObject type
   */
  private Future<JsonObject> executeGetPolicy(
      String query, JsonObject information, Role role) {
    Promise<JsonObject> promise = Promise.promise();
    postgresService
        .executeQuery(query, handler -> {
          if(handler.succeeded())
          {
            boolean isResultFromDbEmpty = handler.result().getJsonArray(RESULTS).isEmpty();
            if (!isResultFromDbEmpty) {
              for(int i = 0; i < handler.result().getJsonArray(RESULTS).size() ; i++)
              {
                JsonObject jsonObject = handler.result().getJsonArray(RESULTS).getJsonObject(i);
                jsonObject.mergeIn(information).mergeIn(getInformation(jsonObject, role));
              }

              JsonObject response =
                      new JsonObject()
                              .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                              .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                              .put(RESULT, handler.result().getJsonArray(RESULTS));
              promise.complete(
                      new JsonObject()
                              .put(RESULT, response)
                              .put(STATUS_CODE, HttpStatusCode.SUCCESS.getValue()));
            } else {
              JsonObject response =
                      new JsonObject()
                              .put(TYPE, HttpStatusCode.NOT_FOUND.getValue())
                              .put(TITLE, ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                              .put(DETAIL, "Policy Not found");
              LOG.error("No policy found!");
              promise.fail(response.encode());
            }
          }else
          {
            LOG.error("Failed : " + handler.cause());
            JsonObject failureMessage =
                    new JsonObject()
                            .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                            .put(TITLE, ResponseUrn.DB_ERROR_URN.getUrn())
                            .put(DETAIL, FAILURE_MESSAGE + ", Failure while executing query");
            promise.fail(failureMessage.encode());
          }
        });

    return promise.future();
  }

  public JsonObject getInformation(JsonObject jsonObject, Role role) {
    if (role.equals(Role.CONSUMER)) {
      return getConsumerInformation(jsonObject);
    }
    return getProviderInformation(jsonObject);
  }

  public JsonObject getConsumerInformation(JsonObject jsonObject) {
    String ownerFirstName = jsonObject.getString("providerFirstName");
    String ownerLastName = jsonObject.getString("providerLastName");
    String ownerId = jsonObject.getString("providerId");
    String ownerEmail = jsonObject.getString("providerEmailId");
    JsonObject providerJson =
        new JsonObject()
            .put("email", ownerEmail)
            .put(
                "name",
                new JsonObject().put("firstName", ownerFirstName).put("lastName", ownerLastName))
            .put("id", ownerId);
    final JsonObject providerInfo = new JsonObject().put("provider", providerJson);
    jsonObject.remove("providerFirstName");
    jsonObject.remove("providerLastName");
    jsonObject.remove("providerId");
    jsonObject.remove("providerEmailId");
    return providerInfo;
  }

  public JsonObject getProviderInformation(JsonObject jsonObject) {
    String consumerFirstName = jsonObject.getString("consumerFirstName");
    String consumerLastName = jsonObject.getString("consumerLastName");
    String consumerId = jsonObject.getString("consumerId");
    String consumerEmail = jsonObject.getString("consumerEmailId");
    JsonObject consumerJson = new JsonObject().put("email", consumerEmail);
    // if the consumer is not present in the db then the response will only contain its email
    // address
    if (consumerFirstName != null) {
      consumerJson
          .put(
              "name",
              new JsonObject()
                  .put("firstName", consumerFirstName)
                  .put("lastName", consumerLastName))
          .put("id", consumerId);
    }
    final JsonObject consumerInfo = new JsonObject().put("consumer", consumerJson);
    jsonObject.remove("consumerFirstName");
    jsonObject.remove("consumerLastName");
    jsonObject.remove("consumerId");
    jsonObject.remove("consumerEmailId");
    return consumerInfo;
  }
}
