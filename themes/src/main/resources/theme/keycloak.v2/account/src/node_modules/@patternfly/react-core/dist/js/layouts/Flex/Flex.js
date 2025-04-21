"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Flex = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const flex_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/layouts/Flex/flex"));
const flexToken = tslib_1.__importStar(require('@patternfly/react-tokens/dist/js/l_flex_item_Order'));
const util_1 = require("../../helpers/util");
const Flex = (_a) => {
    var { children = null, className = '', component = 'div', spacer, spaceItems, grow, shrink, flex, direction, alignItems, alignContent, alignSelf, align, justifyContent, display, fullWidth, flexWrap, order, style } = _a, props = tslib_1.__rest(_a, ["children", "className", "component", "spacer", "spaceItems", "grow", "shrink", "flex", "direction", "alignItems", "alignContent", "alignSelf", "align", "justifyContent", "display", "fullWidth", "flexWrap", "order", "style"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ className: react_styles_1.css(flex_1.default.flex, util_1.formatBreakpointMods(spacer, flex_1.default), util_1.formatBreakpointMods(spaceItems, flex_1.default), util_1.formatBreakpointMods(grow, flex_1.default), util_1.formatBreakpointMods(shrink, flex_1.default), util_1.formatBreakpointMods(flex, flex_1.default), util_1.formatBreakpointMods(direction, flex_1.default), util_1.formatBreakpointMods(alignItems, flex_1.default), util_1.formatBreakpointMods(alignContent, flex_1.default), util_1.formatBreakpointMods(alignSelf, flex_1.default), util_1.formatBreakpointMods(align, flex_1.default), util_1.formatBreakpointMods(justifyContent, flex_1.default), util_1.formatBreakpointMods(display, flex_1.default), util_1.formatBreakpointMods(fullWidth, flex_1.default), util_1.formatBreakpointMods(flexWrap, flex_1.default), className), style: style || order ? Object.assign(Object.assign({}, style), util_1.setBreakpointCssVars(order, flexToken.l_flex_item_Order.name)) : undefined }, props), children));
};
exports.Flex = Flex;
exports.Flex.displayName = 'Flex';
//# sourceMappingURL=Flex.js.map