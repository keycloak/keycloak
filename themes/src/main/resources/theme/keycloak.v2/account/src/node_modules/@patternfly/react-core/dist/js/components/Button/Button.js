"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Button = exports.ButtonType = exports.ButtonVariant = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const button_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Button/button"));
const react_styles_1 = require("@patternfly/react-styles");
const Spinner_1 = require("../Spinner");
const helpers_1 = require("../../helpers");
var ButtonVariant;
(function (ButtonVariant) {
    ButtonVariant["primary"] = "primary";
    ButtonVariant["secondary"] = "secondary";
    ButtonVariant["tertiary"] = "tertiary";
    ButtonVariant["danger"] = "danger";
    ButtonVariant["warning"] = "warning";
    ButtonVariant["link"] = "link";
    ButtonVariant["plain"] = "plain";
    ButtonVariant["control"] = "control";
})(ButtonVariant = exports.ButtonVariant || (exports.ButtonVariant = {}));
var ButtonType;
(function (ButtonType) {
    ButtonType["button"] = "button";
    ButtonType["submit"] = "submit";
    ButtonType["reset"] = "reset";
})(ButtonType = exports.ButtonType || (exports.ButtonType = {}));
const ButtonBase = (_a) => {
    var { children = null, className = '', component = 'button', isActive = false, isBlock = false, isDisabled = false, isAriaDisabled = false, isLoading = null, isDanger = false, spinnerAriaValueText, spinnerAriaLabelledBy, spinnerAriaLabel, isSmall = false, isLarge = false, inoperableEvents = ['onClick', 'onKeyPress'], isInline = false, type = ButtonType.button, variant = ButtonVariant.primary, iconPosition = 'left', 'aria-label': ariaLabel = null, icon = null, ouiaId, ouiaSafe = true, tabIndex = null, innerRef } = _a, props = tslib_1.__rest(_a, ["children", "className", "component", "isActive", "isBlock", "isDisabled", "isAriaDisabled", "isLoading", "isDanger", "spinnerAriaValueText", "spinnerAriaLabelledBy", "spinnerAriaLabel", "isSmall", "isLarge", "inoperableEvents", "isInline", "type", "variant", "iconPosition", 'aria-label', "icon", "ouiaId", "ouiaSafe", "tabIndex", "innerRef"]);
    const ouiaProps = helpers_1.useOUIAProps(exports.Button.displayName, ouiaId, ouiaSafe, variant);
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
    return (React.createElement(Component, Object.assign({}, props, (isAriaDisabled ? preventedEvents : null), { "aria-disabled": isDisabled || isAriaDisabled, "aria-label": ariaLabel, className: react_styles_1.css(button_1.default.button, button_1.default.modifiers[variant], isBlock && button_1.default.modifiers.block, isDisabled && button_1.default.modifiers.disabled, isAriaDisabled && button_1.default.modifiers.ariaDisabled, isActive && button_1.default.modifiers.active, isInline && variant === ButtonVariant.link && button_1.default.modifiers.inline, isDanger && (variant === ButtonVariant.secondary || variant === ButtonVariant.link) && button_1.default.modifiers.danger, isLoading !== null && children !== null && button_1.default.modifiers.progress, isLoading && button_1.default.modifiers.inProgress, isSmall && button_1.default.modifiers.small, isLarge && button_1.default.modifiers.displayLg, className), disabled: isButtonElement ? isDisabled : null, tabIndex: tabIndex !== null ? tabIndex : getDefaultTabIdx(), type: isButtonElement || isInlineSpan ? type : null, role: isInlineSpan ? 'button' : null, ref: innerRef }, ouiaProps),
        isLoading && (React.createElement("span", { className: react_styles_1.css(button_1.default.buttonProgress) },
            React.createElement(Spinner_1.Spinner, { size: Spinner_1.spinnerSize.md, "aria-valuetext": spinnerAriaValueText, "aria-label": spinnerAriaLabel, "aria-labelledby": spinnerAriaLabelledBy }))),
        variant === ButtonVariant.plain && children === null && icon ? icon : null,
        variant !== ButtonVariant.plain && icon && iconPosition === 'left' && (React.createElement("span", { className: react_styles_1.css(button_1.default.buttonIcon, button_1.default.modifiers.start) }, icon)),
        children,
        variant !== ButtonVariant.plain && icon && iconPosition === 'right' && (React.createElement("span", { className: react_styles_1.css(button_1.default.buttonIcon, button_1.default.modifiers.end) }, icon))));
};
exports.Button = React.forwardRef((props, ref) => (React.createElement(ButtonBase, Object.assign({ innerRef: ref }, props))));
exports.Button.displayName = 'Button';
//# sourceMappingURL=Button.js.map