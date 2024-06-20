package iudx.data.marketplace.consentAgreementGenerator.util;

import java.nio.file.Path;

public class Assets {
    public static final String FILE_EXTENSION = ".html";
    public static final String HTML_FILE_NAME = "consentAgreement";
    private final String absolutePath;
    private final Path path;

    public Assets(Path path)
    {
        this.path = path;
        Path absolutePath = path.toAbsolutePath();
        this.absolutePath = absolutePath.toString().replace(HTML_FILE_NAME + FILE_EXTENSION, "src/main/java/iudx/data/marketplace/consentAgreementGenerator/assets");

    }
    public String getAbsolutePath() {
        return this.absolutePath;
    }

    @Override
    public String toString() {
        return "Assets{" +
                "absolutePath='" + absolutePath + '\'' +
                ", path=" + path +
                '}';
    }
}
