package iudx.data.marketplace.consentAgreementGenerator.util;

import com.github.jhonnymertz.wkhtmltopdf.wrapper.params.Param;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

import static iudx.data.marketplace.consentAgreementGenerator.util.Assets.*;

public class ConsentAgreementHtml {
    private static final Logger LOGGER = LogManager.getLogger(ConsentAgreementHtml.class);
    private final Object policyDetails;
    private final Assets assets;
    private boolean isParamSet;
    private List<Param> pageParams;
    public ConsentAgreementHtml(Object object, Assets assets)
    {

        policyDetails = object;
        this.assets = assets;
    }


    public String toString()
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
        TemplateLoader classPathTemplateLoader = new FileTemplateLoader("/home/shreelakshmi/Documents/Project_Workspace/Data_Market_place/DMP_4_PR/iudx_data_marketplace_apd/src/main/java/iudx/data/marketplace/consentAgreementGenerator/assets", FILE_EXTENSION);
        ClassPathTemplateLoader classPathTemplateLoader1 = new ClassPathTemplateLoader("", FILE_EXTENSION);
        TemplateLoader classPathTemplateLoader2 = new ClassPathTemplateLoader("/home/shreelakshmi/Documents/Project_Workspace/Data_Market_place/DMP_4_PR/iudx_data_marketplace_apd/src/main/java/iudx/data/marketplace/consentAgreementGenerator/assets", FILE_EXTENSION);
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


}
