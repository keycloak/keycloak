"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.FormFieldGroup = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const InternalFormFieldGroup_1 = require("./InternalFormFieldGroup");
const FormFieldGroup = (_a) => {
    var { children, className, header } = _a, props = tslib_1.__rest(_a, ["children", "className", "header"]);
    return (React.createElement(InternalFormFieldGroup_1.InternalFormFieldGroup, Object.assign({ className: className, header: header }, props), children));
};
exports.FormFieldGroup = FormFieldGroup;
exports.FormFieldGroup.displayName = 'FormFieldGroup';
//# sourceMappingURL=FormFieldGroup.js.map