/**
 * Games
 *
 * Pokémon games page component.
 * Handles game listing, filtering, creation, edition, deletion and navigation to lockes.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ModalMode } from '../modal-mode';
import { GameCreateRequest, GameResponse, GameUpdateRequest } from '../../pokemon-models';
import { AuthService } from '@core/services/auth';
import { NotificationService } from '@core/services/notification';
import { PokemonApi } from '../../pokemon-api';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-games',
    imports: [FormsModule],
    templateUrl: './games.html',
    styleUrl: './games.scss'
})
export class Games implements OnInit {

    readonly authService = inject(AuthService);

    private readonly pokemonService = inject(PokemonApi);
    private readonly notificationService = inject(NotificationService);
    private readonly router = inject(Router);

    readonly games = signal<GameResponse[]>([]);
    readonly filteredGames = signal<GameResponse[]>([]);
    readonly loading = signal(true);
    readonly submitting = signal(false);
    readonly modalMode = signal<ModalMode>(null);
    readonly selectedGame = signal<GameResponse | null>(null);

    searchTerm = '';

    form: GameCreateRequest = {
        name: '',
        description: '',
        imageUrl: ''
    };

    editForm: GameUpdateRequest = {
        name: '',
        description: '',
        imageUrl: ''
    };

    ngOnInit(): void {
        this.loadGames();
    }

    loadGames(): void {
        this.loading.set(true);

        this.pokemonService.getAllGames().subscribe({
            next: games => {
                this.games.set(games);
                this.applyFilter();
                this.loading.set(false);
            },
            error: () => {
                this.notificationService.error('Error al cargar juegos');
                this.loading.set(false);
            }
        });
    }

    applyFilter(): void {
        const searchTerm = this.searchTerm.toLowerCase().trim();

        this.filteredGames.set(
            searchTerm
                ? this.games().filter(game => game.name.toLowerCase().includes(searchTerm))
                : this.games()
        );
    }

    openCreate(): void {
        this.form = {
            name: '',
            description: '',
            imageUrl: ''
        };

        this.modalMode.set('create');
    }

    openEdit(game: GameResponse): void {
        this.selectedGame.set(game);
        this.editForm = {
            name: game.name,
            description: game.description,
            imageUrl: game.imageUrl
        };

        this.modalMode.set('edit');
    }

    openDelete(game: GameResponse): void {
        this.selectedGame.set(game);
        this.modalMode.set('delete');
    }

    closeModal(): void {
        this.modalMode.set(null);
        this.selectedGame.set(null);
        this.submitting.set(false);
    }

    submitCreate(): void {
        if (!this.form.name.trim() || !this.form.imageUrl.trim()) {
            return;
        }

        this.submitting.set(true);

        this.pokemonService.createGame(this.form).subscribe({
            next: () => {
                this.notificationService.success('Juego creado');
                this.closeModal();
                this.loadGames();
            },
            error: () => {
                this.notificationService.error('Error al crear juego');
                this.submitting.set(false);
            }
        });
    }

    submitEdit(): void {
        const game = this.selectedGame();

        if (!game) {
            return;
        }

        this.submitting.set(true);

        this.pokemonService.updateGame(game.id, this.editForm).subscribe({
            next: () => {
                this.notificationService.success('Juego actualizado');
                this.closeModal();
                this.loadGames();
            },
            error: () => {
                this.notificationService.error('Error al actualizar juego');
                this.submitting.set(false);
            }
        });
    }

    confirmDelete(): void {
        const game = this.selectedGame();

        if (!game) {
            return;
        }

        this.submitting.set(true);

        this.pokemonService.deleteGame(game.id).subscribe({
            next: () => {
                this.notificationService.success('Juego eliminado');
                this.closeModal();
                this.loadGames();
            },
            error: () => {
                this.notificationService.error('Error al eliminar juego');
                this.submitting.set(false);
            }
        });
    }

    navigateToLockes(game: GameResponse): void {
        this.router.navigate(['/admin/pokemon/games', game.id, 'lockes'], {
            state: { game }
        });
    }

    formatDate(iso: string): string {
        return new Date(iso).toLocaleDateString('es-ES', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        });
    }
}
