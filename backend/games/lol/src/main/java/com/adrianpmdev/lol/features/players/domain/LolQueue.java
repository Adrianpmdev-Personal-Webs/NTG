package com.adrianpmdev.lol.features.players.domain;

import java.util.List;

/**
 * The queues this module follows, in the order the screens show them.
 *
 * @param id    queue id of Riot
 * @param label name shown to players
 * @param emptyNotice what to tell while a player has no games in it, or null for the usual text
 *
 * @author adrianpmdev
 */
public record LolQueue(int id, String label, String emptyNotice) {

    /**
     * Every queue followed. ARAM Mayhem stays followed although Riot does not give its games through the API for now:
     * when it does, they are downloaded like the others and the notice goes away by itself.
     */
    public static final List<LolQueue> ALL = List.of(
            new LolQueue(420, "SoloQ", null),
            new LolQueue(440, "Flex", null),
            new LolQueue(400, "Draft", null),
            new LolQueue(2400, "ARAM Mayhem", "De momento, Riot no ofrece las partidas de ARAM Mayhem a través de su API."));
}
