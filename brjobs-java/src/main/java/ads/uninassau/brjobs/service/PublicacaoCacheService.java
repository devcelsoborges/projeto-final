package ads.uninassau.brjobs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicacaoCacheService {

    private final CacheManager cacheManager;

    public void evictAll() {
        for (String cacheName : List.of("publicacoes-lista", "publicacoes-paginadas")) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
        log.info("Publicacao cache invalidated");
    }
}
