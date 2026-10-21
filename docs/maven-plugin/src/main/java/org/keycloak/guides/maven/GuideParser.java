package org.keycloak.guides.maven;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuideParser {

    private final Pattern TEMPLATE_IMPORT_PATTERN = Pattern.compile("<#import \"/templates/guide.adoc\" as (?<importName>[^ ]*)>");
    private final Pattern GUIDE_ELEMENT_PATTERN = Pattern.compile("(?<key>priority|title|summary|tileVisible|levelOffset)=(\\\"(?<valueString>[^\\\"]*)\\\"|(?<valueInt>[\\d]*))");

    /**
     * Parses a FreeMarker template to retrieve Guide attributes
     * @param guidePath
     * @return A Guide instance; or <code>null</code> if not a guide
     * @throws IOException
     */
    public Guide parse(Path root, Path guidePath) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(guidePath)) {
            String importName = getImportName(br);
            String importElement = getGuideElement(br, importName);

            if (importElement != null) {
                Guide guide = new Guide();
                Path relativePath = root.relativize(guidePath);
                guide.setId(Guide.toId(relativePath.toString()));
                guide.setPath(guidePath);
                guide.setRoot(root);
                guide.setTemplate(relativePath.toString());
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
                    break;
                case "tileVisible":
                    guide.setTileVisible(Boolean.parseBoolean(attributeMatcher.group("valueString")));
                    break;
                case "levelOffset":
                    guide.setLevelOffset(Integer.parseInt(attributeMatcher.group("valueInt")));
                    break;
            }
        }
    }
}
