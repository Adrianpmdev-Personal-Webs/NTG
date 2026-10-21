/**
 * TournamentRewardsFacade
 *
 * Tournament rewards facade.
 * Manages tournament rewards and assigned user rewards.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject, signal } from '@angular/core';
import { ModalMode } from '../modal-mode';
import {
    RewardCreateRequest,
    RewardResponse,
    RewardUpdateRequest,
    UserRewardCreateRequest,
    UserRewardResponse
} from '../../pokemon-models';
import { NotificationService } from '@core/services/notification';
import { PokemonApi } from '../../pokemon-api';

@Injectable({ providedIn: 'root' })
export class TournamentRewardsFacade {

    private readonly pokemonService = inject(PokemonApi);
    private readonly notificationService = inject(NotificationService);

    /* Rewards */
    readonly rewards = signal<RewardResponse[]>([]);
    readonly rewardLoading = signal(false);
    readonly rewardModal = signal<ModalMode>(null);
    readonly rewardEditTarget = signal<RewardResponse | null>(null);

    rewardForm: RewardCreateRequest = {
        tournamentId: '',
        position: 1,
        rewardName: ''
    };

    rewardEditForm: RewardUpdateRequest = {};

    /* User rewards */
    readonly userRewards = signal<UserRewardResponse[]>([]);
    readonly urModal = signal<ModalMode>(null);
    readonly urEditTarget = signal<UserRewardResponse | null>(null);

    urForm: UserRewardCreateRequest = {
        userId: '',
        rewardId: '',
        tournamentId: ''
    };

    /* Rewards */
    loadRewards(tournamentId: string): void {
        this.rewardLoading.set(true);

        this.pokemonService.getRewardsByTournament(tournamentId).subscribe({
            next: rewards => {
                this.rewards.set(rewards);
                this.rewardLoading.set(false);
            },
            error: () => {
                this.notificationService.error('Error al cargar recompensas');
                this.rewardLoading.set(false);
            }
        });

        this.pokemonService.getUserRewardsByTournament(tournamentId).subscribe({
            next: userRewards => this.userRewards.set(userRewards),
            error: () => this.notificationService.error('Error al cargar recompensas asignadas')
        });
    }

    getRewardName(rewardId: string): string {
        const reward = this.rewards().find(currentReward => currentReward.id === rewardId);

        return reward ? reward.rewardName : rewardId.slice(0, 8);
    }

    openRewardCreate(tournamentId: string): void {
        this.rewardForm = {
            tournamentId,
            position: 1,
            rewardName: ''
        };

        this.rewardModal.set('create');
    }

    openRewardEdit(reward: RewardResponse): void {
        this.rewardEditTarget.set(reward);
        this.rewardEditForm = {
            position: reward.position,
            rewardName: reward.rewardName,
            rewardValue: reward.rewardValue,
            imageUrl: reward.imageUrl
        };

        this.rewardModal.set('edit');
    }

    openRewardDelete(reward: RewardResponse): void {
        this.rewardEditTarget.set(reward);
        this.rewardModal.set('delete');
    }

    closeRewardModal(): void {
        this.rewardModal.set(null);
        this.rewardEditTarget.set(null);
    }

    submitRewardCreate(tournamentId: string): void {
        this.pokemonService.createReward(this.rewardForm).subscribe({
            next: () => {
                this.notificationService.success('Recompensa creada');
                this.closeRewardModal();
                this.loadRewards(tournamentId);
            },
            error: () => this.notificationService.error('Error')
        });
    }

    submitRewardEdit(tournamentId: string): void {
        const reward = this.rewardEditTarget();

        if (!reward) {
            return;
        }

        this.pokemonService.updateReward(reward.id, this.rewardEditForm).subscribe({
            next: () => {
                this.notificationService.success('Recompensa actualizada');
                this.closeRewardModal();
                this.loadRewards(tournamentId);
            },
            error: () => this.notificationService.error('Error')
        });
    }

    confirmRewardDelete(tournamentId: string): void {
        const reward = this.rewardEditTarget();

        if (!reward) {
            return;
        }

        this.pokemonService.deleteReward(reward.id).subscribe({
            next: () => {
                this.notificationService.success('Recompensa eliminada');
                this.closeRewardModal();
                this.loadRewards(tournamentId);
            },
            error: () => this.notificationService.error('Error')
        });
    }

    /* User rewards */
    openUrCreate(tournamentId: string): void {
        this.urForm = {
            userId: '',
            rewardId: '',
            tournamentId
        };

        this.urModal.set('create');
    }

    openUrDelete(userReward: UserRewardResponse): void {
        this.urEditTarget.set(userReward);
        this.urModal.set('delete');
    }

    closeUrModal(): void {
        this.urModal.set(null);
        this.urEditTarget.set(null);
    }

    submitUrCreate(tournamentId: string): void {
        this.pokemonService.createUserReward(this.urForm).subscribe({
            next: () => {
                this.notificationService.success('Recompensa asignada');
                this.closeUrModal();
                this.loadRewards(tournamentId);
            },
            error: () => this.notificationService.error('Error')
        });
    }

    confirmUrDelete(tournamentId: string): void {
        const userReward = this.urEditTarget();

        if (!userReward) {
            return;
        }

        this.pokemonService.deleteUserReward(userReward.id).subscribe({
            next: () => {
                this.notificationService.success('Recompensa de usuario eliminada');
                this.closeUrModal();
                this.loadRewards(tournamentId);
            },
            error: () => this.notificationService.error('Error')
        });
    }
}