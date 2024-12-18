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

  public static JsonObject getAuthInfo(RoutingContext routingContext){
    return
        new JsonObject()
        .put(API_ENDPOINT, getRequestPath(routingContext))
        .put(HEADER_TOKEN, getToken(routingContext))
        .put(API_METHOD, getMethod(routingContext));
  }

  public static String getToken(RoutingContext routingContext)
  {
    return routingContext.request().headers().get(HEADER_BEARER_AUTHORIZATION);
  }

  public static String getMethod(RoutingContext routingContext)
  {
    return routingContext.request().method().toString();
  }

  public static String getRequestPath(RoutingContext routingContext)
  {
    return routingContext.request().path();
  }

  public static void setJwtData(RoutingContext routingContext, JwtData jwtData)
  {
    routingContext.put(JWT_DATA, jwtData);
  }

  public static JwtData getJwtData(RoutingContext routingContext)
  {
    return routingContext.get(JWT_DATA);
  }



}
