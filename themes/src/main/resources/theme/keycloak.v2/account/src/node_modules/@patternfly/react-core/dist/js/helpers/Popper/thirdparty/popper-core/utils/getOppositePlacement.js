"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const hash = { left: 'right', right: 'left', bottom: 'top', top: 'bottom' };
/**
 * @param placement
 */
function getOppositePlacement(placement) {
    return placement.replace(/left|right|bottom|top/g, matched => hash[matched]);
}
exports.default = getOppositePlacement;
//# sourceMappingURL=getOppositePlacement.js.map