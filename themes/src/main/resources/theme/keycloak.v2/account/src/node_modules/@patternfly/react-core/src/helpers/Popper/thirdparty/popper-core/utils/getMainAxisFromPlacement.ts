// @ts-nocheck
import { Placement } from '../enums';

/**
 * @param placement
 */
export default function getMainAxisFromPlacement(placement: Placement): 'x' | 'y' {
  return ['top', 'bottom'].indexOf(placement) >= 0 ? 'x' : 'y';
}
