"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.distanceAndSkiddingToXY = void 0;
const tslib_1 = require("tslib");
const getBasePlacement_1 = tslib_1.__importDefault(require("../utils/getBasePlacement"));
const enums_1 = require("../enums");
/**
 * @param placement
 * @param rects
 * @param offset
 */
function distanceAndSkiddingToXY(placement, rects, offset) {
    const basePlacement = getBasePlacement_1.default(placement);
    const invertDistance = [enums_1.left, enums_1.top].indexOf(basePlacement) >= 0 ? -1 : 1;
    let [skidding, distance] = typeof offset === 'function'
        ? offset(Object.assign(Object.assign({}, rects), { placement }))
        : offset;
    skidding = skidding || 0;
    distance = (distance || 0) * invertDistance;
    return [enums_1.left, enums_1.right].indexOf(basePlacement) >= 0 ? { x: distance, y: skidding } : { x: skidding, y: distance };
}
exports.distanceAndSkiddingToXY = distanceAndSkiddingToXY;
/**
 *
 */
function offset({ state, options, name }) {
    const { offset = [0, 0] } = options;
    const data = enums_1.placements.reduce((acc, placement) => {
        acc[placement] = distanceAndSkiddingToXY(placement, state.rects, offset);
        return acc;
    }, {});
    const { x, y } = data[state.placement];
    if (state.modifiersData.popperOffsets != null) {
        state.modifiersData.popperOffsets.x += x;
        state.modifiersData.popperOffsets.y += y;
    }
    state.modifiersData[name] = data;
}
exports.default = {
    name: 'offset',
    enabled: true,
    phase: 'main',
    requires: ['popperOffsets'],
    fn: offset
};
//# sourceMappingURL=offset.js.map