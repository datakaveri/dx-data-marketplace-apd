package iudx.data.marketplace.apiserver.provider.linkedAccount.util;

public class Constants {
  public static final String INSERT_MERCHANT_INFO =
      "INSERT INTO merchant_table("
          + " reference_id, phone_number, email, legal_business_name, customer_facing_business_name, account_id,"
          + " provider_id, status, rzp_account_product_id) "
          + " VALUES ('$1', '$2', '$3', '$4', '$5','$6', '$7', '$8', '$9');";

public static final String FETCH_MERCHANT_INFO =
        "SELECT account_id, rzp_account_product_id, status FROM merchant_table WHERE provider_id = '$1'";

public static   String UPDATE_LINKED_ACCOUNT_STATUS =
          "UPDATE merchant_table SET status = 'ACTIVATED' WHERE provider_id = '$1' RETURNING reference_id";

  public static final String ACCOUNT_TYPE = "route";
  public static final String FAILURE_MESSAGE = "User registration incomplete : ";


}
