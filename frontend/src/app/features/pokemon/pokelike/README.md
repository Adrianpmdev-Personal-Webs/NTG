# Pokelike Angular Feature

Feature Angular 21 inspirada en Pokelike, preparada para usar imágenes desde la carpeta `public` raíz de un proyecto Angular normal.

## Dónde colocar los assets

En Angular, todo lo que metas en `public/` se sirve desde la raíz del sitio. Esta feature referencia los assets con rutas como:

```txt
/pokelike/backgrounds/home-bg.png
/pokelike/pokemon/bulbasaur.png
/pokelike/icons/battle.png
```

Por tanto, en tu proyecto deberías crear esta estructura:

```txt
public/
└── pokelike/
    ├── backgrounds/
    │   ├── home-bg.png
    │   ├── starter-bg.png
    │   ├── map-bg.png
    │   ├── battle-bg.png
    │   ├── reward-bg.png
    │   ├── trade-bg.png
    │   ├── hall-bg.png
    │   └── game-over-bg.png
    ├── maps/
    │   ├── forest-map.png
    │   ├── cave-map.png
    │   ├── volcano-map.png
    │   └── league-map.png
    ├── pokemon/
    │   ├── bulbasaur.png
    │   ├── charmander.png
    │   ├── squirtle.png
    │   ├── pikachu.png
    │   ├── gastly.png
    │   ├── eevee.png
    │   ├── dratini.png
    │   └── snorlax.png
    ├── items/
    │   ├── max-potion.png
    │   ├── razor-claw.png
    │   ├── shiny-charm.png
    │   ├── leftovers.png
    │   └── focus-sash.png
    ├── icons/
    │   ├── logo.png
    │   ├── battle.png
    │   ├── wild.png
    │   ├── item.png
    │   ├── trade.png
    │   ├── healing.png
    │   ├── trainer.png
    │   ├── boss.png
    │   ├── mystery.png
    │   ├── badge.png
    │   ├── coin.png
    │   ├── pokedex.png
    │   ├── achievements.png
    │   ├── hall-of-fame.png
    │   └── settings.png
    └── ui/
        ├── panel.png
        ├── button-primary.png
        ├── button-secondary.png
        └── frame.png
```

Si todavía no tienes alguna imagen, la UI no se rompe: los fondos tienen degradados de fallback y las imágenes usan `alt` normal.

## Ruta lazy loaded

```ts
{
    path: 'pokelike',
    loadChildren: () => import('./features/pokelike/pokelike.routes').then(routes => routes.POKELIKE_ROUTES)
}
```

## Nota

Esta implementación está preparada para que sustituyas los assets por los tuyos. No incluye imágenes de terceros.
