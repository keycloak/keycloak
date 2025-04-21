"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.OverflowMenuControl = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const overflow_menu_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/OverflowMenu/overflow-menu"));
const OverflowMenuContext_1 = require("./OverflowMenuContext");
const OverflowMenuControl = (_a) => {
    var { className, children, hasAdditionalOptions } = _a, props = tslib_1.__rest(_a, ["className", "children", "hasAdditionalOptions"]);
    return (React.createElement(OverflowMenuContext_1.OverflowMenuContext.Consumer, null, value => (value.isBelowBreakpoint || hasAdditionalOptions) && (React.createElement("div", Object.assign({ className: react_styles_1.css(overflow_menu_1.default.overflowMenuControl, className) }, props),
        ' ',
        children,
        ' '))));
};
exports.OverflowMenuControl = OverflowMenuControl;
exports.OverflowMenuControl.displayName = 'OverflowMenuControl';
//# sourceMappingURL=OverflowMenuControl.js.map