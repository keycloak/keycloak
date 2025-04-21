"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
// @ts-nocheck
const getBasePlacement_1 = tslib_1.__importDefault(require("./getBasePlacement"));
const getVariation_1 = tslib_1.__importDefault(require("./getVariation"));
const getMainAxisFromPlacement_1 = tslib_1.__importDefault(require("./getMainAxisFromPlacement"));
const enums_1 = require("../enums");
/**
 *
 */
function computeOffsets({ reference, element, placement }) {
    const basePlacement = placement ? getBasePlacement_1.default(placement) : null;
    const variation = placement ? getVariation_1.default(placement) : null;
    const commonX = reference.x + reference.width / 2 - element.width / 2;
    const commonY = reference.y + reference.height / 2 - element.height / 2;
    let offsets;
    switch (basePlacement) {
        case enums_1.top:
            offsets = {
                x: commonX,
                y: reference.y - element.height
            };
            break;
        case enums_1.bottom:
            offsets = {
                x: commonX,
                y: reference.y + reference.height
            };
            break;
        case enums_1.right:
            offsets = {
                x: reference.x + reference.width,
                y: commonY
            };
            break;
        case enums_1.left:
            offsets = {
                x: reference.x - element.width,
                y: commonY
            };
            break;
        default:
            offsets = {
                x: reference.x,
                y: reference.y
            };
    }
    const mainAxis = basePlacement ? getMainAxisFromPlacement_1.default(basePlacement) : null;
    if (mainAxis != null) {
        const len = mainAxis === 'y' ? 'height' : 'width';
        switch (variation) {
            case enums_1.start:
                offsets[mainAxis] = Math.floor(offsets[mainAxis]) - Math.floor(reference[len] / 2 - element[len] / 2);
                break;
            case enums_1.end:
                offsets[mainAxis] = Math.floor(offsets[mainAxis]) + Math.ceil(reference[len] / 2 - element[len] / 2);
                break;
            default:
        }
    }
    return offsets;
}
exports.default = computeOffsets;
//# sourceMappingURL=computeOffsets.js.map