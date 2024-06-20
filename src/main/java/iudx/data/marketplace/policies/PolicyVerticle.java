package iudx.data.marketplace.policies;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.data.marketplace.auditing.AuditingService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.CatalogueService;
import iudx.data.marketplace.consentAgreementGenerator.util.Assets;
import iudx.data.marketplace.postgres.PostgresService;

import java.nio.file.Path;
import java.nio.file.Paths;

import static iudx.data.marketplace.common.Constants.*;
import static iudx.data.marketplace.consentAgreementGenerator.util.Assets.FILE_EXTENSION;
import static iudx.data.marketplace.consentAgreementGenerator.util.Assets.HTML_FILE_NAME;

public class PolicyVerticle extends AbstractVerticle {

  private PostgresService postgresServiceImpl;
  private PolicyServiceImpl policyService;
  private DeletePolicy deletePolicy;
  private CreatePolicy createPolicy;
  private VerifyPolicy verifyPolicy;
  private GetPolicyDetails getPolicyDetails;
  private CatalogueService catalogueService;
  private AuditingService auditingService;
  private Api api;
  private FetchPolicyUsingPvId fetchPolicyUsingPvId;
  private FetchPolicyDetailsWithPolicyId fetchPolicyDetailsWithPolicyId;
  private Assets assets;

  @Override
  public void start() {
    catalogueService = new CatalogueService(vertx, config());
    postgresServiceImpl = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    auditingService = AuditingService.createProxy(vertx, AUDITING_SERVICE_ADDRESS);
    api = Api.getInstance(config().getString("dxApiBasePath"));
    deletePolicy = new DeletePolicy(postgresServiceImpl, auditingService, api);
    getPolicyDetails = new GetPolicyDetails(postgresServiceImpl);
    createPolicy = new CreatePolicy(postgresServiceImpl, auditingService, api);
    verifyPolicy = new VerifyPolicy(postgresServiceImpl);
    fetchPolicyUsingPvId = new FetchPolicyUsingPvId(postgresServiceImpl);

    Path path = Paths.get(HTML_FILE_NAME+FILE_EXTENSION);
    Path absolutePath = path.toAbsolutePath();
    String absPath = absolutePath.toString().replace(HTML_FILE_NAME + FILE_EXTENSION, "src/main/java/iudx/data/marketplace/consentAgreement/assets");
    assets = new Assets(path);
    fetchPolicyDetailsWithPolicyId = new FetchPolicyDetailsWithPolicyId(postgresServiceImpl, assets);
    policyService =
            new PolicyServiceImpl(deletePolicy, createPolicy, getPolicyDetails, verifyPolicy,fetchPolicyUsingPvId, fetchPolicyDetailsWithPolicyId);

    new ServiceBinder(vertx)
        .setAddress(POLICY_SERVICE_ADDRESS)
        .register(PolicyService.class, policyService);
  }
}
