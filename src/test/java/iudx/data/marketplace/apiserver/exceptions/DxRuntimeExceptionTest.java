package iudx.data.marketplace.apiserver.exceptions;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.common.ResponseUrn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class DxRuntimeExceptionTest {

  @Test
  @DisplayName("Test creating instance")
  public void testCreatingInstance(VertxTestContext testContext) {
    DxRuntimeException exception = new DxRuntimeException(400, ResponseUrn.SCHEMA_READ_ERROR_URN);

    assertEquals(exception.getStatusCode(), 400);
    assertEquals(exception.getUrn(), ResponseUrn.SCHEMA_READ_ERROR_URN);
    assertEquals(exception.getMessage(), ResponseUrn.SCHEMA_READ_ERROR_URN.getMessage());
    testContext.completeNow();
  }

  @Test
  @DisplayName("Test creating instance with message")
  public void testCreatingInstanceWithMessage(VertxTestContext testContext) {
    DxRuntimeException exception = new DxRuntimeException(400, ResponseUrn.SCHEMA_READ_ERROR_URN, "New Message");

    assertEquals(exception.getStatusCode(), 400);
    assertEquals(exception.getUrn(), ResponseUrn.SCHEMA_READ_ERROR_URN);
    assertEquals(exception.getMessage(), "New Message");
    testContext.completeNow();
  }

  @Test
  @DisplayName("Test creating instance with Throwable")
  public void testCreatingInstanceWithThrowable(VertxTestContext testContext) {
    Throwable cause =new Throwable("Failure Message");
    DxRuntimeException exception = new DxRuntimeException(400, ResponseUrn.SCHEMA_READ_ERROR_URN, cause);

    assertEquals(exception.getStatusCode(), 400);
    assertEquals(exception.getUrn(), ResponseUrn.SCHEMA_READ_ERROR_URN);
    assertEquals(exception.getMessage(), cause.getMessage());
    testContext.completeNow();
  }
}
