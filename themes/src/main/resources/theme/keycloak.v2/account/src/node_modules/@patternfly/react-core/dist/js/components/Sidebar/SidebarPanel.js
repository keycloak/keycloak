"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SidebarPanel = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const sidebar_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Sidebar/sidebar"));
const util_1 = require("../../helpers/util");
const SidebarPanel = (_a) => {
    var { className, children, variant = 'default', hasNoBackground, width } = _a, props = tslib_1.__rest(_a, ["className", "children", "variant", "hasNoBackground", "width"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(sidebar_1.default.sidebarPanel, variant !== 'default' && sidebar_1.default.modifiers[variant], hasNoBackground && sidebar_1.default.modifiers.noBackground, util_1.formatBreakpointMods(width, sidebar_1.default), className) }, props), children));
};
exports.SidebarPanel = SidebarPanel;
exports.SidebarPanel.displayName = 'SidebarPanel';
//# sourceMappingURL=SidebarPanel.js.map