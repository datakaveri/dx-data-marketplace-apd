package iudx.data.marketplace.consentAgreementGenerator.controller;

import com.github.jhonnymertz.wkhtmltopdf.wrapper.Pdf;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.apiserver.util.Role;
import iudx.data.marketplace.auditing.AuditingService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.consentAgreementGenerator.HtmlTemplateToPdf;
import iudx.data.marketplace.consentAgreementGenerator.PolicyDetails;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.data.marketplace.product.util.Constants.RESULTS;

public class consentAgreementServiceImpl implements ConsentAgreementService {
    private static final Logger LOGGER = LogManager.getLogger(consentAgreementServiceImpl.class);
    private final AuditingService auditingService;
    private final Api api;
    private final Assets assets;
    private Pdf pdf;
    private Object object;
    private PostgresService postgresService;


    consentAgreementServiceImpl(Pdf pdf, Api api, AuditingService auditingService, Assets assets, PostgresService postgresService)
    {

        this.assets = assets;
        this.auditingService = auditingService;
        this.api = api;
        this.pdf = pdf;
        this.postgresService = postgresService;
    }

    // object (Person) that is sent here should have to string method implemented so that
    // the information that is inserted into the pdf can be sent for auditing

    // adding setters and getters in PolicyInfoInPdf and also get policy

    @Override
    public Future<Buffer> initiatePdfGeneration(User user, String policyId) {
        Promise<Buffer> promise = Promise.promise();
    String query =
        "SELECT P.*, R.resource_server, now() >= P.expiry_at AS is_policy_expired FROM policy AS P "
            + "INNER JOIN resource_entity AS R "
            + "ON R._id = P.resource_id "
            + "WHERE P._id = $1 ";
        LOGGER.debug("check if policyId is present in the DB ");
//        check if policyId is present in the DB
        //TODO: get the policy with policyID
        postgresService.executePreparedQuery(query, new JsonObject().put("id", policyId), handler -> {
            if(handler.succeeded())
            {
                boolean isPolicyNotPresent = handler.result().getJsonArray(RESULTS).isEmpty();
                if(isPolicyNotPresent){
                    promise.fail(new RespBuilder().withType(HttpStatusCode.NOT_FOUND.getValue()).withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn()).withDetail("Policy not found").getResponse());
                    return;
                }
                //policy is present
                JsonObject result = handler.result().getJsonArray(RESULTS).getJsonObject(0);
                // check if the policy belongs to the consumer or provider
                String consumerEmailId = result.getString("consumer_email_id");
                String providerId = result.getString("provider_id");
                boolean isPolicyForConsumer = user.getUserRole().equals(Role.CONSUMER) && consumerEmailId.equals(user.getEmailId());
                boolean isPolicyBelongingToProvider = user.getUserRole().equals(Role.PROVIDER) && providerId.equals(user.getUserId());

                if(isPolicyForConsumer || isPolicyBelongingToProvider)
                {
                    // check if the resource server url of user and resource is the same
                    boolean isRsUrlEqual = result.getString("resource_server").equalsIgnoreCase(user.getResourceServerUrl());
                    if(isRsUrlEqual)
                    {
                        // Rate limit
                        // get the htmlstring
                        //generate the pdf
                        // audit data
                        PolicyDetails policyDetails = new PolicyDetails();
                        policyDetails.setPolicyId(policyId);
                        policyDetails.setPolicyStatus(result.getString("status"));
                        policyDetails.setConsumerEmailId(consumerEmailId);
                        policyDetails.setAssets(assets);
                        policyDetails.setResourceId(result.getString("resource_id"));
                        policyDetails.setInvoiceId(result.getString("invoice_id"));
                        policyDetails.setConstraints(result.getJsonObject("constraints"));
                        policyDetails.setProviderId(providerId);
                        policyDetails.setProductVariantId(result.getString("product_variant_id"));
                        policyDetails.setPolicyCreatedAt(result.getString("created_at"));
                        policyDetails.setPolicyExpired(result.getString("is_policy_expired").equalsIgnoreCase("true"));
                        policyDetails.setResourceServerUrl(result.getString("resource_server"));
                        policyDetails.setPolicyExpiryAt(result.getString("expiry_at"));
                        LOGGER.info("policy details is : " + policyDetails);


                        HtmlTemplateToPdf htmlTemplateToPdf = new HtmlTemplateToPdf(policyDetails,assets);
                        String updatedHtml = htmlTemplateToPdf.generateHtmlString(); //another class
                        Buffer pdfBuffer = Buffer.buffer(htmlTemplateToPdf.generatePdf(updatedHtml));
                        promise.complete(pdfBuffer);

                    }
                    else
                    {
                        promise.fail(new RespBuilder().withType(HttpStatusCode.FORBIDDEN.getValue()).withTitle(ResponseUrn.FORBIDDEN_URN.getUrn()).withDetail("consent agreement is forbidden to access").getResponse());

                    }

                }
                else
                {
                    promise.fail(new RespBuilder().withType(HttpStatusCode.FORBIDDEN.getValue()).withTitle(ResponseUrn.FORBIDDEN_URN.getUrn()).withDetail("Fetching the consent agreement is forbidden as it does not belong to the user").getResponse());

                }

            }
            else
            {
                promise.fail(new RespBuilder().withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue()).withTitle(ResponseUrn.DB_ERROR_URN.getUrn()).withDetail(ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage()).getResponse());

            }
        });

        return promise.future();
    }




}
