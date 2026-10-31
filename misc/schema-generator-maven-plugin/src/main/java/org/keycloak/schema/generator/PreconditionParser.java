package org.keycloak.schema.generator;

import org.keycloak.representations.migration.PreconditionContainerDescriptor;
import org.keycloak.representations.migration.PreconditionDescriptor;

import liquibase.precondition.CustomPreconditionWrapper;
import liquibase.precondition.Precondition;
import liquibase.precondition.core.AndPrecondition;
import liquibase.precondition.core.ChangeSetExecutedPrecondition;
import liquibase.precondition.core.ColumnExistsPrecondition;
import liquibase.precondition.core.DBMSPrecondition;
import liquibase.precondition.core.ForeignKeyExistsPrecondition;
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
        // The JDBC migration engine (PreconditionEvaluator) only implements a subset of the Liquibase
        // container options; reject the rest rather than have them silently behave differently at runtime.
        if (container.getOnFail() != null) {
            String onFail = container.getOnFail().name();
            requireSupported("onFail", onFail, "HALT", "MARK_RAN");
            descriptor.setOnFail(onFail);
        }
        if (container.getOnError() != null) {
            String onError = container.getOnError().name();
            // Errors while evaluating a precondition always abort the migration at runtime.
            requireSupported("onError", onError, "HALT");
            descriptor.setOnError(onError);
        }
        if (container.getOnSqlOutput() != null) {
            String onSqlOutput = container.getOnSqlOutput().name();
            requireSupported("onSqlOutput", onSqlOutput, "IGNORE", "TEST");
            descriptor.setOnSqlOutput(onSqlOutput);
        }

        for (Precondition precondition : container.getNestedPreconditions()) {
            PreconditionDescriptor pDesc = parsePrecondition(precondition);
            if (pDesc != null) {
                descriptor.getConditions().add(pDesc);
            }
        }

        return descriptor;
    }

    private static void requireSupported(String attribute, String value, String... supported) {
        for (String s : supported) {
            if (s.equals(value)) {
                return;
            }
        }
        throw new IllegalArgumentException("Unsupported preConditions " + attribute + "=" + value
                + " (supported: " + String.join(", ", supported) + ")");
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
        } else if (precondition instanceof ForeignKeyExistsPrecondition fk) {
            desc.setType("foreignKeyExists");
            desc.setForeignKeyName(fk.getForeignKeyName());
            desc.setTableName(fk.getForeignKeyTableName());
        } else {
            throw new IllegalArgumentException("Unsupported Liquibase precondition type: " + precondition.getClass().getName());
        }

        return desc;
    }
}
