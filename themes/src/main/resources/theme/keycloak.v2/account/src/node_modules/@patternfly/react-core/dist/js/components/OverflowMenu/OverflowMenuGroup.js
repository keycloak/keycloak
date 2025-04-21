"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.OverflowMenuGroup = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const overflow_menu_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/OverflowMenu/overflow-menu"));
const OverflowMenuContext_1 = require("./OverflowMenuContext");
const OverflowMenuGroup = (_a) => {
    var { className, children, isPersistent = false, groupType } = _a, props = tslib_1.__rest(_a, ["className", "children", "isPersistent", "groupType"]);
    return (React.createElement(OverflowMenuContext_1.OverflowMenuContext.Consumer, null, value => (isPersistent || !value.isBelowBreakpoint) && (React.createElement("div", Object.assign({ className: react_styles_1.css(overflow_menu_1.default.overflowMenuGroup, groupType === 'button' && overflow_menu_1.default.modifiers.buttonGroup, groupType === 'icon' && overflow_menu_1.default.modifiers.iconButtonGroup, className) }, props), children))));
};
exports.OverflowMenuGroup = OverflowMenuGroup;
exports.OverflowMenuGroup.displayName = 'OverflowMenuGroup';
//# sourceMappingURL=OverflowMenuGroup.js.map