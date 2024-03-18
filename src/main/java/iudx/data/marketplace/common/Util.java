package iudx.data.marketplace.common;

import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.apiserver.util.Role;
import iudx.data.marketplace.policies.User;

import java.util.List;

public class Util {
  public static final List<String> userInfo = List.of("FirstName", "LastName", "Id", "EmailId");

  /**
   * Removes entries like consumerFirstName, providerEmailId from the json object
   *
   * @param role provider or consumer
   * @param entries JsonObject
   * @return updated Json Object
   */
  private JsonObject removeValues(Role role, JsonObject entries) {
    String userRole = role.getRole();
    for (String valueToBeRemoved : userInfo) {
      entries.remove(userRole.concat(valueToBeRemoved));
    }
    return entries;
  }

  /**
   * Generates consumer or provider json object that can be used to display in the consumer response
   *
   * @param user Provider or consumer object
   * @return JsonObject with user related information like firstName, lastName, emailId, ID
   */
  public JsonObject generateUserJson(User user) {
    JsonObject userJson =
        new JsonObject()
            .put(
                user.getUserRole().getRole(),
                new JsonObject()
                    .put("email", user.getEmailId())
                    .put(
                        "name",
                        new JsonObject()
                            .put("firstName", user.getFirstName())
                            .put("lastName", user.getLastName()))
                    .put("id", user.getUserId()));
    return userJson;
  }

  /**
   * Generates JsonObject user JsonObject by extracting emailId, firstName, lastName, Id specific to
   * consumer or provider from a database row as JsonObjects to
   *
   * @param row DB row entry as JsonObject
   * @param role Consumer or provider
   * @return a consumer or provider object with a specific structure
   */
  public JsonObject getUserJsonFromRowEntry(JsonObject row, Role role) {
    String user = role.getRole();
    JsonObject providerJson =
        new JsonObject()
            .put(
                user,
                new JsonObject()
                    .put("email", row.getString(user + "EmailId"))
                    .put(
                        "name",
                        new JsonObject()
                            .put("firstName", row.getString(user + "FirstName"))
                            .put("lastName", row.getString(user + "LastName")))
                    .put("id", row.getString(user + "Id")));
    removeValues(role, row);
    return providerJson;
  }

  /**
   * Gets product related info from DB row
   *
   * @param row Database row
   * @return creates a product json object with a given structure
   */
  public JsonObject getProductInfo(JsonObject row) {
    JsonObject productJson =
        new JsonObject()
            .put(
                "product",
                new JsonObject()
                    .put("productId", row.getString("productId"))
                    .put("productVariantId", row.getString("productVariantId"))
                    .put("resources", row.getJsonArray("resources"))
                    .put("productVariantName", row.getString("productVariantName"))
                    .put("price", row.getFloat("price"))
                    .put("expiryInMonths", row.getInteger("expiryInMonths")));

    row.remove("productId");
    row.remove("productVariantId");
    row.remove("resources");
    row.remove("price");
    row.remove("expiryInMonths");
    row.remove("productVariantName");
    return productJson;
  }
}
