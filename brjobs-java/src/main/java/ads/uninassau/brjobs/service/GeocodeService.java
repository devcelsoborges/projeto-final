package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.GeocodeRequestDTO;
import ads.uninassau.brjobs.dto.GeocodeResponseDTO;
import ads.uninassau.brjobs.model.GeocodeCache;
import ads.uninassau.brjobs.repository.GeocodeCacheRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodeService {

    private final GeocodeCacheRepository geocodeCacheRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${geocoding.provider:nominatim}")
    private String provider;

    @Value("${nominatim.base-url:https://nominatim.openstreetmap.org}")
    private String nominatimBaseUrl;

    @Value("${nominatim.min-interval-ms:1100}")
    private long minIntervalMs;

    @Value("${nominatim.user-agent:BRJobs/1.0 contato@brjobs.com.br}")
    private String nominatimUserAgent;

    @Value("${geocoding.cache-ttl-days:365}")
    private long cacheTtlDays;

    private long lastRequestAt = 0L;

    public GeocodeResponseDTO geocode(GeocodeRequestDTO request) {
        String normalized = normalize(request);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Endereço é obrigatório para geocoding.");
        }

        String hash = hash(normalized);
        return geocodeCacheRepository.findByAddressHash(hash)
                .filter(cache -> cache.getExpiresAt() == null || cache.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(cache -> {
                    log.debug("geocode_cache_hit addressHash={} provider={}", hash, cache.getSource());
                    return toResponse(cache);
                })
                .orElseGet(() -> buscarNominatim(normalized, hash));
    }

    public String normalize(GeocodeRequestDTO request) {
        if (request == null) {
            return "";
        }

        return String.join(", ",
                safe(request.getEndereco()),
                safe(request.getCidade()),
                safe(request.getEstado()),
                safe(request.getCep()),
                "Brasil"
        ).replaceAll("(,\\s*)+", ", ")
                .replaceAll("^,\\s*|,\\s*$", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    public String hashForAddress(GeocodeRequestDTO request) {
        return hash(normalize(request));
    }

    private GeocodeResponseDTO buscarNominatim(String normalized, String hash) {
        if (!"nominatim".equalsIgnoreCase(provider)) {
            throw new IllegalStateException("Provider de geocoding não suportado: " + provider);
        }

        throttle();

        URI uri = UriComponentsBuilder
                .fromUriString(nominatimBaseUrl + "/search")
                .queryParam("format", "jsonv2")
                .queryParam("limit", "1")
                .queryParam("q", normalized)
                .build()
                .encode()
                .toUri();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, nominatimUserAgent);
            headers.set(HttpHeaders.REFERER, "https://brjobs.com.br");
            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            String body = response.getBody();
            JsonNode root = objectMapper.readTree(body);
            if (!root.isArray() || root.isEmpty()) {
                throw new IllegalArgumentException("Endereço não encontrado.");
            }

            JsonNode first = root.get(0);
            Double lat = Double.valueOf(first.get("lat").asText());
            Double lng = Double.valueOf(first.get("lon").asText());

            GeocodeCache cache = geocodeCacheRepository.save(GeocodeCache.builder()
                    .addressHash(hash)
                    .normalizedAddress(normalized)
                    .lat(lat)
                    .lng(lng)
                    .source("nominatim")
                    .precision(first.hasNonNull("type") ? first.get("type").asText() : "approx")
                    .expiresAt(LocalDateTime.now().plusDays(cacheTtlDays))
                    .build());

            return toResponse(cache);
        } catch (Exception ex) {
            log.warn("geocode_failed provider=nominatim reason={} addressHash={}", ex.getMessage(), hash);
            throw new IllegalArgumentException("Não foi possível validar o endereço agora.");
        }
    }

    private synchronized void throttle() {
        long now = System.currentTimeMillis();
        long wait = minIntervalMs - (now - lastRequestAt);
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestAt = System.currentTimeMillis();
    }

    private GeocodeResponseDTO toResponse(GeocodeCache cache) {
        return GeocodeResponseDTO.builder()
                .lat(cache.getLat())
                .lng(cache.getLng())
                .source(cache.getSource())
                .precision(cache.getPrecision())
                .build();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao gerar hash de endereço.", ex);
        }
    }
}
