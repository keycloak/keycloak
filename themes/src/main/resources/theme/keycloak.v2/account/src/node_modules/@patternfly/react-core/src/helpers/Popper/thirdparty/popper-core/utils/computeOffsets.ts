// @ts-nocheck
import getBasePlacement from './getBasePlacement';
import getVariation from './getVariation';
import getMainAxisFromPlacement from './getMainAxisFromPlacement';
import { Rect, PositioningStrategy, Offsets, ClientRectObject } from '../types';
import { top, right, bottom, left, start, end, Placement } from '../enums';

/**
 *
 */
export default function computeOffsets({
  reference,
  element,
  placement
}: {
  reference: Rect | ClientRectObject;
  element: Rect | ClientRectObject;
  strategy: PositioningStrategy;
  placement?: Placement;
}): Offsets {
  const basePlacement = placement ? getBasePlacement(placement) : null;
  const variation = placement ? getVariation(placement) : null;
  const commonX = reference.x + reference.width / 2 - element.width / 2;
  const commonY = reference.y + reference.height / 2 - element.height / 2;

  let offsets;
  switch (basePlacement) {
    case top:
      offsets = {
        x: commonX,
        y: reference.y - element.height
      };
      break;
    case bottom:
      offsets = {
        x: commonX,
        y: reference.y + reference.height
      };
      break;
    case right:
      offsets = {
        x: reference.x + reference.width,
        y: commonY
      };
      break;
    case left:
      offsets = {
        x: reference.x - element.width,
        y: commonY
      };
      break;
    default:
      offsets = {
        x: reference.x,
        y: reference.y
      };
  }

  const mainAxis = basePlacement ? getMainAxisFromPlacement(basePlacement) : null;

  if (mainAxis != null) {
    const len = mainAxis === 'y' ? 'height' : 'width';

    switch (variation) {
      case start:
        offsets[mainAxis] = Math.floor(offsets[mainAxis]) - Math.floor(reference[len] / 2 - element[len] / 2);
        break;
      case end:
        offsets[mainAxis] = Math.floor(offsets[mainAxis]) + Math.ceil(reference[len] / 2 - element[len] / 2);
        break;
      default:
    }
  }

  return offsets;
}
