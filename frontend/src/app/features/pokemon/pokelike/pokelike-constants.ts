/**
 * pokelike-constants
 *
 * Pokelike feature constants.
 * Contains initial game data and static run configuration.
 *
 * @author @adrianpmdev
 */

import { POKELIKE_ASSETS } from './pokelike-assets';
import {
    Achievement,
    GameMode,
    MapNode,
    MapNodeType,
    PokelikeItem,
    PokelikePokemon
} from './pokelike-models';


export const MAX_TEAM_SIZE = 6;
export const MAX_BADGES = 8;
export const DEFAULT_RUN_SEED = 2147;

export const GAME_MODE_LABELS: Record<GameMode, string> = {
    normal: 'Normal Mode',
    nuzlocke: 'Nuzlocke',
    battleTower: 'Battle Tower'
};

export const NODE_ICON_MAP: Record<MapNodeType, string> = {
    battle: POKELIKE_ASSETS.icons.battle,
    wild: POKELIKE_ASSETS.icons.wild,
    item: POKELIKE_ASSETS.icons.item,
    trade: POKELIKE_ASSETS.icons.trade,
    healing: POKELIKE_ASSETS.icons.healing,
    trainer: POKELIKE_ASSETS.icons.trainer,
    boss: POKELIKE_ASSETS.icons.boss,
    mystery: POKELIKE_ASSETS.icons.mystery
};

export const STARTER_OPTIONS: readonly PokelikePokemon[] = [
    {
        id: 1,
        slug: 'bulbasaur',
        name: 'Bulbasaur',
        imageUrl: POKELIKE_ASSETS.pokemon.bulbasaur,
        level: 5,
        types: ['Planta', 'Veneno'],
        role: 'Soporte',
        currentHp: 45,
        maxHp: 45,
        attack: 49,
        defense: 49,
        speed: 45,
        rarity: 'Común',
        status: 'OK',
        experience: 0,
        heldItemName: null
    },
    {
        id: 4,
        slug: 'charmander',
        name: 'Charmander',
        imageUrl: POKELIKE_ASSETS.pokemon.charmander,
        level: 5,
        types: ['Fuego'],
        role: 'Atacante',
        currentHp: 39,
        maxHp: 39,
        attack: 52,
        defense: 43,
        speed: 65,
        rarity: 'Común',
        status: 'OK',
        experience: 0,
        heldItemName: null
    },
    {
        id: 7,
        slug: 'squirtle',
        name: 'Squirtle',
        imageUrl: POKELIKE_ASSETS.pokemon.squirtle,
        level: 5,
        types: ['Agua'],
        role: 'Defensor',
        currentHp: 44,
        maxHp: 44,
        attack: 48,
        defense: 65,
        speed: 43,
        rarity: 'Común',
        status: 'OK',
        experience: 0,
        heldItemName: null
    }
];

export const WILD_POKEMON_POOL: readonly PokelikePokemon[] = [
    {
        id: 25,
        slug: 'pikachu',
        name: 'Pikachu',
        imageUrl: POKELIKE_ASSETS.pokemon.pikachu,
        level: 8,
        types: ['Eléctrico'],
        role: 'Velocidad',
        currentHp: 35,
        maxHp: 35,
        attack: 55,
        defense: 40,
        speed: 90,
        rarity: 'Raro',
        status: 'OK',
        experience: 0,
        heldItemName: null
    },
    {
        id: 92,
        slug: 'gastly',
        name: 'Gastly',
        imageUrl: POKELIKE_ASSETS.pokemon.gastly,
        level: 9,
        types: ['Fantasma', 'Veneno'],
        role: 'Atacante',
        currentHp: 30,
        maxHp: 30,
        attack: 35,
        defense: 30,
        speed: 80,
        rarity: 'Raro',
        status: 'OK',
        experience: 0,
        heldItemName: null
    },
    {
        id: 133,
        slug: 'eevee',
        name: 'Eevee',
        imageUrl: POKELIKE_ASSETS.pokemon.eevee,
        level: 10,
        types: ['Normal'],
        role: 'Mixto',
        currentHp: 55,
        maxHp: 55,
        attack: 55,
        defense: 50,
        speed: 55,
        rarity: 'Épico',
        status: 'OK',
        experience: 0,
        heldItemName: null
    },
    {
        id: 147,
        slug: 'dratini',
        name: 'Dratini',
        imageUrl: POKELIKE_ASSETS.pokemon.dratini,
        level: 12,
        types: ['Dragón'],
        role: 'Atacante',
        currentHp: 41,
        maxHp: 41,
        attack: 64,
        defense: 45,
        speed: 50,
        rarity: 'Épico',
        status: 'OK',
        experience: 0,
        heldItemName: null
    },
    {
        id: 143,
        slug: 'snorlax',
        name: 'Snorlax',
        imageUrl: POKELIKE_ASSETS.pokemon.snorlax,
        level: 13,
        types: ['Normal'],
        role: 'Defensor',
        currentHp: 160,
        maxHp: 160,
        attack: 110,
        defense: 65,
        speed: 30,
        rarity: 'Legendario',
        status: 'OK',
        experience: 0,
        heldItemName: null
    }
];

export const ITEM_POOL: readonly PokelikeItem[] = [
    {
        id: 1,
        slug: 'max-potion',
        name: 'Poción Máxima',
        imageUrl: POKELIKE_ASSETS.items.maxPotion,
        description: 'Cura al equipo tras cada combate importante.',
        rarity: 'Común',
        effectLabel: 'Curación'
    },
    {
        id: 2,
        slug: 'razor-claw',
        name: 'Garra Afilada',
        imageUrl: POKELIKE_ASSETS.items.razorClaw,
        description: 'Aumenta el daño ofensivo total de la run.',
        rarity: 'Raro',
        effectLabel: '+Ataque'
    },
    {
        id: 3,
        slug: 'shiny-charm',
        name: 'Amuleto Brillante',
        imageUrl: POKELIKE_ASSETS.items.shinyCharm,
        description: 'Incrementa la probabilidad de encuentros raros.',
        rarity: 'Épico',
        effectLabel: '+Rareza'
    },
    {
        id: 4,
        slug: 'leftovers',
        name: 'Restos',
        imageUrl: POKELIKE_ASSETS.items.leftovers,
        description: 'Recupera PS al avanzar de mapa.',
        rarity: 'Raro',
        effectLabel: 'Regeneración'
    },
    {
        id: 5,
        slug: 'focus-sash',
        name: 'Cinta Focus',
        imageUrl: POKELIKE_ASSETS.items.focusSash,
        description: 'Protege al líder del equipo en combates críticos.',
        rarity: 'Épico',
        effectLabel: 'Supervivencia'
    }
];

export const INITIAL_MAP_NODES: readonly MapNode[] = [
    {
        id: 1,
        type: 'wild',
        name: 'Wild Encounter',
        description: 'Encuentro salvaje con opción de captura.',
        reward: 'Pokémon',
        dangerLevel: 2,
        positionX: 18,
        positionY: 62,
        iconUrl: NODE_ICON_MAP.wild,
        locked: false,
        completed: false
    },
    {
        id: 2,
        type: 'item',
        name: 'Item Cache',
        description: 'Obtén un objeto para mejorar la run.',
        reward: 'Objeto',
        dangerLevel: 1,
        positionX: 38,
        positionY: 38,
        iconUrl: NODE_ICON_MAP.item,
        locked: false,
        completed: false
    },
    {
        id: 3,
        type: 'trainer',
        name: 'Trainer Battle',
        description: 'Combate de entrenador con más monedas.',
        reward: 'Monedas',
        dangerLevel: 3,
        positionX: 58,
        positionY: 54,
        iconUrl: NODE_ICON_MAP.trainer,
        locked: false,
        completed: false
    },
    {
        id: 4,
        type: 'boss',
        name: 'Gym Leader',
        description: 'Derrota al líder para conseguir una medalla.',
        reward: 'Medalla',
        dangerLevel: 5,
        positionX: 82,
        positionY: 30,
        iconUrl: NODE_ICON_MAP.boss,
        locked: false,
        completed: false
    }
];

export const INITIAL_ACHIEVEMENTS: readonly Achievement[] = [
    {
        id: 1,
        name: 'Primera Elección',
        description: 'Selecciona tu primer Pokémon inicial.',
        unlocked: false
    },
    {
        id: 2,
        name: 'Equipo Completo',
        description: 'Llena los 6 huecos del equipo.',
        unlocked: false
    },
    {
        id: 3,
        name: 'Camino a la Liga',
        description: 'Consigue tu primera medalla.',
        unlocked: false
    },
    {
        id: 4,
        name: 'Hall of Fame',
        description: 'Completa una run con 8 medallas.',
        unlocked: false
    }
];
