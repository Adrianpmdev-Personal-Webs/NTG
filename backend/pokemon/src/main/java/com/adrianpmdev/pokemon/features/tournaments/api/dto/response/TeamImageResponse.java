package com.adrianpmdev.pokemon.features.tournaments.api.dto.response;

/**
 * Result of a team picture upload.
 *
 * @param url temporary URL of the new picture
 *
 * @author adrianpmdev
 */
public record TeamImageResponse(String url) {
}
