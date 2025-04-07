package org.keycloak.guides.maven;

public class Guide {

    private String template;
    private String id;
    private String title;
    private String summary;
    private int priority = Integer.MAX_VALUE;
    private Boolean tileVisible;

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

    public Boolean getTileVisible() {
        return tileVisible;
    }

    public void setTileVisible(Boolean tileVisible) {
        this.tileVisible = tileVisible;
    }
}
