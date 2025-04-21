// @ts-nocheck
import { ModifierArguments, Modifier } from '../types';
import computeOffsets from '../utils/computeOffsets';

/**
 *
 */
function popperOffsets({ state, name }: ModifierArguments<{}>) {
  // Offsets are the actual position the popper needs to have to be
  // properly positioned near its reference element
  // This is the most basic placement, and will be adjusted by
  // the modifiers in the next step
  state.modifiersData[name] = computeOffsets({
    reference: state.rects.reference,
    element: state.rects.popper,
    strategy: 'absolute',
    placement: state.placement
  });
}

// eslint-disable-next-line import/no-unused-modules
export type PopperOffsetsModifier = Modifier<'popperOffsets', {}>;
export default {
  name: 'popperOffsets',
  enabled: true,
  phase: 'read',
  fn: popperOffsets,
  data: {}
} as PopperOffsetsModifier;
