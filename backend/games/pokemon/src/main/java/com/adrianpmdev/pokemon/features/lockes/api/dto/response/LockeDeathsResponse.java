package com.adrianpmdev.pokemon.features.lockes.api.dto.response;

import com.adrianpmdev.pokemon.shared.dto.RefResponse;

import java.util.List;
import java.util.UUID;

/**
 * The Pokémon lost in a randomlocke, by player.
 *
 * @param total   Pokémon lost by everybody
 * @param players every player of the roster, and anybody else who lost a Pokémon in it, sorted by name
 *
 * @author adrianpmdev
 */
public record LockeDeathsResponse(int total, List<PlayerDeaths> players) {

    /**
     * The Pokémon a player lost.
     *
     * @param player    the player
     * @param total     Pokémon lost
     * @param canAdd    whether the caller may add one for this player
     * @param canDelete whether the caller may delete the ones of this player
     * @param deaths    the Pokémon, oldest first
     */
    public record PlayerDeaths(RefResponse player, int total, boolean canAdd, boolean canDelete, List<Death> deaths) {
    }

    /**
     * A Pokémon lost.
     *
     * @param id      death id
     * @param pokemon name of the Pokémon
     */
    public record Death(UUID id, String pokemon) {
    }
}
