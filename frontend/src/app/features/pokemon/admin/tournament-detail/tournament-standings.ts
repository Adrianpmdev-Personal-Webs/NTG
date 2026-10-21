/**
 * TournamentStandingsFacade
 *
 * Tournament standings facade.
 * Manages final tournament standings and standings dialogs.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject, signal } from '@angular/core';
import { ModalMode } from '../modal-mode';
import {
    TournamentStandingCreateRequest,
    TournamentStandingResponse,
    TournamentStandingUpdateRequest
} from '../../pokemon-models';
import { NotificationService } from '@core/services/notification';
import { PokemonApi } from '../../pokemon-api';

@Injectable({ providedIn: 'root' })
export class TournamentStandingsFacade {

    private readonly pokemonService = inject(PokemonApi);
    private readonly notificationService = inject(NotificationService);

    readonly standings = signal<TournamentStandingResponse[]>([]);
    readonly standLoading = signal(false);
    readonly standModal = signal<ModalMode>(null);
    readonly standEditTarget = signal<TournamentStandingResponse | null>(null);

    standForm: TournamentStandingCreateRequest = {
        tournamentId: '',
        tournamentParticipantId: ''
    };

    standEditForm: TournamentStandingUpdateRequest = {};

    loadStandings(tournamentId: string): void {
        this.standLoading.set(true);

        this.pokemonService.getStandingsByTournament(tournamentId).subscribe({
            next: standings => {
                this.standings.set(standings);
                this.standLoading.set(false);
            },
            error: () => {
                this.notificationService.error('Error al cargar standings');
                this.standLoading.set(false);
            }
        });
    }

    openStandCreate(tournamentId: string): void {
        this.standForm = {
            tournamentId,
            tournamentParticipantId: ''
        };
        this.standModal.set('create');
    }

    openStandEdit(standing: TournamentStandingResponse): void {
        this.standEditTarget.set(standing);
        this.standEditForm = {
            finalPosition: standing.finalPosition,
            totalWins: standing.totalWins,
            totalLosses: standing.totalLosses,
            totalPoints: standing.totalPoints
        };
        this.standModal.set('edit');
    }

    openStandDelete(standing: TournamentStandingResponse): void {
        this.standEditTarget.set(standing);
        this.standModal.set('delete');
    }

    closeStandModal(): void {
        this.standModal.set(null);
        this.standEditTarget.set(null);
    }

    submitStandCreate(tournamentId: string): void {
        this.pokemonService.createStanding(this.standForm).subscribe({
            next: () => {
                this.notificationService.success('Standing creado');
                this.closeStandModal();
                this.loadStandings(tournamentId);
            },
            error: () => this.notificationService.error('Error')
        });
    }

    submitStandEdit(tournamentId: string): void {
        const standing = this.standEditTarget();

        if (!standing) {
            return;
        }

        this.pokemonService.updateStanding(standing.id, this.standEditForm).subscribe({
            next: () => {
                this.notificationService.success('Standing actualizado');
                this.closeStandModal();
                this.loadStandings(tournamentId);
            },
            error: () => this.notificationService.error('Error')
        });
    }

    confirmStandDelete(tournamentId: string): void {
        const standing = this.standEditTarget();

        if (!standing) {
            return;
        }

        this.pokemonService.deleteStanding(standing.id).subscribe({
            next: () => {
                this.notificationService.success('Standing eliminado');
                this.closeStandModal();
                this.loadStandings(tournamentId);
            },
            error: () => this.notificationService.error('Error')
        });
    }
}
