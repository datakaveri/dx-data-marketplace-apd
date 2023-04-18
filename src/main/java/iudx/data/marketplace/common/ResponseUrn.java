package iudx.data.marketplace.common;

import java.util.stream.Stream;

public enum ResponseUrn {
  SUCCESS_URN("urn:dx:dm:success", "Success"),
  INVALID_SYNTAX_URN("urn:dx:dm:invalidSyntax", "Invalid Syntax"),
  INVALID_SCHEMA_URN("urn:dx:dm:InvalidSchema", "Invalid Schema"),
  INVALID_PARAM_URN("urn:dx:dm:invalidParamameter", "Invalid parameter passed"),
  INVALID_ATTR_PARAM_URN("urn:dx:dm:invalidAttributeParam", "Invalid attribute param"),
  INVALID_ATTR_VALUE_URN("urn:dx:dm:invalidAttributeValue", "Invalid attribute value"),
  INVALID_OPERATION_URN("urn:dx:dm:invalidOperation", "Invalid operation"),
  UNAUTHORIZED_ENDPOINT_URN(
      "urn:dx:dm:unauthorizedEndpoint", "Access to endpoint is not available"),
  UNAUTHORIZED_RESOURCE_URN(
      "urn,dx:dm:unauthorizedResource", "Access to resource is not available"),
  EXPIRED_TOKEN_URN("urn:dx:dm:expiredAuthorizationToken", "Token has expired"),
  MISSING_TOKEN_URN("urn:dx:dm:missingAuthorizationToken", "Token needed and not present"),
  INVALID_TOKEN_URN("urn:dx:dm:invalidAuthorizationToken", "Token is invalid"),
  RESOURCE_NOT_FOUND_URN("urn:dx:dm:resourceNotFound", "Document of given id does not exist"),
  RESOURCE_ALREADY_EXISTS_URN("urn:dx:dm:resourceAlreadyExists", "Document of given id already exists"),

  LIMIT_EXCEED_URN(
      "urn:dx:dm:requestLimitExceeded", "Operation exceeds the default value of limit"),

  PAYLOAD_TOO_LARGE_URN("urn:dx:dm:payloadTooLarge", "Response size exceeds limit"),

  // extra urn
  INTERNAL_SERVER_ERR_URN("urn:dx:dm:internalServerError", "Internal Server Error"),
  INVALID_ID_VALUE_URN("urn:dx:dm:invalidIdValue", "Invalid id"),
  INVALID_NAME_URN("urn:dx:dm:invalidNameValue","Invalid name"),
  INVALID_PAYLOAD_FORMAT_URN(
      "urn:dx:dm:invalidPayloadFormat", "Invalid json format in post request [schema mismatch]"),
  INVALID_PARAM_VALUE_URN("urn:dx:dm:invalidParamameterValue", "Invalid parameter value passed"),
  BAD_REQUEST_URN("urn:dx:dm:badRequest", "bad request parameter"),
  INVALID_HEADER_VALUE_URN("urn:dx:dm:invalidHeaderValue", "Invalid header value"),
  DB_ERROR_URN("urn:dx:dm:DatabaseError", "Database error"),
  QUEUE_ERROR_URN("urn:dx:dm:QueueError", "Queue error"),
  INVALID_DATASET_URN("urn:dx:dm:InvalidDatasetID", "Dataset ID is invalid"),

  BACKING_SERVICE_FORMAT_URN(
      "urn:dx:dm:backend", "format error from backing service [cat,auth etc.]"),
  SCHEMA_READ_ERROR_URN("urn:dx:dm:readError", "Fail to read file"),
  YET_NOT_IMPLEMENTED_URN("urn:dx:dm:general", "urn yet not implemented in backend verticle.");

  private final String urn;
  private final String message;

  ResponseUrn(String urn, String message) {
    this.urn = urn;
    this.message = message;
  }

  public String getUrn() {
    return urn;
  }

  public String getMessage() {
    return message;
  }

  public static ResponseUrn fromCode(final String urn) {
    return Stream.of(values())
        .filter(v -> v.urn.equalsIgnoreCase(urn))
        .findAny()
        .orElse(YET_NOT_IMPLEMENTED_URN); // if backend service dont respond with urn
  }

  public String toString() {
    return "[" + urn + " : " + message + " ]";
  }
}
