package com.adrianpmdev.lol.features.players.application;

import com.adrianpmdev.lol.features.players.infrastructure.LolAccount;
import com.adrianpmdev.lol.features.players.infrastructure.LolAccountRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Who is updated at dawn.
 *
 * @author adrianpmdev
 */
class LolDailyUpdateTest {

    private final LolAccountRepository repository = mock(LolAccountRepository.class);
    private final LolSyncWorker worker = mock(LolSyncWorker.class);
    private final LolDailyUpdate update = new LolDailyUpdate(repository, worker);

    @Test
    void onlyThePlayersNobodyOpenedLatelyAreUpdated() {
        LolAccount recent = account(Instant.now().minus(Duration.ofHours(3)));
        LolAccount old = account(Instant.now().minus(Duration.ofHours(30)));
        LolAccount never = account(null);
        when(repository.findAll()).thenReturn(List.of(recent, old, never));

        update.atDawn();

        verify(worker, never()).refresh(recent);
        verify(worker).refresh(old);
        verify(worker).refresh(never);
    }

    private static LolAccount account(Instant checkedAt) {
        LolAccount account = mock(LolAccount.class);
        when(account.getCheckedAt()).thenReturn(checkedAt);
        return account;
    }
}
