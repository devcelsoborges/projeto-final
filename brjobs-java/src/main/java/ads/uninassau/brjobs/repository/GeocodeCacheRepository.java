package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.GeocodeCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GeocodeCacheRepository extends JpaRepository<GeocodeCache, Long> {
    Optional<GeocodeCache> findByAddressHash(String addressHash);
}
