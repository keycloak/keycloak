"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.FlexItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const flex_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/layouts/Flex/flex"));
const flexToken = tslib_1.__importStar(require('@patternfly/react-tokens/dist/js/l_flex_item_Order'));
const util_1 = require("../../helpers/util");
const FlexItem = (_a) => {
    var { children = null, className = '', component = 'div', spacer, grow, shrink, flex, alignSelf, align, fullWidth, order, style } = _a, props = tslib_1.__rest(_a, ["children", "className", "component", "spacer", "grow", "shrink", "flex", "alignSelf", "align", "fullWidth", "order", "style"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({}, props, { className: react_styles_1.css(util_1.formatBreakpointMods(spacer, flex_1.default), util_1.formatBreakpointMods(grow, flex_1.default), util_1.formatBreakpointMods(shrink, flex_1.default), util_1.formatBreakpointMods(flex, flex_1.default), util_1.formatBreakpointMods(alignSelf, flex_1.default), util_1.formatBreakpointMods(align, flex_1.default), util_1.formatBreakpointMods(fullWidth, flex_1.default), className), style: style || order ? Object.assign(Object.assign({}, style), util_1.setBreakpointCssVars(order, flexToken.l_flex_item_Order.name)) : undefined }), children));
};
exports.FlexItem = FlexItem;
exports.FlexItem.displayName = 'FlexItem';
//# sourceMappingURL=FlexItem.js.map