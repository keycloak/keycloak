-- Clear all tables from dbo schema (MSSQL equivalent of PostgreSQL schema reset)

-- Drop all foreign key constraints first
EXEC sp_executesql N'DECLARE @sql NVARCHAR(MAX) = N''''; SELECT @sql += N''ALTER TABLE '' + QUOTENAME(s.name) + ''.'' + QUOTENAME(t.name) + '' DROP CONSTRAINT '' + QUOTENAME(f.name) + '';'' FROM sys.foreign_keys f INNER JOIN sys.tables t ON f.parent_object_id = t.object_id INNER JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE s.name = ''dbo''; EXEC sp_executesql @sql;'

-- Drop all tables
EXEC sp_executesql N'DECLARE @sql NVARCHAR(MAX) = N''''; SELECT @sql += N''DROP TABLE '' + QUOTENAME(s.name) + ''.'' + QUOTENAME(t.name) + '';'' FROM sys.tables t INNER JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE s.name = ''dbo''; EXEC sp_executesql @sql;'
