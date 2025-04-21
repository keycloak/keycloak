// @ts-nocheck
import { Placement } from '../enums';

const hash = { left: 'right', right: 'left', bottom: 'top', top: 'bottom' };

/**
 * @param placement
 */
export default function getOppositePlacement(placement: Placement): Placement {
  return placement.replace(/left|right|bottom|top/g, matched => hash[matched]) as any;
}
