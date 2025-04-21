import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/TextInputGroup/text-input-group';
import { css } from '@patternfly/react-styles';
export const TextInputGroupContext = React.createContext({
    isDisabled: false
});
export const TextInputGroup = (_a) => {
    var { children, className, isDisabled, innerRef } = _a, props = __rest(_a, ["children", "className", "isDisabled", "innerRef"]);
    const textInputGroupRef = innerRef || React.useRef(null);
    return (React.createElement(TextInputGroupContext.Provider, { value: { isDisabled } },
        React.createElement("div", Object.assign({ ref: textInputGroupRef, className: css(styles.textInputGroup, isDisabled && styles.modifiers.disabled, className) }, props), children)));
};
TextInputGroup.displayName = 'TextInputGroup';
//# sourceMappingURL=TextInputGroup.js.map