"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.GridItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const grid_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/layouts/Grid/grid"));
const react_styles_1 = require("@patternfly/react-styles");
const sizes_1 = require("../../styles/sizes");
const gridToken = tslib_1.__importStar(require('@patternfly/react-tokens/dist/js/l_grid_item_Order'));
const util_1 = require("../../helpers/util");
const GridItem = (_a) => {
    var { children = null, className = '', component = 'div', span = null, rowSpan = null, offset = null, order, style } = _a, props = tslib_1.__rest(_a, ["children", "className", "component", "span", "rowSpan", "offset", "order", "style"]);
    const classes = [
        grid_1.default.gridItem,
        span && grid_1.default.modifiers[`${span}Col`],
        rowSpan && grid_1.default.modifiers[`${rowSpan}Row`],
        offset && grid_1.default.modifiers[`offset_${offset}Col`]
    ];
    const Component = component;
    Object.entries(sizes_1.DeviceSizes).forEach(([propKey, classModifier]) => {
        const key = propKey;
        const rowSpanKey = `${key}RowSpan`;
        const offsetKey = `${key}Offset`;
        const spanValue = props[key];
        const rowSpanValue = props[rowSpanKey];
        const offsetValue = props[offsetKey];
        if (spanValue) {
            classes.push(grid_1.default.modifiers[`${spanValue}ColOn${classModifier}`]);
        }
        if (rowSpanValue) {
            classes.push(grid_1.default.modifiers[`${rowSpanValue}RowOn${classModifier}`]);
        }
        if (offsetValue) {
            classes.push(grid_1.default.modifiers[`offset_${offsetValue}ColOn${classModifier}`]);
        }
        delete props[key];
        delete props[rowSpanKey];
        delete props[offsetKey];
    });
    return (React.createElement(Component, Object.assign({ className: react_styles_1.css(...classes, className), style: style || order ? Object.assign(Object.assign({}, style), util_1.setBreakpointCssVars(order, gridToken.l_grid_item_Order.name)) : undefined }, props), children));
};
exports.GridItem = GridItem;
exports.GridItem.displayName = 'GridItem';
//# sourceMappingURL=GridItem.js.map