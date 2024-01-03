package iudx.data.marketplace.policies.util;

public class Constants {
    public static final int DB_RECONNECT_ATTEMPTS = 2;
    public static final long DB_RECONNECT_INTERVAL_MS = 10;
    public static final String CHECK_EXISTING_POLICY =
            "SELECT _id,constraints FROM policy "
                    + "WHERE resource_id =$1::UUID AND provider_id = $2::UUID  AND status = $3::policy_status"
                    + " AND consumer_email_id = $4::text AND expiry_at > now()";

    public static final String ENTITY_TABLE_CHECK =
            "Select _id,provider_id,item_type,resource_server_url from resource_entity where _id = ANY ($1::UUID[]);";
    public static final String INSERT_ENTITY_TABLE =
            "insert into resource_entity(_id,provider_id,resource_group_id,item_type,resource_server_url)"
                    + " values ($1,$2,$3,$4,$5);";

    public static final String CREATE_POLICY_QUERY =
            "insert into policy (user_emailid, item_id, owner_id,expiry_at, constraints,status) "
                    + "values ($1, $2, $3, $4, $5,'ACTIVE') returning *;";
  public static final String GET_POLICY_4_PROVIDER_QUERY =
      "SELECT P._id AS \"policyId\", P.resource_id AS \"resourceId\",\n"
          + "RE.resource_server_url AS \"resourceServerUrl\",\n"
          + "RE.resource_name AS \"resourceName\",\n"
          + "P.product_variant_id AS \"productVariantId\",\n"
          + "RE.accesspolicy AS \"accessPolicy\",\n"
          + "P.consumer_email_id AS \"consumerEmailId\",\n"
          + "U.first_name AS \"consumerFirstName\",\n"
          + "U.last_name AS \"consumerLastName\", U._id AS \"consumerId\",\n"
          + "P.status AS \"status\", P.expiry_at AS \"expiryAt\",\n"
          + "P.constraints AS \"constraints\" FROM policy AS P \n"
          + "LEFT JOIN user_table AS U\n"
          + "ON P.consumer_email_id = U.email_id \n"
          + "INNER JOIN resource_entity AS RE\n"
          + "ON RE._id = P.resource_id\n"
          + "AND P.provider_id = $1::uuid \n"
          + "AND RE.resource_server_url = $2;";
  public static final String GET_POLICY_4_CONSUMER_QUERY =
      "SELECT P._id AS \"policyId\", P.resource_id AS \"resourceId\",\n"
          + "RE.resource_server_url AS \"resourceServerUrl\",\n"
          + "RE.resource_name AS \"resourceName\",\n"
          + "P.product_variant_id AS \"productVariantId\",\n"
          + "RE.accesspolicy AS \"accessPolicy\",\n"
          + "P.provider_id AS \"providerId\", U.first_name AS \"providerFirstName\",\n"
          + "U.last_name AS \"providerLastName\", U.email_id AS \"providerEmailId\",\n"
          + "P.status as \"status\", P.expiry_at AS \"expiryAt\",\n"
          + "P.constraints AS \"constraints\" \n"
          + "FROM policy AS P \n"
          + "INNER JOIN user_table AS U\n"
          + "ON P.provider_id = U._id \n"
          + "INNER JOIN resource_entity AS RE\n"
          + "ON RE._id = P.resource_id\n"
          + "AND P.consumer_email_id = $1  \n"
          + "AND RE.resource_server_url = $2;";
    public static final String DELETE_POLICY_QUERY =
            "UPDATE policy SET status='DELETED' "
                    + "WHERE _id = $1::uuid AND expiry_at > NOW() RETURNING _id";
    public static final String CHECK_IF_POLICY_PRESENT_QUERY =
            "SELECT p.provider_id, p.status, r.resource_server_url"
                    + " FROM policy p"
                    + " INNER JOIN resource_entity r ON p.resource_id = r._id"
                    + " WHERE p._id = $1;";
}
