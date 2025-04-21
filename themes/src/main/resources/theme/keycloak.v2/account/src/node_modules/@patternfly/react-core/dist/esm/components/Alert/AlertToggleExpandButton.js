import { __rest } from "tslib";
import * as React from 'react';
import { Button, ButtonVariant } from '../Button';
import { AlertContext } from './AlertContext';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Alert/alert';
export const AlertToggleExpandButton = (_a) => {
    var { 'aria-label': ariaLabel, variantLabel, onToggleExpand, isExpanded } = _a, props = __rest(_a, ['aria-label', "variantLabel", "onToggleExpand", "isExpanded"]);
    const { title, variantLabel: alertVariantLabel } = React.useContext(AlertContext);
    return (React.createElement(Button, Object.assign({ variant: ButtonVariant.plain, onClick: onToggleExpand, "aria-expanded": isExpanded, "aria-label": ariaLabel === '' ? `Toggle ${variantLabel || alertVariantLabel} alert: ${title}` : ariaLabel }, props),
        React.createElement("span", { className: css(styles.alertToggleIcon) },
            React.createElement(AngleRightIcon, { "aria-hidden": "true" }))));
};
AlertToggleExpandButton.displayName = 'AlertToggleExpandButton';
//# sourceMappingURL=AlertToggleExpandButton.js.map