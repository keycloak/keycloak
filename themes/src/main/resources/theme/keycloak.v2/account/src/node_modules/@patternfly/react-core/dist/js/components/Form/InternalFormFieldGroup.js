"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.InternalFormFieldGroup = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const form_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Form/form"));
const react_styles_1 = require("@patternfly/react-styles");
const FormFieldGroupToggle_1 = require("./FormFieldGroupToggle");
const helpers_1 = require("../../helpers");
const InternalFormFieldGroup = (_a) => {
    var { children, className, header, isExpandable, isExpanded, onToggle, toggleAriaLabel } = _a, props = tslib_1.__rest(_a, ["children", "className", "header", "isExpandable", "isExpanded", "onToggle", "toggleAriaLabel"]);
    const headerTitleText = header ? header.props.titleText : null;
    if (isExpandable && !toggleAriaLabel && !headerTitleText) {
        // eslint-disable-next-line no-console
        console.error('FormFieldGroupExpandable:', 'toggleAriaLabel or the titleText prop of FormFieldGroupHeader is required to make the toggle button accessible');
    }
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(form_1.default.formFieldGroup, isExpanded && isExpandable && form_1.default.modifiers.expanded, className), role: "group" }, (headerTitleText && { 'aria-labelledby': `${header.props.titleText.id}` }), props),
        isExpandable && (React.createElement(helpers_1.GenerateId, { prefix: "form-field-group-toggle" }, id => (React.createElement(FormFieldGroupToggle_1.FormFieldGroupToggle, Object.assign({ onToggle: onToggle, isExpanded: isExpanded, "aria-label": toggleAriaLabel, toggleId: id }, (headerTitleText && { 'aria-labelledby': `${header.props.titleText.id} ${id}` })))))),
        header && header,
        (!isExpandable || (isExpandable && isExpanded)) && (React.createElement("div", { className: react_styles_1.css(form_1.default.formFieldGroupBody) }, children))));
};
exports.InternalFormFieldGroup = InternalFormFieldGroup;
exports.InternalFormFieldGroup.displayName = 'InternalFormFieldGroup';
//# sourceMappingURL=InternalFormFieldGroup.js.map