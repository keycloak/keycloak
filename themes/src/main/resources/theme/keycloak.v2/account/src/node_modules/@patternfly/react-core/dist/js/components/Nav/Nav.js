"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Nav = exports.NavContext = exports.navContextDefaults = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const nav_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Nav/nav"));
const react_styles_1 = require("@patternfly/react-styles");
const helpers_1 = require("../../helpers");
exports.navContextDefaults = {};
exports.NavContext = React.createContext(exports.navContextDefaults);
class Nav extends React.Component {
    constructor() {
        super(...arguments);
        this.state = {
            isScrollable: false,
            ouiaStateId: helpers_1.getDefaultOUIAId(Nav.displayName, this.props.variant),
            flyoutRef: null
        };
    }
    // Callback from NavItem
    onSelect(event, groupId, itemId, to, preventDefault, onClick) {
        if (preventDefault) {
            event.preventDefault();
        }
        this.props.onSelect({ groupId, itemId, event, to });
        if (onClick) {
            onClick(event, itemId, groupId, to);
        }
    }
    // Callback from NavExpandable
    onToggle(event, groupId, toggleValue) {
        this.props.onToggle({
            event,
            groupId,
            isExpanded: toggleValue
        });
    }
    render() {
        const _a = this.props, { 'aria-label': ariaLabel, children, className, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        onSelect, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        onToggle, theme, ouiaId, ouiaSafe, variant } = _a, props = tslib_1.__rest(_a, ['aria-label', "children", "className", "onSelect", "onToggle", "theme", "ouiaId", "ouiaSafe", "variant"]);
        const isHorizontal = ['horizontal', 'tertiary'].includes(variant);
        return (React.createElement(exports.NavContext.Provider, { value: {
                onSelect: (event, groupId, itemId, to, preventDefault, onClick) => this.onSelect(event, groupId, itemId, to, preventDefault, onClick),
                onToggle: (event, groupId, expanded) => this.onToggle(event, groupId, expanded),
                updateIsScrollable: (isScrollable) => this.setState({ isScrollable }),
                isHorizontal: ['horizontal', 'tertiary', 'horizontal-subnav'].includes(variant),
                flyoutRef: this.state.flyoutRef,
                setFlyoutRef: flyoutRef => this.setState({ flyoutRef })
            } },
            React.createElement("nav", Object.assign({ className: react_styles_1.css(nav_1.default.nav, theme === 'light' && nav_1.default.modifiers.light, isHorizontal && nav_1.default.modifiers.horizontal, variant === 'tertiary' && nav_1.default.modifiers.tertiary, variant === 'horizontal-subnav' && nav_1.default.modifiers.horizontalSubnav, this.state.isScrollable && nav_1.default.modifiers.scrollable, className), "aria-label": ariaLabel || (variant === 'tertiary' ? 'Local' : 'Global') }, helpers_1.getOUIAProps(Nav.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe), props), children)));
    }
}
exports.Nav = Nav;
Nav.displayName = 'Nav';
Nav.defaultProps = {
    onSelect: () => undefined,
    onToggle: () => undefined,
    theme: 'dark',
    ouiaSafe: true
};
//# sourceMappingURL=Nav.js.map