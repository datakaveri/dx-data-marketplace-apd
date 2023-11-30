package iudx.data.marketplace.common;

import java.util.stream.Stream;

public enum ResponseUrn {
  SUCCESS_URN("urn:dx:dmp:success", "Success"),
  INVALID_SYNTAX_URN("urn:dx:dmp:invalidSyntax", "Invalid Syntax"),
  INVALID_SCHEMA_URN("urn:dx:dmp:InvalidSchema", "Invalid Schema"),
  INVALID_PARAM_URN("urn:dx:dmp:invalidParamameter", "Invalid parameter passed"),
  INVALID_ATTR_PARAM_URN("urn:dx:dmp:invalidAttributeParam", "Invalid attribute param"),
  INVALID_ATTR_VALUE_URN("urn:dx:dmp:invalidAttributeValue", "Invalid attribute value"),
  INVALID_OPERATION_URN("urn:dx:dmp:invalidOperation", "Invalid operation"),
  UNAUTHORIZED_ENDPOINT_URN(
      "urn:dx:dmp:unauthorizedEndpoint", "Access to endpoint is not available"),
  UNAUTHORIZED_RESOURCE_URN(
      "urn,dx:dmp:unauthorizedResource", "Access to resource is not available"),
  EXPIRED_TOKEN_URN("urn:dx:dmp:expiredAuthorizationToken", "Token has expired"),
  MISSING_TOKEN_URN("urn:dx:dmp:missingAuthorizationToken", "Token needed and not present"),
  INVALID_TOKEN_URN("urn:dx:dmp:invalidAuthorizationToken", "Token is invalid"),
  RESOURCE_NOT_FOUND_URN("urn:dx:dmp:resourceNotFound", "Document of given id does not exist"),
  RESOURCE_ALREADY_EXISTS_URN(
      "urn:dx:dmp:resourceAlreadyExists", "Document of given id already exists"),

  LIMIT_EXCEED_URN(
      "urn:dx:dmp:requestLimitExceeded", "Operation exceeds the default value of limit"),

  PAYLOAD_TOO_LARGE_URN("urn:dx:dmp:payloadTooLarge", "Response size exceeds limit"),

  // extra urn
  INTERNAL_SERVER_ERR_URN("urn:dx:dmp:internalServerError", "Internal Server Error"),
  INVALID_ID_VALUE_URN("urn:dx:dmp:invalidIdValue", "Invalid id"),
  INVALID_NAME_URN("urn:dx:dmp:invalidNameValue", "Invalid name"),
  INVALID_PAYLOAD_FORMAT_URN(
      "urn:dx:dmp:invalidPayloadFormat", "Invalid json format in post request [schema mismatch]"),
  INVALID_PARAM_VALUE_URN("urn:dx:dmp:invalidParamameterValue", "Invalid parameter value passed"),
  BAD_REQUEST_URN("urn:dx:dmp:badRequest", "bad request parameter"),
  INVALID_HEADER_VALUE_URN("urn:dx:dmp:invalidHeaderValue", "Invalid header value"),
  DB_ERROR_URN("urn:dx:dmp:DatabaseError", "Database error"),
  QUEUE_ERROR_URN("urn:dx:dmp:QueueError", "Queue error"),
  INVALID_RESOURCE_URN("urn:dx:dmp:InvalidResourceID", "Resource ID is invalid"),
  INVALID_PROVIDER_URN("urn:dx:dmp:InvalidProviderID", "Provider ID is invalid"),
  ROLE_NOT_FOUND("urn:dx:dm:invalidRole", "Role does not exist"),

  VERIFY_SUCCESS_URN("urn:apd:Allow", "Success"),
  VERIFY_FORBIDDEN_URN("urn:apd:Deny", "Policy does not exist"),

  BACKING_SERVICE_FORMAT_URN(

      "urn:dx:dm:backend", "format error from backing service [cat,auth etc.]"),
  SCHEMA_READ_ERROR_URN("urn:dx:dm:readError", "Fail to read file"),
  ROLE_NOT_FOUND("urn:dx:dm:invalidRole", "Role does not exist"),

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
