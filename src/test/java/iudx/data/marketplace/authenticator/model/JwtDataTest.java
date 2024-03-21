package iudx.data.marketplace.authenticator.model;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.authenticator.AuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class JwtDataTest {
  JwtData jwtData;
  JsonObject jsonObject;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    jsonObject = new JsonObject()
            .put("access_token", "eyJpc3MiOiJjb3MuaXVkeC5pbyIsInR5cCI6IkpXVCIsImFsZyI6IkVTMjU2In0.eyJzdWIiOiJlMGIwYTM4NC02OWI5LTQ4MDgtYTc3MS1kN2EyMmVlY2IyNWMiLCJpc3MiOiJjb3MuaXVkeC5pbyIsImF1ZCI6InJzLXRlc3QtcG0uaXVkeC5pbyIsImV4cCI6MTcxMDEwODM4MSwiaWF0IjoxNzEwMDY1MTgxLCJpaWQiOiJyczpycy10ZXN0LXBtLml1ZHguaW8iLCJyb2xlIjoicHJvdmlkZXIiLCJjb25zIjp7fX0.ApD7IOwvbd1silv3txBpAYIRYx0C1PhRY78Xjvcmv6vbuOlvcmFWroEBNSf96b7NjDN1DH1BaNPmtLsTlSS-8A");
    JsonObject jsonObject1 = new JsonObject("{\"access_token\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJjMTc2MGY1Mi1iYWI4LTQwZTQtYjA5ZC04NmMxZDMxNDZmMTciLCJpc3MiOiJjb3MuaXVkeC5pbyIsImF1ZCI6InJzLml1ZHguaW8iLCJleHAiOjE4ODYxNDU1MTIsImlhdCI6MTY4NjEzNTUxMiwiaWlkIjoicnM6cnMuaXVkeC5pbyIsInJvbGUiOiJkZWxlZ2F0ZSIsImRpZCI6ImIyYzI3ZjNmLTI1MjQtNGE4NC04MTZlLTkxZjlhYjIzZjgzNyIsImRybCI6InByb3ZpZGVyIiwiY29ucyI6e319.MyT4yHeEvLqs7UskNi7ajwo84xrKofoUIiUOlY06X5UyL_jTIPtdxQ6HzD5-ZaLNUh0rg-qK9TzilEEB72ICNw\",\"sub\":\"c1760f52-bab8-40e4-b09d-86c1d3146f17\",\"iss\":\"cos.iudx.io\",\"aud\":\"rs.iudx.io\",\"iid\":\"rs:rs.iudx.io\",\"role\":\"delegate\",\"did\":\"b2c27f3f-2524-4a84-816e-91f9ab23f837\",\"drl\":\"provider\",\"cons\":{}}");

    jwtData = new JwtData(jsonObject1);
    jwtData.setExp(1886145512);
    jwtData.setIat(1686135512);
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test toJson method")
  public void test_toJson(VertxTestContext vertxTestContext) {
    JsonObject actual = jwtData.toJson();
    JsonObject expected = new JsonObject("{\n" +
            "  \"accessToken\" : \"eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJjMTc2MGY1Mi1iYWI4LTQwZTQtYjA5ZC04NmMxZDMxNDZmMTciLCJpc3MiOiJjb3MuaXVkeC5pbyIsImF1ZCI6InJzLml1ZHguaW8iLCJleHAiOjE4ODYxNDU1MTIsImlhdCI6MTY4NjEzNTUxMiwiaWlkIjoicnM6cnMuaXVkeC5pbyIsInJvbGUiOiJkZWxlZ2F0ZSIsImRpZCI6ImIyYzI3ZjNmLTI1MjQtNGE4NC04MTZlLTkxZjlhYjIzZjgzNyIsImRybCI6InByb3ZpZGVyIiwiY29ucyI6e319.MyT4yHeEvLqs7UskNi7ajwo84xrKofoUIiUOlY06X5UyL_jTIPtdxQ6HzD5-ZaLNUh0rg-qK9TzilEEB72ICNw\",\n" +
            "  \"aud\" : \"rs.iudx.io\",\n" +
            "  \"cons\" : { },\n" +
            "  \"did\" : \"b2c27f3f-2524-4a84-816e-91f9ab23f837\",\n" +
            "  \"drl\" : \"provider\",\n" +
            "  \"exp\" : 1886145512,\n" +
            "  \"iat\" : 1686135512,\n" +
            "  \"iid\" : \"rs:rs.iudx.io\",\n" +
            "  \"iss\" : \"cos.iudx.io\",\n" +
            "  \"role\" : \"delegate\",\n" +
            "  \"sub\" : \"c1760f52-bab8-40e4-b09d-86c1d3146f17\"\n" +
            "}");
    assertNotNull(actual);
    assertEquals(expected, actual);
    vertxTestContext.completeNow();
  }



  @Test
  @DisplayName("Test getAccess_token method")
  public void test_getAccess_token(VertxTestContext vertxTestContext) {
    String actual = jwtData.getAccessToken();
    assertEquals(
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJjMTc2MGY1Mi1iYWI4LTQwZTQtYjA5ZC04NmMxZDMxNDZmMTciLCJpc3MiOiJjb3MuaXVkeC5pbyIsImF1ZCI6InJzLml1ZHguaW8iLCJleHAiOjE4ODYxNDU1MTIsImlhdCI6MTY4NjEzNTUxMiwiaWlkIjoicnM6cnMuaXVkeC5pbyIsInJvbGUiOiJkZWxlZ2F0ZSIsImRpZCI6ImIyYzI3ZjNmLTI1MjQtNGE4NC04MTZlLTkxZjlhYjIzZjgzNyIsImRybCI6InByb3ZpZGVyIiwiY29ucyI6e319.MyT4yHeEvLqs7UskNi7ajwo84xrKofoUIiUOlY06X5UyL_jTIPtdxQ6HzD5-ZaLNUh0rg-qK9TzilEEB72ICNw", actual);
    assertNotNull(actual);
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test getIat method")
  public void test_getIat(VertxTestContext vertxTestContext) {
    Integer actual = jwtData.getIat();
    assertEquals(1686135512, actual);
    vertxTestContext.completeNow();
  }
}
