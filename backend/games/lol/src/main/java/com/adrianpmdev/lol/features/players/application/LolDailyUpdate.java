package com.adrianpmdev.lol.features.players.application;

import com.adrianpmdev.lol.features.players.infrastructure.LolAccount;
import com.adrianpmdev.lol.features.players.infrastructure.LolAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Players are updated when somebody opens them; once a night this updates the ones nobody opened. It only queues them:
 * the worker asks Riot one by one on its own thread, within the limits of the key.
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
public class LolDailyUpdate {

    /** A check this recent makes the one at dawn unnecessary. */
    private static final Duration FRESH_AT_DAWN = Duration.ofHours(20);

    private final LolAccountRepository accountRepository;
    private final LolSyncWorker worker;

    /**
     * Queues the players nobody opened lately.
     */
    @Scheduled(cron = "0 30 3 * * *", zone = "Europe/Madrid")
    public void atDawn() {
        Instant limit = Instant.now().minus(FRESH_AT_DAWN);

        for (LolAccount account : accountRepository.findAll()) {
            if (account.getCheckedAt() == null || account.getCheckedAt().isBefore(limit)) {
                worker.refresh(account);
            }
        }
    }
}
