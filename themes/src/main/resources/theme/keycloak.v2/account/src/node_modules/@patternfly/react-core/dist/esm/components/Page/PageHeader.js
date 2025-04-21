import { __rest } from "tslib";
/* eslint-disable no-console */
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Page/page';
import { css } from '@patternfly/react-styles';
import BarsIcon from '@patternfly/react-icons/dist/esm/icons/bars-icon';
import { Button, ButtonVariant } from '../../components/Button';
import { PageContextConsumer } from './Page';
export const PageHeader = (_a) => {
    var { className = '', logo = null, logoProps = null, logoComponent = 'a', headerTools = null, topNav = null, isNavOpen = true, isManagedSidebar: deprecatedIsManagedSidebar = undefined, role = undefined, showNavToggle = false, onNavToggle = () => undefined, 'aria-label': ariaLabel = 'Global navigation', 'aria-controls': ariaControls = null } = _a, props = __rest(_a, ["className", "logo", "logoProps", "logoComponent", "headerTools", "topNav", "isNavOpen", "isManagedSidebar", "role", "showNavToggle", "onNavToggle", 'aria-label', 'aria-controls']);
    const LogoComponent = logoComponent;
    if ([false, true].includes(deprecatedIsManagedSidebar)) {
        console.warn('isManagedSidebar is deprecated in the PageHeader component. To make the sidebar toggle uncontrolled, pass this prop in the Page component');
    }
    return (React.createElement(PageContextConsumer, null, ({ isManagedSidebar, onNavToggle: managedOnNavToggle, isNavOpen: managedIsNavOpen }) => {
        const navToggle = isManagedSidebar ? managedOnNavToggle : onNavToggle;
        const navOpen = isManagedSidebar ? managedIsNavOpen : isNavOpen;
        return (React.createElement("header", Object.assign({ role: role, className: css(styles.pageHeader, className) }, props),
            (showNavToggle || logo) && (React.createElement("div", { className: css(styles.pageHeaderBrand) },
                showNavToggle && (React.createElement("div", { className: css(styles.pageHeaderBrandToggle) },
                    React.createElement(Button, { id: "nav-toggle", onClick: navToggle, "aria-label": ariaLabel, "aria-controls": ariaControls, "aria-expanded": navOpen ? 'true' : 'false', variant: ButtonVariant.plain },
                        React.createElement(BarsIcon, null)))),
                logo && (React.createElement(LogoComponent, Object.assign({ className: css(styles.pageHeaderBrandLink) }, logoProps), logo)))),
            topNav && React.createElement("div", { className: css(styles.pageHeaderNav) }, topNav),
            headerTools));
    }));
};
PageHeader.displayName = 'PageHeader';
//# sourceMappingURL=PageHeader.js.map