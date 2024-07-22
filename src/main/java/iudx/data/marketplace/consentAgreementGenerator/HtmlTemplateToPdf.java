package iudx.data.marketplace.consentAgreementGenerator;

import com.github.jhonnymertz.wkhtmltopdf.wrapper.Pdf;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.configurations.WrapperConfig;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.objects.Page;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.params.Param;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import iudx.data.marketplace.consentAgreementGenerator.controller.Assets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

import static iudx.data.marketplace.consentAgreementGenerator.controller.Assets.*;

public class HtmlTemplateToPdf {
    private static final Logger LOGGER = LogManager.getLogger(HtmlTemplateToPdf.class);
    private final Object policyDetails;
    private final Assets assets;
    private boolean isParamSet;
    private List<Param> pageParams;
    public HtmlTemplateToPdf(Object object, Assets assets)
    {

        policyDetails = object;
        this.assets = assets;
    }


    public String generateHtmlString()
    {
//        assets.setAbsolutePath(ASSETS);
//        try {
//            Path path = Paths.get(HTML_FILE_NAME+FILE_EXTENSION);
//            Path absolutePath = path.toAbsolutePath();
//            LOGGER.info("absolute path is : " + absolutePath);
//            LOGGER.info("changed absolute path is : " + absolutePath.toString().replace(HTML_FILE_NAME + FILE_EXTENSION, "src/main/java/iudx/data/marketplace/pdfGenerator"));
////            new Assets().setAbsolutePath(absolutePath.toString().replace(HTML_FILE_NAME + FILE_EXTENSION, "src/main/java/iudx/data/marketplace/pdfGenerator"));
//            LOGGER.info("canonical path is : {}", new File(".").getCanonicalPath());
//            File file = new File("/home/shreelakshmi/Documents/Project_Workspace/Data_Market_place/DMP_4_PR/iudx_data_marketplace_apd/src/main/java/iudx/data/marketplace/pdfGenerator/consentAgreement.html");
//            LOGGER.info("can read ? : " +  file.canRead());
//            Scanner scanner = new Scanner(file);
//            while (scanner.hasNextLine())
//            {
//                String data = scanner.nextLine();
////                LOGGER.info(data);
//            }
//            scanner.close();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        TemplateLoader classPathTemplateLoader = new FileTemplateLoader("/home/shreelakshmi/Documents/Project_Workspace/Data_Market_place/DMP_4_PR/iudx_data_marketplace_apd/src/main/java/iudx/data/marketplace/consentAgreementGenerator", FILE_EXTENSION);
        ClassPathTemplateLoader classPathTemplateLoader1 = new ClassPathTemplateLoader("", FILE_EXTENSION);
        TemplateLoader classPathTemplateLoader2 = new ClassPathTemplateLoader("/home/shreelakshmi/Documents/Project_Workspace/Data_Market_place/DMP_4_PR/iudx_data_marketplace_apd/src/main/java/iudx/data/marketplace/consentAgreementGenerator", FILE_EXTENSION);
      TemplateLoader loader = new ClassPathTemplateLoader();
      loader. setPrefix("/pdfGenerator");
      loader. setSuffix(".html");
//      Handlebars handlebars = new Handlebars(loader);

    //  Template template = handlebars. compile("mytemplate");
    //
    //  System. out. println(template. apply("Handlebars. java"));
    //
    Handlebars handlebars = new Handlebars(classPathTemplateLoader);
        String htmlString = null;
        try {
            Template template = handlebars.compile(HTML_FILE_NAME);
            htmlString = template.apply(policyDetails);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        LOGGER.info("hereeee : >>>>>>>>>>>>>>>>>>>>>>");
//        LOGGER.info("htmlstring is : {}", htmlString);
        return htmlString;
    }

    //
    public byte[] generatePdf(String htmlString)
    {
        String executable = WrapperConfig.findExecutable();
        Pdf pdf = new Pdf(new WrapperConfig(executable));
        Page page = pdf.addPageFromString(htmlString);
        // TODO: Make params configurable
        pdf.addParam(new Param("--enable-local-file-access"));

        if(this.pageParams == null || getPageParams().isEmpty())
        {
            page.addParam(new Param("--footer-center", "here is the footer"));
            page.addParam(new Param("--header-center", "this is a header"));
        }
        else {
        for(int i = 0; i < getPageParams().size(); i++) {
            page.addParam(this.pageParams.get(i));
        }
        }

        byte[] buffer = null;
        try {
            //TODO: Remove saveAs
//            pdf.saveAs("output.pdf");
//            buffer = Buffer.buffer(pdf.getPDF());
            buffer =  pdf.getPDF();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return buffer;
    }

    public List<Param> getPageParams()
    {
        return this.pageParams;
    }

    public void setPageParams(List<Param> params)
    {
        this.pageParams = params;
    }

}
