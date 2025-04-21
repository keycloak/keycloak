// @ts-nocheck
import { popperGenerator, detectOverflow } from '.';
import eventListeners from './modifiers/eventListeners';
import popperOffsets from './modifiers/popperOffsets';
import computeStyles from './modifiers/computeStyles';
import applyStyles from './modifiers/applyStyles';
import offset from './modifiers/offset';
import flip from './modifiers/flip';
import preventOverflow from './modifiers/preventOverflow';
import arrow from './modifiers/arrow';
import hide from './modifiers/hide';
export * from './types';
const defaultModifiers = [
    eventListeners,
    popperOffsets,
    computeStyles,
    applyStyles,
    offset,
    flip,
    preventOverflow,
    arrow,
    hide
];
const createPopper = popperGenerator({ defaultModifiers });
// eslint-disable-next-line import/no-unused-modules
export { createPopper, popperGenerator, defaultModifiers, detectOverflow };
//# sourceMappingURL=popper.js.map