"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PageSidebar = exports.PageSidebarContext = exports.pageSidebarContextDefaults = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const page_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Page/page"));
const react_styles_1 = require("@patternfly/react-styles");
const Page_1 = require("./Page");
exports.pageSidebarContextDefaults = {
    isNavOpen: true
};
exports.PageSidebarContext = React.createContext(exports.pageSidebarContextDefaults);
const PageSidebar = (_a) => {
    var { className = '', nav, isNavOpen = true, theme = 'dark' } = _a, props = tslib_1.__rest(_a, ["className", "nav", "isNavOpen", "theme"]);
    return (React.createElement(Page_1.PageContextConsumer, null, ({ isManagedSidebar, isNavOpen: managedIsNavOpen }) => {
        const navOpen = isManagedSidebar ? managedIsNavOpen : isNavOpen;
        return (React.createElement("div", Object.assign({ id: "page-sidebar", className: react_styles_1.css(page_1.default.pageSidebar, theme === 'light' && page_1.default.modifiers.light, navOpen && page_1.default.modifiers.expanded, !navOpen && page_1.default.modifiers.collapsed, className), "aria-hidden": !navOpen }, props),
            React.createElement("div", { className: page_1.default.pageSidebarBody },
                React.createElement(exports.PageSidebarContext.Provider, { value: { isNavOpen: navOpen } }, nav))));
    }));
};
exports.PageSidebar = PageSidebar;
exports.PageSidebar.displayName = 'PageSidebar';
//# sourceMappingURL=PageSidebar.js.map