package iudx.data.marketplace.common;

import com.github.jhonnymertz.wkhtmltopdf.wrapper.Pdf;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.configurations.WrapperConfig;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.objects.Page;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.params.Param;

import java.io.IOException;
import java.util.List;

public class HtmlStringToPdf {
    private List<Param> pageParams;
    private final String htmlString;
    public HtmlStringToPdf(String updatedHtml)
    {
        this.htmlString = updatedHtml;
    }
    public byte[] generatePdf()
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
