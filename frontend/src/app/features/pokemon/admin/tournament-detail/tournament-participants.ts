/**
 * TournamentParticipantsFacade
 *
 * Tournament participants facade.
 * Manages tournament participant loading and participant CRUD dialogs.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject, signal } from '@angular/core';
import { DEFAULT_TOURNAMENT_PARTICIPANT_STATUS } from '../../tournament-constants';
import { ModalMode } from '../modal-mode';
import {
    TournamentParticipantCreateRequest,
    TournamentParticipantResponse,
    TournamentParticipantUpdateRequest
} from '../../pokemon-models';
import { NotificationService } from '@core/services/notification';
import { PokemonApi } from '../../pokemon-api';
import { TournamentUsersFacade } from './tournament-users';

@Injectable({ providedIn: 'root' })
export class TournamentParticipantsFacade {

    private readonly pokemonService = inject(PokemonApi);
    private readonly notificationService = inject(NotificationService);
    private readonly usersFacade = inject(TournamentUsersFacade);

    readonly participants = signal<TournamentParticipantResponse[]>([]);
    readonly partLoading = signal(false);
    readonly partModal = signal<ModalMode>(null);
    readonly partEditTarget = signal<TournamentParticipantResponse | null>(null);

    partForm: TournamentParticipantCreateRequest = {
        tournamentId: '',
        userId: '',
        status: DEFAULT_TOURNAMENT_PARTICIPANT_STATUS
    };

    partEditForm: TournamentParticipantUpdateRequest = {
        status: DEFAULT_TOURNAMENT_PARTICIPANT_STATUS
    };

    loadParticipants(tournamentId: string): void {
        this.partLoading.set(true);

        this.pokemonService.getParticipantsByTournament(tournamentId).subscribe({
            next: participants => {
                this.participants.set(participants);
                this.partLoading.set(false);
            },
            error: () => {
                this.notificationService.error('Error al cargar participantes');
                this.partLoading.set(false);
            }
        });
    }

    getParticipantLabel(participantId: string): string {
        const participant = this.participants().find(currentParticipant => currentParticipant.id === participantId);

        return participant ? this.usersFacade.getUserName(participant.userId) : `${participantId.slice(0, 8)}...`;
    }

    openPartCreate(tournamentId: string): void {
        this.partForm = {
            tournamentId,
            userId: '',
            status: DEFAULT_TOURNAMENT_PARTICIPANT_STATUS
        };

        this.partModal.set('create');
    }

    openPartEdit(participant: TournamentParticipantResponse): void {
        this.partEditTarget.set(participant);
        this.partEditForm = {
            status: participant.status
        };

        this.partModal.set('edit');
    }

    openPartDelete(participant: TournamentParticipantResponse): void {
        this.partEditTarget.set(participant);
        this.partModal.set('delete');
    }

    closePartModal(): void {
        this.partModal.set(null);
        this.partEditTarget.set(null);
    }

    submitPartCreate(tournamentId: string): void {
        this.pokemonService.createParticipant(this.partForm).subscribe({
            next: () => {
                this.notificationService.success('Participante añadido');
                this.closePartModal();
                this.loadParticipants(tournamentId);
            },
            error: () => this.notificationService.error('Error al añadir participante')
        });
    }

    submitPartEdit(tournamentId: string): void {
        const participant = this.partEditTarget();

        if (!participant) {
            return;
        }

        this.pokemonService.updateParticipant(participant.id, this.partEditForm).subscribe({
            next: () => {
                this.notificationService.success('Participante actualizado');
                this.closePartModal();
                this.loadParticipants(tournamentId);
            },
            error: () => this.notificationService.error('Error al actualizar participante')
        });
    }

    confirmPartDelete(tournamentId: string): void {
        const participant = this.partEditTarget();

        if (!participant) {
            return;
        }

        this.pokemonService.deleteParticipant(participant.id).subscribe({
            next: () => {
                this.notificationService.success('Participante eliminado');
                this.closePartModal();
                this.loadParticipants(tournamentId);
            },
            error: () => this.notificationService.error('Error al eliminar participante')
        });
    }
}