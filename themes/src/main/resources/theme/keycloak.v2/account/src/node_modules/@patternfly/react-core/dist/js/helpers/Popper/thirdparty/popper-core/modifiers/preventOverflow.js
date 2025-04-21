"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
// @ts-nocheck
const enums_1 = require("../enums");
const getBasePlacement_1 = tslib_1.__importDefault(require("../utils/getBasePlacement"));
const getMainAxisFromPlacement_1 = tslib_1.__importDefault(require("../utils/getMainAxisFromPlacement"));
const getAltAxis_1 = tslib_1.__importDefault(require("../utils/getAltAxis"));
const within_1 = tslib_1.__importDefault(require("../utils/within"));
const getLayoutRect_1 = tslib_1.__importDefault(require("../dom-utils/getLayoutRect"));
const getOffsetParent_1 = tslib_1.__importDefault(require("../dom-utils/getOffsetParent"));
const detectOverflow_1 = tslib_1.__importDefault(require("../utils/detectOverflow"));
const getVariation_1 = tslib_1.__importDefault(require("../utils/getVariation"));
const getFreshSideObject_1 = tslib_1.__importDefault(require("../utils/getFreshSideObject"));
/**
 *
 */
function preventOverflow({ state, options, name }) {
    const { mainAxis: checkMainAxis = true, altAxis: checkAltAxis = false, boundary, rootBoundary, altBoundary, padding, tether = true, tetherOffset = 0 } = options;
    const overflow = detectOverflow_1.default(state, {
        boundary,
        rootBoundary,
        padding,
        altBoundary
    });
    const basePlacement = getBasePlacement_1.default(state.placement);
    const variation = getVariation_1.default(state.placement);
    const isBasePlacement = !variation;
    const mainAxis = getMainAxisFromPlacement_1.default(basePlacement);
    const altAxis = getAltAxis_1.default(mainAxis);
    const popperOffsets = state.modifiersData.popperOffsets;
    const referenceRect = state.rects.reference;
    const popperRect = state.rects.popper;
    const tetherOffsetValue = typeof tetherOffset === 'function'
        ? tetherOffset(Object.assign(Object.assign({}, state.rects), { placement: state.placement }))
        : tetherOffset;
    const data = { x: 0, y: 0 };
    if (!popperOffsets) {
        return;
    }
    if (checkMainAxis) {
        const mainSide = mainAxis === 'y' ? enums_1.top : enums_1.left;
        const altSide = mainAxis === 'y' ? enums_1.bottom : enums_1.right;
        const len = mainAxis === 'y' ? 'height' : 'width';
        const offset = popperOffsets[mainAxis];
        const min = popperOffsets[mainAxis] + overflow[mainSide];
        const max = popperOffsets[mainAxis] - overflow[altSide];
        const additive = tether ? -popperRect[len] / 2 : 0;
        const minLen = variation === enums_1.start ? referenceRect[len] : popperRect[len];
        const maxLen = variation === enums_1.start ? -popperRect[len] : -referenceRect[len];
        // We need to include the arrow in the calculation so the arrow doesn't go
        // outside the reference bounds
        const arrowElement = state.elements.arrow;
        const arrowRect = tether && arrowElement ? getLayoutRect_1.default(arrowElement) : { width: 0, height: 0 };
        const arrowPaddingObject = state.modifiersData['arrow#persistent']
            ? state.modifiersData['arrow#persistent'].padding
            : getFreshSideObject_1.default();
        const arrowPaddingMin = arrowPaddingObject[mainSide];
        const arrowPaddingMax = arrowPaddingObject[altSide];
        // If the reference length is smaller than the arrow length, we don't want
        // to include its full size in the calculation. If the reference is small
        // and near the edge of a boundary, the popper can overflow even if the
        // reference is not overflowing as well (e.g. virtual elements with no
        // width or height)
        const arrowLen = within_1.default(0, referenceRect[len], arrowRect[len]);
        const minOffset = isBasePlacement
            ? referenceRect[len] / 2 - additive - arrowLen - arrowPaddingMin - tetherOffsetValue
            : minLen - arrowLen - arrowPaddingMin - tetherOffsetValue;
        const maxOffset = isBasePlacement
            ? -referenceRect[len] / 2 + additive + arrowLen + arrowPaddingMax + tetherOffsetValue
            : maxLen + arrowLen + arrowPaddingMax + tetherOffsetValue;
        const arrowOffsetParent = state.elements.arrow && getOffsetParent_1.default(state.elements.arrow);
        const clientOffset = arrowOffsetParent
            ? mainAxis === 'y'
                ? arrowOffsetParent.clientTop || 0
                : arrowOffsetParent.clientLeft || 0
            : 0;
        const offsetModifierValue = state.modifiersData.offset ? state.modifiersData.offset[state.placement][mainAxis] : 0;
        const tetherMin = popperOffsets[mainAxis] + minOffset - offsetModifierValue - clientOffset;
        const tetherMax = popperOffsets[mainAxis] + maxOffset - offsetModifierValue;
        const preventedOffset = within_1.default(tether ? Math.min(min, tetherMin) : min, offset, tether ? Math.max(max, tetherMax) : max);
        popperOffsets[mainAxis] = preventedOffset;
        data[mainAxis] = preventedOffset - offset;
    }
    if (checkAltAxis) {
        const mainSide = mainAxis === 'x' ? enums_1.top : enums_1.left;
        const altSide = mainAxis === 'x' ? enums_1.bottom : enums_1.right;
        const offset = popperOffsets[altAxis];
        const min = offset + overflow[mainSide];
        const max = offset - overflow[altSide];
        const preventedOffset = within_1.default(min, offset, max);
        popperOffsets[altAxis] = preventedOffset;
        data[altAxis] = preventedOffset - offset;
    }
    state.modifiersData[name] = data;
}
exports.default = {
    name: 'preventOverflow',
    enabled: true,
    phase: 'main',
    fn: preventOverflow,
    requiresIfExists: ['offset']
};
//# sourceMappingURL=preventOverflow.js.map