import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Form/form';
export const FormHelperText = (_a) => {
    var { children = null, isError = false, isHidden = true, className = '', icon = null, component = 'p' } = _a, props = __rest(_a, ["children", "isError", "isHidden", "className", "icon", "component"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ className: css(styles.formHelperText, isError && styles.modifiers.error, isHidden && styles.modifiers.hidden, className) }, props),
        icon && React.createElement("span", { className: css(styles.formHelperTextIcon) }, icon),
        children));
};
FormHelperText.displayName = 'FormHelperText';
//# sourceMappingURL=FormHelperText.js.map