package iudx.data.marketplace.authenticator.util;

public class Constants {
  public static final String ID = "id";
  public static final String TOKEN = "token";

  public static final String GET_USER = "select * from user_table where _id=$1::UUID;";
  public static final String INSERT_USER_TABLE =
      "insert into user_table(_id,email_id,first_name,last_name) values ($1,$2,$3,$4) returning _id;";
}
