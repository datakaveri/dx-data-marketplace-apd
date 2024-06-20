package iudx.data.marketplace.consentAgreementGenerator.controller;

import static iudx.data.marketplace.common.Constants.*;
import static iudx.data.marketplace.consentAgreementGenerator.util.Assets.FILE_EXTENSION;
import static iudx.data.marketplace.consentAgreementGenerator.util.Assets.HTML_FILE_NAME;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.data.marketplace.consentAgreementGenerator.service.consentAgreementServiceImpl;
import iudx.data.marketplace.consentAgreementGenerator.util.Assets;
import iudx.data.marketplace.policies.PolicyService;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConsentAgreementVerticle extends AbstractVerticle {;
    private consentAgreementServiceImpl pdfGeneratorService;
    private Assets assets;

    @Override
    public void start() throws Exception {
        super.start();
        Path path = Paths.get(HTML_FILE_NAME+FILE_EXTENSION);
        assets = new Assets(path);

        PolicyService fetchPolicyDetailsWithPolicyId = PolicyService.createProxy(vertx, POLICY_SERVICE_ADDRESS);
        pdfGeneratorService = new consentAgreementServiceImpl(fetchPolicyDetailsWithPolicyId, assets);
        new ServiceBinder(vertx)
                .setAddress(CONSENT_AGREEMENT_SERVICE)
                .register(ConsentAgreementService.class, pdfGeneratorService);

    }
}
