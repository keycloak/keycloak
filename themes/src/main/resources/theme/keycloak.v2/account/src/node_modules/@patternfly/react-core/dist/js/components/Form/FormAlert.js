"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.FormAlert = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const FormAlert = (_a) => {
    var { children = null, className = '' } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (
    // There are currently no associated styles with the pf-c-form_alert class.
    // Therefore, it does not exist in react-styles
    React.createElement("div", Object.assign({}, props, { className: react_styles_1.css('pf-c-form__alert', className) }), children));
};
exports.FormAlert = FormAlert;
exports.FormAlert.displayName = 'FormAlert';
//# sourceMappingURL=FormAlert.js.map