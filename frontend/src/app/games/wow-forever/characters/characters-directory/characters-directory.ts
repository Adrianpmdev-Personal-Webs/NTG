/**
 * CharactersDirectory
 *
 * Who plays what in WoW Forever: every character of the group by player or by profession, with filters that work in the
 * browser. A player adds their own by name and an admin can correct anybody's; the rest comes from the official armory,
 * through the backend. What is stored shows at once, and opening the screen asks the backend to check for changes.
 *
 * @author @adrianpmdev
 */

import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subscription, forkJoin } from 'rxjs';
import { NotificationCenter } from '@core/notifications/notification-center';
import { resolveApiError } from '@shared/utils/api-error';
import { GameDataApi } from '../../game-data/game-data-api';
import { GameClass } from '../../game-data/game-data-models';
import { ProfessionGroup } from '../../professions/professions-models';
import { Race } from '../../racials/racials-models';
import { WowButton } from '../../ui/wow-button/wow-button';
import { WowPage } from '../../ui/wow-page/wow-page';
import { creditLine, hideBrokenIcon } from '../../wow-forever-config';
import { CharactersApi } from '../characters-api';
import { CharacterRequest, CharactersData, GameCharacter } from '../characters-models';

type DirectoryView = 'players' | 'professions';

const NAME_PATTERN = /^(?=.{2,40}$)\p{L}+(?:[ '’-]\p{L}+)*$/u;
const DELETED_ACCOUNT = 'Cuenta eliminada';

@Component({
    selector: 'app-characters-directory',
    imports: [NgTemplateOutlet, ReactiveFormsModule, WowButton, WowPage],
    templateUrl: './characters-directory.html',
    styleUrl: './characters-directory.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CharactersDirectory implements OnInit {
    private readonly gameData = inject(GameDataApi);
    private readonly api = inject(CharactersApi);
    private readonly notifications = inject(NotificationCenter);
    private readonly formBuilder = inject(FormBuilder).nonNullable;

    protected readonly hideBrokenIcon = hideBrokenIcon;

    protected readonly loading = signal(true);
    protected readonly failed = signal(false);
    protected readonly saving = signal(false);
    protected readonly updating = signal(false);
    protected readonly credit = signal<string | null>(null);
    protected readonly classes = signal<GameClass[]>([]);
    protected readonly races = signal<Race[]>([]);
    protected readonly groups = signal<ProfessionGroup[]>([]);
    private readonly data = signal<CharactersData | null>(null);

    protected readonly view = signal<DirectoryView>('players');
    protected readonly playerFilter = signal('');
    protected readonly classFilter = signal('');
    protected readonly professionFilter = signal('');

    protected readonly formOpen = signal(false);
    protected readonly editing = signal<GameCharacter | null>(null);
    protected readonly pendingDelete = signal<string | null>(null);

    protected readonly form = this.formBuilder.group({
        ownerId: [''],
        name: ['', [Validators.required, Validators.pattern(NAME_PATTERN)]],
        notes: ['', Validators.maxLength(200)],
    });

    protected readonly characters = computed(() => this.data()?.characters ?? []);
    protected readonly limit = computed(() => this.data()?.limit ?? 0);
    protected readonly owners = computed(() => this.data()?.owners ?? []);
    protected readonly isAdmin = computed(() => this.owners().length > 0);

    protected readonly allProfessions = computed(() => this.groups().flatMap((group) => group.professions));

    protected readonly players = computed(() => {
        const players = new Map<string, string>();

        this.characters().forEach((character) => players.set(character.owner.id, this.ownerName(character)));

        return [...players].map(([id, name]) => ({ id, name }));
    });

    private readonly filtered = computed(() => {
        const player = this.playerFilter();
        const gameClass = this.classFilter();
        const profession = this.professionFilter();

        return this.characters().filter(
            (character) =>
                (!player || character.owner.id === player) &&
                (!gameClass || character.classSlug === gameClass) &&
                (!profession || character.professions.some((known) => known.slug === profession)),
        );
    });

    protected readonly byPlayer = computed(() => {
        const players = new Map<string, { id: string; name: string; characters: GameCharacter[] }>();

        for (const character of this.filtered()) {
            const player = players.get(character.owner.id);

            if (player) {
                player.characters.push(character);
            } else {
                players.set(character.owner.id, {
                    id: character.owner.id,
                    name: this.ownerName(character),
                    characters: [character],
                });
            }
        }

        return [...players.values()];
    });

    protected readonly byProfession = computed(() => {
        const wanted = this.professionFilter();

        return this.allProfessions()
            .filter((profession) => !wanted || profession.slug === wanted)
            .map((profession) => ({
                profession,
                holders: this.filtered()
                    .flatMap((character) => {
                        const known = character.professions.find((candidate) => candidate.slug === profession.slug);

                        return known ? [{ character, skill: known.skill, maxSkill: known.maxSkill }] : [];
                    })
                    .sort((one, other) => (other.skill ?? 0) - (one.skill ?? 0)),
            }))
            .filter((row) => row.holders.length > 0);
    });

    private refreshLoad: Subscription | null = null;

    constructor() {
        inject(DestroyRef).onDestroy(() => this.refreshLoad?.unsubscribe());
    }

    ngOnInit(): void {
        forkJoin({
            index: this.gameData.getIndex(),
            racials: this.gameData.getRacials(),
            professions: this.gameData.getProfessions(),
            characters: this.api.getAll(),
        }).subscribe({
            next: ({ index, racials, professions, characters }) => {
                this.classes.set(index.classes);
                this.races.set(racials.races);
                this.groups.set(professions.groups);
                this.credit.set(creditLine(index.credit));
                this.data.set(characters);
                this.loading.set(false);
                this.refresh();
            },
            error: () => {
                this.failed.set(true);
                this.loading.set(false);
            },
        });
    }

    protected ownerName(character: GameCharacter): string {
        return character.owner.username ?? DELETED_ACCOUNT;
    }

    protected className(slug: string | null): string {
        return this.classes().find((gameClass) => gameClass.slug === slug)?.name ?? slug ?? '';
    }

    protected classIcon(slug: string | null): string {
        return this.classes().find((gameClass) => gameClass.slug === slug)?.icon ?? '';
    }

    protected raceName(slug: string | null): string {
        return this.races().find((race) => race.slug === slug)?.name ?? slug ?? '';
    }

    protected professionName(slug: string): string {
        return this.allProfessions().find((profession) => profession.slug === slug)?.name ?? slug;
    }

    protected professionIcon(slug: string): string {
        return this.allProfessions().find((profession) => profession.slug === slug)?.icon ?? '';
    }

    protected openNew(): void {
        this.editing.set(null);
        this.pendingDelete.set(null);
        this.form.reset();
        this.formOpen.set(true);
    }

    protected openEdit(character: GameCharacter): void {
        this.editing.set(character);
        this.pendingDelete.set(null);
        this.form.reset();
        this.form.patchValue({
            name: character.name,
            notes: character.notes ?? '',
        });
        this.formOpen.set(true);
    }

    protected closeForm(): void {
        this.formOpen.set(false);
        this.editing.set(null);
    }

    protected save(): void {
        if (this.form.invalid || this.saving()) {
            return;
        }

        const editing = this.editing();
        const request = this.toRequest();

        this.stopRefresh();
        this.saving.set(true);
        (editing ? this.api.update(editing.id, request) : this.api.create(request)).subscribe({
            next: (data) => {
                this.data.set(data);
                this.saving.set(false);
                this.closeForm();
                this.notifications.success(editing ? 'Personaje actualizado' : 'Personaje añadido');
            },
            error: (error) => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido guardar el personaje'));
            },
        });
    }

    protected remove(character: GameCharacter): void {
        if (this.saving()) {
            return;
        }

        this.stopRefresh();
        this.saving.set(true);
        this.api.delete(character.id).subscribe({
            next: (data) => {
                this.data.set(data);
                this.saving.set(false);
                this.pendingDelete.set(null);
            },
            error: (error) => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido borrar el personaje'));
            },
        });
    }

    private refresh(): void {
        this.updating.set(true);
        this.refreshLoad = this.api.refresh().subscribe({
            next: (data) => {
                this.data.set(data);
                this.updating.set(false);
            },
            error: () => this.updating.set(false),
        });
    }

    /** A check that ends after a save would answer a list older than the save. */
    private stopRefresh(): void {
        this.refreshLoad?.unsubscribe();
        this.updating.set(false);
    }

    private toRequest(): CharacterRequest {
        const value = this.form.getRawValue();

        return {
            ownerId: value.ownerId || null,
            name: value.name.trim(),
            notes: value.notes.trim() || null,
        };
    }
}
