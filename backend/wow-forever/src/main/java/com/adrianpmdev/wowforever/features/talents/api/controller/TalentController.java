package com.adrianpmdev.wowforever.features.talents.api.controller;

import com.adrianpmdev.wowforever.features.talents.application.service.TalentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * REST controller for the talent trees. Read only: the data changes with the game build, not from the app.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/wow-forever/talents")
@RequiredArgsConstructor
public class TalentController {

    private final TalentService talentService;

    /** Every class with its trees and talents: everything the calculator needs in one call. */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getTalents() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(talentService.getTalentsJson());
    }
}
