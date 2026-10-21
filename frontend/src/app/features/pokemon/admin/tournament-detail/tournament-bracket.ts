/**
 * TournamentBracketFacade
 *
 * Tournament bracket facade.
 * Manages bracket stages and bracket matches.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject, signal } from '@angular/core';
import {
    DEFAULT_BRACKET_MATCH_STATUS,
    DEFAULT_BRACKET_STAGE_STATUS
} from '../../tournament-constants';
import { ModalMode } from '../modal-mode';
import {
    BracketMatchCreateRequest,
    BracketMatchResponse,
    BracketMatchUpdateRequest,
    BracketStageCreateRequest,
    BracketStageResponse,
    BracketStageUpdateRequest
} from '../../pokemon-models';
import { NotificationService } from '@core/services/notification';
import { PokemonApi } from '../../pokemon-api';

@Injectable({ providedIn: 'root' })
export class TournamentBracketFacade {

    private readonly pokemonService = inject(PokemonApi);
    private readonly notificationService = inject(NotificationService);

    /* Bracket stage */
    readonly bracketStages = signal<BracketStageResponse[]>([]);
    readonly bracketLoading = signal(false);
    readonly bsModal = signal<ModalMode>(null);
    readonly bsEditTarget = signal<BracketStageResponse | null>(null);
    readonly selectedStage = signal<BracketStageResponse | null>(null);

    bsForm: BracketStageCreateRequest = {
        tournamentId: '',
        stageName: '',
        stageOrder: 1,
        status: DEFAULT_BRACKET_STAGE_STATUS
    };

    bsEditForm: BracketStageUpdateRequest = {};

    /* Bracket match */
    readonly bracketMatches = signal<BracketMatchResponse[]>([]);
    readonly bmModal = signal<ModalMode>(null);
    readonly bmEditTarget = signal<BracketMatchResponse | null>(null);

    bmForm: BracketMatchCreateRequest = {
        tournamentId: '',
        bracketStageId: '',
        matchNumber: 1,
        status: DEFAULT_BRACKET_MATCH_STATUS
    };

    bmEditForm: BracketMatchUpdateRequest = {};

    /* Bracket stage */
    loadBracketStages(tournamentId: string): void {
        this.bracketLoading.set(true);

        this.pokemonService.getBracketStagesByTournament(tournamentId).subscribe({
            next: bracketStages => {
                this.bracketStages.set(bracketStages);
                this.bracketLoading.set(false);
            },
            error: () => {
                this.notificationService.error('Error al cargar bracket');
                this.bracketLoading.set(false);
            }
        });
    }

    openBsCreate(tournamentId: string): void {
        this.bsForm = {
            tournamentId,
            stageName: '',
            stageOrder: 1,
            status: DEFAULT_BRACKET_STAGE_STATUS
        };

        this.bsModal.set('create');
    }

    openBsEdit(bracketStage: BracketStageResponse): void {
        this.bsEditTarget.set(bracketStage);
        this.bsEditForm = {
            stageName: bracketStage.stageName,
            stageOrder: bracketStage.stageOrder,
            status: bracketStage.status
        };

        this.bsModal.set('edit');
    }

    openBsDelete(bracketStage: BracketStageResponse): void {
        this.bsEditTarget.set(bracketStage);
        this.bsModal.set('delete');
    }

    closeBsModal(): void {
        this.bsModal.set(null);
        this.bsEditTarget.set(null);
    }

    submitBsCreate(tournamentId: string): void {
        this.pokemonService.createBracketStage(this.bsForm).subscribe({
            next: () => {
                this.notificationService.success('Fase de bracket creada');
                this.closeBsModal();
                this.loadBracketStages(tournamentId);
            },
            error: () => this.notificationService.error('Error')
        });
    }

    submitBsEdit(tournamentId: string): void {
        const bracketStage = this.bsEditTarget();

        if (!bracketStage) {
            return;
        }

        this.pokemonService.updateBracketStage(bracketStage.id, this.bsEditForm).subscribe({
            next: () => {
                this.notificationService.success('Fase actualizada');
                this.closeBsModal();
                this.loadBracketStages(tournamentId);
            },
            error: () => this.notificationService.error('Error')
        });
    }

    confirmBsDelete(tournamentId: string): void {
        const bracketStage = this.bsEditTarget();

        if (!bracketStage) {
            return;
        }

        this.pokemonService.deleteBracketStage(bracketStage.id).subscribe({
            next: () => {
                this.notificationService.success('Fase eliminada');
                this.closeBsModal();
                this.loadBracketStages(tournamentId);
            },
            error: () => this.notificationService.error('Error')
        });
    }

    /* Bracket match */
    selectStage(bracketStage: BracketStageResponse, tournamentId: string): void {
        this.selectedStage.set(bracketStage);
        this.bmForm = {
            tournamentId,
            bracketStageId: bracketStage.id,
            matchNumber: 1,
            status: DEFAULT_BRACKET_MATCH_STATUS
        };

        this.loadBracketMatches(bracketStage.id);
    }

    openBmCreate(): void {
        this.bmModal.set('create');
    }

    openBmEdit(bracketMatch: BracketMatchResponse): void {
        this.bmEditTarget.set(bracketMatch);
        this.bmEditForm = {
            participant1Id: bracketMatch.participant1Id,
            participant2Id: bracketMatch.participant2Id,
            winnerId: bracketMatch.winnerId,
            replayUrl: bracketMatch.replayUrl,
            status: bracketMatch.status,
            scheduledAt: bracketMatch.scheduledAt,
            completedAt: bracketMatch.completedAt
        };

        this.bmModal.set('edit');
    }

    openBmDelete(bracketMatch: BracketMatchResponse): void {
        this.bmEditTarget.set(bracketMatch);
        this.bmModal.set('delete');
    }

    closeBmModal(): void {
        this.bmModal.set(null);
        this.bmEditTarget.set(null);
    }

    submitBmCreate(): void {
        const bracketStage = this.selectedStage();

        if (!bracketStage) {
            return;
        }

        this.pokemonService.createBracketMatch(this.bmForm).subscribe({
            next: () => {
                this.notificationService.success('Partido creado');
                this.closeBmModal();
                this.loadBracketMatches(bracketStage.id);
            },
            error: () => this.notificationService.error('Error')
        });
    }

    submitBmEdit(): void {
        const bracketStage = this.selectedStage();
        const bracketMatch = this.bmEditTarget();

        if (!bracketStage || !bracketMatch) {
            return;
        }

        this.pokemonService.updateBracketMatch(bracketMatch.id, this.bmEditForm).subscribe({
            next: () => {
                this.notificationService.success('Partido actualizado');
                this.closeBmModal();
                this.loadBracketMatches(bracketStage.id);
            },
            error: () => this.notificationService.error('Error')
        });
    }

    confirmBmDelete(): void {
        const bracketStage = this.selectedStage();
        const bracketMatch = this.bmEditTarget();

        if (!bracketStage || !bracketMatch) {
            return;
        }

        this.pokemonService.deleteBracketMatch(bracketMatch.id).subscribe({
            next: () => {
                this.notificationService.success('Partido eliminado');
                this.closeBmModal();
                this.loadBracketMatches(bracketStage.id);
            },
            error: () => this.notificationService.error('Error')
        });
    }

    private loadBracketMatches(stageId: string): void {
        this.pokemonService.getBracketMatchesByStage(stageId).subscribe({
            next: bracketMatches => this.bracketMatches.set(bracketMatches)
        });
    }
}