package com.adrianpmdev.wowforever.features.settings.api.controller;

import com.adrianpmdev.wowforever.features.settings.api.dto.request.SettingUpdateRequest;
import com.adrianpmdev.wowforever.features.settings.api.dto.response.SettingResponse;
import com.adrianpmdev.wowforever.features.settings.application.service.SettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for WoW Forever settings. Reading needs the module tag; changing a value needs ADMIN.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/wow-forever/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    @GetMapping
    public ResponseEntity<List<SettingResponse>> getAll() {
        return ResponseEntity.ok(settingService.getAll());
    }

    @PutMapping("/{key}")
    public ResponseEntity<SettingResponse> update(@PathVariable String key, @Valid @RequestBody SettingUpdateRequest request) {
        return ResponseEntity.ok(settingService.update(key, request));
    }
}
