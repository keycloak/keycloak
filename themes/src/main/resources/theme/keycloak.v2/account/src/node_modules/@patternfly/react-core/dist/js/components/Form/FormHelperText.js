"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.FormHelperText = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const form_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Form/form"));
const FormHelperText = (_a) => {
    var { children = null, isError = false, isHidden = true, className = '', icon = null, component = 'p' } = _a, props = tslib_1.__rest(_a, ["children", "isError", "isHidden", "className", "icon", "component"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ className: react_styles_1.css(form_1.default.formHelperText, isError && form_1.default.modifiers.error, isHidden && form_1.default.modifiers.hidden, className) }, props),
        icon && React.createElement("span", { className: react_styles_1.css(form_1.default.formHelperTextIcon) }, icon),
        children));
};
exports.FormHelperText = FormHelperText;
exports.FormHelperText.displayName = 'FormHelperText';
//# sourceMappingURL=FormHelperText.js.map