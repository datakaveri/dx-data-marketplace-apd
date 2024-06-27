package iudx.data.marketplace.policies;

import static iudx.data.marketplace.policies.util.Constants.FETCH_POLICY;
import static iudx.data.marketplace.policies.util.Constants.FETCH_PRODUCT_VARIANT;
import static iudx.data.marketplace.product.util.Constants.RESULTS;
import static iudx.data.marketplace.product.util.Constants.TYPE;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.postgresql.PostgresqlService;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetchPolicyUsingPvId {
  private static final Logger LOG = LoggerFactory.getLogger(FetchPolicyUsingPvId.class);
  private PostgresqlService postgresService;

  public FetchPolicyUsingPvId(PostgresqlService postgresService) {
    this.postgresService = postgresService;
  }

  public Future<JsonObject> checkIfPolicyExists(String productVariantId, User user) {

    String consumerRsUrl = user.getResourceServerUrl();
    String consumerEmailId = user.getEmailId();
    String fetchProductVariantQuery = FETCH_PRODUCT_VARIANT.replace("$1", productVariantId);

    LOG.info("checking if product variant : {} exists", productVariantId);

    Future<JsonObject> future =
        postgresService
            .executeQuery(fetchProductVariantQuery)
            .compose(
                dbResult -> {
                    /* check if result is empty */
                  if (dbResult.getJsonArray(RESULTS).isEmpty()) {
                      return Future.failedFuture(
                        new RespBuilder()
                            .withType(HttpStatusCode.NOT_FOUND.getValue())
                            .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                            .withDetail("Product variant not found")
                            .getResponse());
                  }

                    LOG.info("response from db : {}", dbResult.getJsonArray(RESULTS).encode());
                  JsonObject result = dbResult.getJsonArray(RESULTS).getJsonObject(0);

                  /*get rs url of a resource from product variant */
                  String resourceServerUrl = result.getString("resourceServerUrl");

                  /* get resource ids present for a product variant */
                  List<String> resourceIds =
                      result.getJsonArray("resources").stream()
                          .map(e -> JsonObject.mapFrom(e).getString("id"))
                          .collect(Collectors.toList());
                  LOG.debug("resourceIds : {}", resourceIds);

                  boolean isRsUrlEqual = resourceServerUrl.equalsIgnoreCase(consumerRsUrl);
                  if (isRsUrlEqual) {

                    JsonObject params =
                        new JsonObject().put("$1", resourceIds).put("$2", consumerEmailId);

                    LOG.info("Checking if policy is existing...");
                    return postgresService.checkPolicy(FETCH_POLICY, params);

                  } else {
                    LOG.error("RS URL of consumer not equal to resource");
                    return Future.failedFuture(
                        new RespBuilder()
                            .withType(HttpStatusCode.FORBIDDEN.getValue())
                            .withTitle(ResponseUrn.FORBIDDEN_URN.getUrn())
                            .withDetail(ResponseUrn.FORBIDDEN_URN.getMessage())
                            .getResponse());
                  }
                });
    if (future.failed() && !future.cause().getMessage().contains(TYPE)) {
      future =
          Future.failedFuture(
              new RespBuilder()
                  .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                  .withTitle(ResponseUrn.DB_ERROR_URN.getUrn())
                  .withDetail(ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
                  .getResponse());
    }

    return future;
  }
}
