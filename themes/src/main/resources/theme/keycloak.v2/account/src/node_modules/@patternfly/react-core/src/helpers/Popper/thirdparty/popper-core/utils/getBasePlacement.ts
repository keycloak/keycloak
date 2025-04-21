// @ts-nocheck
import { BasePlacement, Placement, auto } from '../enums';

/**
 * @param placement
 */
export default function getBasePlacement(placement: Placement | typeof auto): BasePlacement {
  return placement.split('-')[0] as any;
}
