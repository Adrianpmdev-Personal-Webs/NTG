/**
 * pokelike-models
 *
 * Pokelike feature models.
 * Defines game modes, run phases, assets, Pokémon, items and map nodes.
 *
 * @author @adrianpmdev
 */

export type PokemonType =
    | 'Normal'
    | 'Fuego'
    | 'Agua'
    | 'Eléctrico'
    | 'Planta'
    | 'Hielo'
    | 'Lucha'
    | 'Veneno'
    | 'Tierra'
    | 'Volador'
    | 'Psíquico'
    | 'Bicho'
    | 'Roca'
    | 'Fantasma'
    | 'Dragón'
    | 'Siniestro'
    | 'Acero'
    | 'Hada';

export type GameMode = 'normal' | 'nuzlocke' | 'battleTower';

export type GamePhase =
    | 'home'
    | 'starter'
    | 'map'
    | 'battle'
    | 'pokemonReward'
    | 'itemReward'
    | 'trade'
    | 'badge'
    | 'gameOver'
    | 'hallOfFame';

export type ActivePanel = 'none' | 'pokedex' | 'achievements' | 'hallOfFame' | 'settings';

export type PokemonRole = 'Atacante' | 'Defensor' | 'Soporte' | 'Velocidad' | 'Mixto';

export type PokemonStatus = 'OK' | 'Debilitado' | 'Veneno' | 'Quemado' | 'Parálisis';

export type Rarity = 'Común' | 'Raro' | 'Épico' | 'Legendario';

export type MapNodeType = 'battle' | 'wild' | 'item' | 'trade' | 'healing' | 'trainer' | 'boss' | 'mystery';

export interface PokelikePokemon {
    readonly id: number;
    readonly slug: string;
    readonly name: string;
    readonly imageUrl: string;
    readonly level: number;
    readonly types: readonly PokemonType[];
    readonly role: PokemonRole;
    readonly currentHp: number;
    readonly maxHp: number;
    readonly attack: number;
    readonly defense: number;
    readonly speed: number;
    readonly rarity: Rarity;
    readonly status: PokemonStatus;
    readonly experience: number;
    readonly heldItemName: string | null;
}

export interface PokelikeItem {
    readonly id: number;
    readonly slug: string;
    readonly name: string;
    readonly imageUrl: string;
    readonly description: string;
    readonly rarity: Rarity;
    readonly effectLabel: string;
}

export interface MapNode {
    readonly id: number;
    readonly type: MapNodeType;
    readonly name: string;
    readonly description: string;
    readonly reward: string;
    readonly dangerLevel: number;
    readonly positionX: number;
    readonly positionY: number;
    readonly iconUrl: string;
    readonly locked: boolean;
    readonly completed: boolean;
}

export interface Achievement {
    readonly id: number;
    readonly name: string;
    readonly description: string;
    readonly unlocked: boolean;
}

export interface HallOfFameEntry {
    readonly id: number;
    readonly mode: GameMode;
    readonly seed: number;
    readonly badges: number;
    readonly teamNames: readonly string[];
}

export interface BattleState {
    readonly enemy: PokelikePokemon | null;
    readonly playerPokemon: PokelikePokemon | null;
    readonly log: readonly string[];
    readonly resolved: boolean;
    readonly won: boolean;
}
