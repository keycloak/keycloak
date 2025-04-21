"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.HelperTextItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const helper_text_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/HelperText/helper-text"));
const react_styles_1 = require("@patternfly/react-styles");
const minus_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/minus-icon'));
const exclamation_triangle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/exclamation-triangle-icon'));
const check_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/check-circle-icon'));
const exclamation_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/exclamation-circle-icon'));
const variantStyle = {
    default: '',
    indeterminate: helper_text_1.default.modifiers.indeterminate,
    warning: helper_text_1.default.modifiers.warning,
    success: helper_text_1.default.modifiers.success,
    error: helper_text_1.default.modifiers.error
};
const HelperTextItem = (_a) => {
    var { children, className, component = 'div', variant = 'default', icon, isDynamic = false, hasIcon = isDynamic, id, screenReaderText = `${variant} status` } = _a, props = tslib_1.__rest(_a, ["children", "className", "component", "variant", "icon", "isDynamic", "hasIcon", "id", "screenReaderText"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ className: react_styles_1.css(helper_text_1.default.helperTextItem, variantStyle[variant], isDynamic && helper_text_1.default.modifiers.dynamic, className), id: id }, props),
        icon && (React.createElement("span", { className: react_styles_1.css(helper_text_1.default.helperTextItemIcon), "aria-hidden": true }, icon)),
        hasIcon && !icon && (React.createElement("span", { className: react_styles_1.css(helper_text_1.default.helperTextItemIcon), "aria-hidden": true },
            (variant === 'default' || variant === 'indeterminate') && React.createElement(minus_icon_1.default, null),
            variant === 'warning' && React.createElement(exclamation_triangle_icon_1.default, null),
            variant === 'success' && React.createElement(check_circle_icon_1.default, null),
            variant === 'error' && React.createElement(exclamation_circle_icon_1.default, null))),
        React.createElement("span", { className: react_styles_1.css(helper_text_1.default.helperTextItemText) },
            children,
            isDynamic && React.createElement("span", { className: "pf-u-screen-reader" },
                ": ",
                screenReaderText,
                ";"))));
};
exports.HelperTextItem = HelperTextItem;
exports.HelperTextItem.displayName = 'HelperTextItem';
//# sourceMappingURL=HelperTextItem.js.map