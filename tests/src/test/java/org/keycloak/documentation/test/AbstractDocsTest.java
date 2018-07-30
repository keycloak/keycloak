package org.keycloak.documentation.test;

import org.apache.log4j.Logger;
import org.junit.*;
import org.keycloak.documentation.test.utils.DocUtils;
import org.keycloak.documentation.test.utils.LinkUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractDocsTest {

    private static final Logger log = Logger.getLogger(AbstractDocsTest.class);

    protected static final Config config = new Config();

    protected static LinkUtils linkUtils;
    protected static String body;
    protected static Boolean verbose = System.getProperties().containsKey("verbose") ? true : false;

    protected DocUtils utils = new DocUtils();

    protected File guideDir;
    protected String guideUrl;

    @BeforeClass
    public static void beforeClass() {
        linkUtils = new LinkUtils(config, verbose);
    }

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

    @AfterClass
    public static void afterClass() {
        linkUtils.close();
        body = null;
    }

    public abstract String getGuideDirName();

    @Test
    public void checkVariables() {
        Set<String> missingVariables = utils.findMissingVariables(body, config.getIgnoredVariables());
        checkFailures("Variables not found", missingVariables);
    }

    @Test
    public void checkIncludes() {
        Set<String> missingIncludes = utils.findMissingIncludes(body);
        checkFailures("Includes not found", missingIncludes);
    }

    @Test
    public void checkImages() {
        Set<String> failures = linkUtils.findInvalidImages(body, guideDir, guideUrl);
        checkFailures("Images not found", failures);
    }

    @Test
    public void checkInternalAnchors() {
        Set<String> invalidInternalAnchors = linkUtils.findInvalidInternalAnchors(body);
        checkFailures("Internal anchors not found", invalidInternalAnchors);
    }

    @Test
    public void checkExternalLinks() throws IOException {
        List<LinkUtils.InvalidLink> invalidLinks = linkUtils.findInvalidLinks(body, config.getIgnoredLinks(), config.getIgnoredLinkRedirects());
        if (!invalidLinks.isEmpty()) {
            Set<String> failures = new HashSet<>();
            for (LinkUtils.InvalidLink l : invalidLinks) {
                failures.add(l.getLink() + " (" + l.getError() + ")");
            }
            throw new Failures("Invalid links", failures);
        }
    }

    private void checkFailures(String message, Set<String> failures) {
        if (!failures.isEmpty()) {
            throw new Failures(message, failures);
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
