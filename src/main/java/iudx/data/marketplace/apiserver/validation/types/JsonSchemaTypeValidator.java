package iudx.data.marketplace.apiserver.validation.types;

import static iudx.data.marketplace.common.ResponseUrn.INVALID_PAYLOAD_FORMAT_URN;
import static iudx.data.marketplace.common.ResponseUrn.SCHEMA_READ_ERROR_URN;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.apiserver.util.RequestType;
import iudx.data.marketplace.common.HttpStatusCode;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JsonSchemaTypeValidator implements Validator {
  private static final Logger LOGGER = LogManager.getLogger(JsonSchemaTypeValidator.class);
  private static String pkg = Validator.class.getPackageName();
  private static final String PACKAGE_NAME = "/" + pkg.replace(".", "/");
  private final JsonObject body;
  private final RequestType requestType;

  public JsonSchemaTypeValidator(JsonObject body, RequestType requestType) {
    this.body = body;
    this.requestType = requestType;
    isValid();
  }

  public static JsonNode loadResource(final String name) throws IOException {
    return JsonLoader.fromResource(name);
  }

  public static JsonNode loadString(final String obj) throws IOException {
    return JsonLoader.fromString(obj);
  }

  @Override
  public boolean isValid() {
    boolean isValid;
    try {
      isValid = validateJson(body, requestType);
    } catch (IOException | ProcessingException e) {
      throw new DxRuntimeException(
          failureCode(), SCHEMA_READ_ERROR_URN, failureMessage(body.encode()));
    }
    if (!isValid) {
      throw new DxRuntimeException(
          failureCode(), INVALID_PAYLOAD_FORMAT_URN, INVALID_PAYLOAD_FORMAT_URN.getMessage());
    } else {
      return true;
    }
  }

  private boolean validateJson(JsonObject body, RequestType requestType)
      throws IOException, ProcessingException, DxRuntimeException {
    LOGGER.debug(body);
    boolean isValid;
    String schemaPath = PACKAGE_NAME + "/".concat(requestType.getFilename()).concat("_schema.json");

    LOGGER.debug(schemaPath);
    final JsonSchema schema;
//    try {
//      final JsonNode sc = loadResource(schemaPath);
//    } catch (Exception e) {
//      LOGGER.error("Validation Error {}", e.getMessage());
//    }
    final JsonNode schemaNode = loadResource(schemaPath);
    LOGGER.debug(schemaNode.asText());
    final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
    schema = factory.getJsonSchema(schemaNode);

    try {
      JsonNode jsonobj = loadString(body.toString());
      ProcessingReport report = schema.validate(jsonobj);
      report.forEach(
          x -> {
            if (x.getLogLevel().toString().equalsIgnoreCase("error")) {
              LOGGER.error(x.getMessage());
              throw new DxRuntimeException(
                  failureCode(), INVALID_PAYLOAD_FORMAT_URN, x.getMessage());
            }
          });
      isValid = report.isSuccess();
    } catch (IOException | ProcessingException e) {
      isValid = false;
    }
    return isValid;
  }

  @Override
  public int failureCode() {
    return HttpStatusCode.BAD_REQUEST.getValue();
  }

  @Override
  public String failureMessage() {
    return INVALID_PAYLOAD_FORMAT_URN.getMessage();
  }
}
