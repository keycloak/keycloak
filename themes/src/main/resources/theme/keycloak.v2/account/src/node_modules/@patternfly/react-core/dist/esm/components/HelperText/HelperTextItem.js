import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/HelperText/helper-text';
import { css } from '@patternfly/react-styles';
import MinusIcon from '@patternfly/react-icons/dist/esm/icons/minus-icon';
import ExclamationTriangleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-triangle-icon';
import CheckCircleIcon from '@patternfly/react-icons/dist/esm/icons/check-circle-icon';
import ExclamationCircleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-circle-icon';
const variantStyle = {
    default: '',
    indeterminate: styles.modifiers.indeterminate,
    warning: styles.modifiers.warning,
    success: styles.modifiers.success,
    error: styles.modifiers.error
};
export const HelperTextItem = (_a) => {
    var { children, className, component = 'div', variant = 'default', icon, isDynamic = false, hasIcon = isDynamic, id, screenReaderText = `${variant} status` } = _a, props = __rest(_a, ["children", "className", "component", "variant", "icon", "isDynamic", "hasIcon", "id", "screenReaderText"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ className: css(styles.helperTextItem, variantStyle[variant], isDynamic && styles.modifiers.dynamic, className), id: id }, props),
        icon && (React.createElement("span", { className: css(styles.helperTextItemIcon), "aria-hidden": true }, icon)),
        hasIcon && !icon && (React.createElement("span", { className: css(styles.helperTextItemIcon), "aria-hidden": true },
            (variant === 'default' || variant === 'indeterminate') && React.createElement(MinusIcon, null),
            variant === 'warning' && React.createElement(ExclamationTriangleIcon, null),
            variant === 'success' && React.createElement(CheckCircleIcon, null),
            variant === 'error' && React.createElement(ExclamationCircleIcon, null))),
        React.createElement("span", { className: css(styles.helperTextItemText) },
            children,
            isDynamic && React.createElement("span", { className: "pf-u-screen-reader" },
                ": ",
                screenReaderText,
                ";"))));
};
HelperTextItem.displayName = 'HelperTextItem';
//# sourceMappingURL=HelperTextItem.js.map