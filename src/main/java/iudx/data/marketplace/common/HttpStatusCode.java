package iudx.data.marketplace.common;

public enum HttpStatusCode {
  // 1xx: Informational
  CONTINUE(100, "Continue", "urn:dx:dm:continue"),
  SWITCHING_PROTOCOLS(101, "Switching Protocols", "urn:dx:dm:switchingProtocols"),
  PROCESSING(102, "Processing", "urn:dx:dm:processing"),
  EARLY_HINTS(103, "Early Hints", "urn:dx:dm:earlyHints"),

  // 2XX: codes
  NO_CONTENT(204, "No Content", "urn:dx:dm:noContent"),
  SUCCESS(200, "Success", "urn:dx:dm:Success"),

  // 4xx: Client Error
  BAD_REQUEST(400, "Bad Request", "urn:dx:dm:badRequest"),
  UNAUTHORIZED(401, "Not Authorized", "urn:dx:dm:notAuthorized"),
  PAYMENT_REQUIRED(402, "Payment Required", "urn:dx:dm:paymentRequired"),
  FORBIDDEN(403, "Forbidden", "urn:dx:dm:forbidden"),
  VERIFY_FORBIDDEN(403, "Policy does not exist", "urn:apd:Deny"),

  NOT_FOUND(404, "Not Found", "urn:dx:dm:notFound"),
  METHOD_NOT_ALLOWED(405, "Method Not Allowed", "urn:dx:dm:methodNotAllowed"),
  NOT_ACCEPTABLE(406, "Not Acceptable", "urn:dx:dm:notAcceptable"),
  PROXY_AUTHENTICATION_REQUIRED(
      407, "Proxy Authentication Required", "urn:dx:dm:proxyAuthenticationRequired"),
  REQUEST_TIMEOUT(408, "Request Timeout", "urn:dx:dm:requestTimeout"),
  CONFLICT(409, "Conflict", "urn:dx:dm:conflict"),
  GONE(410, "Gone", "urn:dx:dm:gone"),
  LENGTH_REQUIRED(411, "Length Required", "urn:dx:dm:lengthRequired"),
  PRECONDITION_FAILED(412, "Precondition Failed", "urn:dx:dm:preconditionFailed"),
  REQUEST_TOO_LONG(413, "Payload Too Large", "urn:dx:dm:payloadTooLarge"),
  REQUEST_URI_TOO_LONG(414, "URI Too Long", "urn:dx:dm:uriTooLong"),
  UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type", "urn:dx:dm:unsupportedMediaType"),
  REQUESTED_RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable", "urn:dx:dm:rangeNotSatisfiable"),
  EXPECTATION_FAILED(417, "Expectation Failed", "urn:dx:dm:expectation Failed"),
  MISDIRECTED_REQUEST(421, "Misdirected Request", "urn:dx:dm:misdirected Request"),
  UNPROCESSABLE_ENTITY(422, "Unprocessable Entity", "urn:dx:dm:unprocessableEntity"),
  LOCKED(423, "Locked", "urn:dx:dm:locked"),
  FAILED_DEPENDENCY(424, "Failed Dependency", "urn:dx:dm:failedDependency"),
  TOO_EARLY(425, "Too Early", "urn:dx:dm:tooEarly"),
  UPGRADE_REQUIRED(426, "Upgrade Required", "urn:dx:dm:upgradeRequired"),
  PRECONDITION_REQUIRED(428, "Precondition Required", "urn:dx:dm:preconditionRequired"),
  TOO_MANY_REQUESTS(429, "Too Many Requests", "urn:dx:dm:tooManyRequests"),
  REQUEST_HEADER_FIELDS_TOO_LARGE(
      431, "Request Header Fields Too Large", "urn:dx:dm:requestHeaderFieldsTooLarge"),
  UNAVAILABLE_FOR_LEGAL_REASONS(
      451, "Unavailable For Legal Reasons", "urn:dx:dm:unavailableForLegalReasons"),

  // 5xx: Server Error
  INTERNAL_SERVER_ERROR(500, "Internal Server Error", "urn:dx:dm:internalServerError"),
  NOT_IMPLEMENTED(501, "Not Implemented", "urn:dx:dm:notImplemented"),
  BAD_GATEWAY(502, "Bad Gateway", "urn:dx:dm:badGateway"),
  SERVICE_UNAVAILABLE(503, "Service Unavailable", "urn:dx:dm:serviceUnavailable"),
  GATEWAY_TIMEOUT(504, "Gateway Timeout", "urn:dx:dm:gatewayTimeout"),
  HTTP_VERSION_NOT_SUPPORTED(
      505, "HTTP Version Not Supported", "urn:dx:dm:httpVersionNotSupported"),
  VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates", "urn:dx:dm:variantAlsoNegotiates"),
  INSUFFICIENT_STORAGE(507, "Insufficient Storage", "urn:dx:dm:insufficientStorage"),
  LOOP_DETECTED(508, "Loop Detected", "urn:dx:dm:loopDetected"),
  NOT_EXTENDED(510, "Not Extended", "urn:dx:dm:notExtended"),
  NETWORK_AUTHENTICATION_REQUIRED(
      511, "Network Authentication Required", "urn:dx:dm:networkAuthenticationRequired");

  private final int value;
  private final String description;
  private final String urn;

  HttpStatusCode(int value, String description, String urn) {
    this.value = value;
    this.description = description;
    this.urn = urn;
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

  public static HttpStatusCode getByValue(int value) {
    for (HttpStatusCode status : values()) {
      if (status.value == value) return status;
    }
    throw new IllegalArgumentException("Invalid status code: " + value);
  }
}
