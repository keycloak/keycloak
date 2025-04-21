"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PageToggleButton = void 0;
const tslib_1 = require("tslib");
/* eslint-disable no-console */
const React = tslib_1.__importStar(require("react"));
const Button_1 = require("../../components/Button");
const Page_1 = require("./Page");
const PageToggleButton = (_a) => {
    var { children, isNavOpen = true, onNavToggle = () => undefined } = _a, props = tslib_1.__rest(_a, ["children", "isNavOpen", "onNavToggle"]);
    return (React.createElement(Page_1.PageContextConsumer, null, ({ isManagedSidebar, onNavToggle: managedOnNavToggle, isNavOpen: managedIsNavOpen }) => {
        const navToggle = isManagedSidebar ? managedOnNavToggle : onNavToggle;
        const navOpen = isManagedSidebar ? managedIsNavOpen : isNavOpen;
        return (React.createElement(Button_1.Button, Object.assign({ id: "nav-toggle", onClick: navToggle, "aria-label": "Side navigation toggle", "aria-expanded": navOpen ? 'true' : 'false', variant: Button_1.ButtonVariant.plain }, props), children));
    }));
};
exports.PageToggleButton = PageToggleButton;
exports.PageToggleButton.displayName = 'PageToggleButton';
//# sourceMappingURL=PageToggleButton.js.map