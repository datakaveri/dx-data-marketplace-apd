package iudx.data.marketplace.consentAgreementGenerator.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import iudx.data.marketplace.common.HtmlStringToPdf;
import iudx.data.marketplace.consentAgreementGenerator.controller.ConsentAgreementService;
import iudx.data.marketplace.consentAgreementGenerator.controller.PolicyDetails;
import iudx.data.marketplace.consentAgreementGenerator.util.Assets;
import iudx.data.marketplace.consentAgreementGenerator.util.ConsentAgreementHtml;
import iudx.data.marketplace.policies.PolicyService;
import iudx.data.marketplace.policies.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class consentAgreementServiceImpl implements ConsentAgreementService {
  private static final Logger LOGGER = LogManager.getLogger(consentAgreementServiceImpl.class);
  private final PolicyService fetchPolicyDetailsWithPolicyId;
  private final Assets assets;

  public consentAgreementServiceImpl(
          PolicyService policyService, Assets assets) {
    fetchPolicyDetailsWithPolicyId = policyService;
    this.assets = assets;
  }

  @Override
  public Future<Buffer> initiatePdfGeneration(User user, String policyId) {
    Promise<Buffer> promise = Promise.promise();
    Future<PolicyDetails> policyDetailsFuture =
            fetchPolicyDetailsWithPolicyId.fetchPolicyWithPolicyId(user, policyId);
    policyDetailsFuture
        .onSuccess(
            policyDetail -> {
              ConsentAgreementHtml consentAgreementHtmlObject = new ConsentAgreementHtml(policyDetail, assets);
              String updatedHtml = consentAgreementHtmlObject.toString();
              Buffer pdfBuffer = Buffer.buffer(new HtmlStringToPdf(updatedHtml).generatePdf());
              promise.complete(pdfBuffer);
            })
        .onFailure(
            failureHandler -> {
              promise.fail(failureHandler.getCause().getMessage());
            });
    return promise.future();
  }
}
