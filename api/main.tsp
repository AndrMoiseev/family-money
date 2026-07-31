import "@typespec/http";
import "@typespec/openapi";
import "@typespec/openapi3";

using TypeSpec.Http;
using TypeSpec.OpenAPI;

@service(#{ title: "Family Money API" })
@info(#{ version: "0.1.0" })
@server("http://localhost:8080", "Local development")
namespace FamilyMoney;

/** Operational status of the locally running application. */
model SystemStatus {
  /** Current application status. */
  status: "UP";
}

@route("/api/v1/system/status")
@get
@operationId("getSystemStatus")
@tag("System")
op getSystemStatus(): {
  @statusCode statusCode: 200;
  @body body: SystemStatus;
};
