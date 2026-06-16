package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.HighlightPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HighlightPlanRepository extends JpaRepository<HighlightPlan, Long> {
    List<HighlightPlan> findByActiveTrueOrderByPriorityAsc();
    Optional<HighlightPlan> findByNameIgnoreCase(String name);
}
