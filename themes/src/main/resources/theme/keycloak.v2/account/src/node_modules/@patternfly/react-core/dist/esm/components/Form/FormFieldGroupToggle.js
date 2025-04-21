import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Form/form';
import { css } from '@patternfly/react-styles';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import { Button } from '../Button';
export const FormFieldGroupToggle = (_a) => {
    var { className, onToggle, isExpanded, 'aria-label': ariaLabel, 'aria-labelledby': ariaLabelledby, toggleId } = _a, props = __rest(_a, ["className", "onToggle", "isExpanded", 'aria-label', 'aria-labelledby', "toggleId"]);
    return (React.createElement("div", Object.assign({ className: css(styles.formFieldGroupToggle, className) }, props),
        React.createElement("div", { className: css(styles.formFieldGroupToggleButton) },
            React.createElement(Button, { variant: "plain", "aria-label": ariaLabel, onClick: onToggle, "aria-expanded": isExpanded, "aria-labelledby": ariaLabelledby, id: toggleId },
                React.createElement("span", { className: css(styles.formFieldGroupToggleIcon) },
                    React.createElement(AngleRightIcon, { "aria-hidden": "true" }))))));
};
FormFieldGroupToggle.displayName = 'FormFieldGroupToggle';
//# sourceMappingURL=FormFieldGroupToggle.js.map