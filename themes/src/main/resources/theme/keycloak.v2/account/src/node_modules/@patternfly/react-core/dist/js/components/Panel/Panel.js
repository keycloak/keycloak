"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Panel = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const panel_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Panel/panel"));
const react_styles_1 = require("@patternfly/react-styles");
const PanelBase = (_a) => {
    var { className, children, variant, isScrollable, innerRef } = _a, props = tslib_1.__rest(_a, ["className", "children", "variant", "isScrollable", "innerRef"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(panel_1.default.panel, variant === 'raised' && panel_1.default.modifiers.raised, variant === 'bordered' && panel_1.default.modifiers.bordered, isScrollable && panel_1.default.modifiers.scrollable, className), ref: innerRef }, props), children));
};
exports.Panel = React.forwardRef((props, ref) => (React.createElement(PanelBase, Object.assign({ innerRef: ref }, props))));
exports.Panel.displayName = 'Panel';
//# sourceMappingURL=Panel.js.map