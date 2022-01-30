package org.keycloak.guides.maven;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class Context {

    private File srcDir;
    private Options options;
    private Features features;
    private List<Guide> guides;

    public Context(File srcDir) throws IOException {
        this.srcDir = srcDir;
        this.options = new Options();
        this.features = new Features();

        this.guides = new LinkedList<>();

        GuideParser parser = new GuideParser();
        for (File f : new File(srcDir, "server").listFiles((dir, f) -> f.endsWith(".adoc") && !f.equals("index.adoc"))) {
            Guide guide = parser.parse(f);
            if (guide != null) {
                guides.add(guide);
            }
        }

        Collections.sort(guides, Comparator.comparingInt(Guide::getPriority));
    }

    public Options getOptions() {
        return options;
    }

    public Features getFeatures() {
        return features;
    }

    public List<Guide> getGuides() {
        return guides;
    }

}
