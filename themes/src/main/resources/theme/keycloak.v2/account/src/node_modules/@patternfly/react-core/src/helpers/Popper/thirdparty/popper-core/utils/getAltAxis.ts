// @ts-nocheck

/**
 * @param axis
 */
export default function getAltAxis(axis: 'x' | 'y'): 'x' | 'y' {
  return axis === 'x' ? 'y' : 'x';
}
