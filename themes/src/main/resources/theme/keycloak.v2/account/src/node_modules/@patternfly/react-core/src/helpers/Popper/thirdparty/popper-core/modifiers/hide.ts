// @ts-nocheck
import { ModifierArguments, Modifier, Rect, SideObject, Offsets } from '../types';
import { top, bottom, left, right } from '../enums';
import detectOverflow from '../utils/detectOverflow';

/**
 * @param overflow
 * @param rect
 * @param preventedOffsets
 */
function getSideOffsets(overflow: SideObject, rect: Rect, preventedOffsets: Offsets = { x: 0, y: 0 }): SideObject {
  return {
    top: overflow.top - rect.height - preventedOffsets.y,
    right: overflow.right - rect.width + preventedOffsets.x,
    bottom: overflow.bottom - rect.height + preventedOffsets.y,
    left: overflow.left - rect.width - preventedOffsets.x
  };
}

/**
 * @param overflow
 */
function isAnySideFullyClipped(overflow: SideObject): boolean {
  return [top, right, bottom, left].some(side => overflow[side] >= 0);
}

/**
 *
 */
function hide({ state, name }: ModifierArguments<{}>) {
  const referenceRect = state.rects.reference;
  const popperRect = state.rects.popper;
  const preventedOffsets = state.modifiersData.preventOverflow;

  const referenceOverflow = detectOverflow(state, {
    elementContext: 'reference'
  });
  const popperAltOverflow = detectOverflow(state, {
    altBoundary: true
  });

  const referenceClippingOffsets = getSideOffsets(referenceOverflow, referenceRect);
  const popperEscapeOffsets = getSideOffsets(popperAltOverflow, popperRect, preventedOffsets);

  const isReferenceHidden = isAnySideFullyClipped(referenceClippingOffsets);
  const hasPopperEscaped = isAnySideFullyClipped(popperEscapeOffsets);

  state.modifiersData[name] = {
    referenceClippingOffsets,
    popperEscapeOffsets,
    isReferenceHidden,
    hasPopperEscaped
  };

  state.attributes.popper = {
    ...state.attributes.popper,
    'data-popper-reference-hidden': isReferenceHidden,
    'data-popper-escaped': hasPopperEscaped
  };
}

// eslint-disable-next-line import/no-unused-modules
export type HideModifier = Modifier<'hide', {}>;
export default {
  name: 'hide',
  enabled: true,
  phase: 'main',
  requiresIfExists: ['preventOverflow'],
  fn: hide
} as HideModifier;
