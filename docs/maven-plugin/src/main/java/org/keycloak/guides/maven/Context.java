package org.keycloak.guides.maven;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Context {

    private final Options options;
    private final Features features;
    private final List<Guide> guides;

    public Context(Path srcPath) throws IOException {
        this.options = new Options();
        this.features = new Features();
        this.guides = new LinkedList<>();

        Path partials = srcPath.resolve("partials");
        Map<String, Integer> guidePriorities = loadPinnedGuides(srcPath);

        List<Path> guidePaths;
        try (Stream<Path> files = Files.walk(srcPath)) {
            guidePaths = files
                  .filter(Files::isRegularFile)
                  .filter(p -> !p.startsWith(partials))
                  .filter(p -> p.getFileName().toString().endsWith(".adoc"))
                  .filter(p -> !p.getFileName().toString().equals("index.adoc"))
                  .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load guides from " + srcPath, e);
        }

        GuideParser parser = new GuideParser();
        for (Path guidePath : guidePaths) {
            Guide guide = parser.parse(srcPath, guidePath);

            if (guide != null) {
                if (guidePriorities != null) {
                    Integer priority = guidePriorities.get(guide.getId());
                    if (priority != null) {
                        if (guide.getPriority() != Integer.MAX_VALUE) {
                            throw new RuntimeException("Guide is pinned, but has a priority specified: " + guidePath.getFileName());
                        }
                        guidePriorities.remove(guide.getId());
                        guide.setPriority(priority);
                    }
                }

                if (!guide.isTileVisible() && guide.getPriority() == Integer.MAX_VALUE) {
                    throw new RuntimeException("Invisible tiles should be pinned or have an explicit priority: " + guidePath.getFileName());
                }

                guides.add(guide);
            }
        }

        if (guidePriorities != null && !guidePriorities.isEmpty()) {
            throw new RuntimeException("File 'pinned-guides' contains files that no longer exist or are misspelled: " + guidePriorities.keySet());
        }

        guides.sort((o1, o2) -> {
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

    private Map<String, Integer> loadPinnedGuides(Path src) throws IOException {
        Path pinnedGuides = src.resolve("pinned-guides");
        if (Files.notExists(pinnedGuides) || Files.isDirectory(pinnedGuides)) {
            return null;
        }
        Map<String, Integer> priorities = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(pinnedGuides)) {
            int c = 1;
            for (String l = br.readLine(); l != null; l = br.readLine()) {
                l = l.trim();
                if (!l.isEmpty()) {
                    priorities.put(Guide.toId(l), c);
                }
                c++;
            }
            return priorities;
        }
    }
}
