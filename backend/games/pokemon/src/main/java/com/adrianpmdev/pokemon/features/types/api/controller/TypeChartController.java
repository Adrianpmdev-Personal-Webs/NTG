package com.adrianpmdev.pokemon.features.types.api.controller;

import com.adrianpmdev.platform.web.JsonResponses;
import com.adrianpmdev.pokemon.features.types.application.service.TypeChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the Pokémon types and their damage chart (read only), used by the type calculator.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping(value = "/api/v1/pokemon/types", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TypeChartController {

    /**
     * The chart only changes with a new deploy, and players must see it right after one: the browser keeps it but checks
     * its ETag on every request (a 304 with no body if unchanged).
     */
    private static final CacheControl CACHE = CacheControl.noCache().cachePrivate();

    private final TypeChartService typeChartService;

    /** Types in display order and the multiplier of every attacking type against each defending type (1 when absent). */
    @GetMapping
    public ResponseEntity<String> getChart() {
        return JsonResponses.cacheable(typeChartService.getChart(), CACHE);
    }
}
