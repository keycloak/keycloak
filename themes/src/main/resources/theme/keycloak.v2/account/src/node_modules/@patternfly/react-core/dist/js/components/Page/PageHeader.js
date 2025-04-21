"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PageHeader = void 0;
const tslib_1 = require("tslib");
/* eslint-disable no-console */
const React = tslib_1.__importStar(require("react"));
const page_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Page/page"));
const react_styles_1 = require("@patternfly/react-styles");
const bars_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/bars-icon'));
const Button_1 = require("../../components/Button");
const Page_1 = require("./Page");
const PageHeader = (_a) => {
    var { className = '', logo = null, logoProps = null, logoComponent = 'a', headerTools = null, topNav = null, isNavOpen = true, isManagedSidebar: deprecatedIsManagedSidebar = undefined, role = undefined, showNavToggle = false, onNavToggle = () => undefined, 'aria-label': ariaLabel = 'Global navigation', 'aria-controls': ariaControls = null } = _a, props = tslib_1.__rest(_a, ["className", "logo", "logoProps", "logoComponent", "headerTools", "topNav", "isNavOpen", "isManagedSidebar", "role", "showNavToggle", "onNavToggle", 'aria-label', 'aria-controls']);
    const LogoComponent = logoComponent;
    if ([false, true].includes(deprecatedIsManagedSidebar)) {
        console.warn('isManagedSidebar is deprecated in the PageHeader component. To make the sidebar toggle uncontrolled, pass this prop in the Page component');
    }
    return (React.createElement(Page_1.PageContextConsumer, null, ({ isManagedSidebar, onNavToggle: managedOnNavToggle, isNavOpen: managedIsNavOpen }) => {
        const navToggle = isManagedSidebar ? managedOnNavToggle : onNavToggle;
        const navOpen = isManagedSidebar ? managedIsNavOpen : isNavOpen;
        return (React.createElement("header", Object.assign({ role: role, className: react_styles_1.css(page_1.default.pageHeader, className) }, props),
            (showNavToggle || logo) && (React.createElement("div", { className: react_styles_1.css(page_1.default.pageHeaderBrand) },
                showNavToggle && (React.createElement("div", { className: react_styles_1.css(page_1.default.pageHeaderBrandToggle) },
                    React.createElement(Button_1.Button, { id: "nav-toggle", onClick: navToggle, "aria-label": ariaLabel, "aria-controls": ariaControls, "aria-expanded": navOpen ? 'true' : 'false', variant: Button_1.ButtonVariant.plain },
                        React.createElement(bars_icon_1.default, null)))),
                logo && (React.createElement(LogoComponent, Object.assign({ className: react_styles_1.css(page_1.default.pageHeaderBrandLink) }, logoProps), logo)))),
            topNav && React.createElement("div", { className: react_styles_1.css(page_1.default.pageHeaderNav) }, topNav),
            headerTools));
    }));
};
exports.PageHeader = PageHeader;
exports.PageHeader.displayName = 'PageHeader';
//# sourceMappingURL=PageHeader.js.map