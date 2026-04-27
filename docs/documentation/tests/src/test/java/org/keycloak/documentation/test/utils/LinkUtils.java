package org.keycloak.documentation.test.utils;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.keycloak.documentation.test.Config;
import org.keycloak.documentation.test.Guide;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkUtils {

    private static final LinkUtils instance = new LinkUtils();

    private HttpUtils http = new HttpUtils();
    private File verifiedLinksCacheFile;
    private Map<String, Long> verifiedLinks;

    public static LinkUtils getInstance() {
        return instance;
    }

    private LinkUtils() {
        this.verifiedLinksCacheFile = Config.getInstance().getVerifiedLinksCache();
        this.verifiedLinks = loadCheckedLinksCache();
    }

    public void close() {
        saveCheckedLinksCache();
    }

    public Set<String> findInvalidInternalAnchors(Guide guide) {
        Set<String> invalidInternalAnchors = new HashSet<>();
        Pattern p = Pattern.compile("<a href=\"([^ \"]*)[^>]*\">");
        Matcher m = p.matcher(guide.getBody());
        while (m.find()) {
            String link = m.group(1);

            if (link.startsWith("#")) {
                if (!guide.getBody().contains("id=\"" + link.substring(1) + "\"")) {
                    invalidInternalAnchors.add(link.substring(1));
                }
            }
        }
        return invalidInternalAnchors;
    }

    public List<InvalidLink> findInvalidLinks(Guide guide) throws IOException {
        List<InvalidLink> invalidLinks = new LinkedList<>();
        Pattern p = Pattern.compile("<a href=\"([^ \"]*)[^>]*\">");
        Matcher m = p.matcher(guide.getBody());
        while (m.find()) {
            String link = m.group(1);

            if (verifyLink(link, Config.getInstance().getIgnoredLinks(), invalidLinks)) {
                if (link.startsWith("http")) {
                    String anchor = link.contains("#") ? link.split("#")[1] : null;
                    String error = null;

                    HttpUtils.Response response = http.load(link);

                    if (response.getRedirectLocation() != null) {
                        if (!validRedirect(response.getRedirectLocation(), Config.getInstance().getIgnoredLinkRedirects())) {
                            error = "invalid redirect to " + response.getRedirectLocation();
                        }
                    } else if (response.isSuccess()) {
                        if (response.getContent().contains("http-equiv")) {
                            // The contains() will scan the document fast, while Jsoup parse will take extra CPU cycles to parse the document.
                            // Using Jsoup avoid parsing getting false positives from the document's contents.
                            Document doc = Jsoup.parse(response.getContent());
                            Element refresh = doc.selectFirst("head > meta[http-equiv=refresh]");
                            if (refresh != null) {
                                String content = refresh.attribute("content").getValue();
                                if (content.contains(";")) {
                                    String url = content.substring(content.indexOf(";") + 1).trim();
                                    if (url.startsWith("url=")) {
                                        url = url.substring("url=".length()).trim();
                                        if (!validRedirect(url, Config.getInstance().getIgnoredLinkRedirects())) {
                                            error = "invalid redirect to " + url;
                                        }
                                    }
                                }
                            }
                        }
                        if (anchor != null) {
                            if (!(response.getContent().contains("id=\"" + anchor + "\"") || response.getContent().contains("name=\"" + anchor + "\"") || response.getContent().contains("href=\"#" + anchor + "\""))) {
                                error = "invalid anchor " + anchor;
                            }
                        }
                    } else {
                        error = response.getError();
                    }

                    if (error == null) {
                        verifiedLinks.put(link, System.currentTimeMillis());
                    } else {
                        invalidLinks.add(new InvalidLink(link, error));
                    }
                } else if (link.startsWith("file")) {
                    File f = new File(new URL(link).getFile());
                    if (!f.isFile()) {
                        invalidLinks.add(new InvalidLink(link, "local guide not found"));
                    } else {
                        String anchor = link.contains("#") ? link.split("#")[1] : null;
                        if (anchor != null) {
                            String w = FileUtils.readFileToString(f, "utf-8");
                            if (!(w.contains("id=\"" + anchor + "\"") || w.contains("name=\"" + anchor + "\""))) {
                                invalidLinks.add(new InvalidLink(link, "invalid anchor " + anchor));
                            }
                        }
                    }
                }
            }
        }

        return invalidLinks;
    }

    public Set<String> findInvalidImages(Guide guide) {
        Set<String> missingImages = new HashSet<>();
        Pattern p = Pattern.compile("<img src=\"([^ \"]*)[^>]*\"");
        Matcher m = p.matcher(guide.getBody());
        while (m.find()) {
            String image = m.group(1);
            if (Config.getInstance().isLoadFromFiles()) {
                File f = new File(guide.getDir(), image);
                if (!f.isFile()) {
                    missingImages.add(image);
                }
            } else {
                if (image.startsWith("./")) {
                    image = guide.getUrl() + image;
                }

                if (!verifiedLinks.containsKey(image)) {
                    boolean valid = http.isValid(image).isSuccess();
                    if (valid) {
                        verifiedLinks.put(image, System.currentTimeMillis());
                    } else {
                        missingImages.add(image);
                    }
                }
            }
        }
        return missingImages;
    }

    private boolean verifyLink(String link, List<String> ignoredLinks, List<InvalidLink> invalidLinks) {
        for (String ignored : ignoredLinks) {
            if (ignored.endsWith("*") && link.startsWith(ignored.substring(0, ignored.length() - 1))) {
                return false;
            } else if (ignored.startsWith("REGEX:") && link.matches(ignored.substring(6))) {
                return true;
            } else if (ignored.equals(link)) {
                return false;
            }
        }

        if (verifiedLinks.containsKey(link)) {
            return false;
        }

        for (InvalidLink l : invalidLinks) {
            if (l.getLink().equals(link)) {
                return false;
            }
        }

        return true;
    }

    private boolean validRedirect(String location, List<String> ignoredLinkRedirects) {
        for (String valid : ignoredLinkRedirects) {
            if (valid.endsWith("*") && location.startsWith(valid.substring(0, valid.length() - 1))) {
                return true;
            } else if (valid.startsWith("REGEX:") && location.matches(valid.substring(6))) {
                return true;
            } else if (valid.equals(location)) {
                return true;
            }
        }
        return false;
    }


    public static class InvalidLink {

        private String link;
        private String error;

        public InvalidLink(String link, String error) {
            this.link = link;
            this.error = error;
        }

        public String getLink() {
            return link;
        }

        public String getError() {
            return error;
        }
    }

    private Map<String, Long> loadCheckedLinksCache() {
        Map<String, Long> m = new HashMap<>();
        try {
            if (verifiedLinksCacheFile.isFile()) {
                Properties p = new Properties();
                p.load(new FileInputStream(verifiedLinksCacheFile));
                for(Map.Entry<Object, Object> e : p.entrySet()) {
                    long checked = Long.valueOf((String) e.getValue());
                    if (checked + Constants.LINK_CHECK_EXPIRATION >= System.currentTimeMillis()) {
                        m.put((String) e.getKey(), checked);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return m;
    }

    private void saveCheckedLinksCache() {
        try {
            Properties p = new Properties();
            for (Map.Entry<String, Long> e : verifiedLinks.entrySet()) {
                p.put(e.getKey(), Long.toString(e.getValue()));
            }
            FileOutputStream os = new FileOutputStream(verifiedLinksCacheFile);
            p.store(os, null);
            os.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
