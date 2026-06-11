package ads.uninassau.brjobs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class HighlightSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        boolean publicacoesTableExists = Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
            SELECT COUNT(*) > 0
            FROM information_schema.tables
            WHERE lower(table_name) = 'publicacoes_servico'
            """, Boolean.class));

        if (!publicacoesTableExists) {
            log.warn("Highlight schema alignment skipped: publicacoes_servico table not found");
            return;
        }

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS highlight_plans (
                id BIGSERIAL PRIMARY KEY,
                name VARCHAR(60) NOT NULL UNIQUE,
                price DOUBLE PRECISION NOT NULL,
                duration_days INTEGER NOT NULL,
                priority INTEGER NOT NULL,
                active BOOLEAN NOT NULL DEFAULT TRUE
            )
            """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS highlight_payments (
                id BIGSERIAL PRIMARY KEY,
                publicacao_id BIGINT NOT NULL,
                highlight_plan_id BIGINT NOT NULL,
                usuario_id BIGINT NOT NULL,
                stripe_session_id VARCHAR(255) NOT NULL UNIQUE,
                stripe_event_id VARCHAR(255),
                status VARCHAR(20) NOT NULL,
                amount DOUBLE PRECISION NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP
            )
            """);

        jdbcTemplate.execute("ALTER TABLE publicacoes_servico ADD COLUMN IF NOT EXISTS is_highlighted BOOLEAN NOT NULL DEFAULT FALSE");
        jdbcTemplate.execute("ALTER TABLE publicacoes_servico ADD COLUMN IF NOT EXISTS highlight_expires_at TIMESTAMP");
        jdbcTemplate.execute("ALTER TABLE publicacoes_servico ADD COLUMN IF NOT EXISTS highlight_plan_id BIGINT");

        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_publicacoes_destaque ON publicacoes_servico (is_highlighted, highlight_expires_at, data_criacao DESC)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_highlight_plan_priority ON highlight_plans (priority)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_highlight_payment_publicacao ON highlight_payments (publicacao_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_highlight_payment_status ON highlight_payments (status)");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_highlight_payment_session ON highlight_payments (stripe_session_id)");

        log.info("Highlight schema checked and aligned");
    }
}
