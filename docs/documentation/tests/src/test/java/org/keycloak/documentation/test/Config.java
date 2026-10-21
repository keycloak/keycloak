package org.keycloak.documentation.test;

import org.apache.commons.io.IOUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.ast.Document;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {

    protected static final Config instance = new Config();

    private File docsRootDir;

    private List<String> ignoredLinkRedirects;
    private List<String> ignoredVariables;
    private List<String> ignoredLinks;

    private Map<String, String> documentAttributes;
    private String docBaseUrl;

    private String guideBaseUrl;

    private Map<String, String> guideDirToFragment;
    private Map<String, String> guideFragmentToDir;

    public static Config getInstance() {
        return instance;
    }

    private Config() {
        docsRootDir = findDocsRoot();
        ignoredLinkRedirects = loadConfig("/ignored-link-redirects");
        ignoredVariables = loadConfig("/ignored-variables");
        ignoredLinks = loadConfig("/ignored-links");

        guideDirToFragment = loadConfigMap("/guide-url-fragments");

        guideFragmentToDir = new HashMap<>();
        for (Map.Entry<String, String> e : guideDirToFragment.entrySet()) {
            guideFragmentToDir.put(e.getValue(), e.getKey());
        }

        documentAttributes = loadDocumentAttributes();
        docBaseUrl = documentAttributes.get("project_doc_base_url");

        guideBaseUrl = System.getProperty("guideBaseUrl");
        if (guideBaseUrl != null) {
            if (guideBaseUrl.endsWith("/")) {
                guideBaseUrl = guideBaseUrl.substring(0, guideBaseUrl.length() - 1);
            }
            guideBaseUrl = guideBaseUrl.replace("{version}", documentAttributes.get("project_versionDoc"));
        }

        if (isLoadFromFiles()) {
            // Ignore api-docs link in unpublished docs
            String apiDocsLink = documentAttributes.get("apidocs_link");
            ignoredLinks.add(apiDocsLink);
        }
    }

    public File getVerifiedLinksCache() {
        return new File(docsRootDir, ".verified-links");
    }

    public File getDocsRootDir() {
        return docsRootDir;
    }

    public List<String> getIgnoredLinkRedirects() {
        return ignoredLinkRedirects;
    }

    public List<String> getIgnoredVariables() {
        return ignoredVariables;
    }

    public List<String> getIgnoredLinks() {
        return ignoredLinks;
    }

    public boolean isLoadFromFiles() {
        return guideBaseUrl == null;
    }

    public Map<String, String> getDocumentAttributes() {
        return documentAttributes;
    }

    public String getDocBaseUrl() {
        return docBaseUrl;
    }

    public String getGuideBaseUrl() {
        return guideBaseUrl;
    }

    public String getGuideUrlFragment(String guideDir) {
        return guideDirToFragment.get(guideDir);
    }

    public Map<String, String> getGuideFragmentToDir() {
        return guideFragmentToDir;
    }

    public File getGuideDir(String guideDirName) {
        return new File(docsRootDir, guideDirName + "/target/generated-docs");
    }

    public File getGuideHtmlFile(String guideDirName) {
        return new File(getGuideDir(guideDirName), "index.html");
    }

    private File findDocsRoot() {
        File f = new File("").getAbsoluteFile();
        if (f.getName().equals("tests")) {
            f = f.getParentFile();
        }
        return f;
    }

    private Map<String, String> loadDocumentAttributes() {
        try {
            File f = new File(docsRootDir, "/topics/templates/document-attributes.adoc");

            String buildType = System.getProperty("latest") != null ? "latest" : "archive";

            Asciidoctor asciidoctor = Asciidoctor.Factory.create();

            Map<String, Object> options = OptionsBuilder.options()
                    .inPlace(true)
                    .attributes(AttributesBuilder.attributes().backend("html5").asMap())
                    .asMap();

            Document document = asciidoctor.loadFile(f, options);

            Map<String, String> attributes = new HashMap<>();

            for (Map.Entry<String, Object> a : document.getAttributes().entrySet()) {
                attributes.put(a.getKey(), a.getValue().toString());
            }

            return attributes;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> loadConfig(String resource) {
        return IOUtils.readLines(Config.class.getResourceAsStream(resource), "utf-8");
    }

    private Map<String, String> loadConfigMap(String resource) {
        List<String> lines = IOUtils.readLines(Config.class.getResourceAsStream(resource), "utf-8");
        Map<String, String> m = new HashMap<>();
        for (String l : lines) {
            String[] s = l.split("=");
            m.put(s[0], s[1]);
        }
        return m;
    }

}
