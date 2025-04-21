import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/MenuToggle/menu-toggle';
import { css } from '@patternfly/react-styles';
import CaretDownIcon from '@patternfly/react-icons/dist/esm/icons/caret-down-icon';
export class MenuToggleBase extends React.Component {
    constructor() {
        super(...arguments);
        this.displayName = 'MenuToggleBase';
    }
    render() {
        const _a = this.props, { children, className, icon, badge, isExpanded, isDisabled, isFullHeight, isFullWidth, variant, innerRef } = _a, props = __rest(_a, ["children", "className", "icon", "badge", "isExpanded", "isDisabled", "isFullHeight", "isFullWidth", "variant", "innerRef"]);
        const isPlain = variant === 'plain';
        const isPlainText = variant === 'plainText';
        const content = (React.createElement(React.Fragment, null,
            icon && React.createElement("span", { className: css(styles.menuToggleIcon) }, icon),
            React.createElement("span", { className: "pf-c-menu-toggle__text" }, children),
            badge && React.createElement("span", { className: css(styles.menuToggleCount) }, badge),
            React.createElement("span", { className: css(styles.menuToggleControls) },
                React.createElement("span", { className: css(styles.menuToggleToggleIcon) },
                    React.createElement(CaretDownIcon, { "aria-hidden": true })))));
        return (React.createElement("button", Object.assign({ className: css(styles.menuToggle, isExpanded && styles.modifiers.expanded, variant === 'primary' && styles.modifiers.primary, variant === 'secondary' && styles.modifiers.secondary, (isPlain || isPlainText) && styles.modifiers.plain, isPlainText && styles.modifiers.text, isFullHeight && styles.modifiers.fullHeight, isFullWidth && styles.modifiers.fullWidth, className), type: "button", "aria-expanded": false, ref: innerRef }, (isExpanded && { 'aria-expanded': true }), (isDisabled && { disabled: true }), props),
            isPlain && children,
            !isPlain && content));
    }
}
MenuToggleBase.defaultProps = {
    className: '',
    isExpanded: false,
    isDisabled: false,
    isFullWidth: false,
    isFullHeight: false,
    variant: 'default'
};
export const MenuToggle = React.forwardRef((props, ref) => (React.createElement(MenuToggleBase, Object.assign({ innerRef: ref }, props))));
MenuToggle.displayName = 'MenuToggle';
//# sourceMappingURL=MenuToggle.js.map