/**
 * StatCard
 *
 * Model of the stat cards displayed by the admin dashboard.
 *
 * @author @adrianpmdev
 */

export interface StatCard {
    label: string;
    value: number | string;
    icon: string;
    color: string;
    suffix?: string;
}
