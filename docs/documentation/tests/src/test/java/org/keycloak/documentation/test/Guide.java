package org.keycloak.documentation.test;

import org.keycloak.documentation.test.utils.DocUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class Guide {

    protected DocUtils utils = new DocUtils();

    private String guide;

    private String body;

    private File guideDir;
    private String guideUrl;

    public Guide(String guide) throws IOException {
        this.guide = guide;

        Config config = Config.getInstance();

        guideDir = config.getGuideDir(guide);
        guideUrl = config.getGuideBaseUrl() + "/" + config.getGuideUrlFragment(guide) + "/";

        if (body == null) {
            if (config.isLoadFromFiles()) {
                File htmlFile = config.getGuideHtmlFile(guide);
                body = utils.readBody(htmlFile);
            } else {
                body = utils.readBody(new URL(guideUrl));
            }

            body = rewriteLinksToGuides(config, body);
        }
    }

    public String getBody() {
        return body;
    }

    public File getDir() {
        return guideDir;
    }

    public String getUrl() {
        return guideUrl;
    }

    private String rewriteLinksToGuides(Config config, String body) throws MalformedURLException {
        if (config.isLoadFromFiles()) {
            for (Map.Entry<String, String> e : config.getGuideFragmentToDir().entrySet()) {
                String originalUrl = config.getDocBaseUrl() + "/" + e.getKey() + "/";
                String replacementUrl = config.getGuideHtmlFile(e.getValue()).toURI().toURL().toString();

                body = body.replace("href=\"" + originalUrl, "href=\"" + replacementUrl);
            }
        } else {
            body = body.replace("href=\"" + config.getDocBaseUrl(), "href=\"" + config.getGuideBaseUrl());
        }
        return body;
    }

}
