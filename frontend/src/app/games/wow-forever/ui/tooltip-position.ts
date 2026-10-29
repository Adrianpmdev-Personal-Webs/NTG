/**
 * tooltipPosition
 *
 * Where a tooltip that hangs from the pointer goes: beside the pointer, on the side with more room, above it in the
 * lower half of the screen, and centred on screen when neither side leaves room for it (phones). Shared by the talent
 * and the ability tooltips.
 *
 * @author @adrianpmdev
 */

const OFFSET = 16;

const MAX_WIDTH = 320;

const EDGE = 16;

export const tooltipPosition = (x: number, y: number): Record<string, string> => {
    const screenWidth = window.innerWidth;
    const width = Math.min(MAX_WIDTH, screenWidth - 2 * EDGE);
    const onLeft = x > screenWidth / 2;
    const room = (onLeft ? x : screenWidth - x) - OFFSET - EDGE;

    return {
        ...(room < width
            ? { left: `${(screenWidth - width) / 2}px` }
            : onLeft
              ? { right: `${screenWidth - x + OFFSET}px` }
              : { left: `${x + OFFSET}px` }),
        ...(y > window.innerHeight / 2
            ? { bottom: `${window.innerHeight - y + OFFSET}px` }
            : { top: `${y + OFFSET}px` }),
    };
};
