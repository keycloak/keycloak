package org.keycloak.documentation.test.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.keycloak.documentation.test.Config;
import org.keycloak.documentation.test.Guide;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocUtils {

    public static String readBody(File htmlFile) throws IOException {
        String s = FileUtils.readFileToString(htmlFile, "utf-8");

        Pattern p = Pattern.compile("<body.*?>(.*?)</body>.*?",Pattern.DOTALL);
        Matcher m = p.matcher(s);

        m.find();
        return m.group(1);
    }

    public static String readBody(URL url) throws IOException {
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(Constants.HTTP_CONNECTION_TIMEOUT);
            connection.setReadTimeout(Constants.HTTP_READ_TIMEOUT);

            if (connection.getResponseCode() != 200) {
                throw new IOException("Invalid status code returned " + connection.getResponseCode());
            }

            StringWriter w = new StringWriter();
            IOUtils.copy(connection.getInputStream(), w, "utf-8");
            String s = w.toString();

            Pattern p;
            if (s.contains("<article class=\"rh_docs\">")) {
                p = Pattern.compile("<article class=\"rh_docs\">(.*?)</article>.*?", Pattern.DOTALL);
            } else {
                p = Pattern.compile("<body.*?>(.*?)</body>.*?",Pattern.DOTALL);
            }

            Matcher m = p.matcher(s);
            if (!m.find()) {
                throw new RuntimeException("Couldn't find body");
            }
            return m.group(1);

        } finally {
            connection.disconnect();
        }
    }

    public static Set<String> findMissingVariables(Guide guide) {
        List<String> ignoredVariables = Config.getInstance().getIgnoredVariables();
        Set<String> missingVariables = new HashSet<>();
        Pattern p = Pattern.compile("[^$/=\n]\\{([^ }\"]*)}");
        Matcher m = p.matcher(guide.getBody());
        while (m.find()) {
            String key = m.group(1);
            if (!key.isEmpty() && !ignoredVariables.contains(key)) {
                missingVariables.add(key);
            }
        }
        return missingVariables;
    }

    public static Set<String> findMissingIncludes(Guide guide) {
        Set<String> missingIncludes = new HashSet<>();
        Pattern p = Pattern.compile("Unresolved directive.*");
        Matcher m = p.matcher(guide.getBody());
        if (m.find()) {
            missingIncludes.add(m.group());
        }
        return missingIncludes;
    }

}
