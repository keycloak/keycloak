// @ts-nocheck
import { popperGenerator, detectOverflow } from '.';
import eventListeners from './modifiers/eventListeners';
import popperOffsets from './modifiers/popperOffsets';
import computeStyles from './modifiers/computeStyles';
import applyStyles from './modifiers/applyStyles';
export * from './types';
const defaultModifiers = [eventListeners, popperOffsets, computeStyles, applyStyles];
const createPopper = popperGenerator({ defaultModifiers });
// eslint-disable-next-line import/no-unused-modules
export { createPopper, popperGenerator, defaultModifiers, detectOverflow };
//# sourceMappingURL=popper-lite.js.map