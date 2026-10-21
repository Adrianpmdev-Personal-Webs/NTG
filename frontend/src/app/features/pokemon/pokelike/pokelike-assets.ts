/**
 * pokelike-assets
 *
 * Pokelike asset paths.
 * Centralizes image references served from the Angular public root folder.
 *
 * @author @adrianpmdev
 */

export const POKELIKE_ASSETS = {
    backgrounds: {
        home: '/pokelike/backgrounds/home-bg.png',
        starter: '/pokelike/backgrounds/starter-bg.png',
        map: '/pokelike/backgrounds/map-bg.png',
        battle: '/pokelike/backgrounds/battle-bg.png',
        reward: '/pokelike/backgrounds/reward-bg.png',
        trade: '/pokelike/backgrounds/trade-bg.png',
        hall: '/pokelike/backgrounds/hall-bg.png',
        gameOver: '/pokelike/backgrounds/game-over-bg.png'
    },
    maps: {
        forest: '/pokelike/maps/forest-map.png',
        cave: '/pokelike/maps/cave-map.png',
        volcano: '/pokelike/maps/volcano-map.png',
        league: '/pokelike/maps/league-map.png'
    },
    pokemon: {
        bulbasaur: '/pokelike/pokemon/bulbasaur.png',
        charmander: '/pokelike/pokemon/charmander.png',
        squirtle: '/pokelike/pokemon/squirtle.png',
        pikachu: '/pokelike/pokemon/pikachu.png',
        gastly: '/pokelike/pokemon/gastly.png',
        eevee: '/pokelike/pokemon/eevee.png',
        dratini: '/pokelike/pokemon/dratini.png',
        snorlax: '/pokelike/pokemon/snorlax.png'
    },
    items: {
        maxPotion: '/pokelike/items/max-potion.png',
        razorClaw: '/pokelike/items/razor-claw.png',
        shinyCharm: '/pokelike/items/shiny-charm.png',
        leftovers: '/pokelike/items/leftovers.png',
        focusSash: '/pokelike/items/focus-sash.png'
    },
    icons: {
        logo: '/pokelike/icons/logo.png',
        battle: '/pokelike/icons/battle.png',
        wild: '/pokelike/icons/wild.png',
        item: '/pokelike/icons/item.png',
        trade: '/pokelike/icons/trade.png',
        healing: '/pokelike/icons/healing.png',
        trainer: '/pokelike/icons/trainer.png',
        boss: '/pokelike/icons/boss.png',
        mystery: '/pokelike/icons/mystery.png',
        badge: '/pokelike/icons/badge.png',
        coin: '/pokelike/icons/coin.png',
        pokedex: '/pokelike/icons/pokedex.png',
        achievements: '/pokelike/icons/achievements.png',
        hallOfFame: '/pokelike/icons/hall-of-fame.png',
        settings: '/pokelike/icons/settings.png'
    },
    ui: {
        panel: '/pokelike/ui/panel.png',
        primaryButton: '/pokelike/ui/button-primary.png',
        secondaryButton: '/pokelike/ui/button-secondary.png',
        frame: '/pokelike/ui/frame.png'
    }
} as const;
