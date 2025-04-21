"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
const enums_1 = require("../enums");
const detectOverflow_1 = tslib_1.__importDefault(require("../utils/detectOverflow"));
/**
 * @param overflow
 * @param rect
 * @param preventedOffsets
 */
function getSideOffsets(overflow, rect, preventedOffsets = { x: 0, y: 0 }) {
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
function isAnySideFullyClipped(overflow) {
    return [enums_1.top, enums_1.right, enums_1.bottom, enums_1.left].some(side => overflow[side] >= 0);
}
/**
 *
 */
function hide({ state, name }) {
    const referenceRect = state.rects.reference;
    const popperRect = state.rects.popper;
    const preventedOffsets = state.modifiersData.preventOverflow;
    const referenceOverflow = detectOverflow_1.default(state, {
        elementContext: 'reference'
    });
    const popperAltOverflow = detectOverflow_1.default(state, {
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
    state.attributes.popper = Object.assign(Object.assign({}, state.attributes.popper), { 'data-popper-reference-hidden': isReferenceHidden, 'data-popper-escaped': hasPopperEscaped });
}
exports.default = {
    name: 'hide',
    enabled: true,
    phase: 'main',
    requiresIfExists: ['preventOverflow'],
    fn: hide
};
//# sourceMappingURL=hide.js.map