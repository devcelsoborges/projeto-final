package ads.uninassau.brjobs.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class PublicConfigController {

    @Value("${brjobs.feature.structural-auth:true}")
    private boolean structuralAuth;

    @Value("${brjobs.feature.publication-geo:true}")
    private boolean publicationGeo;

    @Value("${brjobs.feature.dynamic-notifications:true}")
    private boolean dynamicNotifications;

    @GetMapping("/public")
    public ResponseEntity<Map<String, Boolean>> publicConfig() {
        return ResponseEntity.ok(Map.of(
                "structuralAuth", structuralAuth,
                "publicationGeo", publicationGeo,
                "dynamicNotifications", dynamicNotifications
        ));
    }
}
