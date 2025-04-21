import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Alert/alert';
import CheckCircleIcon from '@patternfly/react-icons/dist/esm/icons/check-circle-icon';
import ExclamationCircleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-circle-icon';
import ExclamationTriangleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-triangle-icon';
import InfoCircleIcon from '@patternfly/react-icons/dist/esm/icons/info-circle-icon';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';
export const variantIcons = {
    success: CheckCircleIcon,
    danger: ExclamationCircleIcon,
    warning: ExclamationTriangleIcon,
    info: InfoCircleIcon,
    default: BellIcon
};
export const AlertIcon = (_a) => {
    var { variant, customIcon, className = '' } = _a, props = __rest(_a, ["variant", "customIcon", "className"]);
    const Icon = variantIcons[variant];
    return (React.createElement("div", Object.assign({}, props, { className: css(styles.alertIcon, className) }), customIcon || React.createElement(Icon, null)));
};
//# sourceMappingURL=AlertIcon.js.map