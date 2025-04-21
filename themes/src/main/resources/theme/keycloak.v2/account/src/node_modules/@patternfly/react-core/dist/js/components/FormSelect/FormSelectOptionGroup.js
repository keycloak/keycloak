"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.FormSelectOptionGroup = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const FormSelectOptionGroup = (_a) => {
    var { children = null, className = '', isDisabled = false, label } = _a, props = tslib_1.__rest(_a, ["children", "className", "isDisabled", "label"]);
    return (React.createElement("optgroup", Object.assign({}, props, { disabled: !!isDisabled, className: className, label: label }), children));
};
exports.FormSelectOptionGroup = FormSelectOptionGroup;
exports.FormSelectOptionGroup.displayName = 'FormSelectOptionGroup';
//# sourceMappingURL=FormSelectOptionGroup.js.map