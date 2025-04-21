"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.FormFieldGroupExpandable = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_1 = require("react");
const InternalFormFieldGroup_1 = require("./InternalFormFieldGroup");
const FormFieldGroupExpandable = (_a) => {
    var { children, className, header, isExpanded = false, toggleAriaLabel } = _a, props = tslib_1.__rest(_a, ["children", "className", "header", "isExpanded", "toggleAriaLabel"]);
    const [localIsExpanded, setIsExpanded] = react_1.useState(isExpanded);
    return (React.createElement(InternalFormFieldGroup_1.InternalFormFieldGroup, Object.assign({ className: className, header: header, isExpandable: true, isExpanded: localIsExpanded, toggleAriaLabel: toggleAriaLabel, onToggle: () => setIsExpanded(!localIsExpanded) }, props), children));
};
exports.FormFieldGroupExpandable = FormFieldGroupExpandable;
exports.FormFieldGroupExpandable.displayName = 'FormFieldGroupExpandable';
//# sourceMappingURL=FormFieldGroupExpandable.js.map