"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
const getBasePlacement_1 = tslib_1.__importDefault(require("../utils/getBasePlacement"));
const getLayoutRect_1 = tslib_1.__importDefault(require("../dom-utils/getLayoutRect"));
const contains_1 = tslib_1.__importDefault(require("../dom-utils/contains"));
const getOffsetParent_1 = tslib_1.__importDefault(require("../dom-utils/getOffsetParent"));
const getMainAxisFromPlacement_1 = tslib_1.__importDefault(require("../utils/getMainAxisFromPlacement"));
const within_1 = tslib_1.__importDefault(require("../utils/within"));
const mergePaddingObject_1 = tslib_1.__importDefault(require("../utils/mergePaddingObject"));
const expandToHashMap_1 = tslib_1.__importDefault(require("../utils/expandToHashMap"));
const enums_1 = require("../enums");
const instanceOf_1 = require("../dom-utils/instanceOf");
/**
 *
 */
function arrow({ state, name }) {
    const arrowElement = state.elements.arrow;
    const popperOffsets = state.modifiersData.popperOffsets;
    const basePlacement = getBasePlacement_1.default(state.placement);
    const axis = getMainAxisFromPlacement_1.default(basePlacement);
    const isVertical = [enums_1.left, enums_1.right].indexOf(basePlacement) >= 0;
    const len = isVertical ? 'height' : 'width';
    if (!arrowElement || !popperOffsets) {
        return;
    }
    const paddingObject = state.modifiersData[`${name}#persistent`].padding;
    const arrowRect = getLayoutRect_1.default(arrowElement);
    const minProp = axis === 'y' ? enums_1.top : enums_1.left;
    const maxProp = axis === 'y' ? enums_1.bottom : enums_1.right;
    const endDiff = state.rects.reference[len] + state.rects.reference[axis] - popperOffsets[axis] - state.rects.popper[len];
    const startDiff = popperOffsets[axis] - state.rects.reference[axis];
    const arrowOffsetParent = getOffsetParent_1.default(arrowElement);
    const clientSize = arrowOffsetParent
        ? axis === 'y'
            ? arrowOffsetParent.clientHeight || 0
            : arrowOffsetParent.clientWidth || 0
        : 0;
    const centerToReference = endDiff / 2 - startDiff / 2;
    // Make sure the arrow doesn't overflow the popper if the center point is
    // outside of the popper bounds
    const min = paddingObject[minProp];
    const max = clientSize - arrowRect[len] - paddingObject[maxProp];
    const center = clientSize / 2 - arrowRect[len] / 2 + centerToReference;
    const offset = within_1.default(min, center, max);
    // Prevents breaking syntax highlighting...
    const axisProp = axis;
    state.modifiersData[name] = {
        [axisProp]: offset,
        centerOffset: offset - center
    };
}
/**
 *
 */
function effect({ state, options, name }) {
    let { element: arrowElement = '[data-popper-arrow]', padding = 0 } = options;
    if (arrowElement == null) {
        return;
    }
    // CSS selector
    if (typeof arrowElement === 'string') {
        arrowElement = state.elements.popper.querySelector(arrowElement);
        if (!arrowElement) {
            return;
        }
    }
    if (false /* __DEV__*/) {
        if (!instanceOf_1.isHTMLElement(arrowElement)) {
            console.error([
                'Popper: "arrow" element must be an HTMLElement (not an SVGElement).',
                'To use an SVG arrow, wrap it in an HTMLElement that will be used as',
                'the arrow.'
            ].join(' '));
        }
    }
    if (!contains_1.default(state.elements.popper, arrowElement)) {
        if (false /* __DEV__*/) {
            console.error(['Popper: "arrow" modifier\'s `element` must be a child of the popper', 'element.'].join(' '));
        }
        return;
    }
    state.elements.arrow = arrowElement;
    state.modifiersData[`${name}#persistent`] = {
        padding: mergePaddingObject_1.default(typeof padding !== 'number' ? padding : expandToHashMap_1.default(padding, enums_1.basePlacements))
    };
}
exports.default = {
    name: 'arrow',
    enabled: true,
    phase: 'main',
    fn: arrow,
    effect,
    requires: ['popperOffsets'],
    requiresIfExists: ['preventOverflow']
};
//# sourceMappingURL=arrow.js.map