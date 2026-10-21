/**
 * PokelikeRunStore
 *
 * Pokelike run store.
 * Centralizes roguelike state, asset selectors and game actions.
 *
 * @author @adrianpmdev
 */

import { computed, Injectable, signal } from '@angular/core';
import { POKELIKE_ASSETS } from './pokelike-assets';
import {
    DEFAULT_RUN_SEED,
    GAME_MODE_LABELS,
    INITIAL_ACHIEVEMENTS,
    INITIAL_MAP_NODES,
    ITEM_POOL,
    MAX_BADGES,
    MAX_TEAM_SIZE,
    NODE_ICON_MAP,
    STARTER_OPTIONS,
    WILD_POKEMON_POOL
} from './pokelike-constants';
import {
    Achievement,
    ActivePanel,
    BattleState,
    GameMode,
    GamePhase,
    HallOfFameEntry,
    MapNode,
    MapNodeType,
    PokelikeItem,
    PokelikePokemon,
    PokemonType,
    Rarity
} from './pokelike-models';

@Injectable({
    providedIn: 'root'
})
export class PokelikeRunStore {

    readonly maxTeamSize = MAX_TEAM_SIZE;
    readonly maxBadges = MAX_BADGES;
    readonly modeLabels = GAME_MODE_LABELS;
    readonly starterOptions = STARTER_OPTIONS;
    readonly assets = POKELIKE_ASSETS;

    readonly currentPhase = signal<GamePhase>('home');
    readonly activePanel = signal<ActivePanel>('none');
    readonly selectedMode = signal<GameMode>('normal');
    readonly selectedGeneration = signal(1);
    readonly currentMap = signal(1);
    readonly currentBadges = signal(0);
    readonly coins = signal(0);
    readonly runSeed = signal(DEFAULT_RUN_SEED);

    readonly team = signal<PokelikePokemon[]>([]);
    readonly inventory = signal<PokelikeItem[]>([]);
    readonly mapNodes = signal<MapNode[]>([...INITIAL_MAP_NODES]);
    readonly wildPokemonOptions = signal<PokelikePokemon[]>(this.createPokemonRewards(0));
    readonly itemOptions = signal<PokelikeItem[]>(this.createItemRewards(0));
    readonly achievements = signal<Achievement[]>([...INITIAL_ACHIEVEMENTS]);
    readonly hallOfFameEntries = signal<HallOfFameEntry[]>([]);

    readonly selectedPokemonId = signal<number | null>(null);
    readonly selectedItemId = signal<number | null>(null);

    readonly battleState = signal<BattleState>({
        enemy: null,
        playerPokemon: null,
        log: [],
        resolved: false,
        won: false
    });

    readonly modeLabel = computed(() => {
        return this.modeLabels[this.selectedMode()];
    });

    readonly teamSlotsLabel = computed(() => {
        return `${this.team().length}/${this.maxTeamSize}`;
    });

    readonly badgesLabel = computed(() => {
        return `${this.currentBadges()}/${this.maxBadges}`;
    });

    readonly teamPower = computed(() => {
        return this.team().reduce((totalPower, pokemon) => {
            return totalPower + pokemon.currentHp + pokemon.attack + pokemon.defense + pokemon.speed;
        }, 0);
    });

    readonly unlockedAchievements = computed(() => {
        return this.achievements().filter(achievement => achievement.unlocked);
    });

    readonly isTeamFull = computed(() => {
        return this.team().length >= this.maxTeamSize;
    });

    readonly selectedPokemon = computed(() => {
        const selectedId = this.selectedPokemonId();

        if (!selectedId) {
            return null;
        }

        return this.wildPokemonOptions().find(pokemon => pokemon.id === selectedId) ?? null;
    });

    readonly selectedItem = computed(() => {
        const selectedId = this.selectedItemId();

        if (!selectedId) {
            return null;
        }

        return this.itemOptions().find(item => item.id === selectedId) ?? null;
    });

    readonly currentBackgroundImage = computed(() => {
        const phase = this.currentPhase();

        if (phase === 'home') {
            return this.assets.backgrounds.home;
        }

        if (phase === 'starter') {
            return this.assets.backgrounds.starter;
        }

        if (phase === 'map') {
            return this.assets.backgrounds.map;
        }

        if (phase === 'battle') {
            return this.assets.backgrounds.battle;
        }

        if (phase === 'trade') {
            return this.assets.backgrounds.trade;
        }

        if (phase === 'hallOfFame' || phase === 'badge') {
            return this.assets.backgrounds.hall;
        }

        if (phase === 'gameOver') {
            return this.assets.backgrounds.gameOver;
        }

        return this.assets.backgrounds.reward;
    });

    readonly currentMapImage = computed(() => {
        const mapIndex = this.currentMap() % 4;

        if (mapIndex === 0) {
            return this.assets.maps.league;
        }

        if (mapIndex === 1) {
            return this.assets.maps.forest;
        }

        if (mapIndex === 2) {
            return this.assets.maps.cave;
        }

        return this.assets.maps.volcano;
    });

    selectMode(mode: GameMode): void {
        this.selectedMode.set(mode);
    }

    selectGeneration(generation: number): void {
        this.selectedGeneration.set(generation);
    }

    openPanel(panel: ActivePanel): void {
        this.activePanel.set(panel);
    }

    closePanel(): void {
        this.activePanel.set('none');
    }

    startRun(): void {
        this.resetRunState();
        this.currentPhase.set('starter');
    }

    selectStarter(pokemon: PokelikePokemon): void {
        this.team.set([this.preparePokemonForRun(pokemon, 0)]);
        this.unlockAchievement(1);
        this.currentPhase.set('map');
    }

    selectPokemon(pokemon: PokelikePokemon): void {
        this.selectedPokemonId.set(pokemon.id);
    }

    selectItem(item: PokelikeItem): void {
        this.selectedItemId.set(item.id);
    }

    keepSelectedPokemon(): void {
        const pokemon = this.selectedPokemon();

        if (!pokemon) {
            return;
        }

        if (this.isTeamFull()) {
            this.team.set([...this.team().slice(1), pokemon]);
        } else {
            this.team.set([...this.team(), pokemon]);
        }

        if (this.team().length >= this.maxTeamSize) {
            this.unlockAchievement(2);
        }

        this.selectedPokemonId.set(null);
        this.currentPhase.set('itemReward');
    }

    skipPokemonReward(): void {
        this.selectedPokemonId.set(null);
        this.currentPhase.set('itemReward');
    }

    keepSelectedItem(): void {
        const item = this.selectedItem();

        if (!item) {
            return;
        }

        this.inventory.set([...this.inventory(), item]);
        this.selectedItemId.set(null);
        this.currentPhase.set('trade');
    }

    skipItemReward(): void {
        this.selectedItemId.set(null);
        this.currentPhase.set('trade');
    }

    acceptTrade(): void {
        const currentTeam = this.team();

        if (currentTeam.length < 2) {
            this.currentPhase.set('map');
            return;
        }

        const upgradedPokemon: PokelikePokemon = {
            ...currentTeam[0],
            level: currentTeam[0].level + 2,
            rarity: this.getUpgradedRarity(currentTeam[0].rarity),
            experience: currentTeam[0].experience + 20
        };

        this.team.set([upgradedPokemon, ...currentTeam.slice(2)]);
        this.currentPhase.set('map');
    }

    declineTrade(): void {
        this.currentPhase.set('map');
    }

    chooseMapNode(node: MapNode): void {
        if (node.locked) {
            return;
        }

        this.mapNodes.set(this.mapNodes().map(mapNode => {
            if (mapNode.id !== node.id) {
                return mapNode;
            }

            return {
                ...mapNode,
                completed: true
            };
        }));

        this.currentMap.update(currentMap => currentMap + 1);
        this.runSeed.update(seed => seed + node.id + this.currentMap());

        if (node.type === 'item') {
            this.refreshRewards(node.id);
            this.currentPhase.set('itemReward');
            return;
        }

        if (node.type === 'trade') {
            this.currentPhase.set('trade');
            return;
        }

        this.startBattle(node);
    }

    resolveBattle(): void {
        const enemy = this.battleState().enemy;

        if (!enemy) {
            return;
        }

        const won = this.teamPower() >= this.getEnemyPower(enemy);
        const updatedLog = [
            ...this.battleState().log,
            won ? 'Victoria conseguida. Recompensas desbloqueadas.' : 'El equipo ha sido derrotado.'
        ];

        this.battleState.set({
            enemy,
            playerPokemon: this.battleState().playerPokemon,
            log: updatedLog,
            resolved: true,
            won
        });

        if (!won) {
            this.currentPhase.set('gameOver');
            return;
        }

        this.coins.update(coins => coins + 25);

        if (this.currentMap() % 4 === 0) {
            this.currentPhase.set('badge');
            return;
        }

        this.refreshRewards(enemy.id);
        this.currentPhase.set('pokemonReward');
    }

    claimBadge(): void {
        this.currentBadges.update(badges => Math.min(badges + 1, this.maxBadges));
        this.unlockAchievement(3);

        if (this.currentBadges() >= this.maxBadges) {
            this.finishRun();
            return;
        }

        this.refreshMapNodes();
        this.currentPhase.set('map');
    }

    continueToMap(): void {
        this.refreshMapNodes();
        this.currentPhase.set('map');
    }

    restartRun(): void {
        this.resetRunState();
        this.currentPhase.set('home');
    }

    isPokemonSelected(pokemon: PokelikePokemon): boolean {
        return this.selectedPokemonId() === pokemon.id;
    }

    isItemSelected(item: PokelikeItem): boolean {
        return this.selectedItemId() === item.id;
    }

    getTypeClass(type: PokemonType): string {
        return `type-${type.toLowerCase()}`;
    }

    getRarityClass(rarity: Rarity): string {
        return `rarity-${rarity.toLowerCase()}`;
    }

    getHealthPercent(pokemon: PokelikePokemon): number {
        return Math.round((pokemon.currentHp / pokemon.maxHp) * 100);
    }

    getDangerBlocks(dangerLevel: number): readonly number[] {
        return Array.from({ length: dangerLevel }, (_, index) => index + 1);
    }

    getNodeTypeLabel(type: MapNodeType): string {
        const labelMap: Record<MapNodeType, string> = {
            battle: 'Battle',
            wild: 'Wild',
            item: 'Item',
            trade: 'Trade',
            healing: 'Heal',
            trainer: 'Trainer',
            boss: 'Boss',
            mystery: 'Mystery'
        };

        return labelMap[type];
    }

    private startBattle(node: MapNode): void {
        const enemy = this.createEnemyPokemon(node);
        const playerPokemon = this.team()[0] ?? null;
        const enemyPower = this.getEnemyPower(enemy);

        this.battleState.set({
            enemy,
            playerPokemon,
            log: [
                `${node.name} iniciado.`,
                `${enemy.name} apareció.`,
                `Poder enemigo detectado: ${enemyPower}.`
            ],
            resolved: false,
            won: false
        });

        this.currentPhase.set('battle');
    }

    private refreshRewards(seedOffset: number): void {
        this.wildPokemonOptions.set(this.createPokemonRewards(seedOffset));
        this.itemOptions.set(this.createItemRewards(seedOffset));
    }

    private refreshMapNodes(): void {
        const map = this.currentMap();
        const baseId = map * 10;

        this.mapNodes.set([
            this.createMapNode(baseId + 1, 'wild', 'Wild Encounter', 'Elige esta ruta para conseguir un nuevo Pokémon.', 'Pokémon', 2, 18, 62),
            this.createMapNode(baseId + 2, 'item', 'Item Cache', 'Ruta segura centrada en obtener recursos.', 'Objeto', 1, 38, 35),
            this.createMapNode(baseId + 3, map % 3 === 0 ? 'trade' : 'trainer', map % 3 === 0 ? 'Trade Offer' : 'Trainer Battle', map % 3 === 0 ? 'Intercambia riesgo por una mejora fuerte.' : 'Combate más duro con mayor recompensa.', map % 3 === 0 ? 'Trade' : 'Monedas', 3, 58, 55),
            this.createMapNode(baseId + 4, 'boss', map % 4 === 0 ? 'Gym Leader' : 'Route Boss', 'Nodo principal de progreso de la run.', map % 4 === 0 ? 'Medalla' : 'Recompensa', 5, 82, 30)
        ]);
    }

    private createMapNode(id: number, type: MapNodeType, name: string, description: string, reward: string, dangerLevel: number, positionX: number, positionY: number): MapNode {
        return {
            id,
            type,
            name,
            description,
            reward,
            dangerLevel,
            positionX,
            positionY,
            iconUrl: NODE_ICON_MAP[type],
            locked: false,
            completed: false
        };
    }

    private createPokemonRewards(seedOffset: number): PokelikePokemon[] {
        return WILD_POKEMON_POOL.slice(0, 3).map((pokemon, index) => {
            return this.preparePokemonForRun(pokemon, seedOffset + index);
        });
    }

    private createItemRewards(seedOffset: number): PokelikeItem[] {
        const firstIndex = seedOffset % ITEM_POOL.length;

        return [
            ITEM_POOL[firstIndex],
            ITEM_POOL[(firstIndex + 1) % ITEM_POOL.length],
            ITEM_POOL[(firstIndex + 2) % ITEM_POOL.length]
        ];
    }

    private createEnemyPokemon(node: MapNode): PokelikePokemon {
        const basePokemon = WILD_POKEMON_POOL[node.id % WILD_POKEMON_POOL.length];

        return {
            ...basePokemon,
            level: basePokemon.level + node.dangerLevel + this.currentBadges(),
            currentHp: basePokemon.maxHp + node.dangerLevel * 5,
            maxHp: basePokemon.maxHp + node.dangerLevel * 5,
            attack: basePokemon.attack + node.dangerLevel * 3,
            defense: basePokemon.defense + node.dangerLevel * 2,
            speed: basePokemon.speed + node.dangerLevel
        };
    }

    private preparePokemonForRun(pokemon: PokelikePokemon, seedOffset: number): PokelikePokemon {
        return {
            ...pokemon,
            level: pokemon.level + this.currentBadges() + seedOffset,
            currentHp: pokemon.maxHp + seedOffset * 2,
            maxHp: pokemon.maxHp + seedOffset * 2,
            experience: pokemon.experience + seedOffset * 5
        };
    }

    private finishRun(): void {
        this.unlockAchievement(4);

        this.hallOfFameEntries.set([
            ...this.hallOfFameEntries(),
            {
                id: Date.now(),
                mode: this.selectedMode(),
                seed: this.runSeed(),
                badges: this.currentBadges(),
                teamNames: this.team().map(pokemon => pokemon.name)
            }
        ]);

        this.currentPhase.set('hallOfFame');
    }

    private resetRunState(): void {
        this.currentMap.set(1);
        this.currentBadges.set(0);
        this.coins.set(0);
        this.runSeed.set(DEFAULT_RUN_SEED);
        this.team.set([]);
        this.inventory.set([]);
        this.selectedPokemonId.set(null);
        this.selectedItemId.set(null);
        this.mapNodes.set([...INITIAL_MAP_NODES]);
        this.wildPokemonOptions.set(this.createPokemonRewards(0));
        this.itemOptions.set(this.createItemRewards(0));
        this.battleState.set({
            enemy: null,
            playerPokemon: null,
            log: [],
            resolved: false,
            won: false
        });
    }

    private unlockAchievement(achievementId: number): void {
        this.achievements.set(this.achievements().map(achievement => {
            if (achievement.id !== achievementId || achievement.unlocked) {
                return achievement;
            }

            return {
                ...achievement,
                unlocked: true
            };
        }));
    }

    private getEnemyPower(enemy: PokelikePokemon): number {
        return enemy.currentHp + enemy.attack + enemy.defense + enemy.speed;
    }

    private getUpgradedRarity(rarity: Rarity): Rarity {
        const rarityMap: Record<Rarity, Rarity> = {
            Común: 'Raro',
            Raro: 'Épico',
            Épico: 'Legendario',
            Legendario: 'Legendario'
        };

        return rarityMap[rarity];
    }
}
