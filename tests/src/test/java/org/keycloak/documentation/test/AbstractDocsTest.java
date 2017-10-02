package org.keycloak.documentation.test;

import org.apache.log4j.Logger;
import org.junit.*;
import org.keycloak.documentation.test.utils.DocUtils;
import org.keycloak.documentation.test.utils.LinkUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public abstract class AbstractDocsTest {

    private static final Logger log = Logger.getLogger(AbstractDocsTest.class);

    protected static final Config config = new Config();
    
    protected DocUtils utils = new DocUtils();
    protected LinkUtils linkUtils = new LinkUtils(config);

    protected File guideDir;
    protected static String body;
    protected String guideUrl;

    @Before
    public void before() throws IOException {
        guideDir = config.getGuideDir(getGuideDirName());
        guideUrl = config.getGuideBaseUrl() + "/" + config.getGuideUrlFragment(getGuideDirName()) + "/";

        if (body == null) {
            if (config.isLoadFromFiles()) {
                File htmlFile = config.getGuideHtmlFile(getGuideDirName());
                body = utils.readBody(htmlFile);
            } else {
                log.info("Loading guide from '" + guideUrl);
                body = utils.readBody(new URL(guideUrl));
            }

            body = rewriteLinksToGuides(body);
        }
    }

    @After
    public void after() {
        linkUtils.close();
    }

    @AfterClass
    public static void afterClass() {
        body = null;
    }

    public abstract String getGuideDirName();

    @Test
    public void checkVariables() {
        List<String> missingVariables = utils.findMissingVariables(body, config.getIgnoredVariables());
        checkFailures("Variables not found", missingVariables);
    }

    @Test
    public void checkIncludes() {
        List<String> missingIncludes = utils.findMissingIncludes(body);
        checkFailures("Includes not found", missingIncludes);
    }

    @Test
    public void checkImages() {
        List<String> failures = linkUtils.findInvalidImages(body, guideDir, guideUrl);
        checkFailures("Images not found", failures);
    }

    @Test
    public void checkInternalAnchors() {
        List<String> invalidInternalAnchors = linkUtils.findInvalidInternalAnchors(body);
        checkFailures("Internal anchors not found", invalidInternalAnchors);
    }

    @Test
    public void checkExternalLinks() throws IOException {
        List<LinkUtils.InvalidLink> invalidLinks = linkUtils.findInvalidLinks(body, config.getIgnoredLinks(), config.getIgnoredLinkRedirects());
        if (!invalidLinks.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Invalid links:");
            for (LinkUtils.InvalidLink l : invalidLinks) {
                sb.append("\n");
                sb.append(" * " + l.getLink() + " (" + l.getError() + ")");
            }
            Assert.fail(sb.toString());
        }
    }

    private void checkFailures(String message, List<String> failures) {
        if (!failures.isEmpty()) {
            Assert.fail(message + ":\n * " + String.join("\n * ", failures));
        }
    }

    private String rewriteLinksToGuides(String body) throws MalformedURLException {
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
