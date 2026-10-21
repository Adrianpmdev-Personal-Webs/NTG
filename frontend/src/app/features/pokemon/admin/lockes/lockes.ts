/**
 * Lockes
 *
 * Pokémon lockes page component.
 * Handles locke listing, rules visualization, creation, edition, deletion and tournament navigation.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { RulesModalMode } from '../modal-mode';
import { GameResponse, LockeCreateRequest, LockeResponse, LockeUpdateRequest } from '../../pokemon-models';
import { AuthService } from '@core/services/auth';
import { NotificationService } from '@core/services/notification';
import { PokemonApi } from '../../pokemon-api';
import { LockeRule, LockeRules } from '../../locke-rules';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-lockes',
    imports: [FormsModule],
    templateUrl: './lockes.html',
    styleUrl: './lockes.scss'
})
export class Lockes implements OnInit {

    readonly authService = inject(AuthService);

    private readonly pokemonService = inject(PokemonApi);
    private readonly notificationService = inject(NotificationService);
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);

    readonly gameId = signal('');
    readonly game = signal<GameResponse | null>(null);
    readonly lockes = signal<LockeResponse[]>([]);
    readonly loading = signal(true);
    readonly submitting = signal(false);
    readonly modalMode = signal<RulesModalMode>(null);
    readonly selectedLocke = signal<LockeResponse | null>(null);

    form: LockeCreateRequest = {
        gameId: '',
        name: '',
        description: '',
        rules: null
    };

    editForm: LockeUpdateRequest = {
        name: '',
        description: '',
        rules: null
    };

    rulesText = '';
    editRulesText = '';

    ngOnInit(): void {
        const gameId = this.route.snapshot.paramMap.get('gameId') || '';

        this.gameId.set(gameId);
        this.loadGame(gameId);
        this.loadLockes();
    }

    loadLockes(): void {
        this.loading.set(true);

        this.pokemonService.getLockesByGameId(this.gameId()).subscribe({
            next: lockes => {
                this.lockes.set(lockes);
                this.loading.set(false);
            },
            error: () => {
                this.notificationService.error('Error al cargar lockes');
                this.loading.set(false);
            }
        });
    }

    openCreate(): void {
        this.form = {
            gameId: this.gameId(),
            name: '',
            description: '',
            rules: null
        };

        this.rulesText = '';
        this.modalMode.set('create');
    }

    openEdit(locke: LockeResponse): void {
        this.selectedLocke.set(locke);
        this.editForm = {
            name: locke.name,
            description: locke.description,
            rules: locke.rules
        };
        this.editRulesText = locke.rules ? JSON.stringify(locke.rules, null, 2) : '';
        this.modalMode.set('edit');
    }

    openDelete(locke: LockeResponse): void {
        this.selectedLocke.set(locke);
        this.modalMode.set('delete');
    }

    openRules(locke: LockeResponse): void {
        this.selectedLocke.set(locke);
        this.modalMode.set('rules');
    }

    closeModal(): void {
        this.modalMode.set(null);
        this.selectedLocke.set(null);
        this.submitting.set(false);
    }

    parseRules(text: string): LockeRules | null {
        if (!text.trim()) {
            return null;
        }

        try {
            const parsedRules = JSON.parse(text) as LockeRules;

            if (!this.isLockeRules(parsedRules)) {
                this.notificationService.error('El JSON de reglas no tiene el formato esperado');
                return null;
            }

            return parsedRules;
        } catch {
            this.notificationService.error('El JSON de reglas no es válido');
            return null;
        }
    }

    getSelectedRules(): LockeRules | null {
        const rules = this.selectedLocke()?.rules as LockeRules | null;

        if (!this.isLockeRules(rules)) {
            return null;
        }

        return rules;
    }

    getSelectedRuleItems(): LockeRule[] {
        return this.getSelectedRules()?.rules ?? [];
    }

    private isLockeRules(value: LockeRules | null): value is LockeRules {
        return value !== null
            && typeof value === 'object'
            && !Array.isArray(value)
            && (!value.rules || Array.isArray(value.rules));
    }

    submitCreate(): void {
        if (!this.form.name.trim()) {
            return;
        }

        const parsedRules = this.parseRules(this.rulesText);

        if (this.rulesText.trim() && parsedRules === null) {
            return;
        }

        this.form.rules = parsedRules;
        this.submitting.set(true);

        this.pokemonService.createLocke(this.form).subscribe({
            next: () => {
                this.notificationService.success('Locke creado');
                this.closeModal();
                this.loadLockes();
            },
            error: () => {
                this.notificationService.error('Error al crear locke');
                this.submitting.set(false);
            }
        });
    }

    submitEdit(): void {
        const locke = this.selectedLocke();

        if (!locke) {
            return;
        }

        const parsedRules = this.parseRules(this.editRulesText);

        if (this.editRulesText.trim() && parsedRules === null) {
            return;
        }

        this.editForm.rules = parsedRules;
        this.submitting.set(true);

        this.pokemonService.updateLocke(locke.id, this.editForm).subscribe({
            next: () => {
                this.notificationService.success('Locke actualizado');
                this.closeModal();
                this.loadLockes();
            },
            error: () => {
                this.notificationService.error('Error al actualizar locke');
                this.submitting.set(false);
            }
        });
    }

    confirmDelete(): void {
        const locke = this.selectedLocke();

        if (!locke) {
            return;
        }

        this.submitting.set(true);

        this.pokemonService.deleteLocke(locke.id).subscribe({
            next: () => {
                this.notificationService.success('Locke eliminado');
                this.closeModal();
                this.loadLockes();
            },
            error: () => {
                this.notificationService.error('Error al eliminar locke');
                this.submitting.set(false);
            }
        });
    }

    navigateToTournaments(locke: LockeResponse): void {
        this.router.navigate(['/admin/pokemon/games', this.gameId(), 'lockes', locke.id, 'tournaments'], {
            state: {
                game: this.game(),
                locke
            }
        });
    }

    formatDate(iso: string): string {
        return new Date(iso).toLocaleDateString('es-ES', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        });
    }

    goBack(): void {
        this.router.navigate(['/admin/pokemon/games']);
    }

    private loadGame(gameId: string): void {
        const state = history.state as { game?: GameResponse };

        if (state?.game) {
            this.game.set(state.game);
            return;
        }

        this.pokemonService.getGameById(gameId).subscribe({
            next: game => this.game.set(game)
        });
    }
}
