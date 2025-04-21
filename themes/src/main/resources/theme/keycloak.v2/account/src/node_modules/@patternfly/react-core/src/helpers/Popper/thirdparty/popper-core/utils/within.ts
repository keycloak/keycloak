// @ts-nocheck

/**
 * @param min
 * @param value
 * @param max
 */
export default function within(min: number, value: number, max: number): number {
  return Math.max(min, Math.min(value, max));
}
