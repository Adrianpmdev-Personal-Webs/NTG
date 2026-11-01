/**
 * CharactersDirectory
 *
 * Who plays what in WoW Forever: every character of the group by player or by profession, with filters that work in the
 * browser. A player adds and changes their own and an admin can correct anybody's; the form only offers the classes,
 * the races of that class and the professions the game data has.
 *
 * @author @adrianpmdev
 */

import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin, map, startWith } from 'rxjs';
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
import { CharacterProfession, CharacterRequest, CharactersData, GameCharacter } from '../characters-models';

type DirectoryView = 'players' | 'professions';

const SECONDARY_GROUP = 'secondary';
const MAX_SKILL = 300;
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
    private readonly destroyRef = inject(DestroyRef);

    protected readonly hideBrokenIcon = hideBrokenIcon;
    protected readonly maxSkill = MAX_SKILL;

    protected readonly loading = signal(true);
    protected readonly failed = signal(false);
    protected readonly saving = signal(false);
    protected readonly credit = signal<string | null>(null);
    protected readonly classes = signal<GameClass[]>([]);
    protected readonly races = signal<Race[]>([]);
    protected readonly groups = signal<ProfessionGroup[]>([]);
    protected readonly maxLevel = signal(1);
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
        classSlug: ['', Validators.required],
        raceSlug: ['', Validators.required],
        level: [1, [Validators.required, Validators.min(1)]],
        primaryOne: [''],
        primaryOneSkill: this.skillControl(),
        primaryTwo: [''],
        primaryTwoSkill: this.skillControl(),
        secondaries: this.formBuilder.array<ReturnType<CharactersDirectory['secondaryControl']>>([]),
        notes: ['', Validators.maxLength(200)],
    });

    protected readonly formValue = toSignal(
        this.form.valueChanges.pipe(
            startWith(null),
            map(() => this.form.getRawValue()),
        ),
        { requireSync: true },
    );

    protected readonly characters = computed(() => this.data()?.characters ?? []);
    protected readonly limit = computed(() => this.data()?.limit ?? 0);
    protected readonly owners = computed(() => this.data()?.owners ?? []);
    protected readonly isAdmin = computed(() => this.owners().length > 0);

    protected readonly primaryGroups = computed(() => this.groups().filter((group) => group.id !== SECONDARY_GROUP));
    protected readonly secondaryProfessions = computed(
        () => this.groups().find((group) => group.id === SECONDARY_GROUP)?.professions ?? [],
    );
    protected readonly allProfessions = computed(() => this.groups().flatMap((group) => group.professions));

    protected readonly racesOfClass = computed(() => this.racesFor(this.formValue().classSlug));

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

                        return known ? [{ character, skill: known.skill }] : [];
                    })
                    .sort((one, other) => (other.skill ?? 0) - (one.skill ?? 0)),
            }))
            .filter((row) => row.holders.length > 0);
    });

    constructor() {
        this.form.controls.classSlug.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((classSlug) => {
            const race = this.form.controls.raceSlug;

            if (race.value && !this.racesFor(classSlug).some((candidate) => candidate.slug === race.value)) {
                race.setValue('');
            }
        });

        this.form.controls.primaryOne.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((slug) => {
            if (!slug) {
                this.form.controls.primaryOneSkill.setValue(null);
            }

            if (slug && slug === this.form.controls.primaryTwo.value) {
                this.form.controls.primaryTwo.setValue('');
            }
        });

        this.form.controls.primaryTwo.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((slug) => {
            if (!slug) {
                this.form.controls.primaryTwoSkill.setValue(null);
            }
        });
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
                this.maxLevel.set(index.talentPointLevels.at(-1) ?? 1);
                this.credit.set(creditLine(index.credit));
                this.data.set(characters);
                this.form.controls.level.addValidators(Validators.max(this.maxLevel()));
                professions.groups
                    .find((group) => group.id === SECONDARY_GROUP)
                    ?.professions.forEach((profession) =>
                        this.form.controls.secondaries.push(this.secondaryControl(profession.slug)),
                    );
                this.loading.set(false);
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

    protected className(slug: string): string {
        return this.classes().find((gameClass) => gameClass.slug === slug)?.name ?? slug;
    }

    protected classIcon(slug: string): string {
        return this.classes().find((gameClass) => gameClass.slug === slug)?.icon ?? '';
    }

    protected raceName(slug: string): string {
        return this.races().find((race) => race.slug === slug)?.name ?? slug;
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
        this.form.controls.level.setValue(this.maxLevel());
        this.formOpen.set(true);
    }

    protected openEdit(character: GameCharacter): void {
        const secondarySlugs = this.secondaryProfessions().map((profession) => profession.slug);
        const primaries = character.professions.filter((known) => !secondarySlugs.includes(known.slug));

        this.editing.set(character);
        this.pendingDelete.set(null);
        this.form.reset();
        this.form.patchValue({
            name: character.name,
            classSlug: character.classSlug,
            raceSlug: character.raceSlug,
            level: character.level,
            primaryOne: primaries[0]?.slug ?? '',
            primaryOneSkill: primaries[0]?.skill ?? null,
            primaryTwo: primaries[1]?.slug ?? '',
            primaryTwoSkill: primaries[1]?.skill ?? null,
            notes: character.notes ?? '',
        });
        this.form.controls.secondaries.controls.forEach((control) => {
            const known = character.professions.find((candidate) => candidate.slug === control.controls.slug.value);

            control.patchValue({ known: known !== undefined, skill: known?.skill ?? null });
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

    private racesFor(classSlug: string): Race[] {
        const className = this.classes().find((gameClass) => gameClass.slug === classSlug)?.name;

        return className ? this.races().filter((race) => race.classNames.includes(className)) : [];
    }

    private toRequest(): CharacterRequest {
        const value = this.form.getRawValue();
        const professions: CharacterProfession[] = [];

        if (value.primaryOne) {
            professions.push({ slug: value.primaryOne, skill: value.primaryOneSkill });
        }

        if (value.primaryTwo) {
            professions.push({ slug: value.primaryTwo, skill: value.primaryTwoSkill });
        }

        value.secondaries
            .filter((secondary) => secondary.known)
            .forEach((secondary) => professions.push({ slug: secondary.slug, skill: secondary.skill }));

        return {
            ownerId: value.ownerId || null,
            name: value.name.trim(),
            classSlug: value.classSlug,
            raceSlug: value.raceSlug,
            level: value.level,
            professions,
            notes: value.notes.trim() || null,
        };
    }

    private skillControl() {
        return this.formBuilder.control<number | null>(null, [Validators.min(1), Validators.max(MAX_SKILL)]);
    }

    private secondaryControl(slug: string) {
        const control = this.formBuilder.group({
            slug: [slug],
            known: [false],
            skill: this.skillControl(),
        });

        control.controls.known.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((known) => {
            if (!known) {
                control.controls.skill.setValue(null);
            }
        });

        return control;
    }
}
