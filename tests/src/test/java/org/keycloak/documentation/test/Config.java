package org.keycloak.documentation.test;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {

    private static final Logger log = Logger.getLogger(Config.class);

    private File docsRootDir;

    private List<String> ignoredLinkRedirects;
    private List<String> ignoredVariables;
    private List<String> ignoredLinks;

    private boolean community;

    private Map<String, String> documentAttributes;
    private String docBaseUrl;

    private String guideBaseUrl;

    private Map<String, String> guideDirToFragment;
    private Map<String, String> guideFragmentToDir;

    public Config() {
        docsRootDir = findDocsRoot();
        ignoredLinkRedirects = loadConfig("/ignored-link-redirects");
        ignoredVariables = loadConfig("/ignored-variables");
        ignoredLinks = loadConfig("/ignored-links");

        community = !System.getProperties().containsKey("product");

        if (community) {
            guideDirToFragment = loadConfigMap("/guide-url-fragments-community");
        } else {
            guideDirToFragment = loadConfigMap("/guide-url-fragments-product");
        }

        guideFragmentToDir = new HashMap<>();
        for (Map.Entry<String, String> e : guideDirToFragment.entrySet()) {
            guideFragmentToDir.put(e.getValue(), e.getKey());
        }

        documentAttributes = loadDocumentAttributes();
        docBaseUrl = documentAttributes.get("project_doc_base_url").replace("{project_versionDoc}", documentAttributes.get("project_versionDoc"));

        guideBaseUrl = System.getProperty("guideBaseUrl");
        if (guideBaseUrl != null) {
            if (guideBaseUrl.endsWith("/")) {
                guideBaseUrl = guideBaseUrl.substring(0, guideBaseUrl.length() - 1);
            }
            guideBaseUrl = guideBaseUrl.replace("{version}", documentAttributes.get("project_versionDoc"));
        }

        log.info("Testing " + (community ? "community" : "product") + " documentation");
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

    public boolean isCommunity() {
        return community;
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
        return new File(getGuideDir(guideDirName), community ? "index.html" : "master.html");
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
            File f;
            if (community) {
                f = new File(docsRootDir, "/topics/templates/document-attributes-community.adoc");
            } else {
                f = new File(docsRootDir, "/topics/templates/document-attributes-product.adoc");
            }

            Map<String, String> attributes = new HashMap<>();

            for (String l : FileUtils.readLines(f, "utf-8")) {
                if (l.startsWith(":")) {
                    String[] s = l.split(": ");
                    attributes.put(s[0].substring(1).trim(), s[1].trim());
                }
            }

            return attributes;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> loadConfig(String resource) {
        try {
            return IOUtils.readLines(AbstractDocsTest.class.getResourceAsStream(resource), "utf-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> loadConfigMap(String resource) {
        try {
            List<String> lines = IOUtils.readLines(AbstractDocsTest.class.getResourceAsStream(resource), "utf-8");
            Map<String, String> m = new HashMap<>();
            for (String l : lines) {
                String[] s = l.split("=");
                m.put(s[0], s[1]);
            }
            return m;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
