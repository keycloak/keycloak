"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const hash = { start: 'end', end: 'start' };
/**
 * @param placement
 */
function getOppositeVariationPlacement(placement) {
    return placement.replace(/start|end/g, matched => hash[matched]);
}
exports.default = getOppositeVariationPlacement;
//# sourceMappingURL=getOppositeVariationPlacement.js.map