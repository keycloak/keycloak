"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
const getVariation_1 = tslib_1.__importDefault(require("./getVariation"));
const enums_1 = require("../enums");
const detectOverflow_1 = tslib_1.__importDefault(require("./detectOverflow"));
const getBasePlacement_1 = tslib_1.__importDefault(require("./getBasePlacement"));
/* :: type OverflowsMap = { [ComputedPlacement]: number }; */
/* ;; type OverflowsMap = { [key in ComputedPlacement]: number }; */
/**
 * @param state
 * @param options
 */
function computeAutoPlacement(state, options = {}) {
    const { placement, boundary, rootBoundary, padding, flipVariations, allowedAutoPlacements = enums_1.placements } = options;
    const variation = getVariation_1.default(placement);
    const placements = variation
        ? flipVariations
            ? enums_1.variationPlacements
            : enums_1.variationPlacements.filter(placement => getVariation_1.default(placement) === variation)
        : enums_1.basePlacements;
    // $FlowFixMe
    let allowedPlacements = placements.filter(placement => allowedAutoPlacements.indexOf(placement) >= 0);
    if (allowedPlacements.length === 0) {
        allowedPlacements = placements;
        if (false /* __DEV__*/) {
            console.error([
                'Popper: The `allowedAutoPlacements` option did not allow any',
                'placements. Ensure the `placement` option matches the variation',
                'of the allowed placements.',
                'For example, "auto" cannot be used to allow "bottom-start".',
                'Use "auto-start" instead.'
            ].join(' '));
        }
    }
    // $FlowFixMe: Flow seems to have problems with two array unions...
    const overflows = allowedPlacements.reduce((acc, placement) => {
        acc[placement] = detectOverflow_1.default(state, {
            placement,
            boundary,
            rootBoundary,
            padding
        })[getBasePlacement_1.default(placement)];
        return acc;
    }, {});
    return Object.keys(overflows).sort((a, b) => overflows[a] - overflows[b]);
}
exports.default = computeAutoPlacement;
//# sourceMappingURL=computeAutoPlacement.js.map