import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
export const WizardNavItem = (_a) => {
    var { children = null, content = '', isCurrent = false, isDisabled = false, step, onNavItemClick = () => undefined, navItemComponent = 'button', href = null, isExpandable = false, id } = _a, rest = __rest(_a, ["children", "content", "isCurrent", "isDisabled", "step", "onNavItemClick", "navItemComponent", "href", "isExpandable", "id"]);
    const NavItemComponent = navItemComponent;
    const [isExpanded, setIsExpanded] = React.useState(false);
    React.useEffect(() => {
        setIsExpanded(isCurrent);
    }, [isCurrent]);
    if (navItemComponent === 'a' && !href && process.env.NODE_ENV !== 'production') {
        // eslint-disable-next-line no-console
        console.error('WizardNavItem: When using an anchor, please provide an href');
    }
    const btnProps = {
        disabled: isDisabled
    };
    const linkProps = {
        tabIndex: isDisabled ? -1 : undefined,
        href
    };
    return (React.createElement("li", { className: css(styles.wizardNavItem, isExpandable && styles.modifiers.expandable, isExpandable && isExpanded && styles.modifiers.expanded) },
        React.createElement(NavItemComponent, Object.assign({}, rest, (navItemComponent === 'a' ? Object.assign({}, linkProps) : Object.assign({}, btnProps)), (id && { id: id.toString() }), { onClick: () => (isExpandable ? setIsExpanded(!isExpanded || isCurrent) : onNavItemClick(step)), className: css(styles.wizardNavLink, isCurrent && styles.modifiers.current, isDisabled && styles.modifiers.disabled), "aria-disabled": isDisabled ? true : null, "aria-current": isCurrent && !children ? 'step' : false }, (isExpandable && { 'aria-expanded': isExpanded })), isExpandable ? (React.createElement(React.Fragment, null,
            React.createElement("span", { className: css(styles.wizardNavLinkText) }, content),
            React.createElement("span", { className: css(styles.wizardNavLinkToggle) },
                React.createElement("span", { className: css(styles.wizardNavLinkToggleIcon) },
                    React.createElement(AngleRightIcon, null))))) : (content)),
        children));
};
WizardNavItem.displayName = 'WizardNavItem';
//# sourceMappingURL=WizardNavItem.js.map