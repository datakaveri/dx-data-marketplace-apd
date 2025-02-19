package iudx.data.marketplace.common;

import static iudx.data.marketplace.apiserver.util.Constants.*;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.authenticator.model.JwtData;
import iudx.data.marketplace.policies.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RoutingContextHelper {
  private static final Logger LOGGER = LogManager.getLogger(RoutingContextHelper.class);
  private static final String JWT_DATA = "jwtData";

  public static void setUser(RoutingContext routingContext, User user) {
    routingContext.put(USER, user);
  }

  public static User getUser(RoutingContext routingContext) {
    return routingContext.get(USER);
  }

  public static JsonObject getAuthInfo(RoutingContext routingContext) {
    return new JsonObject()
        .put(API_ENDPOINT, getRequestPath(routingContext))
        .put(HEADER_TOKEN, getToken(routingContext))
        .put(API_METHOD, getMethod(routingContext));
  }

  public static JsonObject getVerifyAuthInfo(RoutingContext routingContext) {
    return new JsonObject()
        .put(API_ENDPOINT, getRequestPath(routingContext))
        .put(HEADER_TOKEN, getVerifyToken(routingContext))
        .put(API_METHOD, getMethod(routingContext));
  }

  private static String getVerifyToken(RoutingContext routingContext) {
    String token = routingContext.request().headers().get(HEADER_BEARER_AUTHORIZATION);
    if (token.trim().split(" ").length == 2) {
      token = token.trim().split(" ")[1];
      return token;
    }
    LOGGER.error("Invalid verify token");
    throw new DxRuntimeException(
        HttpStatusCode.getByValue(401).getValue(), ResponseUrn.BAD_REQUEST_URN);
  }

  public static String getToken(RoutingContext routingContext) {
    /* token would can be of the type : Bearer <JWT-Token>, <JWT-Token> */
    /* Send Bearer <JWT-Token> if Authorization header is present */
    /* allowing both the tokens to be authenticated for now */
    /* TODO: later, 401 error is thrown if the token does not contain Bearer keyword */
    String token = routingContext.request().headers().get(HEADER_BEARER_AUTHORIZATION);
    boolean isBearerAuthHeaderPresent = token != null && token.contains(HEADER_TOKEN_BEARER);
    if (isBearerAuthHeaderPresent && token.trim().split(" ").length == 2) {
      String[] tokenWithoutBearer = token.split(HEADER_TOKEN_BEARER);
      token = tokenWithoutBearer[1].replaceAll("\\s", "");
      return token;
    }
    return routingContext.request().headers().get(HEADER_TOKEN);
  }

  public static String getMethod(RoutingContext routingContext) {
    return routingContext.request().method().toString();
  }

  public static String getRequestPath(RoutingContext routingContext) {
    return routingContext.request().path();
  }

  public static void setJwtData(RoutingContext routingContext, JwtData jwtData) {
    routingContext.put(JWT_DATA, jwtData);
  }

  public static JwtData getJwtData(RoutingContext routingContext) {
    return routingContext.get(JWT_DATA);
  }
}
