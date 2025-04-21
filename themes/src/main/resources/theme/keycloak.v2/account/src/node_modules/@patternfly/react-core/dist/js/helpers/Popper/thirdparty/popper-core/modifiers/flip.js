"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
const getOppositePlacement_1 = tslib_1.__importDefault(require("../utils/getOppositePlacement"));
const getBasePlacement_1 = tslib_1.__importDefault(require("../utils/getBasePlacement"));
const getOppositeVariationPlacement_1 = tslib_1.__importDefault(require("../utils/getOppositeVariationPlacement"));
const detectOverflow_1 = tslib_1.__importDefault(require("../utils/detectOverflow"));
const computeAutoPlacement_1 = tslib_1.__importDefault(require("../utils/computeAutoPlacement"));
const enums_1 = require("../enums");
const getVariation_1 = tslib_1.__importDefault(require("../utils/getVariation"));
/**
 * @param placement
 */
function getExpandedFallbackPlacements(placement) {
    if (getBasePlacement_1.default(placement) === enums_1.auto) {
        return [];
    }
    const oppositePlacement = getOppositePlacement_1.default(placement);
    return [
        getOppositeVariationPlacement_1.default(placement),
        oppositePlacement,
        getOppositeVariationPlacement_1.default(oppositePlacement)
    ];
}
/**
 *
 */
function flip({ state, options, name }) {
    if (state.modifiersData[name]._skip) {
        return;
    }
    const { mainAxis: checkMainAxis = true, altAxis: checkAltAxis = true, fallbackPlacements: specifiedFallbackPlacements, padding, boundary, rootBoundary, altBoundary, flipVariations = true, allowedAutoPlacements } = options;
    const preferredPlacement = state.options.placement;
    const basePlacement = getBasePlacement_1.default(preferredPlacement);
    const isBasePlacement = basePlacement === preferredPlacement;
    const fallbackPlacements = specifiedFallbackPlacements ||
        (isBasePlacement || !flipVariations
            ? [getOppositePlacement_1.default(preferredPlacement)]
            : getExpandedFallbackPlacements(preferredPlacement));
    const placements = [preferredPlacement, ...fallbackPlacements].reduce((acc, placement) => acc.concat(getBasePlacement_1.default(placement) === enums_1.auto
        ? computeAutoPlacement_1.default(state, {
            placement,
            boundary,
            rootBoundary,
            padding,
            flipVariations,
            allowedAutoPlacements
        })
        : placement), []);
    const referenceRect = state.rects.reference;
    const popperRect = state.rects.popper;
    const checksMap = new Map();
    let makeFallbackChecks = true;
    let firstFittingPlacement = placements[0];
    for (let i = 0; i < placements.length; i++) {
        const placement = placements[i];
        const basePlacement = getBasePlacement_1.default(placement);
        const isStartVariation = getVariation_1.default(placement) === enums_1.start;
        const isVertical = [enums_1.top, enums_1.bottom].indexOf(basePlacement) >= 0;
        const len = isVertical ? 'width' : 'height';
        const overflow = detectOverflow_1.default(state, {
            placement,
            boundary,
            rootBoundary,
            altBoundary,
            padding
        });
        let mainVariationSide = isVertical ? (isStartVariation ? enums_1.right : enums_1.left) : isStartVariation ? enums_1.bottom : enums_1.top;
        if (referenceRect[len] > popperRect[len]) {
            mainVariationSide = getOppositePlacement_1.default(mainVariationSide);
        }
        const altVariationSide = getOppositePlacement_1.default(mainVariationSide);
        const checks = [];
        if (checkMainAxis) {
            checks.push(overflow[basePlacement] <= 0);
        }
        if (checkAltAxis) {
            checks.push(overflow[mainVariationSide] <= 0, overflow[altVariationSide] <= 0);
        }
        if (checks.every(check => check)) {
            firstFittingPlacement = placement;
            makeFallbackChecks = false;
            break;
        }
        checksMap.set(placement, checks);
    }
    if (makeFallbackChecks) {
        // `2` may be desired in some cases – research later
        const numberOfChecks = flipVariations ? 3 : 1;
        for (let i = numberOfChecks; i > 0; i--) {
            const fittingPlacement = placements.find(placement => {
                const checks = checksMap.get(placement);
                if (checks) {
                    return checks.slice(0, i).every(check => check);
                }
            });
            if (fittingPlacement) {
                firstFittingPlacement = fittingPlacement;
                break;
            }
        }
    }
    if (state.placement !== firstFittingPlacement) {
        state.modifiersData[name]._skip = true;
        state.placement = firstFittingPlacement;
        state.reset = true;
    }
}
exports.default = {
    name: 'flip',
    enabled: true,
    phase: 'main',
    fn: flip,
    requiresIfExists: ['offset'],
    data: { _skip: false }
};
//# sourceMappingURL=flip.js.map