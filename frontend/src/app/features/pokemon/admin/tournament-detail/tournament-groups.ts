/**
 * TournamentGroupsFacade
 *
 * Tournament groups facade.
 * Manages groups, group participants and group matches.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject, signal } from '@angular/core';
import {
    DEFAULT_GROUP_MATCH_STATUS,
    DEFAULT_TOURNAMENT_GROUP_STATUS
} from '../../tournament-constants';
import { ModalMode } from '../modal-mode';
import {
    GroupMatchCreateRequest,
    GroupMatchResponse,
    GroupMatchUpdateRequest,
    GroupParticipantCreateRequest,
    GroupParticipantResponse,
    GroupParticipantUpdateRequest,
    TournamentGroupCreateRequest,
    TournamentGroupResponse
} from '../../pokemon-models';
import { NotificationService } from '@core/services/notification';
import { PokemonApi } from '../../pokemon-api';

@Injectable({ providedIn: 'root' })
export class TournamentGroupsFacade {

    private readonly pokemonService = inject(PokemonApi);
    private readonly notificationService = inject(NotificationService);

    /* Groups */
    readonly groups = signal<TournamentGroupResponse[]>([]);
    readonly groupLoading = signal(false);
    readonly groupModal = signal<ModalMode>(null);
    readonly groupEditTarget = signal<TournamentGroupResponse | null>(null);
    readonly selectedGroup = signal<TournamentGroupResponse | null>(null);

    groupForm: TournamentGroupCreateRequest = {
        tournamentId: '',
        groupLetter: 'A',
        status: DEFAULT_TOURNAMENT_GROUP_STATUS
    };

    /* Group participants */
    readonly groupParticipants = signal<GroupParticipantResponse[]>([]);
    readonly gpModal = signal<ModalMode>(null);
    readonly gpEditTarget = signal<GroupParticipantResponse | null>(null);

    gpForm: GroupParticipantCreateRequest = {
        groupId: '',
        tournamentParticipantId: '',
        wins: 0,
        losses: 0,
        points: 0
    };

    gpEditForm: GroupParticipantUpdateRequest = {};

    /* Group matches */
    readonly groupMatches = signal<GroupMatchResponse[]>([]);
    readonly gmModal = signal<ModalMode>(null);
    readonly gmEditTarget = signal<GroupMatchResponse | null>(null);

    gmForm: GroupMatchCreateRequest = {
        groupId: '',
        participant1Id: '',
        participant2Id: '',
        status: DEFAULT_GROUP_MATCH_STATUS
    };

    gmEditForm: GroupMatchUpdateRequest = {};

    /* Groups */
    loadGroups(tournamentId: string): void {
        this.groupLoading.set(true);

        this.pokemonService.getGroupsByTournament(tournamentId).subscribe({
            next: groups => {
                this.groups.set(groups);
                this.groupLoading.set(false);
            },
            error: () => {
                this.notificationService.error('Error al cargar grupos');
                this.groupLoading.set(false);
            }
        });
    }

    openGroupCreate(tournamentId: string): void {
        this.groupForm = {
            tournamentId,
            groupLetter: 'A',
            status: DEFAULT_TOURNAMENT_GROUP_STATUS
        };

        this.groupModal.set('create');
    }

    openGroupEdit(group: TournamentGroupResponse): void {
        this.groupEditTarget.set(group);
        this.groupForm = {
            tournamentId: '',
            groupLetter: group.groupLetter,
            status: group.status
        };

        this.groupModal.set('edit');
    }

    openGroupDelete(group: TournamentGroupResponse): void {
        this.groupEditTarget.set(group);
        this.groupModal.set('delete');
    }

    closeGroupModal(): void {
        this.groupModal.set(null);
        this.groupEditTarget.set(null);
    }

    submitGroupCreate(tournamentId: string): void {
        this.pokemonService.createGroup(this.groupForm).subscribe({
            next: () => {
                this.notificationService.success('Grupo creado');
                this.closeGroupModal();
                this.loadGroups(tournamentId);
            },
            error: () => this.notificationService.error('Error al crear grupo')
        });
    }

    submitGroupEdit(tournamentId: string): void {
        const group = this.groupEditTarget();

        if (!group) {
            return;
        }

        this.pokemonService.updateGroup(group.id, {
            groupLetter: this.groupForm.groupLetter,
            status: this.groupForm.status
        }).subscribe({
            next: () => {
                this.notificationService.success('Grupo actualizado');
                this.closeGroupModal();
                this.loadGroups(tournamentId);
            },
            error: () => this.notificationService.error('Error al actualizar grupo')
        });
    }

    confirmGroupDelete(tournamentId: string): void {
        const group = this.groupEditTarget();

        if (!group) {
            return;
        }

        this.pokemonService.deleteGroup(group.id).subscribe({
            next: () => {
                this.notificationService.success('Grupo eliminado');
                this.closeGroupModal();
                this.loadGroups(tournamentId);
            },
            error: () => this.notificationService.error('Error al eliminar grupo')
        });
    }

    selectGroup(group: TournamentGroupResponse): void {
        this.selectedGroup.set(group);
        this.gpForm = {
            groupId: group.id,
            tournamentParticipantId: '',
            wins: 0,
            losses: 0,
            points: 0
        };
        this.gmForm = {
            groupId: group.id,
            participant1Id: '',
            participant2Id: '',
            status: DEFAULT_GROUP_MATCH_STATUS
        };

        this.loadSelectedGroupData(group.id);
    }

    /* Group participants */
    openGpCreate(): void {
        this.gpModal.set('create');
    }

    openGpEdit(groupParticipant: GroupParticipantResponse): void {
        this.gpEditTarget.set(groupParticipant);
        this.gpEditForm = {
            groupPosition: groupParticipant.groupPosition,
            wins: groupParticipant.wins,
            losses: groupParticipant.losses,
            points: groupParticipant.points
        };

        this.gpModal.set('edit');
    }

    openGpDelete(groupParticipant: GroupParticipantResponse): void {
        this.gpEditTarget.set(groupParticipant);
        this.gpModal.set('delete');
    }

    closeGpModal(): void {
        this.gpModal.set(null);
        this.gpEditTarget.set(null);
    }

    submitGpCreate(): void {
        const group = this.selectedGroup();

        if (!group) {
            return;
        }

        this.pokemonService.createGroupParticipant(this.gpForm).subscribe({
            next: () => {
                this.notificationService.success('Participante de grupo añadido');
                this.closeGpModal();
                this.selectGroup(group);
            },
            error: () => this.notificationService.error('Error')
        });
    }

    submitGpEdit(): void {
        const group = this.selectedGroup();
        const groupParticipant = this.gpEditTarget();

        if (!group || !groupParticipant) {
            return;
        }

        this.pokemonService.updateGroupParticipant(groupParticipant.id, this.gpEditForm).subscribe({
            next: () => {
                this.notificationService.success('Actualizado');
                this.closeGpModal();
                this.selectGroup(group);
            },
            error: () => this.notificationService.error('Error')
        });
    }

    confirmGpDelete(): void {
        const group = this.selectedGroup();
        const groupParticipant = this.gpEditTarget();

        if (!group || !groupParticipant) {
            return;
        }

        this.pokemonService.deleteGroupParticipant(groupParticipant.id).subscribe({
            next: () => {
                this.notificationService.success('Eliminado');
                this.closeGpModal();
                this.selectGroup(group);
            },
            error: () => this.notificationService.error('Error')
        });
    }

    /* Group matches */
    openGmCreate(): void {
        this.gmModal.set('create');
    }

    openGmEdit(groupMatch: GroupMatchResponse): void {
        this.gmEditTarget.set(groupMatch);
        this.gmEditForm = {
            winnerId: groupMatch.winnerId,
            replayUrl: groupMatch.replayUrl,
            status: groupMatch.status,
            scheduledAt: groupMatch.scheduledAt,
            completedAt: groupMatch.completedAt
        };

        this.gmModal.set('edit');
    }

    openGmDelete(groupMatch: GroupMatchResponse): void {
        this.gmEditTarget.set(groupMatch);
        this.gmModal.set('delete');
    }

    closeGmModal(): void {
        this.gmModal.set(null);
        this.gmEditTarget.set(null);
    }

    submitGmCreate(): void {
        const group = this.selectedGroup();

        if (!group) {
            return;
        }

        this.pokemonService.createGroupMatch(this.gmForm).subscribe({
            next: () => {
                this.notificationService.success('Partido de grupo creado');
                this.closeGmModal();
                this.selectGroup(group);
            },
            error: () => this.notificationService.error('Error')
        });
    }

    submitGmEdit(): void {
        const group = this.selectedGroup();
        const groupMatch = this.gmEditTarget();

        if (!group || !groupMatch) {
            return;
        }

        this.pokemonService.updateGroupMatch(groupMatch.id, this.gmEditForm).subscribe({
            next: () => {
                this.notificationService.success('Partido actualizado');
                this.closeGmModal();
                this.selectGroup(group);
            },
            error: () => this.notificationService.error('Error')
        });
    }

    confirmGmDelete(): void {
        const group = this.selectedGroup();
        const groupMatch = this.gmEditTarget();

        if (!group || !groupMatch) {
            return;
        }

        this.pokemonService.deleteGroupMatch(groupMatch.id).subscribe({
            next: () => {
                this.notificationService.success('Partido eliminado');
                this.closeGmModal();
                this.selectGroup(group);
            },
            error: () => this.notificationService.error('Error')
        });
    }

    private loadSelectedGroupData(groupId: string): void {
        this.pokemonService.getGroupParticipants(groupId).subscribe({
            next: groupParticipants => this.groupParticipants.set(groupParticipants)
        });
        this.pokemonService.getGroupMatches(groupId).subscribe({
            next: groupMatches => this.groupMatches.set(groupMatches)
        });
    }
}