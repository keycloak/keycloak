// @ts-nocheck
import { Placement } from '../enums';

const hash = { start: 'end', end: 'start' };

/**
 * @param placement
 */
export default function getOppositeVariationPlacement(placement: Placement): Placement {
  return placement.replace(/start|end/g, matched => hash[matched]) as any;
}
