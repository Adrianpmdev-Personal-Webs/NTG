/**
 * TypeCalculator
 *
 * Pokémon type calculator page with two modes.
 * Defense: pick up to two types of a Pokémon and see the damage it receives from every attacking type.
 * Attack: pick up to two attacking types (a Pokémon's moves) and see the best damage they deal to every type.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import {
    CALCULATOR_MODES,
    CalculatorMode,
    MAX_SELECTED_TYPES,
    POKEMON_TYPES,
    PokemonType,
    RESULT_CATEGORIES,
    ResultCategory,
    TypeDamageResult,
    typeMultiplier
} from './type-calculator-constants';

/** One block of results (for example the weaknesses) ready for the template. */
interface ResultGroup {
    category: ResultCategory;
    results: TypeDamageResult[];
}

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-type-calculator',
    imports: [],
    templateUrl: './type-calculator.html',
    styleUrl: './type-calculator.scss'
})
export class TypeCalculator {

    readonly pokemonTypes = POKEMON_TYPES;
    readonly modes = CALCULATOR_MODES;
    readonly maxSelectedTypes = MAX_SELECTED_TYPES;

    readonly mode = signal<CalculatorMode>(CALCULATOR_MODES[0]);
    readonly selectedTypes = signal<PokemonType[]>([]);

    /** Multiplier against (attack) or from (defense) every type, for the current selection. */
    private readonly damageResults = computed<TypeDamageResult[]>(() => {
        const selected = this.selectedTypes();
        const attacking = this.mode().id === 'attack';

        return this.pokemonTypes.map(type => ({
            type,
            multiplier: attacking ? this.bestAttackMultiplier(selected, type) : this.defenseMultiplier(type, selected)
        }));
    });

    /** Results split into weaknesses / resistances / immunities / neutral, strongest first. */
    readonly resultGroups = computed<ResultGroup[]>(() => {
        const results = this.damageResults();

        return RESULT_CATEGORIES.map(category => ({
            category,
            results: results
                .filter(result => category.matches(result.multiplier))
                .sort((first, second) => second.multiplier - first.multiplier)
        }));
    });

    setMode(modeId: CalculatorMode['id']): void {
        const mode = this.modes.find(current => current.id === modeId);

        if (mode && mode !== this.mode()) {
            this.mode.set(mode);
            this.selectedTypes.set([]);
        }
    }

    toggleType(type: PokemonType): void {
        const current = this.selectedTypes();

        if (current.includes(type)) {
            this.selectedTypes.set(current.filter(selected => selected !== type));
        } else if (current.length >= this.maxSelectedTypes) {
            // Replace the oldest pick so the user can keep clicking without clearing first.
            this.selectedTypes.set([...current.slice(1), type]);
        } else {
            this.selectedTypes.set([...current, type]);
        }
    }

    clearSelection(): void {
        this.selectedTypes.set([]);
    }

    isTypeSelected(type: PokemonType): boolean {
        return this.selectedTypes().includes(type);
    }

    typeClass(type: PokemonType): string {
        return `type-${type.toLowerCase()}`;
    }

    formatMultiplier(multiplier: number): string {
        return `${multiplier}x`;
    }

    /** Damage a Pokémon with the defending types receives from one attacking type. */
    private defenseMultiplier(attackType: PokemonType, defendingTypes: readonly PokemonType[]): number {
        return defendingTypes.reduce((total, defendingType) => total * typeMultiplier(attackType, defendingType), 1);
    }

    /** Best damage any of the attacking types deals to one defending type (move coverage). */
    private bestAttackMultiplier(attackTypes: readonly PokemonType[], defendingType: PokemonType): number {
        if (attackTypes.length === 0) {
            return 1;
        }

        return Math.max(...attackTypes.map(attackType => typeMultiplier(attackType, defendingType)));
    }
}
