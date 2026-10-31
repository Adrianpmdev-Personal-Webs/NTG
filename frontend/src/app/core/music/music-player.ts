/**
 * MusicPlayer
 *
 * The one background music player of the web. It only works on computers (main input is a mouse or a trackpad): on
 * phones and tablets it never asks for the playlists nor downloads a song.
 * The area comes from the first segment of the URL: a game id plays that game's playlist and anything else (welcome,
 * admin screens, including the management of a game) plays the general one. An area without songs plays nothing.
 * A single song repeats; several play in order and start again after the last one. Moving between screens of the same
 * area never restarts the music; changing area fades it out and starts the new playlist.
 * Muting pauses the song. Mute and volume are remembered by this browser. Browsers refuse to play sound before the
 * user interacts with the page, so after a reload the music starts on the first click or key.
 *
 * @author @adrianpmdev
 */

import { Injectable, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { catchError, distinctUntilChanged, filter, map, of } from 'rxjs';
import { AuthSession } from '@core/auth/auth-session';
import { MusicArea, MusicTrack } from '@shared/models/music';
import { MusicApi } from './music-api';

const PLATFORM_AREA = 'platform';
const DESKTOP_QUERY = '(hover: hover) and (pointer: fine)';
const MUTED_KEY = 'ntg.music.muted';
const VOLUME_KEY = 'ntg.music.volume';
const DEFAULT_VOLUME = 0.4;
const FADE_MS = 600;
const FADE_STEPS = 12;

@Injectable({ providedIn: 'root' })
export class MusicPlayer {
    private readonly api = inject(MusicApi);
    private readonly router = inject(Router);

    private readonly desktop = window.matchMedia(DESKTOP_QUERY);

    readonly available = signal(this.desktop.matches);
    readonly signedIn = signal(false);
    readonly muted = signal(readStored(MUTED_KEY) === 'true');
    readonly volume = signal(readVolume());
    readonly visible = computed(() => this.available() && this.signedIn());

    private readonly areas = signal<MusicArea[]>([]);
    private audio: HTMLAudioElement | null = null;
    private playlist: MusicTrack[] = [];
    private playlistArea: string | null = null;
    private index = 0;
    private failures = 0;
    private fadeTimer: ReturnType<typeof setInterval> | null = null;
    private waitingForGesture = false;

    constructor() {
        this.desktop.addEventListener('change', (event) => this.onDeviceChange(event.matches));

        inject(AuthSession)
            .currentUser$.pipe(
                map((user) => user?.id ?? null),
                distinctUntilChanged(),
            )
            .subscribe((userId) => this.onUserChange(userId));

        this.router.events
            .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
            .subscribe(() => this.syncArea());
    }

    toggleMute(): void {
        this.muted.update((muted) => !muted);
        writeStored(MUTED_KEY, String(this.muted()));

        if (this.muted()) {
            this.audio?.pause();
        } else {
            this.play();
        }
    }

    setVolume(value: number): void {
        const volume = Math.min(1, Math.max(0, value));

        this.volume.set(volume);
        writeStored(VOLUME_KEY, String(volume));

        if (this.audio && !this.fadeTimer) {
            this.audio.volume = loudness(volume);
        }
    }

    reload(): void {
        if (!this.visible()) {
            return;
        }

        this.api
            .getAreas()
            .pipe(catchError(() => of<MusicArea[]>([])))
            .subscribe((areas) => {
                this.areas.set(areas);
                this.playlistArea = null;
                this.syncArea();
            });
    }

    private onUserChange(userId: string | null): void {
        this.signedIn.set(userId !== null);
        this.stop();
        this.areas.set([]);

        if (userId) {
            this.reload();
        }
    }

    private onDeviceChange(desktop: boolean): void {
        this.available.set(desktop);

        if (desktop) {
            this.reload();
        } else {
            this.stop();
            this.areas.set([]);
        }
    }

    private syncArea(): void {
        if (!this.visible()) {
            return;
        }

        const segment = this.router.url.split(/[/?#]/).filter(Boolean)[0] ?? '';
        const areaId = this.areas().some((area) => area.area === segment && segment !== PLATFORM_AREA)
            ? segment
            : PLATFORM_AREA;

        if (areaId === this.playlistArea) {
            return;
        }

        const tracks = this.areas().find((area) => area.area === areaId)?.tracks ?? [];

        this.playlistArea = areaId;
        this.fadeOut(() => this.start(tracks));
    }

    private start(tracks: MusicTrack[]): void {
        this.playlist = tracks;
        this.index = 0;
        this.failures = 0;

        if (tracks.length === 0) {
            this.audio?.removeAttribute('src');
            this.audio?.load();
            return;
        }

        this.load(tracks[0]);
        this.play();
    }

    private load(track: MusicTrack): void {
        const audio = this.element();

        audio.loop = this.playlist.length === 1;
        audio.src = track.url;
        audio.volume = loudness(this.volume());
    }

    private play(): void {
        if (!this.audio?.src || this.muted() || this.playlist.length === 0) {
            return;
        }

        this.audio.play().catch((error: unknown) => {
            if (error instanceof DOMException && error.name === 'NotAllowedError') {
                this.waitForGesture();
            }
        });
    }

    private next(): void {
        if (this.playlist.length === 0) {
            return;
        }

        this.index = (this.index + 1) % this.playlist.length;
        this.load(this.playlist[this.index]);
        this.play();
    }

    private onError(): void {
        this.failures++;

        if (this.failures < this.playlist.length) {
            this.next();
        }
    }

    private stop(): void {
        this.clearFade();
        this.playlist = [];
        this.playlistArea = null;

        if (this.audio) {
            this.audio.pause();
            this.audio.removeAttribute('src');
            this.audio.load();
        }
    }

    private fadeOut(done: () => void): void {
        const audio = this.audio;

        this.clearFade();

        if (!audio || audio.paused) {
            done();
            return;
        }

        const step = audio.volume / FADE_STEPS;

        this.fadeTimer = setInterval(() => {
            audio.volume = Math.max(0, audio.volume - step);

            if (audio.volume <= 0) {
                this.clearFade();
                audio.pause();
                done();
            }
        }, FADE_MS / FADE_STEPS);
    }

    private clearFade(): void {
        if (this.fadeTimer) {
            clearInterval(this.fadeTimer);
            this.fadeTimer = null;
        }
    }

    private waitForGesture(): void {
        if (this.waitingForGesture) {
            return;
        }

        this.waitingForGesture = true;

        const resume = (): void => {
            this.waitingForGesture = false;
            document.removeEventListener('pointerdown', resume);
            document.removeEventListener('keydown', resume);
            this.play();
        };

        document.addEventListener('pointerdown', resume);
        document.addEventListener('keydown', resume);
    }

    private element(): HTMLAudioElement {
        if (!this.audio) {
            this.audio = new Audio();
            this.audio.preload = 'auto';
            this.audio.addEventListener('ended', () => this.next());
            this.audio.addEventListener('playing', () => (this.failures = 0));
            this.audio.addEventListener('error', () => this.onError());
        }

        return this.audio;
    }
}

function loudness(volume: number): number {
    return volume * volume;
}

function readVolume(): number {
    const stored = Number(readStored(VOLUME_KEY));

    return readStored(VOLUME_KEY) !== null && Number.isFinite(stored)
        ? Math.min(1, Math.max(0, stored))
        : DEFAULT_VOLUME;
}

function readStored(key: string): string | null {
    try {
        return localStorage.getItem(key);
    } catch {
        return null;
    }
}

function writeStored(key: string, value: string): void {
    try {
        localStorage.setItem(key, value);
    } catch {
        return;
    }
}
