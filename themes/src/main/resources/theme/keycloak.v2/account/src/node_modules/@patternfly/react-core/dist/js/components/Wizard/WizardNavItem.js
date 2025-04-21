"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.WizardNavItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const wizard_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Wizard/wizard"));
const angle_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-right-icon'));
const WizardNavItem = (_a) => {
    var { children = null, content = '', isCurrent = false, isDisabled = false, step, onNavItemClick = () => undefined, navItemComponent = 'button', href = null, isExpandable = false, id } = _a, rest = tslib_1.__rest(_a, ["children", "content", "isCurrent", "isDisabled", "step", "onNavItemClick", "navItemComponent", "href", "isExpandable", "id"]);
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
    return (React.createElement("li", { className: react_styles_1.css(wizard_1.default.wizardNavItem, isExpandable && wizard_1.default.modifiers.expandable, isExpandable && isExpanded && wizard_1.default.modifiers.expanded) },
        React.createElement(NavItemComponent, Object.assign({}, rest, (navItemComponent === 'a' ? Object.assign({}, linkProps) : Object.assign({}, btnProps)), (id && { id: id.toString() }), { onClick: () => (isExpandable ? setIsExpanded(!isExpanded || isCurrent) : onNavItemClick(step)), className: react_styles_1.css(wizard_1.default.wizardNavLink, isCurrent && wizard_1.default.modifiers.current, isDisabled && wizard_1.default.modifiers.disabled), "aria-disabled": isDisabled ? true : null, "aria-current": isCurrent && !children ? 'step' : false }, (isExpandable && { 'aria-expanded': isExpanded })), isExpandable ? (React.createElement(React.Fragment, null,
            React.createElement("span", { className: react_styles_1.css(wizard_1.default.wizardNavLinkText) }, content),
            React.createElement("span", { className: react_styles_1.css(wizard_1.default.wizardNavLinkToggle) },
                React.createElement("span", { className: react_styles_1.css(wizard_1.default.wizardNavLinkToggleIcon) },
                    React.createElement(angle_right_icon_1.default, null))))) : (content)),
        children));
};
exports.WizardNavItem = WizardNavItem;
exports.WizardNavItem.displayName = 'WizardNavItem';
//# sourceMappingURL=WizardNavItem.js.map