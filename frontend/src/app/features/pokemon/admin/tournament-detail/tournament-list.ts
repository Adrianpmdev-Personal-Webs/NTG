/**
 * TournamentListFacade
 *
 * Tournament list facade.
 * Manages tournament loading, selection and tournament CRUD dialogs.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject, signal } from '@angular/core';
import {
    DEFAULT_TOURNAMENT_STATUS,
    DEFAULT_TOURNAMENT_TYPE
} from '../../tournament-constants';
import { ModalMode } from '../modal-mode';
import {
    TournamentCreateRequest,
    TournamentResponse,
    TournamentUpdateRequest
} from '../../pokemon-models';
import { NotificationService } from '@core/services/notification';
import { PokemonApi } from '../../pokemon-api';

@Injectable({ providedIn: 'root' })
export class TournamentListFacade {

    private readonly pokemonService = inject(PokemonApi);
    private readonly notificationService = inject(NotificationService);

    readonly tournaments = signal<TournamentResponse[]>([]);
    readonly selectedTournament = signal<TournamentResponse | null>(null);
    readonly tourLoading = signal(true);
    readonly tourModal = signal<ModalMode>(null);
    readonly tourEditTarget = signal<TournamentResponse | null>(null);

    tourForm: TournamentCreateRequest = {
        lockeId: '',
        name: '',
        description: '',
        tournamentType: DEFAULT_TOURNAMENT_TYPE,
        status: DEFAULT_TOURNAMENT_STATUS
    };

    tourEditForm: TournamentUpdateRequest = {};

    loadTournaments(lockeId: string): void {
        this.tourLoading.set(true);

        this.pokemonService.getTournamentsByLockeId(lockeId).subscribe({
            next: tournaments => {
                this.tournaments.set(tournaments);
                this.tourLoading.set(false);
            },
            error: () => {
                this.notificationService.error('Error al cargar torneos');
                this.tourLoading.set(false);
            }
        });
    }

    selectTournament(tournament: TournamentResponse): void {
        this.selectedTournament.set(tournament);
    }

    openTourCreate(lockeId: string): void {
        this.tourForm = {
            lockeId,
            name: '',
            description: '',
            tournamentType: DEFAULT_TOURNAMENT_TYPE,
            status: DEFAULT_TOURNAMENT_STATUS
        };

        this.tourModal.set('create');
    }

    openTourEdit(tournament: TournamentResponse): void {
        this.tourEditTarget.set(tournament);
        this.tourEditForm = {
            name: tournament.name,
            description: tournament.description,
            tournamentType: tournament.tournamentType,
            status: tournament.status,
            maxParticipants: tournament.maxParticipants,
            startDate: tournament.startDate,
            endDate: tournament.endDate
        };

        this.tourModal.set('edit');
    }

    openTourDelete(tournament: TournamentResponse): void {
        this.tourEditTarget.set(tournament);
        this.tourModal.set('delete');
    }

    closeTourModal(): void {
        this.tourModal.set(null);
        this.tourEditTarget.set(null);
    }

    submitTourCreate(lockeId: string): void {
        this.pokemonService.createTournament(this.tourForm).subscribe({
            next: () => {
                this.notificationService.success('Torneo creado');
                this.closeTourModal();
                this.loadTournaments(lockeId);
            },
            error: () => this.notificationService.error('Error al crear torneo')
        });
    }

    submitTourEdit(lockeId: string): void {
        const tournament = this.tourEditTarget();

        if (!tournament) {
            return;
        }

        this.pokemonService.updateTournament(tournament.id, this.tourEditForm).subscribe({
            next: () => {
                this.notificationService.success('Torneo actualizado');
                this.closeTourModal();
                this.loadTournaments(lockeId);
            },
            error: () => this.notificationService.error('Error al actualizar torneo')
        });
    }

    confirmTourDelete(lockeId: string): void {
        const tournament = this.tourEditTarget();

        if (!tournament) {
            return;
        }

        this.pokemonService.deleteTournament(tournament.id).subscribe({
            next: () => {
                this.notificationService.success('Torneo eliminado');
                this.closeTourModal();

                if (this.selectedTournament()?.id === tournament.id) {
                    this.selectedTournament.set(null);
                }

                this.loadTournaments(lockeId);
            },
            error: () => this.notificationService.error('Error al eliminar torneo')
        });
    }
}