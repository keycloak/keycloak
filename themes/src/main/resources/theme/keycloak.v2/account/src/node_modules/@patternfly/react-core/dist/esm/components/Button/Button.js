import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Button/button';
import { css } from '@patternfly/react-styles';
import { Spinner, spinnerSize } from '../Spinner';
import { useOUIAProps } from '../../helpers';
export var ButtonVariant;
(function (ButtonVariant) {
    ButtonVariant["primary"] = "primary";
    ButtonVariant["secondary"] = "secondary";
    ButtonVariant["tertiary"] = "tertiary";
    ButtonVariant["danger"] = "danger";
    ButtonVariant["warning"] = "warning";
    ButtonVariant["link"] = "link";
    ButtonVariant["plain"] = "plain";
    ButtonVariant["control"] = "control";
})(ButtonVariant || (ButtonVariant = {}));
export var ButtonType;
(function (ButtonType) {
    ButtonType["button"] = "button";
    ButtonType["submit"] = "submit";
    ButtonType["reset"] = "reset";
})(ButtonType || (ButtonType = {}));
const ButtonBase = (_a) => {
    var { children = null, className = '', component = 'button', isActive = false, isBlock = false, isDisabled = false, isAriaDisabled = false, isLoading = null, isDanger = false, spinnerAriaValueText, spinnerAriaLabelledBy, spinnerAriaLabel, isSmall = false, isLarge = false, inoperableEvents = ['onClick', 'onKeyPress'], isInline = false, type = ButtonType.button, variant = ButtonVariant.primary, iconPosition = 'left', 'aria-label': ariaLabel = null, icon = null, ouiaId, ouiaSafe = true, tabIndex = null, innerRef } = _a, props = __rest(_a, ["children", "className", "component", "isActive", "isBlock", "isDisabled", "isAriaDisabled", "isLoading", "isDanger", "spinnerAriaValueText", "spinnerAriaLabelledBy", "spinnerAriaLabel", "isSmall", "isLarge", "inoperableEvents", "isInline", "type", "variant", "iconPosition", 'aria-label', "icon", "ouiaId", "ouiaSafe", "tabIndex", "innerRef"]);
    const ouiaProps = useOUIAProps(Button.displayName, ouiaId, ouiaSafe, variant);
    const Component = component;
    const isButtonElement = Component === 'button';
    const isInlineSpan = isInline && Component === 'span';
    const preventedEvents = inoperableEvents.reduce((handlers, eventToPrevent) => (Object.assign(Object.assign({}, handlers), { [eventToPrevent]: (event) => {
            event.preventDefault();
        } })), {});
    const getDefaultTabIdx = () => {
        if (isDisabled) {
            return isButtonElement ? null : -1;
        }
        else if (isAriaDisabled) {
            return null;
        }
        else if (isInlineSpan) {
            return 0;
        }
    };
    return (React.createElement(Component, Object.assign({}, props, (isAriaDisabled ? preventedEvents : null), { "aria-disabled": isDisabled || isAriaDisabled, "aria-label": ariaLabel, className: css(styles.button, styles.modifiers[variant], isBlock && styles.modifiers.block, isDisabled && styles.modifiers.disabled, isAriaDisabled && styles.modifiers.ariaDisabled, isActive && styles.modifiers.active, isInline && variant === ButtonVariant.link && styles.modifiers.inline, isDanger && (variant === ButtonVariant.secondary || variant === ButtonVariant.link) && styles.modifiers.danger, isLoading !== null && children !== null && styles.modifiers.progress, isLoading && styles.modifiers.inProgress, isSmall && styles.modifiers.small, isLarge && styles.modifiers.displayLg, className), disabled: isButtonElement ? isDisabled : null, tabIndex: tabIndex !== null ? tabIndex : getDefaultTabIdx(), type: isButtonElement || isInlineSpan ? type : null, role: isInlineSpan ? 'button' : null, ref: innerRef }, ouiaProps),
        isLoading && (React.createElement("span", { className: css(styles.buttonProgress) },
            React.createElement(Spinner, { size: spinnerSize.md, "aria-valuetext": spinnerAriaValueText, "aria-label": spinnerAriaLabel, "aria-labelledby": spinnerAriaLabelledBy }))),
        variant === ButtonVariant.plain && children === null && icon ? icon : null,
        variant !== ButtonVariant.plain && icon && iconPosition === 'left' && (React.createElement("span", { className: css(styles.buttonIcon, styles.modifiers.start) }, icon)),
        children,
        variant !== ButtonVariant.plain && icon && iconPosition === 'right' && (React.createElement("span", { className: css(styles.buttonIcon, styles.modifiers.end) }, icon))));
};
export const Button = React.forwardRef((props, ref) => (React.createElement(ButtonBase, Object.assign({ innerRef: ref }, props))));
Button.displayName = 'Button';
//# sourceMappingURL=Button.js.map