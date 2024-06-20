package iudx.data.marketplace.consentAgreementGenerator.controller;

import com.github.jhonnymertz.wkhtmltopdf.wrapper.Pdf;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.configurations.WrapperConfig;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.params.Param;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.data.marketplace.auditing.AuditingService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.consentAgreementGenerator.util.Assets;
import iudx.data.marketplace.consentAgreementGenerator.service.consentAgreementServiceImpl;
import iudx.data.marketplace.policies.FetchPolicyDetailsWithPolicyId;
import iudx.data.marketplace.policies.PolicyService;
import iudx.data.marketplace.postgres.PostgresService;

import java.nio.file.Path;
import java.nio.file.Paths;

import static iudx.data.marketplace.common.Constants.*;
import static iudx.data.marketplace.consentAgreementGenerator.util.Assets.FILE_EXTENSION;

public class ConsentAgreementVerticle extends AbstractVerticle {
    private Api api;
    private AuditingService auditingService;
    private consentAgreementServiceImpl pdfGeneratorService;
    private Assets assets;
    private static final String ASSETS_PATH = "/home/shreelakshmi/Documents/Project_Workspace/Data-Market-place/DMP-4-PR/iudx-data-marketplace-apd/src/main/java/iudx/data/marketplace/pdfGenerator/assets/";
    private static final String HTML_FILE_NAME = "consentAgreement";
    private PostgresService postgresService;
    private FetchPolicyDetailsWithPolicyId fetchPolicyDetailsWithPolicyId;

    @Override
    public void start() throws Exception {
        super.start();
        api = Api.getInstance(config().getString("dxApiBasePath"));
        postgresService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
        auditingService = AuditingService.createProxy(vertx, AUDITING_SERVICE_ADDRESS);
        ClassPathTemplateLoader classPathTemplateLoader = new ClassPathTemplateLoader(ASSETS_PATH, ".html");
        Handlebars handlebars = new Handlebars(classPathTemplateLoader);
//        Template template = handlebars.compile(HTML_FILE_NAME);
        String executable = WrapperConfig.findExecutable();
        Pdf pdf = new Pdf(new WrapperConfig(executable));
//        Page page = pdf.addPageFromString(HTML_FILE_NAME);
//        HtmlToPdf htmlToPdf = new HtmlToPdf(null,assets);
    /* configure pdf elements like header, footer etc., */
    pdf.addParam(new Param("--enable-local-file-access"));
//    page.addParam(new Param("footer-center", "this is footer"));
//    page.addParam(new Param("header-center", "this is header"));

        Path path = Paths.get(HTML_FILE_NAME+FILE_EXTENSION);
        Path absolutePath = path.toAbsolutePath();
        String absPath = absolutePath.toString().replace(HTML_FILE_NAME + FILE_EXTENSION, "src/main/java/iudx/data/marketplace/consentAgreementGenerator/assets");
        assets = new Assets(path);

        PolicyService fetchPolicyDetailsWithPolicyId = PolicyService.createProxy(vertx, POLICY_SERVICE_ADDRESS);
        pdfGeneratorService = new consentAgreementServiceImpl(fetchPolicyDetailsWithPolicyId, assets);
        new ServiceBinder(vertx)
                .setAddress(CONSENT_AGREEMENT_SERVICE)
                .register(ConsentAgreementService.class, pdfGeneratorService);

    }
}
