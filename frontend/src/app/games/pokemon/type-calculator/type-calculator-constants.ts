/**
 * type-calculator-constants
 *
 * Pokémon type calculator constants: the shape of the type chart the backend sends, the calculator modes and the
 * result categories.
 *
 * @author @adrianpmdev
 */

export type PokemonType = string;

export interface TypeChart {
    types: PokemonType[];
    chart: Partial<Record<PokemonType, Partial<Record<PokemonType, number>>>>;
}

export interface TypeDamageResult {
    type: PokemonType;
    multiplier: number;
}

export function typeMultiplier(chart: TypeChart, attackType: PokemonType, defendingType: PokemonType): number {
    return chart.chart[attackType]?.[defendingType] ?? 1;
}

export const MAX_SELECTED_TYPES = 2;

export interface CalculatorMode {
    id: 'defense' | 'attack';
    label: string;
    prompt: string;
    title: string;
    description: string;
    selectorTitle: string;
}

export const CALCULATOR_MODES: readonly CalculatorMode[] = [
    {
        id: 'defense',
        label: 'DEFENSA',
        prompt: 'DEFENSA>',
        title: 'SELECCIONA LOS TIPOS DEL POKÉMON',
        description: 'Calcula el daño que recibe un Pokémon con uno o dos tipos de cada tipo de ataque.',
        selectorTitle: '// TIPOS DEFENSIVOS',
    },
    {
        id: 'attack',
        label: 'ATAQUE',
        prompt: 'ATAQUE>',
        title: 'SELECCIONA LOS TIPOS DE ATAQUE',
        description: 'Calcula el mejor daño que hacen uno o dos tipos de movimiento contra cada tipo.',
        selectorTitle: '// TIPOS DE ATAQUE',
    },
];

export interface ResultCategory {
    id: 'strong' | 'weak' | 'none' | 'neutral';
    labels: Record<CalculatorMode['id'], string>;
    tones: Record<CalculatorMode['id'], string>;
    matches: (multiplier: number) => boolean;
}

export const RESULT_CATEGORIES: readonly ResultCategory[] = [
    {
        id: 'strong',
        labels: { defense: 'DEBILIDADES', attack: 'SÚPER EFICAZ' },
        tones: { defense: 'danger', attack: 'success' },
        matches: (multiplier) => multiplier > 1,
    },
    {
        id: 'weak',
        labels: { defense: 'RESISTENCIAS', attack: 'POCO EFICAZ' },
        tones: { defense: 'success', attack: 'danger' },
        matches: (multiplier) => multiplier > 0 && multiplier < 1,
    },
    {
        id: 'none',
        labels: { defense: 'INMUNIDADES', attack: 'SIN EFECTO' },
        tones: { defense: 'immune', attack: 'immune' },
        matches: (multiplier) => multiplier === 0,
    },
    {
        id: 'neutral',
        labels: { defense: 'NEUTRALES', attack: 'NEUTRAL' },
        tones: { defense: 'neutral', attack: 'neutral' },
        matches: (multiplier) => multiplier === 1,
    },
];
