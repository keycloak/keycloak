package org.keycloak.schema.generator;

import java.lang.reflect.Method;

import org.keycloak.representations.migration.PreconditionContainerDescriptor;
import org.keycloak.representations.migration.PreconditionDescriptor;

import liquibase.precondition.CustomPreconditionWrapper;
import liquibase.precondition.Precondition;
import liquibase.precondition.core.AndPrecondition;
import liquibase.precondition.core.ChangeSetExecutedPrecondition;
import liquibase.precondition.core.ColumnExistsPrecondition;
import liquibase.precondition.core.DBMSPrecondition;
import liquibase.precondition.core.IndexExistsPrecondition;
import liquibase.precondition.core.NotPrecondition;
import liquibase.precondition.core.OrPrecondition;
import liquibase.precondition.core.PreconditionContainer;
import liquibase.precondition.core.PrimaryKeyExistsPrecondition;
import liquibase.precondition.core.SqlPrecondition;
import liquibase.precondition.core.TableExistsPrecondition;
import liquibase.precondition.core.UniqueConstraintExistsPrecondition;

public class PreconditionParser {

    public static PreconditionContainerDescriptor parseContainer(PreconditionContainer container) {
        if (container == null || container.getNestedPreconditions() == null || container.getNestedPreconditions().isEmpty()) {
            return null;
        }

        PreconditionContainerDescriptor descriptor = new PreconditionContainerDescriptor();
        if (container.getOnFail() != null) {
            descriptor.setOnFail(container.getOnFail().name());
        }
        if (container.getOnError() != null) {
            descriptor.setOnError(container.getOnError().name());
        }
        if (container.getOnSqlOutput() != null) {
            descriptor.setOnSqlOutput(container.getOnSqlOutput().name());
        }

        for (Precondition precondition : container.getNestedPreconditions()) {
            PreconditionDescriptor pDesc = parsePrecondition(precondition);
            if (pDesc != null) {
                descriptor.getConditions().add(pDesc);
            }
        }

        return descriptor;
    }

    public static PreconditionDescriptor parsePrecondition(Precondition precondition) {
        if (precondition == null) {
            return null;
        }

        PreconditionDescriptor desc = new PreconditionDescriptor();

        if (precondition instanceof TableExistsPrecondition tep) {
            desc.setType("tableExists");
            desc.setTableName(tep.getTableName());
        } else if (precondition instanceof ColumnExistsPrecondition cep) {
            desc.setType("columnExists");
            desc.setTableName(cep.getTableName());
            desc.setColumnName(cep.getColumnName());
        } else if (precondition instanceof IndexExistsPrecondition iep) {
            desc.setType("indexExists");
            desc.setTableName(iep.getTableName());
            desc.setIndexName(iep.getIndexName());
        } else if (precondition instanceof PrimaryKeyExistsPrecondition pk) {
            desc.setType("primaryKeyExists");
            desc.setPrimaryKeyName(pk.getPrimaryKeyName());
            desc.setTableName(pk.getTableName());
        } else if (precondition instanceof UniqueConstraintExistsPrecondition uc) {
            desc.setType("uniqueConstraintExists");
            desc.setTableName(uc.getTableName());
            desc.setConstraintName(uc.getConstraintName());
        } else if (precondition instanceof DBMSPrecondition dbms) {
            desc.setType("dbms");
            desc.setDbmsType(dbms.getType());
        } else if (precondition instanceof SqlPrecondition sql) {
            desc.setType("sqlCheck");
            desc.setSql(sql.getSql());
            desc.setExpectedResult(sql.getExpectedResult());
        } else if (precondition instanceof ChangeSetExecutedPrecondition cse) {
            desc.setType("changeSetExecuted");
            desc.setChangeSetId(cse.getId());
            desc.setChangeSetAuthor(cse.getAuthor());
            desc.setChangeSetFile(cse.getChangeLogFile());
        } else if (precondition instanceof AndPrecondition and) {
            desc.setType("and");
            for (Precondition nested : and.getNestedPreconditions()) {
                PreconditionDescriptor nestedDesc = parsePrecondition(nested);
                if (nestedDesc != null) {
                    desc.addNestedCondition(nestedDesc);
                }
            }
        } else if (precondition instanceof OrPrecondition or) {
            desc.setType("or");
            for (Precondition nested : or.getNestedPreconditions()) {
                PreconditionDescriptor nestedDesc = parsePrecondition(nested);
                if (nestedDesc != null) {
                    desc.addNestedCondition(nestedDesc);
                }
            }
        } else if (precondition instanceof NotPrecondition not) {
            desc.setType("not");
            for (Precondition nested : not.getNestedPreconditions()) {
                PreconditionDescriptor nestedDesc = parsePrecondition(nested);
                if (nestedDesc != null) {
                    desc.addNestedCondition(nestedDesc);
                }
            }
        } else if (precondition instanceof CustomPreconditionWrapper custom) {
            desc.setType("custom");
            desc.setSql(custom.getClassName());
        } else if (precondition.getName() != null && precondition.getName().equalsIgnoreCase("foreignKeyConstraintExists")) {
            desc.setType("foreignKeyExists");
            try {
                Method getFkName = precondition.getClass().getMethod("getForeignKeyName");
                desc.setForeignKeyName((String) getFkName.invoke(precondition));
            } catch (Exception ignored) {}
            try {
                Method getTableName = precondition.getClass().getMethod("getForeignKeyTableName");
                desc.setTableName((String) getTableName.invoke(precondition));
            } catch (Exception ignored) {}
        } else {
            desc.setType(precondition.getName());
        }

        return desc;
    }
}
