package com.adrianpmdev.pokemon.features.lockes.application.service;

import com.adrianpmdev.platform.port.CurrentUser;
import com.adrianpmdev.pokemon.features.lockes.api.dto.request.LockeDeathRequest;
import com.adrianpmdev.pokemon.features.lockes.api.dto.response.LockeDeathsResponse;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.entity.Locke;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.entity.LockeDeath;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.repository.LockeDeathRepository;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.repository.LockeRepository;
import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Who may add and delete the Pokémon lost in a randomlocke, and how they are counted.
 *
 * @author adrianpmdev
 */
class LockeDeathServiceTest {

    private static final UUID LOCKE_ID = UUID.randomUUID();
    private static final UUID ANA_ACCOUNT = UUID.randomUUID();

    private final LockeRepository lockeRepository = mock(LockeRepository.class);
    private final LockeDeathRepository deathRepository = mock(LockeDeathRepository.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final LockeDeathService service = new LockeDeathService(lockeRepository, deathRepository, currentUser);

    private final Player ana = player("Ana", ANA_ACCOUNT);
    private final Player bruno = player("bruno", UUID.randomUUID());
    private final Player noAccount = player("Carla", null);
    private final Player former = player("Dani", ANA_ACCOUNT);
    private final Locke locke = new Locke();

    @BeforeEach
    void setUp() {
        locke.setId(LOCKE_ID);
        locke.getRoster().addAll(List.of(ana, bruno, noAccount));
        when(lockeRepository.findWithRosterById(LOCKE_ID)).thenReturn(Optional.of(locke));
        when(deathRepository.findByLockeIdOrderByCreatedAtAsc(LOCKE_ID)).thenReturn(List.of());
        signedInAs(ANA_ACCOUNT, false);
    }

    @Test
    void aPlayerAddsTheirOwnWithTheNameNormalized() {
        service.add(LOCKE_ID, new LockeDeathRequest(ana.getId(), "  pIKAchu "));

        ArgumentCaptor<LockeDeath> saved = ArgumentCaptor.forClass(LockeDeath.class);
        verify(deathRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPokemon()).isEqualTo("Pikachu");
        assertThat(saved.getValue().getPlayer()).isSameAs(ana);
        assertThat(saved.getValue().getLocke()).isSameAs(locke);
    }

    @Test
    void aPlayerCannotAddForSomebodyElse() {
        assertThatThrownBy(() -> service.add(LOCKE_ID, new LockeDeathRequest(bruno.getId(), "Pikachu")))
                .isInstanceOf(ResponseStatusException.class);

        verify(deathRepository, never()).saveAndFlush(any());
    }

    @Test
    void anAccountWithoutPlayerCannotAdd() {
        signedInAs(UUID.randomUUID(), false);

        assertThatThrownBy(() -> service.add(LOCKE_ID, new LockeDeathRequest(noAccount.getId(), "Pikachu")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void anAdminAddsForAnybodyInTheRoster() {
        signedInAs(UUID.randomUUID(), true);

        service.add(LOCKE_ID, new LockeDeathRequest(noAccount.getId(), "Pikachu"));

        verify(deathRepository).saveAndFlush(any());
    }

    @Test
    void nobodyAddsForSomebodyOutOfTheRoster() {
        signedInAs(UUID.randomUUID(), true);

        assertThatThrownBy(() -> service.add(LOCKE_ID, new LockeDeathRequest(former.getId(), "Pikachu")))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void aPlayerDeletesTheirOwn() {
        LockeDeath death = death(ana, "Pikachu");
        when(deathRepository.findByIdAndLockeId(death.getId(), LOCKE_ID)).thenReturn(Optional.of(death));

        service.delete(LOCKE_ID, death.getId());

        verify(deathRepository).delete(death);
    }

    @Test
    void aPlayerCannotDeleteSomebodyElses() {
        LockeDeath death = death(bruno, "Pikachu");
        when(deathRepository.findByIdAndLockeId(death.getId(), LOCKE_ID)).thenReturn(Optional.of(death));

        assertThatThrownBy(() -> service.delete(LOCKE_ID, death.getId())).isInstanceOf(ResponseStatusException.class);

        verify(deathRepository, never()).delete(any());
    }

    @Test
    void anAdminDeletesAnybodys() {
        signedInAs(UUID.randomUUID(), true);
        LockeDeath death = death(bruno, "Pikachu");
        when(deathRepository.findByIdAndLockeId(death.getId(), LOCKE_ID)).thenReturn(Optional.of(death));

        service.delete(LOCKE_ID, death.getId());

        verify(deathRepository).delete(death);
    }

    @Test
    void aDeathOfAnotherLockeIsNotFound() {
        UUID deathId = UUID.randomUUID();
        when(deathRepository.findByIdAndLockeId(deathId, LOCKE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(LOCKE_ID, deathId)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void everyPlayerIsListedWithTheirTotalAndWhatTheCallerMayDo() {
        when(deathRepository.findByLockeIdOrderByCreatedAtAsc(LOCKE_ID)).thenReturn(List.of(
                death(ana, "Pikachu"), death(ana, "Pikachu"), death(bruno, "Eevee"), death(former, "Onix")));

        LockeDeathsResponse response = service.list(LOCKE_ID);

        assertThat(response.total()).isEqualTo(4);
        assertThat(response.players()).extracting(row -> row.player().name())
                .containsExactly("Ana", "bruno", "Carla", "Dani");
        assertThat(response.players()).extracting(LockeDeathsResponse.PlayerDeaths::total)
                .containsExactly(2, 1, 0, 1);
        assertThat(response.players()).extracting(LockeDeathsResponse.PlayerDeaths::canAdd)
                .containsExactly(true, false, false, false);
        assertThat(response.players()).extracting(LockeDeathsResponse.PlayerDeaths::canDelete)
                .containsExactly(true, false, false, true);
    }

    private void signedInAs(UUID account, boolean admin) {
        when(currentUser.id()).thenReturn(Optional.of(account));
        when(currentUser.isAdmin()).thenReturn(admin);
    }

    private static Player player(String name, UUID account) {
        Player player = new Player();
        player.setId(UUID.randomUUID());
        player.setName(name);
        player.setUserId(account);
        return player;
    }

    private LockeDeath death(Player player, String pokemon) {
        LockeDeath death = new LockeDeath();
        death.setId(UUID.randomUUID());
        death.setLocke(locke);
        death.setPlayer(player);
        death.setPokemon(pokemon);
        return death;
    }
}
