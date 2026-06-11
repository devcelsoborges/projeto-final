package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.model.HighlightPlan;
import ads.uninassau.brjobs.repository.HighlightPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class HighlightPlanInitializer implements CommandLineRunner {

    private final HighlightPlanRepository highlightPlanRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        boolean highlightPlansTableExists = Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
            SELECT COUNT(*) > 0
            FROM information_schema.tables
            WHERE lower(table_name) = 'highlight_plans'
            """, Boolean.class));

        if (!highlightPlansTableExists) {
            log.warn("Highlight plan initialization skipped: highlight_plans table not found");
            return;
        }

        upsertPlan("Basico", 10.0, 5, 1);
        upsertPlan("Plus", 20.0, 11, 2);
        upsertPlan("Premium", 40.0, 25, 3);
    }

    private void upsertPlan(String name, double price, int durationDays, int priority) {
        HighlightPlan plan = highlightPlanRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> HighlightPlan.builder().name(name).build());

        plan.setPrice(price);
        plan.setDurationDays(durationDays);
        plan.setPriority(priority);
        plan.setActive(true);
        highlightPlanRepository.save(plan);
    }
}
