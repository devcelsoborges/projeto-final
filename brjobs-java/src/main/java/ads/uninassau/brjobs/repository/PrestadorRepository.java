package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.Prestador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;


@Repository
public interface PrestadorRepository extends JpaRepository<Prestador, Long> {

    Optional<Prestador> findByEmail(String email);
}