package ru.astrakhan.admin.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * Дополняет общую PostgreSQL (module3) колонками, которые ожидает JPA, но которых нет в старом дампе {@code docs/db/astra_schema.sql}.
 * Без них падают /admin, /admin/routes и GET /api/v1/routes (module2).
 */
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        migrateRoutesLifecycleColumns();
    }

    private void migrateRoutesLifecycleColumns() {
        if (!tableExists("routes")) {
            log.warn("Table routes not found — skip route lifecycle migration");
            return;
        }
        addColumnIfMissing("routes", "priority", "INTEGER", "0");
        addColumnIfMissing("routes", "status", "VARCHAR(32)", "'DRAFT'");
        addColumnIfMissing("routes", "outdated_reason", "TEXT", null);
        jdbcTemplate.update("UPDATE routes SET priority = 0 WHERE priority IS NULL");
        jdbcTemplate.update("UPDATE routes SET status = 'DRAFT' WHERE status IS NULL OR TRIM(status) = ''");
        log.info("Route lifecycle columns verified (priority, status, outdated_reason)");
    }

    private boolean tableExists(String tableName) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog();
            String schema = resolveSchema(conn);
            try (ResultSet rs = meta.getTables(catalog, schema, tableName, new String[]{"TABLE"})) {
                if (rs.next()) {
                    return true;
                }
            }
            try (ResultSet rs = meta.getTables(catalog, schema, tableName.toUpperCase(), new String[]{"TABLE"})) {
                return rs.next();
            }
        } catch (Exception e) {
            log.warn("Could not check table {}: {}", tableName, e.getMessage());
            return false;
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog();
            String schema = resolveSchema(conn);
            try (ResultSet rs = meta.getColumns(catalog, schema, tableName, columnName)) {
                if (rs.next()) {
                    return true;
                }
            }
            try (ResultSet rs = meta.getColumns(catalog, schema, tableName.toUpperCase(), columnName.toUpperCase())) {
                return rs.next();
            }
        } catch (Exception e) {
            log.warn("Could not check column {}.{}: {}", tableName, columnName, e.getMessage());
            return false;
        }
    }

    private static String resolveSchema(Connection conn) throws java.sql.SQLException {
        String schema = conn.getSchema();
        if (schema != null && !schema.isBlank()) {
            return schema;
        }
        return null;
    }

    private void addColumnIfMissing(String table, String column, String sqlType, String defaultLiteral) {
        if (columnExists(table, column)) {
            return;
        }
        StringBuilder sql = new StringBuilder("ALTER TABLE ")
                .append(table)
                .append(" ADD COLUMN ")
                .append(column)
                .append(" ")
                .append(sqlType);
        if (defaultLiteral != null) {
            sql.append(" DEFAULT ").append(defaultLiteral);
        }
        jdbcTemplate.execute(sql.toString());
        log.info("Added column {}.{}", table, column);
    }
}
