/**
 * TypeCalculator
 *
 * Pokémon type calculator page.
 * Allows selecting up to two defending types and calculates received damage multipliers.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import {
    POKEMON_TYPES,
    PokemonType,
    TYPE_CHART,
    TypeDamageResult
} from './type-calculator-constants';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-type-calculator',
    imports: [],
    templateUrl: './type-calculator.html',
    styleUrl: './type-calculator.scss'
})
export class TypeCalculator {

    private readonly maxSelectedTypes = 2;
    private readonly defaultMultiplier = 1;

    readonly pokemonTypes = POKEMON_TYPES;
    readonly selectedTypes = signal<PokemonType[]>([]);

    readonly damageResults = computed<TypeDamageResult[]>(() => {
        return this.pokemonTypes.map(attackType => ({
            type: attackType,
            multiplier: this.calculateDamageMultiplier(attackType)
        }));
    });

    readonly weaknesses = computed(() => {
        return this.damageResults()
            .filter(result => result.multiplier > 1)
            .sort((first, second) => second.multiplier - first.multiplier);
    });

    readonly resistances = computed(() => {
        return this.damageResults()
            .filter(result => result.multiplier > 0 && result.multiplier < 1)
            .sort((first, second) => first.multiplier - second.multiplier);
    });

    readonly immunities = computed(() => {
        return this.damageResults().filter(result => result.multiplier === 0);
    });

    readonly neutralTypes = computed(() => {
        return this.damageResults().filter(result => result.multiplier === 1);
    });

    toggleType(type: PokemonType): void {
        const currentTypes = this.selectedTypes();

        if (currentTypes.includes(type)) {
            this.selectedTypes.set(currentTypes.filter(currentType => currentType !== type));
            return;
        }

        if (currentTypes.length >= this.maxSelectedTypes) {
            this.selectedTypes.set([currentTypes[1], type]);
            return;
        }

        this.selectedTypes.set([...currentTypes, type]);
    }

    clearSelection(): void {
        this.selectedTypes.set([]);
    }

    isTypeSelected(type: PokemonType): boolean {
        return this.selectedTypes().includes(type);
    }

    getTypeClass(type: PokemonType): string {
        return `type-${type.toLowerCase()}`;
    }

    formatMultiplier(multiplier: number): string {
        return `${multiplier}x`;
    }

    private calculateDamageMultiplier(attackType: PokemonType): number {
        const defendingTypes = this.selectedTypes();

        if (defendingTypes.length === 0) {
            return this.defaultMultiplier;
        }

        return defendingTypes.reduce((totalMultiplier, defendingType) => {
            return totalMultiplier * this.getTypeMultiplier(attackType, defendingType);
        }, this.defaultMultiplier);
    }

    private getTypeMultiplier(attackType: PokemonType, defendingType: PokemonType): number {
        return TYPE_CHART[attackType]?.[defendingType] ?? this.defaultMultiplier;
    }
}
