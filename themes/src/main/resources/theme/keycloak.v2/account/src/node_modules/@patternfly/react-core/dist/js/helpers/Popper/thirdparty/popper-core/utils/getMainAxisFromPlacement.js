"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @param placement
 */
function getMainAxisFromPlacement(placement) {
    return ['top', 'bottom'].indexOf(placement) >= 0 ? 'x' : 'y';
}
exports.default = getMainAxisFromPlacement;
//# sourceMappingURL=getMainAxisFromPlacement.js.map