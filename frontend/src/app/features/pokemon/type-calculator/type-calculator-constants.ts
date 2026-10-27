/**
 * type-calculator-constants
 *
 * Pokémon type calculator constants.
 * Defines the Pokémon types in Spanish, the damage chart, the calculator modes and the result categories.
 *
 * @author @adrianpmdev
 */

export const POKEMON_TYPES = [
    'Normal',
    'Fuego',
    'Agua',
    'Eléctrico',
    'Planta',
    'Hielo',
    'Lucha',
    'Veneno',
    'Tierra',
    'Volador',
    'Psíquico',
    'Bicho',
    'Roca',
    'Fantasma',
    'Dragón',
    'Siniestro',
    'Acero',
    'Hada'
] as const;

export type PokemonType = typeof POKEMON_TYPES[number];

export interface TypeDamageResult {
    type: PokemonType;
    multiplier: number;
}

export const TYPE_CHART: Partial<Record<PokemonType, Partial<Record<PokemonType, number>>>> = {
    'Normal': {
        'Roca': 0.5,
        'Fantasma': 0,
        'Acero': 0.5
    },
    'Fuego': {
        'Fuego': 0.5,
        'Agua': 0.5,
        'Planta': 2,
        'Hielo': 2,
        'Bicho': 2,
        'Roca': 0.5,
        'Dragón': 0.5,
        'Acero': 2
    },
    'Agua': {
        'Fuego': 2,
        'Agua': 0.5,
        'Planta': 0.5,
        'Tierra': 2,
        'Roca': 2,
        'Dragón': 0.5
    },
    'Eléctrico': {
        'Agua': 2,
        'Eléctrico': 0.5,
        'Planta': 0.5,
        'Tierra': 0,
        'Volador': 2,
        'Dragón': 0.5
    },
    'Planta': {
        'Fuego': 0.5,
        'Agua': 2,
        'Planta': 0.5,
        'Veneno': 0.5,
        'Tierra': 2,
        'Volador': 0.5,
        'Bicho': 0.5,
        'Roca': 2,
        'Dragón': 0.5,
        'Acero': 0.5
    },
    'Hielo': {
        'Fuego': 0.5,
        'Agua': 0.5,
        'Planta': 2,
        'Hielo': 0.5,
        'Tierra': 2,
        'Volador': 2,
        'Dragón': 2,
        'Acero': 0.5
    },
    'Lucha': {
        'Normal': 2,
        'Hielo': 2,
        'Veneno': 0.5,
        'Volador': 0.5,
        'Psíquico': 0.5,
        'Bicho': 0.5,
        'Roca': 2,
        'Fantasma': 0,
        'Siniestro': 2,
        'Acero': 2,
        'Hada': 0.5
    },
    'Veneno': {
        'Planta': 2,
        'Veneno': 0.5,
        'Tierra': 0.5,
        'Roca': 0.5,
        'Fantasma': 0.5,
        'Acero': 0,
        'Hada': 2
    },
    'Tierra': {
        'Fuego': 2,
        'Eléctrico': 2,
        'Planta': 0.5,
        'Veneno': 2,
        'Volador': 0,
        'Bicho': 0.5,
        'Roca': 2,
        'Acero': 2
    },
    'Volador': {
        'Eléctrico': 0.5,
        'Planta': 2,
        'Lucha': 2,
        'Bicho': 2,
        'Roca': 0.5,
        'Acero': 0.5
    },
    'Psíquico': {
        'Lucha': 2,
        'Veneno': 2,
        'Psíquico': 0.5,
        'Siniestro': 0,
        'Acero': 0.5
    },
    'Bicho': {
        'Fuego': 0.5,
        'Planta': 2,
        'Lucha': 0.5,
        'Veneno': 0.5,
        'Volador': 0.5,
        'Psíquico': 2,
        'Fantasma': 0.5,
        'Siniestro': 2,
        'Acero': 0.5,
        'Hada': 0.5
    },
    'Roca': {
        'Fuego': 2,
        'Hielo': 2,
        'Lucha': 0.5,
        'Tierra': 0.5,
        'Volador': 2,
        'Bicho': 2,
        'Acero': 0.5
    },
    'Fantasma': {
        'Normal': 0,
        'Psíquico': 2,
        'Fantasma': 2,
        'Siniestro': 0.5
    },
    'Dragón': {
        'Dragón': 2,
        'Acero': 0.5,
        'Hada': 0
    },
    'Siniestro': {
        'Lucha': 0.5,
        'Psíquico': 2,
        'Fantasma': 2,
        'Siniestro': 0.5,
        'Hada': 0.5
    },
    'Acero': {
        'Fuego': 0.5,
        'Agua': 0.5,
        'Eléctrico': 0.5,
        'Hielo': 2,
        'Roca': 2,
        'Acero': 0.5,
        'Hada': 2
    },
    'Hada': {
        'Fuego': 0.5,
        'Lucha': 2,
        'Veneno': 0.5,
        'Dragón': 2,
        'Siniestro': 2,
        'Acero': 0.5
    }
};

/** Damage multiplier of one attacking type against one defending type. */
export function typeMultiplier(attackType: PokemonType, defendingType: PokemonType): number {
    return TYPE_CHART[attackType]?.[defendingType] ?? 1;
}

/** A Pokémon has at most two types, and the attack mode compares at most two move types. */
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
        selectorTitle: '// TIPOS DEFENSIVOS'
    },
    {
        id: 'attack',
        label: 'ATAQUE',
        prompt: 'ATAQUE>',
        title: 'SELECCIONA LOS TIPOS DE ATAQUE',
        description: 'Calcula el mejor daño que hacen uno o dos tipos de movimiento contra cada tipo.',
        selectorTitle: '// TIPOS DE ATAQUE'
    }
];

/** Labels per mode: the same multiplier is bad news in defense and good news in attack. */
export interface ResultCategory {
    id: 'strong' | 'weak' | 'none' | 'neutral';
    labels: Record<CalculatorMode['id'], string>;
    /** CSS modifier per mode: danger, success, immune or neutral. */
    tones: Record<CalculatorMode['id'], string>;
    matches: (multiplier: number) => boolean;
}

export const RESULT_CATEGORIES: readonly ResultCategory[] = [
    {
        id: 'strong',
        labels: { defense: 'DEBILIDADES', attack: 'SÚPER EFICAZ' },
        tones: { defense: 'danger', attack: 'success' },
        matches: multiplier => multiplier > 1
    },
    {
        id: 'weak',
        labels: { defense: 'RESISTENCIAS', attack: 'POCO EFICAZ' },
        tones: { defense: 'success', attack: 'danger' },
        matches: multiplier => multiplier > 0 && multiplier < 1
    },
    {
        id: 'none',
        labels: { defense: 'INMUNIDADES', attack: 'SIN EFECTO' },
        tones: { defense: 'immune', attack: 'immune' },
        matches: multiplier => multiplier === 0
    },
    {
        id: 'neutral',
        labels: { defense: 'NEUTRALES', attack: 'NEUTRAL' },
        tones: { defense: 'neutral', attack: 'neutral' },
        matches: multiplier => multiplier === 1
    }
];
