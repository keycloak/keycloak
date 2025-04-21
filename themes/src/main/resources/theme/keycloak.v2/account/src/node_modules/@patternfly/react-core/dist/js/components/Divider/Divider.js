"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Divider = exports.DividerVariant = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const divider_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Divider/divider"));
const util_1 = require("../../helpers/util");
var DividerVariant;
(function (DividerVariant) {
    DividerVariant["hr"] = "hr";
    DividerVariant["li"] = "li";
    DividerVariant["div"] = "div";
})(DividerVariant = exports.DividerVariant || (exports.DividerVariant = {}));
const Divider = (_a) => {
    var { className, component = DividerVariant.hr, isVertical = false, inset, orientation } = _a, props = tslib_1.__rest(_a, ["className", "component", "isVertical", "inset", "orientation"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ className: react_styles_1.css(divider_1.default.divider, isVertical && divider_1.default.modifiers.vertical, util_1.formatBreakpointMods(inset, divider_1.default), util_1.formatBreakpointMods(orientation, divider_1.default), className) }, (component !== 'hr' && { role: 'separator' }), props)));
};
exports.Divider = Divider;
exports.Divider.displayName = 'Divider';
//# sourceMappingURL=Divider.js.map