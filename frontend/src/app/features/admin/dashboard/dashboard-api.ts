/**
 * DashboardApi
 *
 * HTTP service and models of the admin dashboard. Everything comes in one call, made when the screen opens and when
 * the admin refreshes it. A value the backend could not read comes as null.
 *
 * @author @adrianpmdev
 */

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@core/api/api';
import { NavIconName } from '@shared/components/nav-icon/nav-icon';

export interface DashboardServer {
    version: string | null;
    uptimeSeconds: number;
    cpuPercent: number | null;
    processBytes: number | null;
    heapUsedBytes: number;
    heapMaxBytes: number;
    memoryLimitBytes: number;
}

export interface DashboardBucket {
    name: string;
    objects: number;
    bytes: number;
}

export interface DashboardStorage {
    databaseBytes: number | null;
    databaseLimitBytes: number;
    storageBytes: number | null;
    storageLimitBytes: number;
    buckets: DashboardBucket[];
}

export interface DashboardModuleLine {
    label: string;
    value: string;
    level: 'OK' | 'WARNING' | 'ERROR';
    icon: NavIconName;
}

export interface DashboardModule {
    title: string;
    lines: DashboardModuleLine[];
}

export interface DashboardGameUsers {
    name: string;
    iconUrl: string | null;
    users: number;
}

export interface DashboardAccount {
    username: string;
    email: string;
    role: string;
    createdAt: string | null;
    lastActiveAt: string | null;
}

export interface DashboardUsers {
    total: number;
    admins: number;
    players: number;
    signInsKnown: boolean;
    neverSignedIn: number | null;
    perGame: DashboardGameUsers[];
    accounts: DashboardAccount[];
}

export interface DashboardTraffic {
    counters: {
        requests: number;
        clientErrors: number;
        serverErrors: number;
        averageMillis: number;
        failedLogins: number;
        rateLimited: number;
    };
    connected: number;
}

export interface DashboardLog {
    at: string;
    level: string;
    source: string;
    message: string;
    requestId: string | null;
}

export interface DashboardData {
    server: DashboardServer;
    storage: DashboardStorage;
    modules: DashboardModule[];
    users: DashboardUsers;
    traffic: DashboardTraffic;
    logs: DashboardLog[];
}

@Injectable({ providedIn: 'root' })
export class DashboardApi {
    private readonly http = inject(HttpClient);

    get(): Observable<DashboardData> {
        return this.http.get<DashboardData>(`${API_BASE_URL}/dashboard`);
    }
}
