import getVariation from './getVariation';
import { variationPlacements, basePlacements, placements as allPlacements } from '../enums';
import detectOverflow from './detectOverflow';
import getBasePlacement from './getBasePlacement';
/* :: type OverflowsMap = { [ComputedPlacement]: number }; */
/* ;; type OverflowsMap = { [key in ComputedPlacement]: number }; */
/**
 * @param state
 * @param options
 */
export default function computeAutoPlacement(state, options = {}) {
    const { placement, boundary, rootBoundary, padding, flipVariations, allowedAutoPlacements = allPlacements } = options;
    const variation = getVariation(placement);
    const placements = variation
        ? flipVariations
            ? variationPlacements
            : variationPlacements.filter(placement => getVariation(placement) === variation)
        : basePlacements;
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
        acc[placement] = detectOverflow(state, {
            placement,
            boundary,
            rootBoundary,
            padding
        })[getBasePlacement(placement)];
        return acc;
    }, {});
    return Object.keys(overflows).sort((a, b) => overflows[a] - overflows[b]);
}
//# sourceMappingURL=computeAutoPlacement.js.map