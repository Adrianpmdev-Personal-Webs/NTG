/**
 * CharactersDirectory
 *
 * Who plays what in WoW Forever: the characters of the group in one fold per player. The caller's own fold opens and
 * reads the armory at once; the others only read it when they are opened. Each character shows its level, race, class
 * and professions in short and opens its whole sheet. A player adds their own by name and an admin can correct
 * anybody's.
 *
 * @author @adrianpmdev
 */

import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { NotificationCenter } from '@core/notifications/notification-center';
import { resolveApiError } from '@shared/utils/api-error';
import { GameDataApi } from '../../game-data/game-data-api';
import { GameClass } from '../../game-data/game-data-models';
import { WowButton } from '../../ui/wow-button/wow-button';
import { WowPage } from '../../ui/wow-page/wow-page';
import { hideBrokenIcon } from '../../wow-forever-config';
import { CharactersApi } from '../characters-api';
import { ARMORY_CREDIT } from '../characters-texts';
import { CharacterRequest, CharacterSummary, CharactersData, GameCharacter } from '../characters-models';

const NAME_PATTERN = /^(?=.{2,40}$)\p{L}+(?:[ '’-]\p{L}+)*$/u;
const DELETED_ACCOUNT = 'Cuenta eliminada';

interface PlayerFold {
    id: string;
    name: string;
    characters: GameCharacter[];
}

interface FoldSummaries {
    loading: boolean;
    failed: boolean;
    byId: Record<string, CharacterSummary>;
}

@Component({
    selector: 'app-characters-directory',
    imports: [NgTemplateOutlet, ReactiveFormsModule, RouterLink, WowButton, WowPage],
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
    protected readonly credit = ARMORY_CREDIT;

    protected readonly loading = signal(true);
    protected readonly failed = signal(false);
    protected readonly saving = signal(false);
    protected readonly classes = signal<GameClass[]>([]);
    private readonly data = signal<CharactersData | null>(null);

    protected readonly opened = signal<ReadonlySet<string>>(new Set());
    protected readonly summaries = signal<Readonly<Record<string, FoldSummaries>>>({});

    protected readonly formOpen = signal(false);
    protected readonly editing = signal<GameCharacter | null>(null);
    protected readonly pendingDelete = signal<string | null>(null);

    protected readonly form = this.formBuilder.group({
        ownerId: [''],
        name: ['', [Validators.required, Validators.pattern(NAME_PATTERN)]],
    });

    protected readonly characters = computed(() => this.data()?.characters ?? []);
    protected readonly limit = computed(() => this.data()?.limit ?? 0);
    protected readonly owners = computed(() => this.data()?.owners ?? []);
    protected readonly isAdmin = computed(() => this.owners().length > 0);
    private readonly myUserId = computed(() => this.data()?.myUserId ?? null);

    protected readonly folds = computed(() => {
        const folds = new Map<string, PlayerFold>();
        const mine = this.myUserId();

        if (mine) {
            folds.set(mine, { id: mine, name: 'Mis personajes', characters: [] });
        }

        for (const character of this.characters()) {
            const fold = folds.get(character.owner.id);

            if (fold) {
                fold.characters.push(character);
            } else {
                folds.set(character.owner.id, {
                    id: character.owner.id,
                    name: this.ownerName(character),
                    characters: [character],
                });
            }
        }

        return [...folds.values()];
    });

    ngOnInit(): void {
        forkJoin({ index: this.gameData.getIndex(), characters: this.api.getAll() }).subscribe({
            next: ({ index, characters }) => {
                this.classes.set(index.classes);
                this.data.set(characters);
                this.loading.set(false);

                if (characters.myUserId) {
                    this.toggle(characters.myUserId);
                }
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

    protected classIcon(slug: string | null): string {
        return this.classes().find((gameClass) => gameClass.slug === slug)?.icon ?? '';
    }

    protected summaryOf(playerId: string, characterId: string): CharacterSummary | null {
        return this.summaries()[playerId]?.byId[characterId] ?? null;
    }

    protected toggle(playerId: string): void {
        const opened = new Set(this.opened());

        if (opened.has(playerId)) {
            opened.delete(playerId);
        } else {
            opened.add(playerId);

            if (!this.summaries()[playerId] || this.summaries()[playerId].failed) {
                this.loadSummaries(playerId);
            }
        }

        this.opened.set(opened);
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
        this.form.patchValue({ name: character.name });
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

        this.saving.set(true);
        (editing ? this.api.update(editing.id, request) : this.api.create(request)).subscribe({
            next: (data) => {
                this.afterWrite(data);
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

        this.saving.set(true);
        this.api.delete(character.id).subscribe({
            next: (data) => {
                this.afterWrite(data);
                this.pendingDelete.set(null);
            },
            error: (error) => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido borrar el personaje'));
            },
        });
    }

    /** A write can add, rename or move characters, so the open folds read their summaries again. */
    private afterWrite(data: CharactersData): void {
        this.data.set(data);
        this.saving.set(false);
        this.summaries.set({});
        this.opened().forEach((playerId) => this.loadSummaries(playerId));
    }

    private loadSummaries(playerId: string): void {
        const previous = this.summaries()[playerId];

        this.summaries.update((all) => ({
            ...all,
            [playerId]: { loading: true, failed: false, byId: previous?.byId ?? {} },
        }));
        this.api.summaries(playerId).subscribe({
            next: (list) =>
                this.summaries.update((all) => ({
                    ...all,
                    [playerId]: {
                        loading: false,
                        failed: false,
                        byId: Object.fromEntries(list.map((summary) => [summary.id, summary])),
                    },
                })),
            error: () =>
                this.summaries.update((all) => ({
                    ...all,
                    [playerId]: { loading: false, failed: true, byId: previous?.byId ?? {} },
                })),
        });
    }

    private toRequest(): CharacterRequest {
        const value = this.form.getRawValue();

        return {
            ownerId: value.ownerId || null,
            name: value.name.trim(),
        };
    }
}
