package iudx.data.marketplace.razorpay;

import com.razorpay.Account;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static iudx.data.marketplace.product.util.Constants.PRICE;
import static iudx.data.marketplace.product.util.Constants.STATUS;
import static iudx.data.marketplace.razorpay.Constants.*;

public class RazorPayServiceImpl implements RazorPayService {

  private static Logger LOGGER = LogManager.getLogger(RazorPayServiceImpl.class);
  RazorpayClient razorpayClient;

  RazorPayServiceImpl(RazorpayClient razorpayClient, PostgresService postgresService) {
    this.razorpayClient = razorpayClient;
  }

  @Override
  public Future<JsonObject> createOrder(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();

    Integer amountInPaise = (int) (request.getDouble(PRICE) * 100);
    JSONObject orderRequest =
        new JSONObject()
            .put(AMOUNT, amountInPaise)
            .put(CURRENCY, INR)
            .put(RECEIPT, "receipt") // TODO: what is receipt?
            .put(
                TRANFERS,
                new JSONArray()
                    .put(
                        0,
                        new JSONObject()
                            .put(ACCOUNT, request.getString(ACCOUNT_ID))
                            .put(AMOUNT, amountInPaise)
                            .put(CURRENCY, INR)
                            .put(
                                NOTES,
                                new JSONObject()
                                    .put("VARIANT", request.getString("product_variant_name"))
                                    .put("PRODUCT", request.getString("product_id"))
                                    .put("PROVIDER", request.getString("provider_id")))
                            .put(ON_HOLD, ZERO)));

    try {
      LOGGER.debug("order request at razorpay: {} ", orderRequest);
      Order order = razorpayClient.orders.create(orderRequest);
      JSONObject accountRequest = new JSONObject();
      Account account = razorpayClient.account.create(accountRequest);
      String status = order.get(STATUS);
      if (!status.equals(CREATED)) {
        throw new DxRuntimeException(
            400,
            ResponseUrn.ORDER_NOT_CREATED,
            ("Order creation returned with status : " + status));
      }
      JSONArray transfersArray = order.get(TRANFERS);
      JSONObject transferResponse = transfersArray.getJSONObject(0);
      JSONObject responseError = transferResponse.getJSONObject(ERROR);
      LOGGER.debug("error response from razorpay : {}", responseError);

      Object reason =  responseError.get(REASON);
      if(reason!=null) {
        LOGGER.error("razorpay error : ", responseError);
        String message;
        switch ((String) reason) {
          case "amount_less_than_minimum_amount":
          case "authentication_failed":
          case "gateway_technical_error":
          case "invalid_response_from_gateway":
          case "server_error":
            message = "RazorPay Error - Try again later";
            break;
          case "invalid_request":
            message = "Invalid Request Made to RazorPay";
            break;
          default:
            message = "Unexpected Error while order creation - Try again later";
        }
        throw new DxRuntimeException(400, ResponseUrn.ORDER_CREATION_FAILED, message);
      }

      LOGGER.info("order response razorpay : {}", order.toJson());

      /* Converting org.json.JSONObject to Java.Util.String to io.vertx.core.json.JsonObject */
      promise.complete(new JsonObject(order.toJson().toString()));
    } catch (RazorpayException e) {
      LOGGER.error("order created on razorpay failed with exception : {}", e.getMessage());
      RespBuilder respBuilder =
          new RespBuilder()
              .withType(ResponseUrn.ORDER_CREATION_FAILED.getUrn())
              .withTitle("RazorPay Error")
              .withDetail(e.getMessage());
      promise.fail(respBuilder.getResponse());
    } catch (DxRuntimeException e) {
      LOGGER.error("order not created on razorpay, : {}", e.getMessage());
      RespBuilder respBuilder =
          new RespBuilder()
              .withType(e.getUrn().getUrn())
              .withTitle("RazorPay Error")
              .withDetail(e.getUrn().getMessage());
      promise.fail(respBuilder.getResponse());
    } catch (JSONException e) {
      LOGGER.error("parse execption during order creation, : {}", e.getMessage());

      RespBuilder respBuilder =
          new RespBuilder()
              .withType(ResponseUrn.ORDER_CREATION_FAILED.getUrn())
              .withTitle("RazorPay Error")
              .withDetail(e.getMessage());
      promise.fail(respBuilder.getResponse());
    }

    return promise.future();
  }
}
