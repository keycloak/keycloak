// @ts-nocheck
import { Variation, Placement } from '../enums';

/**
 * @param placement
 */
export default function getVariation(placement: Placement): Variation | null | undefined {
  return placement.split('-')[1] as any;
}
