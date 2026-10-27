package com.adrianpmdev.pokemon.features.types.api.controller;

import com.adrianpmdev.pokemon.features.types.application.service.TypeChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * REST controller for the Pokémon types and their damage chart (read only), used by the type calculator.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping(value = "/api/v1/pokemon/types", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TypeChartController {

    /** The chart only changes with a new deploy, so the browser can keep it for a day. */
    private static final CacheControl CACHE = CacheControl.maxAge(1, TimeUnit.DAYS).cachePrivate();

    private final TypeChartService typeChartService;

    /** Types in display order and the multiplier of every attacking type against each defending type (1 when absent). */
    @GetMapping
    public ResponseEntity<String> getChart() {
        return ResponseEntity.ok().cacheControl(CACHE).body(typeChartService.getChart());
    }
}
