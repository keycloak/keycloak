"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
const getBoundingClientRect_1 = tslib_1.__importDefault(require("../dom-utils/getBoundingClientRect"));
const getClippingRect_1 = tslib_1.__importDefault(require("../dom-utils/getClippingRect"));
const getDocumentElement_1 = tslib_1.__importDefault(require("../dom-utils/getDocumentElement"));
const computeOffsets_1 = tslib_1.__importDefault(require("./computeOffsets"));
const rectToClientRect_1 = tslib_1.__importDefault(require("./rectToClientRect"));
const enums_1 = require("../enums");
const instanceOf_1 = require("../dom-utils/instanceOf");
const mergePaddingObject_1 = tslib_1.__importDefault(require("./mergePaddingObject"));
const expandToHashMap_1 = tslib_1.__importDefault(require("./expandToHashMap"));
/**
 * @param state
 * @param options
 */
function detectOverflow(state, options = {}) {
    const { placement = state.placement, boundary = enums_1.clippingParents, rootBoundary = enums_1.viewport, elementContext = enums_1.popper, altBoundary = false, padding = 0 } = options;
    const paddingObject = mergePaddingObject_1.default(typeof padding !== 'number' ? padding : expandToHashMap_1.default(padding, enums_1.basePlacements));
    const altContext = elementContext === enums_1.popper ? enums_1.reference : enums_1.popper;
    const referenceElement = state.elements.reference;
    const popperRect = state.rects.popper;
    const element = state.elements[altBoundary ? altContext : elementContext];
    const clippingClientRect = getClippingRect_1.default(instanceOf_1.isElement(element) ? element : element.contextElement || getDocumentElement_1.default(state.elements.popper), boundary, rootBoundary);
    const referenceClientRect = getBoundingClientRect_1.default(referenceElement);
    const popperOffsets = computeOffsets_1.default({
        reference: referenceClientRect,
        element: popperRect,
        strategy: 'absolute',
        placement
    });
    const popperClientRect = rectToClientRect_1.default(Object.assign(Object.assign({}, popperRect), popperOffsets));
    const elementClientRect = elementContext === enums_1.popper ? popperClientRect : referenceClientRect;
    // positive = overflowing the clipping rect
    // 0 or negative = within the clipping rect
    const overflowOffsets = {
        top: clippingClientRect.top - elementClientRect.top + paddingObject.top,
        bottom: elementClientRect.bottom - clippingClientRect.bottom + paddingObject.bottom,
        left: clippingClientRect.left - elementClientRect.left + paddingObject.left,
        right: elementClientRect.right - clippingClientRect.right + paddingObject.right
    };
    const offsetData = state.modifiersData.offset;
    // Offsets can be applied only to the popper element
    if (elementContext === enums_1.popper && offsetData) {
        const offset = offsetData[placement];
        Object.keys(overflowOffsets).forEach(key => {
            const multiply = [enums_1.right, enums_1.bottom].indexOf(key) >= 0 ? 1 : -1;
            const axis = [enums_1.top, enums_1.bottom].indexOf(key) >= 0 ? 'y' : 'x';
            overflowOffsets[key] += offset[axis] * multiply;
        });
    }
    return overflowOffsets;
}
exports.default = detectOverflow;
//# sourceMappingURL=detectOverflow.js.map