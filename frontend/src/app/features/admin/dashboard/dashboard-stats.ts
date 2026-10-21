/**
 * DashboardStats
 *
 * Models of the numbers and cards displayed by the admin dashboard.
 *
 * @author @adrianpmdev
 */

export interface DashboardStats {
    totalUsers: number;
    activeUsers: number;
    admins: number;
    registersToday: number;
}

export interface StatCard {
    label: string;
    value: number | string;
    icon: string;
    color: string;
    suffix?: string;
}
