package iudx.data.marketplace.consentAgreementGenerator;

import java.nio.file.Path;

public class Assets {
//    public static final String absolutePath1 = "src/main/java/iudx/data/marketplace/pdfGenerator";
    public static final String ASSETS =  "assets";
    public static final String FILE_EXTENSION = ".html";
    public static final String HTML_FILE_NAME = "consentAgreement";
    private String absolutePath;
    private Path path;
 //

    Assets(Path path)
    {
        this.path = path;
//        this.path = Paths.get(HTML_FILE_NAME+FILE_EXTENSION);
        Path absolutePath = path.toAbsolutePath();
        this.absolutePath = absolutePath.toString().replace(HTML_FILE_NAME + FILE_EXTENSION, "src/main/java/iudx/data/marketplace/consentAgreementGenerator");

    }
//    public void setAbsolutePath(String path)
//    {
//        Path path = Paths.get(HTML_FILE_NAME+FILE_EXTENSION);
//        Path absolutePath = path.toAbsolutePath();
//        String absPath = absolutePath.toString().replace(HTML_FILE_NAME + FILE_EXTENSION, "src/main/java/iudx/data/marketplace/pdfGenerator");
//
//        File file = new File(path);
//        this.absolutePath = file.getAbsolutePath();
//    }
    public String getAbsolutePath() {
    System.out.println(this.absolutePath);
        return this.absolutePath;
    }
}
