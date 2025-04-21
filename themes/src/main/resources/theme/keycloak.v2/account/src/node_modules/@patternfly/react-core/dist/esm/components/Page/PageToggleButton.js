import { __rest } from "tslib";
/* eslint-disable no-console */
import * as React from 'react';
import { Button, ButtonVariant } from '../../components/Button';
import { PageContextConsumer } from './Page';
export const PageToggleButton = (_a) => {
    var { children, isNavOpen = true, onNavToggle = () => undefined } = _a, props = __rest(_a, ["children", "isNavOpen", "onNavToggle"]);
    return (React.createElement(PageContextConsumer, null, ({ isManagedSidebar, onNavToggle: managedOnNavToggle, isNavOpen: managedIsNavOpen }) => {
        const navToggle = isManagedSidebar ? managedOnNavToggle : onNavToggle;
        const navOpen = isManagedSidebar ? managedIsNavOpen : isNavOpen;
        return (React.createElement(Button, Object.assign({ id: "nav-toggle", onClick: navToggle, "aria-label": "Side navigation toggle", "aria-expanded": navOpen ? 'true' : 'false', variant: ButtonVariant.plain }, props), children));
    }));
};
PageToggleButton.displayName = 'PageToggleButton';
//# sourceMappingURL=PageToggleButton.js.map