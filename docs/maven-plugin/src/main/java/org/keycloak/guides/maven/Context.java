package org.keycloak.guides.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Context {

    private File srcDir;
    private Options options;
    private List<Guide> guides;

    public Context(File srcDir) throws IOException {
        this.srcDir = srcDir;
        this.options = new Options();

        this.guides = new LinkedList<>();

        for (File f : new File(srcDir, "server").listFiles((dir, f) -> f.endsWith(".adoc") && !f.equals("index.adoc"))) {
            Guide g = getGuide(f);
            if (g != null) {
                guides.add(g);
            }
        }

        Collections.sort(guides, Comparator.comparingInt(Guide::getPriority));
    }

    private Guide getGuide(File f) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        StringBuilder sb = new StringBuilder();

        String l = br.readLine();
        while (l != null) {
            if (l.contains("<@tmpl.guide")) {
                sb.append(l.trim());

                while (!l.contains(">")) {
                    l = br.readLine();
                    sb.append(" " + l.trim());
                }

                Guide g = new Guide();
                g.setTemplate(f.getName());
                g.setPriority(999);

                g.setId(f.getName().replaceAll(".adoc", ""));

                Matcher matcher = Pattern.compile("(priority|title|summary)=(\\\"([^\\\"]*)\\\"|([\\d]*))").matcher(sb.toString());
                while (matcher.find()) {
                    String k = matcher.group(1);
                    switch (k) {
                        case "title":
                            g.setTitle(matcher.group(3));
                            break;
                        case "summary":
                            g.setSummary(matcher.group(3));
                            break;
                        case "priority":
                            g.setPriority(Integer.parseInt(matcher.group(4)));
                    }
                }

                return g;
            }
            l = br.readLine();
        }

        return null;
    }

    public Options getOptions() {
        return options;
    }

    public List<Guide> getGuides() {
        return guides;
    }
}
