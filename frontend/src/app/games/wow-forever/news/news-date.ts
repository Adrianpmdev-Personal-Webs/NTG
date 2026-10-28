/**
 * NewsDate
 *
 * The two dates of a news item in Spanish: how long ago it was published ("hace 5 minutos", "ayer") and the day and
 * time in Spain ("28 oct 2026, 23:17").
 *
 * @author @adrianpmdev
 */

const RELATIVE = new Intl.RelativeTimeFormat('es', { numeric: 'auto' });

const ABSOLUTE = new Intl.DateTimeFormat('es-ES', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'Europe/Madrid',
});

const STEPS: readonly [Intl.RelativeTimeFormatUnit, number][] = [
    ['minute', 60],
    ['hour', 60],
    ['day', 24],
    ['week', 7],
    ['month', 4.345],
    ['year', 12],
];

export const relativeDate = (publishedAt: string, now: number): string => {
    let amount = (now - Date.parse(publishedAt)) / 60_000;

    if (amount < 1) {
        return 'ahora mismo';
    }

    for (let step = 0; step < STEPS.length; step++) {
        const [unit] = STEPS[step];
        const next = STEPS[step + 1];

        if (!next || amount < next[1]) {
            return RELATIVE.format(-Math.floor(amount), unit);
        }

        amount /= next[1];
    }

    return '';
};

export const absoluteDate = (publishedAt: string): string => ABSOLUTE.format(new Date(publishedAt));
