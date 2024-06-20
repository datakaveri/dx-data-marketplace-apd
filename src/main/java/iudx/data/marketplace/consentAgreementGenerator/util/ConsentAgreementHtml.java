package iudx.data.marketplace.consentAgreementGenerator.util;

import static iudx.data.marketplace.consentAgreementGenerator.util.Assets.*;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsentAgreementHtml {
    private static final Logger LOGGER = LogManager.getLogger(ConsentAgreementHtml.class);
    private final Object policyDetails;
    private final Assets assets;
    public ConsentAgreementHtml(Object object, Assets assets)
    {

        policyDetails = object;
        this.assets = assets;
    }


    public String toString()
    {

        TemplateLoader classPathTemplateLoader = new FileTemplateLoader(assets.getAbsolutePath(), FILE_EXTENSION);

    Handlebars handlebars = new Handlebars(classPathTemplateLoader);
        String htmlString = null;
        try {
            Template template = handlebars.compile(HTML_FILE_NAME);
            htmlString = template.apply(policyDetails);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return htmlString;
    }


}
