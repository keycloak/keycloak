// @ts-nocheck
import { Modifier } from '../types';

/**
 * @param modifiers
 */
export default function mergeByName(modifiers: Partial<Modifier<any, any>>[]): Partial<Modifier<any, any>>[] {
  const merged = modifiers.reduce((merged, current) => {
    const existing = merged[current.name];
    merged[current.name] = existing
      ? {
          ...existing,
          ...current,
          options: { ...existing.options, ...current.options },
          data: { ...existing.data, ...current.data }
        }
      : current;
    return merged;
  }, {});

  // IE11 does not support Object.values
  return Object.keys(merged).map(key => merged[key]);
}
