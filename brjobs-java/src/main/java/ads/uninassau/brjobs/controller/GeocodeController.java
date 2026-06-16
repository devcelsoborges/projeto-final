package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.GeocodeRequestDTO;
import ads.uninassau.brjobs.dto.GeocodeResponseDTO;
import ads.uninassau.brjobs.security.ValidateTenant;
import ads.uninassau.brjobs.service.GeocodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/geocode")
@RequiredArgsConstructor
public class GeocodeController {

    private final GeocodeService geocodeService;

    @PostMapping
    @ValidateTenant
    public ResponseEntity<GeocodeResponseDTO> geocode(@RequestBody GeocodeRequestDTO request) {
        return ResponseEntity.ok(geocodeService.geocode(request));
    }
}
