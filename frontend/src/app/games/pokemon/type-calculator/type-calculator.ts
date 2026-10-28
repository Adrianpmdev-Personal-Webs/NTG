/**
 * TypeCalculator
 *
 * Pokémon type calculator page with two modes.
 * Defense: pick up to two types of a Pokémon and see the damage it receives from every attacking type.
 * Attack: pick up to two attacking types (a Pokémon's moves) and see the best damage they deal to every type.
 * The types and the damage chart come from the backend (once per session); the calculation runs here.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { PokemonApi } from '../data/pokemon-api';
import {
    CALCULATOR_MODES,
    CalculatorMode,
    MAX_SELECTED_TYPES,
    PokemonType,
    RESULT_CATEGORIES,
    ResultCategory,
    TypeChart,
    TypeDamageResult,
    typeMultiplier,
} from './type-calculator-constants';

interface ResultGroup {
    category: ResultCategory;
    results: TypeDamageResult[];
}

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-type-calculator',
    imports: [],
    templateUrl: './type-calculator.html',
    styleUrl: './type-calculator.scss',
})
export class TypeCalculator {
    private readonly emptyChart: TypeChart = { types: [], chart: {} };

    private readonly typeChart = toSignal(
        inject(PokemonApi)
            .getTypeChart()
            .pipe(catchError(() => of(this.emptyChart))),
        { initialValue: null },
    );

    protected readonly loading = computed(() => this.typeChart() === null);
    protected readonly failed = computed(() => this.typeChart()?.types.length === 0);
    protected readonly pokemonTypes = computed<PokemonType[]>(() => this.typeChart()?.types ?? []);
    protected readonly modes = CALCULATOR_MODES;
    protected readonly maxSelectedTypes = MAX_SELECTED_TYPES;

    protected readonly mode = signal<CalculatorMode>(CALCULATOR_MODES[0]);
    protected readonly selectedTypes = signal<PokemonType[]>([]);

    private readonly damageResults = computed<TypeDamageResult[]>(() => {
        const selected = this.selectedTypes();
        const attacking = this.mode().id === 'attack';

        return this.pokemonTypes().map((type) => ({
            type,
            multiplier: attacking ? this.bestAttackMultiplier(selected, type) : this.defenseMultiplier(type, selected),
        }));
    });

    protected readonly resultGroups = computed<ResultGroup[]>(() => {
        const results = this.damageResults();

        return RESULT_CATEGORIES.map((category) => ({
            category,
            results: results
                .filter((result) => category.matches(result.multiplier))
                .sort((first, second) => second.multiplier - first.multiplier),
        }));
    });

    protected setMode(modeId: CalculatorMode['id']): void {
        const mode = this.modes.find((current) => current.id === modeId);

        if (mode && mode !== this.mode()) {
            this.mode.set(mode);
            this.selectedTypes.set([]);
        }
    }

    protected toggleType(type: PokemonType): void {
        const current = this.selectedTypes();

        if (current.includes(type)) {
            this.selectedTypes.set(current.filter((selected) => selected !== type));
        } else if (current.length >= this.maxSelectedTypes) {
            this.selectedTypes.set([...current.slice(1), type]);
        } else {
            this.selectedTypes.set([...current, type]);
        }
    }

    protected clearSelection(): void {
        this.selectedTypes.set([]);
    }

    protected isTypeSelected(type: PokemonType): boolean {
        return this.selectedTypes().includes(type);
    }

    protected typeClass(type: PokemonType): string {
        return `type-${type.toLowerCase()}`;
    }

    protected formatMultiplier(multiplier: number): string {
        return `${multiplier}x`;
    }

    private defenseMultiplier(attackType: PokemonType, defendingTypes: readonly PokemonType[]): number {
        const chart = this.typeChart() ?? this.emptyChart;

        return defendingTypes.reduce(
            (total, defendingType) => total * typeMultiplier(chart, attackType, defendingType),
            1,
        );
    }

    private bestAttackMultiplier(attackTypes: readonly PokemonType[], defendingType: PokemonType): number {
        if (attackTypes.length === 0) {
            return 1;
        }

        const chart = this.typeChart() ?? this.emptyChart;

        return Math.max(...attackTypes.map((attackType) => typeMultiplier(chart, attackType, defendingType)));
    }
}
