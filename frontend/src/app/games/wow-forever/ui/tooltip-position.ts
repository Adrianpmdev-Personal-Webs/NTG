/**
 * tooltipPosition
 *
 * Where a tooltip that hangs from the pointer goes: beside the pointer, on the side with more room, above it in the
 * lower half of the screen, and centred on screen when neither side leaves room for it (phones). Shared by the talent
 * and the ability tooltips. The pointer comes in real pixels and the tooltip is placed in those of the page, which are
 * bigger on large screens (--screen-zoom).
 *
 * @author @adrianpmdev
 */

const OFFSET = 16;

const MAX_WIDTH = 320;

const EDGE = 16;

export const tooltipPosition = (pointerX: number, pointerY: number): Record<string, string> => {
    const zoom = Number(getComputedStyle(document.documentElement).zoom) || 1;
    const x = pointerX / zoom;
    const y = pointerY / zoom;
    const screenWidth = window.innerWidth / zoom;
    const screenHeight = window.innerHeight / zoom;
    const width = Math.min(MAX_WIDTH, screenWidth - 2 * EDGE);
    const onLeft = x > screenWidth / 2;
    const room = (onLeft ? x : screenWidth - x) - OFFSET - EDGE;

    return {
        ...(room < width
            ? { left: `${(screenWidth - width) / 2}px` }
            : onLeft
              ? { right: `${screenWidth - x + OFFSET}px` }
              : { left: `${x + OFFSET}px` }),
        ...(y > screenHeight / 2 ? { bottom: `${screenHeight - y + OFFSET}px` } : { top: `${y + OFFSET}px` }),
    };
};
