package iudx.data.marketplace.apiserver.provider.linkedAccount.util;

public class Constants {
  public static final String INSERT_MERCHANT_INFO =
      "INSERT INTO merchant_table("
          + " reference_id, phone_number, email, legal_business_name, customer_facing_business_name, account_id,"
          + " provider_id, status) "
          + " VALUES ('$1', '$2', '$3', '$4', '$5','$6', '$7', '$8');";
}
