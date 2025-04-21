"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Sidebar = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const sidebar_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Sidebar/sidebar"));
const Sidebar = (_a) => {
    var { className, children, orientation, isPanelRight = false, hasGutter, hasNoBackground } = _a, props = tslib_1.__rest(_a, ["className", "children", "orientation", "isPanelRight", "hasGutter", "hasNoBackground"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(sidebar_1.default.sidebar, hasGutter && sidebar_1.default.modifiers.gutter, hasNoBackground && sidebar_1.default.modifiers.noBackground, isPanelRight && sidebar_1.default.modifiers.panelRight, sidebar_1.default.modifiers[orientation], className) }, props),
        React.createElement("div", { className: sidebar_1.default.sidebarMain }, children)));
};
exports.Sidebar = Sidebar;
exports.Sidebar.displayName = 'Sidebar';
//# sourceMappingURL=Sidebar.js.map