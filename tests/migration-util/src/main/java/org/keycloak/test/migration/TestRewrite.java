package org.keycloak.test.migration;

import java.util.List;

public abstract class TestRewrite {

    protected List<String> content;

    public abstract void rewrite();

    protected int findLine(String regex) {
        for (int i = 0; i < content.size(); i++) {
            if (content.get(i).matches(regex)) {
                return i;
            }
        }
        return -1;
    }

    protected int findLastLine(String regex) {
        int m = -1;
        for (int i = 0; i < content.size(); i++) {
            if (content.get(i).matches(regex)) {
                m = i;
            }
        }
        return m;
    }

    protected int findClassDeclaration() {
        return findLine("public class .*");
    }

    public void setContent(List<String> content) {
        this.content = content;
    }

    public void replaceLine(int line, String updated) {
        content.remove(line);
        content.add(line, updated);
    }

    public void addImport(String clazzName) {
        String add = "import " + clazzName + ";";

        int l = -1;
        int lastImport = -1;

        for (int i = 0; i < content.size(); i++) {
            String c = content.get(i);
            if (c.matches("import [^ ]*;")) {
                lastImport = i;
                if (c.compareTo(add) > 1) {
                    l = i;
                    break;
                }
            } else if (c.matches("^\\b(?:public\\s+)?class\\b.*Test\\s+\\{$")) {
                l = lastImport + 1;
                break;
            }
        }

        content.add(l, add);
        info(l, "Importing: " + clazzName);
    }

    protected void info(int line, String message) {
        System.out.println(String.format("%5s", line) + " " + message);
    }

}
