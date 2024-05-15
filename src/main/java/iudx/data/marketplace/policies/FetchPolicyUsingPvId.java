package iudx.data.marketplace.policies;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.postgres.PostgresService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static iudx.data.marketplace.policies.util.Constants.FETCH_POLICY;
import static iudx.data.marketplace.policies.util.Constants.FETCH_PRODUCT_VARIANT;
import static iudx.data.marketplace.product.util.Constants.RESULTS;
public class FetchPolicyUsingPvId {
  private static final Logger LOG = LoggerFactory.getLogger(FetchPolicyUsingPvId.class);
  private PostgresService postgresService;

  FetchPolicyUsingPvId(PostgresService postgresService) {
    this.postgresService = postgresService;
  }

  public Future<JsonObject> checkIfPolicyExists(String productVariantId, User user) {
    Promise<JsonObject> promise = Promise.promise();

    String consumerRsUrl = user.getResourceServerUrl();
    String consumerEmailId = user.getEmailId();
    String fetchProductVariantQuery = FETCH_PRODUCT_VARIANT.replace("$1", productVariantId);

    LOG.info("checking if product variant : {} exists", productVariantId);

    postgresService.executeQuery(
        fetchProductVariantQuery,
        pvExistenceHandler -> {
          if (pvExistenceHandler.succeeded()) {
            /* check if result is empty */
            if (pvExistenceHandler.result().getJsonArray(RESULTS).isEmpty()) {
              promise.fail(
                  new RespBuilder()
                      .withType(HttpStatusCode.NOT_FOUND.getValue())
                      .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                      .withDetail("Product variant not found")
                      .getResponse());
            } else {
              LOG.debug(
                  "response from db : {}",
                  pvExistenceHandler.result().getJsonArray(RESULTS).encodePrettily());
              JsonObject result =
                  pvExistenceHandler.result().getJsonArray(RESULTS).getJsonObject(0);

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
                postgresService.checkPolicy(
                    FETCH_POLICY,
                    params,
                    policyExistenceQuery -> {
                      if (policyExistenceQuery.succeeded()) {
                        promise.complete(policyExistenceQuery.result());
                      } else {
                        promise.fail(policyExistenceQuery.cause().getMessage());
                      }
                    });

              } else {
                LOG.error("RS URL of consumer not equal to resource");
                promise.fail(
                    new RespBuilder()
                        .withType(HttpStatusCode.FORBIDDEN.getValue())
                        .withTitle(ResponseUrn.FORBIDDEN_URN.getUrn())
                        .withDetail(ResponseUrn.FORBIDDEN_URN.getMessage())
                        .getResponse());
              }
            }
          } else {
            LOG.error(
                "Failure while fetching product variant info : {}",
                pvExistenceHandler.cause().getMessage());
            promise.fail(
                new RespBuilder()
                    .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                    .withTitle(ResponseUrn.DB_ERROR_URN.getUrn())
                    .withDetail(ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
                    .getResponse());
          }
        });

    return promise.future();
  }
}
