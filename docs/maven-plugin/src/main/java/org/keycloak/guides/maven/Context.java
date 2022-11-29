package org.keycloak.guides.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

        Map<String, Integer> guidePriorities = loadPinnedGuides(new File(srcDir, "pinned-guides"));

        for (File f : srcDir.listFiles((dir, f) -> f.endsWith(".adoc") && !f.equals("index.adoc"))) {
            Guide guide = parser.parse(f);

            if (guidePriorities != null) {
                Integer priority = guidePriorities.get(guide.getId());
                guide.setPriority(priority != null ? priority : Integer.MAX_VALUE);
            }

            if (guide != null) {
                guides.add(guide);
            }
        }

        Collections.sort(guides, (o1, o2) -> {
            if (o1.getPriority() == o2.getPriority()) {
                return o1.getTitle().compareTo(o2.getTitle());
            } else {
                return Integer.compare(o1.getPriority(), o2.getPriority());
            }
        });
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

    private Map<String, Integer> loadPinnedGuides(File pinnedGuides) throws IOException {
        if (!pinnedGuides.isFile()) {
            return null;
        }
        Map<String, Integer> priorities = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(pinnedGuides))) {
            int c = 1;
            for (String l = br.readLine(); l != null; l = br.readLine()) {
                l = l.trim();
                if (!l.isEmpty()) {
                    priorities.put(l, c);
                }
                c++;
            }
            return priorities;
        }
    }

}
