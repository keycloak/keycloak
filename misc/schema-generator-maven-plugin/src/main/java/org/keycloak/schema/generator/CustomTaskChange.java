package org.keycloak.schema.generator;

import java.util.HashMap;
import java.util.Map;

import liquibase.change.AbstractChange;
import liquibase.change.ChangeMetaData;
import liquibase.change.DatabaseChange;
import liquibase.change.DatabaseChangeProperty;
import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.parser.core.ParsedNode;
import liquibase.parser.core.ParsedNodeException;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;

@DatabaseChange(
        name = "customChange",
        description = "Custom migration task placeholder",
        priority = ChangeMetaData.PRIORITY_DEFAULT + 10
)
public class CustomTaskChange extends AbstractChange {

    private String className;
    private Map<String, String> params = new HashMap<>();

    @DatabaseChangeProperty(description = "Class name of the custom task", requiredForDatabase = "all")
    public String getClassName() {
        return className;
    }

    public void setClass(String className) {
        this.className = className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @DatabaseChangeProperty(description = "Parameters for custom task")
    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public void setParam(String name, String value) {
        this.params.put(name, value);
    }

    @Override
    public void load(ParsedNode parsedNode, ResourceAccessor resourceAccessor) throws ParsedNodeException {
        this.className = parsedNode.getChildValue(null, "class", String.class);
        if (this.className == null) {
            this.className = parsedNode.getChildValue(null, "className", String.class);
        }
        super.load(parsedNode, resourceAccessor);
        if (this.className == null) {
            String val = parsedNode.getChildValue(null, "class", String.class);
            if (val != null) {
                this.className = val;
            }
        }
    }

    @Override
    public boolean supports(Database database) {
        return true;
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }

    @Override
    public SqlStatement[] generateStatements(Database database) {
        return new SqlStatement[0];
    }

    @Override
    public String getConfirmationMessage() {
        return "Custom task: " + className;
    }
}
