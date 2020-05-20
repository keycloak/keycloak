package org.keycloak.connections.jpa;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.NullPrecedence;
import org.hibernate.ScrollMode;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.dialect.ColumnAliasExtractor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.LobMergeStrategy;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.loader.BatchLoadSizingStrategy;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.keycloak.runtime.KeycloakRecorder;

/**
 * Temporary solution for multiple database support on Quarkus until we get a capability from Quarkus to re-build the application
 */
public class DelegatingDialect extends Dialect {

    private Dialect dialect;

    private Dialect getInstance() {
        if (dialect == null) {
            // dialect is initialized during startup when hibernate is booting
            String dialectClazz = KeycloakRecorder.getDatabaseDialect();

            try {
                dialect = (Dialect) Class.forName(dialectClazz).getDeclaredConstructor().newInstance();
            } catch (Exception cause) {
                throw new RuntimeException("Failed to create dialect instance for [" + dialectClazz + "]", cause);
            }
        }

        return dialect;
    }

    @Override
    public void contributeTypes(TypeContributions typeContributions,
            ServiceRegistry serviceRegistry) {
        getInstance().contributeTypes(typeContributions, serviceRegistry);
    }

    @Override
    public String getTypeName(int code) throws HibernateException {
        return getInstance().getTypeName(code);
    }

    @Override
    public String getTypeName(int code, long length, int precision, int scale) throws HibernateException {
        return getInstance().getTypeName(code, length, precision, scale);
    }

    @Override
    public String getCastTypeName(int code) {
        return getInstance().getCastTypeName(code);
    }

    @Override
    public String cast(String value, int jdbcTypeCode, int length, int precision, int scale) {
        return getInstance().cast(value, jdbcTypeCode, length, precision, scale);
    }

    @Override
    public String cast(String value, int jdbcTypeCode, int length) {
        return getInstance().cast(value, jdbcTypeCode, length);
    }

    @Override
    public String cast(String value, int jdbcTypeCode, int precision, int scale) {
        return getInstance().cast(value, jdbcTypeCode, precision, scale);
    }

    @Override
    public SqlTypeDescriptor remapSqlTypeDescriptor(
            SqlTypeDescriptor sqlTypeDescriptor) {
        return getInstance().remapSqlTypeDescriptor(sqlTypeDescriptor);
    }

    @Override
    public LobMergeStrategy getLobMergeStrategy() {
        return getInstance().getLobMergeStrategy();
    }

    @Override
    public String getHibernateTypeName(int code) throws HibernateException {
        return getInstance().getHibernateTypeName(code);
    }

    @Override
    public boolean isTypeNameRegistered(String typeName) {
        return getInstance().isTypeNameRegistered(typeName);
    }

    @Override
    public String getHibernateTypeName(int code, int length, int precision, int scale) throws HibernateException {
        return getInstance().getHibernateTypeName(code, length, precision, scale);
    }

    @Override
    @Deprecated
    public Class getNativeIdentifierGeneratorClass() {
        return getInstance().getNativeIdentifierGeneratorClass();
    }

    @Override
    public String getNativeIdentifierGeneratorStrategy() {
        return getInstance().getNativeIdentifierGeneratorStrategy();
    }

    @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
        return getInstance().getIdentityColumnSupport();
    }

    @Override
    public boolean supportsSequences() {
        return getInstance().supportsSequences();
    }

    @Override
    public boolean supportsPooledSequences() {
        return getInstance().supportsPooledSequences();
    }

    @Override
    public String getSequenceNextValString(String sequenceName) throws MappingException {
        return getInstance().getSequenceNextValString(sequenceName);
    }

    @Override
    public String getSelectSequenceNextValString(String sequenceName) throws MappingException {
        return getInstance().getSelectSequenceNextValString(sequenceName);
    }

    @Override
    @Deprecated
    public String[] getCreateSequenceStrings(String sequenceName) throws MappingException {
        return getInstance().getCreateSequenceStrings(sequenceName);
    }

    @Override
    public String[] getCreateSequenceStrings(String sequenceName, int initialValue, int incrementSize) throws MappingException {
        return getInstance().getCreateSequenceStrings(sequenceName, initialValue, incrementSize);
    }

    @Override
    public String[] getDropSequenceStrings(String sequenceName) throws MappingException {
        return getInstance().getDropSequenceStrings(sequenceName);
    }

    @Override
    public String getQuerySequencesString() {
        return getInstance().getQuerySequencesString();
    }

    @Override
    public SequenceInformationExtractor getSequenceInformationExtractor() {
        return getInstance().getSequenceInformationExtractor();
    }

    @Override
    public String getSelectGUIDString() {
        return getInstance().getSelectGUIDString();
    }

    @Override
    public LimitHandler getLimitHandler() {
        return getInstance().getLimitHandler();
    }

    @Override
    @Deprecated
    public boolean supportsLimit() {
        return getInstance().supportsLimit();
    }

    @Override
    @Deprecated
    public boolean supportsLimitOffset() {
        return getInstance().supportsLimitOffset();
    }

    @Override
    @Deprecated
    public boolean supportsVariableLimit() {
        return getInstance().supportsVariableLimit();
    }

    @Override
    @Deprecated
    public boolean bindLimitParametersInReverseOrder() {
        return getInstance().bindLimitParametersInReverseOrder();
    }

    @Override
    @Deprecated
    public boolean bindLimitParametersFirst() {
        return getInstance().bindLimitParametersFirst();
    }

    @Override
    @Deprecated
    public boolean useMaxForLimit() {
        return getInstance().useMaxForLimit();
    }

    @Override
    @Deprecated
    public boolean forceLimitUsage() {
        return getInstance().forceLimitUsage();
    }

    @Override
    @Deprecated
    public String getLimitString(String query, int offset, int limit) {
        return getInstance().getLimitString(query, offset, limit);
    }

    @Override
    @Deprecated
    public int convertToFirstRowValue(int zeroBasedFirstResult) {
        return getInstance().convertToFirstRowValue(zeroBasedFirstResult);
    }

    @Override
    public boolean supportsLockTimeouts() {
        return getInstance().supportsLockTimeouts();
    }

    @Override
    public boolean isLockTimeoutParameterized() {
        return getInstance().isLockTimeoutParameterized();
    }

    @Override
    public LockingStrategy getLockingStrategy(Lockable lockable,
            LockMode lockMode) {
        return getInstance().getLockingStrategy(lockable, lockMode);
    }

    @Override
    public String getForUpdateString(LockOptions lockOptions) {
        return getInstance().getForUpdateString(lockOptions);
    }

    @Override
    public String getForUpdateString(LockMode lockMode) {
        return getInstance().getForUpdateString(lockMode);
    }

    @Override
    public String getForUpdateString() {
        return getInstance().getForUpdateString();
    }

    @Override
    public String getWriteLockString(int timeout) {
        return getInstance().getWriteLockString(timeout);
    }

    @Override
    public String getWriteLockString(String aliases, int timeout) {
        return getInstance().getWriteLockString(aliases, timeout);
    }

    @Override
    public String getReadLockString(int timeout) {
        return getInstance().getReadLockString(timeout);
    }

    @Override
    public String getReadLockString(String aliases, int timeout) {
        return getInstance().getReadLockString(aliases, timeout);
    }

    @Override
    public boolean forUpdateOfColumns() {
        return getInstance().forUpdateOfColumns();
    }

    @Override
    public boolean supportsOuterJoinForUpdate() {
        return getInstance().supportsOuterJoinForUpdate();
    }

    @Override
    public String getForUpdateString(String aliases) {
        return getInstance().getForUpdateString(aliases);
    }

    @Override
    public String getForUpdateString(String aliases, LockOptions lockOptions) {
        return getInstance().getForUpdateString(aliases, lockOptions);
    }

    @Override
    public String getForUpdateNowaitString() {
        return getInstance().getForUpdateNowaitString();
    }

    @Override
    public String getForUpdateSkipLockedString() {
        return getInstance().getForUpdateSkipLockedString();
    }

    @Override
    public String getForUpdateNowaitString(String aliases) {
        return getInstance().getForUpdateNowaitString(aliases);
    }

    @Override
    public String getForUpdateSkipLockedString(String aliases) {
        return getInstance().getForUpdateSkipLockedString(aliases);
    }

    @Override
    @Deprecated
    public String appendLockHint(LockMode mode, String tableName) {
        return getInstance().appendLockHint(mode, tableName);
    }

    @Override
    public String appendLockHint(LockOptions lockOptions, String tableName) {
        return getInstance().appendLockHint(lockOptions, tableName);
    }

    @Override
    public String applyLocksToSql(String sql, LockOptions aliasedLockOptions,
            Map<String, String[]> keyColumnNames) {
        return getInstance().applyLocksToSql(sql, aliasedLockOptions, keyColumnNames);
    }

    @Override
    public String getCreateTableString() {
        return getInstance().getCreateTableString();
    }

    @Override
    public String getAlterTableString(String tableName) {
        return getInstance().getAlterTableString(tableName);
    }

    @Override
    public String getCreateMultisetTableString() {
        return getInstance().getCreateMultisetTableString();
    }

    @Override
    public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
        return getInstance().getDefaultMultiTableBulkIdStrategy();
    }

    @Override
    public int registerResultSetOutParameter(CallableStatement statement, int position) throws SQLException {
        return getInstance().registerResultSetOutParameter(statement, position);
    }

    @Override
    public int registerResultSetOutParameter(CallableStatement statement, String name) throws SQLException {
        return getInstance().registerResultSetOutParameter(statement, name);
    }

    @Override
    public ResultSet getResultSet(CallableStatement statement) throws SQLException {
        return getInstance().getResultSet(statement);
    }

    @Override
    public ResultSet getResultSet(CallableStatement statement, int position) throws SQLException {
        return getInstance().getResultSet(statement, position);
    }

    @Override
    public ResultSet getResultSet(CallableStatement statement, String name) throws SQLException {
        return getInstance().getResultSet(statement, name);
    }

    @Override
    public boolean supportsCurrentTimestampSelection() {
        return getInstance().supportsCurrentTimestampSelection();
    }

    @Override
    public boolean isCurrentTimestampSelectStringCallable() {
        return getInstance().isCurrentTimestampSelectStringCallable();
    }

    @Override
    public String getCurrentTimestampSelectString() {
        return getInstance().getCurrentTimestampSelectString();
    }

    @Override
    public String getCurrentTimestampSQLFunctionName() {
        return getInstance().getCurrentTimestampSQLFunctionName();
    }

    @Override
    @Deprecated
    public SQLExceptionConverter buildSQLExceptionConverter() {
        return getInstance().buildSQLExceptionConverter();
    }

    @Override
    public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
        return getInstance().buildSQLExceptionConversionDelegate();
    }

    @Override
    public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
        return getInstance().getViolatedConstraintNameExtracter();
    }

    @Override
    public String getSelectClauseNullString(int sqlType) {
        return getInstance().getSelectClauseNullString(sqlType);
    }

    @Override
    public boolean supportsUnionAll() {
        return getInstance().supportsUnionAll();
    }

    @Override
    public JoinFragment createOuterJoinFragment() {
        return getInstance().createOuterJoinFragment();
    }

    @Override
    public CaseFragment createCaseFragment() {
        return getInstance().createCaseFragment();
    }

    @Override
    public String getNoColumnsInsertString() {
        return getInstance().getNoColumnsInsertString();
    }

    @Override
    public boolean supportsNoColumnsInsert() {
        return getInstance().supportsNoColumnsInsert();
    }

    @Override
    public String getLowercaseFunction() {
        return getInstance().getLowercaseFunction();
    }

    @Override
    public String getCaseInsensitiveLike() {
        return getInstance().getCaseInsensitiveLike();
    }

    @Override
    public boolean supportsCaseInsensitiveLike() {
        return getInstance().supportsCaseInsensitiveLike();
    }

    @Override
    public String transformSelectString(String select) {
        return getInstance().transformSelectString(select);
    }

    @Override
    public int getMaxAliasLength() {
        return getInstance().getMaxAliasLength();
    }

    @Override
    public String toBooleanValueString(boolean bool) {
        return getInstance().toBooleanValueString(bool);
    }

    @Override
    @Deprecated
    public Set<String> getKeywords() {
        return getInstance().getKeywords();
    }

    @Override
    public IdentifierHelper buildIdentifierHelper(
            IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData) throws SQLException {
        return getInstance().buildIdentifierHelper(builder, dbMetaData);
    }

    @Override
    public char openQuote() {
        return getInstance().openQuote();
    }

    @Override
    public char closeQuote() {
        return getInstance().closeQuote();
    }

    @Override
    public Exporter<Table> getTableExporter() {
        return getInstance().getTableExporter();
    }

    @Override
    public Exporter<Sequence> getSequenceExporter() {
        return getInstance().getSequenceExporter();
    }

    @Override
    public Exporter<Index> getIndexExporter() {
        return getInstance().getIndexExporter();
    }

    @Override
    public Exporter<ForeignKey> getForeignKeyExporter() {
        return getInstance().getForeignKeyExporter();
    }

    @Override
    public Exporter<Constraint> getUniqueKeyExporter() {
        return getInstance().getUniqueKeyExporter();
    }

    @Override
    public Exporter<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjectExporter() {
        return getInstance().getAuxiliaryDatabaseObjectExporter();
    }

    @Override
    public boolean canCreateCatalog() {
        return getInstance().canCreateCatalog();
    }

    @Override
    public String[] getCreateCatalogCommand(String catalogName) {
        return getInstance().getCreateCatalogCommand(catalogName);
    }

    @Override
    public String[] getDropCatalogCommand(String catalogName) {
        return getInstance().getDropCatalogCommand(catalogName);
    }

    @Override
    public boolean canCreateSchema() {
        return getInstance().canCreateSchema();
    }

    @Override
    public String[] getCreateSchemaCommand(String schemaName) {
        return getInstance().getCreateSchemaCommand(schemaName);
    }

    @Override
    public String[] getDropSchemaCommand(String schemaName) {
        return getInstance().getDropSchemaCommand(schemaName);
    }

    @Override
    public String getCurrentSchemaCommand() {
        return getInstance().getCurrentSchemaCommand();
    }

    @Override
    public SchemaNameResolver getSchemaNameResolver() {
        return getInstance().getSchemaNameResolver();
    }

    @Override
    public boolean hasAlterTable() {
        return getInstance().hasAlterTable();
    }

    @Override
    public boolean dropConstraints() {
        return getInstance().dropConstraints();
    }

    @Override
    public boolean qualifyIndexName() {
        return getInstance().qualifyIndexName();
    }

    @Override
    public String getAddColumnString() {
        return getInstance().getAddColumnString();
    }

    @Override
    public String getAddColumnSuffixString() {
        return getInstance().getAddColumnSuffixString();
    }

    @Override
    public String getDropForeignKeyString() {
        return getInstance().getDropForeignKeyString();
    }

    @Override
    public String getTableTypeString() {
        return getInstance().getTableTypeString();
    }

    @Override
    public String getAddForeignKeyConstraintString(String constraintName, String[] foreignKey, String referencedTable,
            String[] primaryKey, boolean referencesPrimaryKey) {
        return getInstance().getAddForeignKeyConstraintString(constraintName, foreignKey, referencedTable, primaryKey,
                referencesPrimaryKey);
    }

    @Override
    public String getAddForeignKeyConstraintString(String constraintName, String foreignKeyDefinition) {
        return getInstance().getAddForeignKeyConstraintString(constraintName, foreignKeyDefinition);
    }

    @Override
    public String getAddPrimaryKeyConstraintString(String constraintName) {
        return getInstance().getAddPrimaryKeyConstraintString(constraintName);
    }

    @Override
    public boolean hasSelfReferentialForeignKeyBug() {
        return getInstance().hasSelfReferentialForeignKeyBug();
    }

    @Override
    public String getNullColumnString() {
        return getInstance().getNullColumnString();
    }

    @Override
    public boolean supportsCommentOn() {
        return getInstance().supportsCommentOn();
    }

    @Override
    public String getTableComment(String comment) {
        return getInstance().getTableComment(comment);
    }

    @Override
    public String getColumnComment(String comment) {
        return getInstance().getColumnComment(comment);
    }

    @Override
    public boolean supportsIfExistsBeforeTableName() {
        return getInstance().supportsIfExistsBeforeTableName();
    }

    @Override
    public boolean supportsIfExistsAfterTableName() {
        return getInstance().supportsIfExistsAfterTableName();
    }

    @Override
    public boolean supportsIfExistsBeforeConstraintName() {
        return getInstance().supportsIfExistsBeforeConstraintName();
    }

    @Override
    public boolean supportsIfExistsAfterConstraintName() {
        return getInstance().supportsIfExistsAfterConstraintName();
    }

    @Override
    public boolean supportsIfExistsAfterAlterTable() {
        return getInstance().supportsIfExistsAfterAlterTable();
    }

    @Override
    public String getDropTableString(String tableName) {
        return getInstance().getDropTableString(tableName);
    }

    @Override
    public boolean supportsColumnCheck() {
        return getInstance().supportsColumnCheck();
    }

    @Override
    public boolean supportsTableCheck() {
        return getInstance().supportsTableCheck();
    }

    @Override
    public boolean supportsCascadeDelete() {
        return getInstance().supportsCascadeDelete();
    }

    @Override
    public String getCascadeConstraintsString() {
        return getInstance().getCascadeConstraintsString();
    }

    @Override
    public String getCrossJoinSeparator() {
        return getInstance().getCrossJoinSeparator();
    }

    @Override
    public ColumnAliasExtractor getColumnAliasExtractor() {
        return getInstance().getColumnAliasExtractor();
    }

    @Override
    public boolean supportsEmptyInList() {
        return getInstance().supportsEmptyInList();
    }

    @Override
    public boolean areStringComparisonsCaseInsensitive() {
        return getInstance().areStringComparisonsCaseInsensitive();
    }

    @Override
    public boolean supportsRowValueConstructorSyntax() {
        return getInstance().supportsRowValueConstructorSyntax();
    }

    @Override
    public boolean supportsRowValueConstructorSyntaxInInList() {
        return getInstance().supportsRowValueConstructorSyntaxInInList();
    }

    @Override
    public boolean useInputStreamToInsertBlob() {
        return getInstance().useInputStreamToInsertBlob();
    }

    @Override
    public boolean supportsParametersInInsertSelect() {
        return getInstance().supportsParametersInInsertSelect();
    }

    @Override
    public boolean replaceResultVariableInOrderByClauseWithPosition() {
        return getInstance().replaceResultVariableInOrderByClauseWithPosition();
    }

    @Override
    public String renderOrderByElement(String expression, String collation, String order, NullPrecedence nulls) {
        return getInstance().renderOrderByElement(expression, collation, order, nulls);
    }

    @Override
    public boolean requiresCastingOfParametersInSelectClause() {
        return getInstance().requiresCastingOfParametersInSelectClause();
    }

    @Override
    public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
        return getInstance().supportsResultSetPositionQueryMethodsOnForwardOnlyCursor();
    }

    @Override
    public boolean supportsCircularCascadeDeleteConstraints() {
        return getInstance().supportsCircularCascadeDeleteConstraints();
    }

    @Override
    public boolean supportsSubselectAsInPredicateLHS() {
        return getInstance().supportsSubselectAsInPredicateLHS();
    }

    @Override
    public boolean supportsExpectedLobUsagePattern() {
        return getInstance().supportsExpectedLobUsagePattern();
    }

    @Override
    public boolean supportsLobValueChangePropogation() {
        return getInstance().supportsLobValueChangePropogation();
    }

    @Override
    public boolean supportsUnboundedLobLocatorMaterialization() {
        return getInstance().supportsUnboundedLobLocatorMaterialization();
    }

    @Override
    public boolean supportsSubqueryOnMutatingTable() {
        return getInstance().supportsSubqueryOnMutatingTable();
    }

    @Override
    public boolean supportsExistsInSelect() {
        return getInstance().supportsExistsInSelect();
    }

    @Override
    public boolean doesReadCommittedCauseWritersToBlockReaders() {
        return getInstance().doesReadCommittedCauseWritersToBlockReaders();
    }

    @Override
    public boolean doesRepeatableReadCauseReadersToBlockWriters() {
        return getInstance().doesRepeatableReadCauseReadersToBlockWriters();
    }

    @Override
    public boolean supportsBindAsCallableArgument() {
        return getInstance().supportsBindAsCallableArgument();
    }

    @Override
    public boolean supportsTupleCounts() {
        return getInstance().supportsTupleCounts();
    }

    @Override
    public boolean supportsTupleDistinctCounts() {
        return getInstance().supportsTupleDistinctCounts();
    }

    @Override
    public boolean requiresParensForTupleDistinctCounts() {
        return getInstance().requiresParensForTupleDistinctCounts();
    }

    @Override
    public int getInExpressionCountLimit() {
        return getInstance().getInExpressionCountLimit();
    }

    @Override
    public boolean forceLobAsLastValue() {
        return getInstance().forceLobAsLastValue();
    }

    @Override
    @Deprecated
    public boolean useFollowOnLocking() {
        return getInstance().useFollowOnLocking();
    }

    @Override
    public boolean useFollowOnLocking(QueryParameters parameters) {
        return getInstance().useFollowOnLocking(parameters);
    }

    @Override
    public String getNotExpression(String expression) {
        return getInstance().getNotExpression(expression);
    }

    @Override
    public UniqueDelegate getUniqueDelegate() {
        return getInstance().getUniqueDelegate();
    }

    @Override
    @Deprecated
    public boolean supportsUnique() {
        return getInstance().supportsUnique();
    }

    @Override
    @Deprecated
    public boolean supportsUniqueConstraintInCreateAlterTable() {
        return getInstance().supportsUniqueConstraintInCreateAlterTable();
    }

    @Override
    @Deprecated
    public String getAddUniqueConstraintString(String constraintName) {
        return getInstance().getAddUniqueConstraintString(constraintName);
    }

    @Override
    @Deprecated
    public boolean supportsNotNullUnique() {
        return getInstance().supportsNotNullUnique();
    }

    @Override
    public String getQueryHintString(String query, List<String> hintList) {
        return getInstance().getQueryHintString(query, hintList);
    }

    @Override
    public String getQueryHintString(String query, String hints) {
        return getInstance().getQueryHintString(query, hints);
    }

    @Override
    public ScrollMode defaultScrollMode() {
        return getInstance().defaultScrollMode();
    }

    @Override
    public boolean supportsTuplesInSubqueries() {
        return getInstance().supportsTuplesInSubqueries();
    }

    @Override
    public CallableStatementSupport getCallableStatementSupport() {
        return getInstance().getCallableStatementSupport();
    }

    @Override
    public NameQualifierSupport getNameQualifierSupport() {
        return getInstance().getNameQualifierSupport();
    }

    @Override
    public BatchLoadSizingStrategy getDefaultBatchLoadSizingStrategy() {
        return getInstance().getDefaultBatchLoadSizingStrategy();
    }

    @Override
    public boolean isJdbcLogWarningsEnabledByDefault() {
        return getInstance().isJdbcLogWarningsEnabledByDefault();
    }

    @Override
    public void augmentRecognizedTableTypes(List<String> tableTypesList) {
        getInstance().augmentRecognizedTableTypes(tableTypesList);
    }

    @Override
    public boolean supportsPartitionBy() {
        return getInstance().supportsPartitionBy();
    }

    @Override
    public boolean supportsNamedParameters(DatabaseMetaData databaseMetaData) throws SQLException {
        return getInstance().supportsNamedParameters(databaseMetaData);
    }

    @Override
    public boolean supportsNationalizedTypes() {
        return getInstance().supportsNationalizedTypes();
    }

    @Override
    public boolean supportsNonQueryWithCTE() {
        return getInstance().supportsNonQueryWithCTE();
    }

    @Override
    public boolean supportsValuesList() {
        return getInstance().supportsValuesList();
    }

    @Override
    public boolean supportsSkipLocked() {
        return getInstance().supportsSkipLocked();
    }

    @Override
    public boolean supportsNoWait() {
        return getInstance().supportsNoWait();
    }

    @Override
    public boolean isLegacyLimitHandlerBehaviorEnabled() {
        return getInstance().isLegacyLimitHandlerBehaviorEnabled();
    }

    @Override
    public String inlineLiteral(String literal) {
        return getInstance().inlineLiteral(literal);
    }

    @Override
    public boolean supportsJdbcConnectionLobCreation(DatabaseMetaData databaseMetaData) {
        return getInstance().supportsJdbcConnectionLobCreation(databaseMetaData);
    }

    @Override
    public String addSqlHintOrComment(String sql, QueryParameters parameters, boolean commentsEnabled) {
        return getInstance().addSqlHintOrComment(sql, parameters, commentsEnabled);
    }

    @Override
    public boolean supportsSelectAliasInGroupByClause() {
        return getInstance().supportsSelectAliasInGroupByClause();
    }

    @Override
    public String toString() {
        return getInstance().toString();
    }
}
