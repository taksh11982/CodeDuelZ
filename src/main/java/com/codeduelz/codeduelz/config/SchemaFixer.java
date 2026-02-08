package com.codeduelz.codeduelz.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaFixer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaFixer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("Attempting to drop unique constraint on matches.problem_id...");
            // PostgreSQL specific syntax to drop constraint if it exists
            jdbcTemplate.execute("ALTER TABLE matches DROP CONSTRAINT IF EXISTS matches_problem_id_key");
            // Also drop index if it was created separately (Hibernate often names it uk_...)
            // But usually dropping the constraint is enough.
            System.out.println("Schema fix executed successfully.");
        } catch (Exception e) {
            System.out.println("Schema fix failed (might already be fixed or different DB): " + e.getMessage());
        }
    }
}
