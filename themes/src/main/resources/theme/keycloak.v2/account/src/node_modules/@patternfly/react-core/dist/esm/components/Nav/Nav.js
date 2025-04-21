import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Nav/nav';
import { css } from '@patternfly/react-styles';
import { getOUIAProps, getDefaultOUIAId } from '../../helpers';
export const navContextDefaults = {};
export const NavContext = React.createContext(navContextDefaults);
export class Nav extends React.Component {
    constructor() {
        super(...arguments);
        this.state = {
            isScrollable: false,
            ouiaStateId: getDefaultOUIAId(Nav.displayName, this.props.variant),
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
        onToggle, theme, ouiaId, ouiaSafe, variant } = _a, props = __rest(_a, ['aria-label', "children", "className", "onSelect", "onToggle", "theme", "ouiaId", "ouiaSafe", "variant"]);
        const isHorizontal = ['horizontal', 'tertiary'].includes(variant);
        return (React.createElement(NavContext.Provider, { value: {
                onSelect: (event, groupId, itemId, to, preventDefault, onClick) => this.onSelect(event, groupId, itemId, to, preventDefault, onClick),
                onToggle: (event, groupId, expanded) => this.onToggle(event, groupId, expanded),
                updateIsScrollable: (isScrollable) => this.setState({ isScrollable }),
                isHorizontal: ['horizontal', 'tertiary', 'horizontal-subnav'].includes(variant),
                flyoutRef: this.state.flyoutRef,
                setFlyoutRef: flyoutRef => this.setState({ flyoutRef })
            } },
            React.createElement("nav", Object.assign({ className: css(styles.nav, theme === 'light' && styles.modifiers.light, isHorizontal && styles.modifiers.horizontal, variant === 'tertiary' && styles.modifiers.tertiary, variant === 'horizontal-subnav' && styles.modifiers.horizontalSubnav, this.state.isScrollable && styles.modifiers.scrollable, className), "aria-label": ariaLabel || (variant === 'tertiary' ? 'Local' : 'Global') }, getOUIAProps(Nav.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe), props), children)));
    }
}
Nav.displayName = 'Nav';
Nav.defaultProps = {
    onSelect: () => undefined,
    onToggle: () => undefined,
    theme: 'dark',
    ouiaSafe: true
};
//# sourceMappingURL=Nav.js.map