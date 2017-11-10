SELECT CONCAT(
    'SELECT "', table_name, '" AS table_name, COUNT(*) AS exact_row_count FROM `', table_schema, '`.`', table_name, '` UNION '
) FROM INFORMATION_SCHEMA.TABLES WHERE table_schema = 'keycloak';
