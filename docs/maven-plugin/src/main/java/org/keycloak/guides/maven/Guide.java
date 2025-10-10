package org.keycloak.guides.maven;

import java.nio.file.Path;

public class Guide {

    private String template;
    private String id;
    private String title;
    private String summary;
    private int priority = Integer.MAX_VALUE;
    private boolean tileVisible = true;
    private Path root;
    private Path path;
    private int levelOffset = 1;

    public static String toId(String path) {
        return path.replace("/", "-").replace("\\", "-").replace(".adoc", "");
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isTileVisible() {
        return tileVisible;
    }

    public void setTileVisible(boolean tileVisible) {
        this.tileVisible = tileVisible;
    }

    public Path getRoot() {
        return root;
    }

    public void setRoot(Path root) {
        this.root = root;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public int getLevelOffset() {
        return levelOffset;
    }

    public void setLevelOffset(int levelOffset) {
        this.levelOffset = levelOffset;
    }
}
