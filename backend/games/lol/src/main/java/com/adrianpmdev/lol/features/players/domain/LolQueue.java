package com.adrianpmdev.lol.features.players.domain;

import java.util.List;

/**
 * The queues this module follows, in the order the screens show them.
 *
 * @param id    queue id of Riot
 * @param label name shown to players
 *
 * @author adrianpmdev
 */
public record LolQueue(int id, String label) {

    /** Every queue followed. */
    public static final List<LolQueue> ALL = List.of(
            new LolQueue(420, "SoloQ"),
            new LolQueue(440, "Flex"),
            new LolQueue(400, "Draft"),
            new LolQueue(2400, "ARAM Mayhem"));
}
