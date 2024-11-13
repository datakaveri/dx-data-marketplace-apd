package iudx.data.marketplace.policies.util;

public class Constants {
  public static final String CHECK_EXISTING_POLICY =
      "SELECT _id,constraints FROM policy "
          + "WHERE resource_id =$1::UUID AND provider_id = $2::UUID  AND status = $3::policy_status"
          + " AND consumer_email_id = $4::text AND expiry_at > now()";

  public static final String CHECK_POLICY_FROM_ORDER_ID =
      " SELECT _id,constraints FROM policy  "
          + " WHERE resource_id =$1::UUID  "
          + " AND provider_id = $2::UUID  AND status = $3::policy_status  "
          + " AND consumer_email_id = $4::text  "
          + " AND expiry_at > now() "
          + " AND invoice_id = ( SELECT I._id FROM invoice AS I WHERE I.order_id = $5::text)";

  public static final String CREATE_POLICY_QUERY =
      "INSERT INTO public.policy( "
          + " _id, resource_id, invoice_id, constraints, provider_id, consumer_email_id, expiry_at, "
          + " status, product_variant_id) "
          + " VALUES ('$1', '$2', '$3', '$4'::JSON, '$5', '$6', '$7', '$8', '$9') RETURNING _id;";

  public static final String GET_POLICY_4_PROVIDER_QUERY =
      "SELECT P._id AS \"policyId\", P.resource_id AS \"resourceId\",\n"
          + "RE.resource_server AS \"resourceServerUrl\",\n"
          + "P.invoice_id AS \"purchaseId\",\n"
          + "RE.resource_name AS \"resourceName\",\n"
          + "P.product_variant_id AS \"productVariantId\",\n"
          + "RE.accesspolicy AS \"accessPolicy\",\n"
          + "P.consumer_email_id AS \"consumerEmailId\",\n"
          + "U.first_name AS \"consumerFirstName\",\n"
          + "U.last_name AS \"consumerLastName\", U._id AS \"consumerId\",\n"
          + "P.status AS \"status\", P.expiry_at AS \"expiryAt\",\n"
          + "P.constraints AS \"constraints\" "
          + " ,P.modified_at AS \"updatedAt\" "
          + " ,P.created_at AS \"createdAt\" "
          + " FROM policy AS P \n"
          + "LEFT JOIN user_table AS U\n"
          + "ON P.consumer_email_id = U.email_id \n"
          + "INNER JOIN resource_entity AS RE\n"
          + "ON RE._id = P.resource_id\n"
          + "AND P.provider_id = $1::uuid \n"
          + "AND RE.resource_server = $2 "
          + " ORDER BY P.modified_at DESC";
  public static final String GET_POLICY_4_CONSUMER_QUERY =
      "SELECT P._id AS \"policyId\", P.resource_id AS \"resourceId\",\n"
          + "RE.resource_server AS \"resourceServerUrl\",\n"
          + "P.invoice_id AS \"purchaseId\",\n"
          + "RE.resource_name AS \"resourceName\",\n"
          + "P.product_variant_id AS \"productVariantId\",\n"
          + "RE.accesspolicy AS \"accessPolicy\",\n"
          + "P.provider_id AS \"providerId\", U.first_name AS \"providerFirstName\",\n"
          + "U.last_name AS \"providerLastName\", U.email_id AS \"providerEmailId\",\n"
          + "P.status as \"status\", P.expiry_at AS \"expiryAt\",\n"
          + "P.constraints AS \"constraints\"  \n"
          + " ,P.modified_at AS \"updatedAt\" "
          + " ,P.created_at AS \"createdAt\" "
          + "FROM policy AS P \n"
          + "INNER JOIN user_table AS U\n"
          + "ON P.provider_id = U._id \n"
          + "INNER JOIN resource_entity AS RE\n"
          + "ON RE._id = P.resource_id\n"
          + "AND P.consumer_email_id = $1  \n"
          + "AND RE.resource_server = $2 "
          + " ORDER BY P.modified_at DESC";
  public static final String DELETE_POLICY_QUERY =
      "UPDATE policy SET status='DELETED' "
          + "WHERE _id = $1::uuid AND expiry_at > NOW() RETURNING _id";
  public static final String CHECK_IF_POLICY_PRESENT_QUERY =
      "SELECT p.provider_id, p.status, r.resource_server"
          + " FROM policy p"
          + " INNER JOIN resource_entity r ON p.resource_id = r._id"
          + " WHERE p._id = $1;";

  public static final String GET_REQUIRED_INFO_QUERY =
      "SELECT DISTINCT I._id AS \"invoiceId\", I.product_variant_id AS \"productVariantId\", I.expiry, "
          + " PV.provider_id AS \"providerId\", U.email_id AS \"consumerEmailId\", "
          + " PV.resource_info AS \"resourceInfo\" , "
          + " U.email_id AS \"emailId\", U.first_name AS \"firstName\", U.last_name AS \"lastName\", "
          + " R.resource_server AS \"resourceServerUrl\""
          + " FROM invoice AS I "
          + " INNER JOIN product_variant AS PV "
          + " ON I.product_variant_id = PV._id "
          + " INNER JOIN user_table AS U "
          + " ON I.consumer_id = U._id "
          + " INNER JOIN resource_entity  R "
          + " ON PV.provider_id = R.provider_id "
          + " WHERE payment_status = 'SUCCEEDED' "
          + " AND order_id = '$1';";

  public static final String FETCH_PRODUCT_VARIANT =
      " SELECT \"resourceServerUrl\", \"resources\" "
          + "FROM product_variant_view "
          + "WHERE \"productVariantId\" = '$1'  "
          + "AND \"productVariantStatus\" = 'ACTIVE'";

  public static final String FETCH_POLICY =
      " SELECT DISTINCT resource_id AS \"resources\" FROM policy "
          + "WHERE resource_id = ANY($1) "
          + "AND status = 'ACTIVE'  "
          + " AND consumer_email_id = $2"
          + " AND expiry_at > NOW(); ";
}
