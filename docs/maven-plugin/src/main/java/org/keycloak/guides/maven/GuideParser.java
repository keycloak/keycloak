package org.keycloak.guides.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuideParser {

    private Pattern TEMPLATE_IMPORT_PATTERN = Pattern.compile("<#import \"/templates/guide.adoc\" as (?<importName>[^ ]*)>");
    private Pattern GUIDE_ELEMENT_PATTERN = Pattern.compile("(?<key>priority|title|summary)=(\\\"(?<valueString>[^\\\"]*)\\\"|(?<valueInt>[\\d]*))");

    /**
     * Parses a FreeMarker template to retrieve Guide attributes
     * @param file
     * @return A Guide instance; or <code>null</code> if not a guide
     * @throws IOException
     */
    public Guide parse(File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String importName = getImportName(br);
            String importElement = getGuideElement(br, importName);

            if (importElement != null) {
                Guide guide = new Guide();
                guide.setTemplate(file.getName());
                guide.setPriority(999);

                guide.setId(file.getName().replaceAll(".adoc", ""));

                setAttributes(importElement, guide);

                return guide;
            }

            return null;
        }
    }

    private String getImportName(BufferedReader br) throws IOException {
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            Matcher templateImportMatcher = TEMPLATE_IMPORT_PATTERN.matcher(line);
            if (templateImportMatcher.matches()) {
                return templateImportMatcher.group("importName");
            }
        }
        return null;
    }

    private String getGuideElement(BufferedReader br, String importName) throws IOException {
        if (importName != null) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (line.contains("<@" + importName + ".guide")) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(line.trim());
                    while (!line.contains(">")) {
                        line = br.readLine();
                        sb.append(" " + line.trim());
                    }
                    return sb.toString();
                }
            }
        }
        return null;
    }

    private void setAttributes(String importElement, Guide guide) {
        Matcher attributeMatcher = GUIDE_ELEMENT_PATTERN.matcher(importElement);
        while (attributeMatcher.find()) {
            String key = attributeMatcher.group("key");
            switch (key) {
                case "title":
                    guide.setTitle(attributeMatcher.group("valueString"));
                    break;
                case "summary":
                    guide.setSummary(attributeMatcher.group("valueString"));
                    break;
                case "priority":
                    guide.setPriority(Integer.parseInt(attributeMatcher.group("valueInt")));
            }
        }
    }

}
