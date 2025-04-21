import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/TextInputGroup/text-input-group';
import { css } from '@patternfly/react-styles';
import { TextInputGroupContext } from './TextInputGroup';
export const TextInputGroupMain = (_a) => {
    var { children, className, icon, type = 'text', hint, onChange = () => undefined, onFocus, onBlur, 'aria-label': ariaLabel = 'Type to filter', value: inputValue, placeholder: inputPlaceHolder, innerRef } = _a, props = __rest(_a, ["children", "className", "icon", "type", "hint", "onChange", "onFocus", "onBlur", 'aria-label', "value", "placeholder", "innerRef"]);
    const { isDisabled } = React.useContext(TextInputGroupContext);
    const textInputGroupInputInputRef = innerRef || React.useRef(null);
    const handleChange = (event) => {
        onChange(event.currentTarget.value, event);
    };
    return (React.createElement("div", Object.assign({ className: css(styles.textInputGroupMain, icon && styles.modifiers.icon, className) }, props),
        children,
        React.createElement("span", { className: css(styles.textInputGroupText) },
            hint && (React.createElement("input", { className: css(styles.textInputGroupTextInput, styles.modifiers.hint), type: "text", disabled: true, "aria-hidden": "true", value: hint })),
            icon && React.createElement("span", { className: css(styles.textInputGroupIcon) }, icon),
            React.createElement("input", { ref: textInputGroupInputInputRef, type: type, className: css(styles.textInputGroupTextInput), "aria-label": ariaLabel, disabled: isDisabled, onChange: handleChange, onFocus: onFocus, onBlur: onBlur, value: inputValue || '', placeholder: inputPlaceHolder }))));
};
TextInputGroupMain.displayName = 'TextInputGroupMain';
//# sourceMappingURL=TextInputGroupMain.js.map