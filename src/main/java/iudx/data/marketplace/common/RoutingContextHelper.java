package iudx.data.marketplace.common;

import static iudx.data.marketplace.apiserver.util.Constants.*;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
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
        .put(HEADER_TOKEN, getToken(routingContext))
        .put(API_METHOD, getMethod(routingContext));
  }

  public static String getToken(RoutingContext routingContext) {
    /* token would can be of the type : Bearer <JWT-Token>, <JWT-Token> */
    /* Send Bearer <JWT-Token> if Authorization header is present */
    /* allowing both the tokens to be authenticated for now */
    /* TODO: later, 401 error is thrown if the token does not contain Bearer keyword */
    String token = routingContext.request().headers().get(AUTHORIZATION_KEY);
    boolean isValidBearerToken = token != null && token.trim().split(" ").length == 2;
    boolean isBearerAuthHeaderPresent = isValidBearerToken && (token.contains(HEADER_TOKEN_BEARER));
    boolean isKcTokenPresent = isValidBearerToken && (token.contains("bearer"));
    String[] tokenWithoutBearer = new String[] {};
    if (isValidBearerToken) {
      if (isBearerAuthHeaderPresent) {
        tokenWithoutBearer = (token.split(HEADER_TOKEN_BEARER));
      } else if (isKcTokenPresent) {
        tokenWithoutBearer = (token.split("bearer"));
      }
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