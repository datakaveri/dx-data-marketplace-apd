package iudx.data.marketplace.common;

public enum HttpStatusCode {
  // 1xx: Informational
  CONTINUE(100, "Continue", "urn:dx:dmp:continue"),
  SWITCHING_PROTOCOLS(101, "Switching Protocols", "urn:dx:dmp:switchingProtocols"),
  PROCESSING(102, "Processing", "urn:dx:dmp:processing"),
  EARLY_HINTS(103, "Early Hints", "urn:dx:dmp:earlyHints"),

  // 2XX: codes
  NO_CONTENT(204, "No Content", "urn:dx:dmp:noContent"),
  SUCCESS(200, "Success", "urn:dx:dmp:Success"),

  // 4xx: Client Error
  BAD_REQUEST(400, "Bad Request", "urn:dx:dmp:badRequest"),
  UNAUTHORIZED(401, "Not Authorized", "urn:dx:dmp:notAuthorized"),
  PAYMENT_REQUIRED(402, "Payment Required", "urn:dx:dmp:paymentRequired"),
  FORBIDDEN(403, "Forbidden", "urn:dx:dmp:forbidden"),
  NOT_FOUND(404, "Not Found", "urn:dx:dmp:notFound"),
  VERIFY_FORBIDDEN(403, "Policy does not exist", "urn:apd:Deny"),

  METHOD_NOT_ALLOWED(405, "Method Not Allowed", "urn:dx:dmp:methodNotAllowed"),
  NOT_ACCEPTABLE(406, "Not Acceptable", "urn:dx:dmp:notAcceptable"),
  PROXY_AUTHENTICATION_REQUIRED(
      407, "Proxy Authentication Required", "urn:dx:dmp:proxyAuthenticationRequired"),
  REQUEST_TIMEOUT(408, "Request Timeout", "urn:dx:dmp:requestTimeout"),
  CONFLICT(409, "Conflict", "urn:dx:dmp:conflict"),
  GONE(410, "Gone", "urn:dx:dmp:gone"),
  LENGTH_REQUIRED(411, "Length Required", "urn:dx:dmp:lengthRequired"),
  PRECONDITION_FAILED(412, "Precondition Failed", "urn:dx:dmp:preconditionFailed"),
  REQUEST_TOO_LONG(413, "Payload Too Large", "urn:dx:dmp:payloadTooLarge"),
  REQUEST_URI_TOO_LONG(414, "URI Too Long", "urn:dx:dmp:uriTooLong"),
  UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type", "urn:dx:dmp:unsupportedMediaType"),
  REQUESTED_RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable", "urn:dx:dmp:rangeNotSatisfiable"),
  EXPECTATION_FAILED(417, "Expectation Failed", "urn:dx:dmp:expectation Failed"),
  MISDIRECTED_REQUEST(421, "Misdirected Request", "urn:dx:dmp:misdirected Request"),
  UNPROCESSABLE_ENTITY(422, "Unprocessable Entity", "urn:dx:dmp:unprocessableEntity"),
  LOCKED(423, "Locked", "urn:dx:dmp:locked"),
  FAILED_DEPENDENCY(424, "Failed Dependency", "urn:dx:dmp:failedDependency"),
  TOO_EARLY(425, "Too Early", "urn:dx:dmp:tooEarly"),
  UPGRADE_REQUIRED(426, "Upgrade Required", "urn:dx:dmp:upgradeRequired"),
  PRECONDITION_REQUIRED(428, "Precondition Required", "urn:dx:dmp:preconditionRequired"),
  TOO_MANY_REQUESTS(429, "Too Many Requests", "urn:dx:dmp:tooManyRequests"),
  REQUEST_HEADER_FIELDS_TOO_LARGE(
      431, "Request Header Fields Too Large", "urn:dx:dmp:requestHeaderFieldsTooLarge"),
  UNAVAILABLE_FOR_LEGAL_REASONS(
      451, "Unavailable For Legal Reasons", "urn:dx:dmp:unavailableForLegalReasons"),

  // 5xx: Server Error
  INTERNAL_SERVER_ERROR(500, "Internal Server Error", "urn:dx:dmp:internalServerError"),
  NOT_IMPLEMENTED(501, "Not Implemented", "urn:dx:dmp:notImplemented"),
  BAD_GATEWAY(502, "Bad Gateway", "urn:dx:dmp:badGateway"),
  SERVICE_UNAVAILABLE(503, "Service Unavailable", "urn:dx:dmp:serviceUnavailable"),
  GATEWAY_TIMEOUT(504, "Gateway Timeout", "urn:dx:dmp:gatewayTimeout"),
  HTTP_VERSION_NOT_SUPPORTED(
      505, "HTTP Version Not Supported", "urn:dx:dmp:httpVersionNotSupported"),
  VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates", "urn:dx:dmp:variantAlsoNegotiates"),
  INSUFFICIENT_STORAGE(507, "Insufficient Storage", "urn:dx:dmp:insufficientStorage"),
  LOOP_DETECTED(508, "Loop Detected", "urn:dx:dmp:loopDetected"),
  NOT_EXTENDED(510, "Not Extended", "urn:dx:dmp:notExtended"),
  NETWORK_AUTHENTICATION_REQUIRED(
      511, "Network Authentication Required", "urn:dx:dmp:networkAuthenticationRequired");

  private final int value;
  private final String description;
  private final String urn;

  HttpStatusCode(int value, String description, String urn) {
    this.value = value;
    this.description = description;
    this.urn = urn;
  }

  public static HttpStatusCode getByValue(int value) {
    for (HttpStatusCode status : values()) {
      if (status.value == value) {
        return status;
      }
    }
    throw new IllegalArgumentException("Invalid status code: " + value);
  }

  public int getValue() {
    return value;
  }

  public String getDescription() {
    return description;
  }

  public String getUrn() {
    return urn;
  }

  @Override
  public String toString() {
    return value + " " + description;
  }
}
