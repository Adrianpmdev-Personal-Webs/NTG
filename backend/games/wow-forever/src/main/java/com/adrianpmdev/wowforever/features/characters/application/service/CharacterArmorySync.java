package com.adrianpmdev.wowforever.features.characters.application.service;

import com.adrianpmdev.wowforever.features.characters.infrastructure.armory.ArmoryCharacter;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.entity.GameCharacter;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.repository.GameCharacterRepository;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Keeps the characters up to date with the official armory: a round asks for each character, one by one with a pause,
 * and saves only the ones that changed. Somebody opening the screen starts a round, unless one has just ended
 * ({@code cooldownMinutes} of config.json); if nobody opened it for a while, one runs at dawn. A character below the
 * level the armory shows simply is not found yet, and fills in by itself when it appears. When the armory fails the
 * round stops and everything stays as it was. Rounds share the single scheduler thread with the build reads, so the two
 * never run at the same time.
 *
 * @author adrianpmdev
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterArmorySync {

    /** A round this recent makes the one at dawn unnecessary. */
    private static final Duration FRESH_AT_DAWN = Duration.ofHours(20);

    private final GameCharacterRepository characterRepository;
    private final CharacterArmory characterArmory;
    private final GameDataService gameDataService;
    private final TaskScheduler scheduler;

    /** Time between two characters of a round, to go easy on the armory. */
    Duration pause = Duration.ofSeconds(1);

    private CompletableFuture<Void> running;
    private Instant lastRound;

    /**
     * Starts a round because somebody opened the screen, unless one is running or has just ended.
     *
     * @return what finishes when the round does; already finished when no round is needed
     */
    public CompletableFuture<Void> refresh() {
        return round(Duration.ofMinutes(gameDataService.getConfig().armory().cooldownMinutes()));
    }

    /**
     * The round for the days nobody opens the screen, before the restart that may follow a build read.
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Europe/Madrid")
    public void atDawn() {
        round(FRESH_AT_DAWN);
    }

    private synchronized CompletableFuture<Void> round(Duration freshFor) {
        if (running != null) {
            return running;
        }

        if (lastRound != null && lastRound.plus(freshFor).isAfter(Instant.now())) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> round = new CompletableFuture<>();
        running = round;
        scheduler.schedule(this::runRound, Instant.now());

        return round;
    }

    private void runRound() {
        boolean finished = false;

        try {
            finished = refreshAll();
        } finally {
            synchronized (this) {
                if (finished) {
                    lastRound = Instant.now();
                }

                running.complete(null);
                running = null;
            }
        }
    }

    /** Goes over every character; false when the armory failed and the round stopped. */
    private boolean refreshAll() {
        int updated = 0;

        try {
            for (GameCharacter character : characterRepository.findAll()) {
                Optional<ArmoryCharacter> changes = characterArmory.changesOf(character);

                if (changes.isPresent() && save(character, changes.get())) {
                    updated++;
                }

                Thread.sleep(pause);
            }

            return true;
        } catch (IOException exception) {
            log.warn("The characters could not be updated from the armory: {}", exception.getMessage());
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (updated > 0) {
                log.info("{} characters updated from the armory", updated);
            }
        }
    }

    /** Saves on a fresh copy, because during the read the owner may have changed its notes, renamed it or deleted it. */
    private boolean save(GameCharacter read, ArmoryCharacter changes) {
        return characterRepository.findById(read.getId())
                .filter(fresh -> fresh.getName().equalsIgnoreCase(read.getName()))
                .map(fresh -> {
                    characterArmory.apply(fresh, changes);
                    characterRepository.saveAndFlush(fresh);
                    return true;
                })
                .orElse(false);
    }
}
