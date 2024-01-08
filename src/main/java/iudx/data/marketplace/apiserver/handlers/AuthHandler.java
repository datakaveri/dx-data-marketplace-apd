package iudx.data.marketplace.apiserver.handlers;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Tuple;
import iudx.data.marketplace.authenticator.AuthClient;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.authenticator.AuthenticationService;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.postgres.PostgresServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.authenticator.util.Constants.GET_USER;
import static iudx.data.marketplace.authenticator.util.Constants.INSERT_USER_TABLE;
import static iudx.data.marketplace.common.Constants.AUTH_INFO;
import static iudx.data.marketplace.common.Constants.AUTH_SERVICE_ADDRESS;
import static iudx.data.marketplace.common.ResponseUrn.INVALID_TOKEN_URN;
import static iudx.data.marketplace.common.ResponseUrn.RESOURCE_NOT_FOUND_URN;

public class AuthHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(AuthHandler.class);

  static AuthenticationService authenticator;
  private static Api api;
  private static PostgresService postgresServiceImpl;
  private static AuthClient authClient;
  private HttpServerRequest request;

  public static AuthHandler create(AuthenticationService authenticationService, Vertx vertx, Api apis,
          PostgresService postgresService, AuthClient authClientObj) {
    authenticator = authenticationService;
    api = apis;
    postgresServiceImpl = postgresService;
    authClient = authClientObj;
    return new AuthHandler();
  }

  @Override
  public void handle(RoutingContext context) {
    request = context.request();
    JsonObject requestJson = context.body().asJsonObject();

    if (requestJson == null) {
      requestJson = new JsonObject();
    }

    if(context.pathParams().containsKey(PROVIDER_ID)) {
      requestJson.put(PROVIDER_ID, context.pathParam(PROVIDER_ID));
    }

    LOGGER.debug("Info : path " + request.path());

    String token = request.headers().get(AUTHORIZATION_KEY);
    final String path = getNormalisedPath(request.path());
    final String method = context.request().method().toString();

    JsonObject authInfo =
        new JsonObject().put(API_ENDPOINT, path).put(HEADER_TOKEN, token).put(API_METHOD, method);

    if(path.equals(api.getVerifyUrl()))
    {
   // removes `bearer` from the token by trimming the leading and trailing spaces
      if(token.trim().split(" ").length == 2)
      {
        token = token.trim().split(" ")[1];
        authInfo.put(HEADER_TOKEN, token);
        authenticator.tokenIntrospect4Verify(authInfo, handler -> {
          if(handler.succeeded())
          {
            LOGGER.info("User verified successfully");
            context.next();
          }
          else
          {
            LOGGER.error("User verification failed : {}", handler.cause().getMessage());
            processAuthFailure(context, handler.cause().getMessage());
          }
        });
      }else
      {
        processAuthFailure(context, "Invalid token");
      }
    }
    else // for all the other endpoints
    {
      checkAuth(requestJson, authInfo)
              .onSuccess(userObject -> {
                LOGGER.info("User verification successful");
                context.put("user", userObject);
                context.next();
              })
              .onFailure(failureHandler -> {
                failureHandler.printStackTrace();
                LOGGER.error("User verification failure : {}", failureHandler.getMessage());
                processAuthFailure(context, failureHandler.getMessage());
              });

    }
  }


  Future<User> checkAuth(JsonObject request, JsonObject authInfo)
  {
    Promise<User> promise = Promise.promise();
    authenticator.tokenIntrospect(request, authInfo, handler -> {
      if(handler.succeeded())
      {
        JsonObject tokenIntrospectResult = handler.result();
        Future<User> userInfoFuture = getUserInfo(tokenIntrospectResult)
                .onSuccess(promise::complete)
                .onFailure(promise::fail);
      }
      else
      {
        LOGGER.error("Token introspection failed");
        promise.fail(handler.cause().getMessage());
      }

    });
    return promise.future();
  }


  private Future<User> getUserInfo(JsonObject tokenIntrospectResult)
  {
    LOGGER.info("Getting user info..");
    Promise<User> promise = Promise.promise();
    UserContainer userContainer = new UserContainer();
    JsonObject params = new JsonObject()
            .put("$1",tokenIntrospectResult.getString("userId"));
    postgresServiceImpl
            .executePreparedQuery(GET_USER, params, handler -> {
              if(handler.succeeded())
              {
                JsonArray info = handler.result().getJsonArray(RESULTS);
                if(!info.isEmpty())
                {
                  JsonObject userInfo = info.getJsonObject(0);
                  LOGGER.info("User found in Database");
                  JsonObject userJson = new JsonObject()
                          .put(USERID, tokenIntrospectResult.getString(USERID))
                          .put(USER_ROLE, tokenIntrospectResult.getString(ROLE))
                          .put(EMAIL_ID, userInfo.getString("email_id"))
                          .put(FIRST_NAME, userInfo.getString("first_name"))
                          .put(LAST_NAME, userInfo.getString("last_name"))
                          .put(RS_SERVER_URL, tokenIntrospectResult.getString(AUD));
                  User user = new User(userJson);
                  promise.complete(user);
                }
                else
                {
                  LOGGER.info("user not present in DB, getting user information from Auth");
                  Future<User> getUserFromAuth = authClient.fetchUserInfo(tokenIntrospectResult);
                  Future<Void> insertInDb = getUserFromAuth.compose(user -> {
                    userContainer.user = user;
                    return insertUserIntoDb(user);
                  });

                  insertInDb.onSuccess(successHandler -> {
                    LOGGER.debug("User successfully inserted in DB");
                    promise.complete(userContainer.user);
                  }).onFailure(failureHandler -> {
                    LOGGER.error("Failed to insert user in DB");
                    promise.fail(failureHandler.getMessage());
                  });

                }
              } else {
                LOGGER.error("Fetch user from DB failure : {}", handler.cause().getMessage());
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
  }

  private Future<Void> insertUserIntoDb(User user) {
    Promise<Void> promise = Promise.promise();

    JsonObject params = new JsonObject()
            .put("$1", user.getUserId())
            .put("$2", user.getEmailId())
            .put("$3", user.getFirstName())
            .put("$4", user.getLastName());
    postgresServiceImpl
            .executePreparedQuery(INSERT_USER_TABLE, params, handler -> {
              if(handler.succeeded())
              {
                LOGGER.debug("User inserted ");
                promise.complete();
              }
              else
              {
                LOGGER.debug("Something went wrong while inserting user in DB : {}", handler.cause().getMessage());
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
}

    /**
     * Returns normalised path without ID
     * <br>
     * ID would be present as path param
     *
     * @param url as String
     * @return requested endpoint as String
     */
  private String getNormalisedPath(String url) {
    LOGGER.debug("URL : {}", url);
    if (url.matches(api.getVerifyUrl())) {
      return api.getVerifyUrl();
    } else if (url.matches(api.getPoliciesUrl())) {
      return api.getPoliciesUrl();
    } else if (url.matches(api.getProviderProductPath())) {
      return api.getProviderProductPath();
    } else if (url.matches(api.getProviderListProductsPath())) {
      return api.getProviderListProductsPath();
    } else if (url.matches(api.getProviderListPurchasesPath())) {
      return api.getProviderListPurchasesPath();
    } else if (url.matches(api.getProviderProductVariantPath())) {
      return api.getProviderProductVariantPath();
    } else if (url.matches(api.getProductUserMapsPath())) {
      return api.getProductUserMapsPath();
    } else if (url.matches(api.getConsumerListDatasets())) {
      return api.getConsumerListDatasets();
    } else if (url.matches(api.getConsumerListProviders())) {
      return api.getConsumerListProviders();
    } else if (url.matches(api.getConsumerListPurchases())) {
      return api.getConsumerListPurchases();
    } else if (url.matches(api.getConsumerListProducts())) {
      return api.getConsumerListProducts();
    }
    return null;
  }

  private void processAuthFailure(RoutingContext ctx, String result) {
    if (result.contains("Not Found")) {
      LOGGER.error("Error : Item Not Found");
      LOGGER.error("Error : " + result);
      HttpStatusCode statusCode = HttpStatusCode.getByValue(404);
      generateResponse(ctx, RESOURCE_NOT_FOUND_URN, statusCode);
    } else {
      LOGGER.error("Error : Authentication Failure");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(401);
      LOGGER.error("Error : " + result);
      generateResponse(ctx, INVALID_TOKEN_URN, statusCode);
    }
  }

  private void generateResponse(RoutingContext ctx, ResponseUrn urn, HttpStatusCode statusCode) {
    String response =
        new RespBuilder()
            .withType(urn.getUrn())
            .withTitle(statusCode.getDescription())
            .withDetail(statusCode.getDescription())
            .getResponse();

    ctx.response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode.getValue())
        .end(response);
  }

 static final class UserContainer{
    User user;
 }
}
