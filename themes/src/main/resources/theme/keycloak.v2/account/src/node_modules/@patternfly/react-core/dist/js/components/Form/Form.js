"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Form = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const form_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Form/form"));
const react_styles_1 = require("@patternfly/react-styles");
const Form = (_a) => {
    var { children = null, className = '', isHorizontal = false, isWidthLimited = false, maxWidth = '' } = _a, props = tslib_1.__rest(_a, ["children", "className", "isHorizontal", "isWidthLimited", "maxWidth"]);
    return (React.createElement("form", Object.assign({ noValidate: true }, (maxWidth && {
        style: Object.assign({ '--pf-c-form--m-limit-width--MaxWidth': maxWidth }, props.style)
    }), props, { className: react_styles_1.css(form_1.default.form, isHorizontal && form_1.default.modifiers.horizontal, (isWidthLimited || maxWidth) && form_1.default.modifiers.limitWidth, className) }), children));
};
exports.Form = Form;
exports.Form.displayName = 'Form';
//# sourceMappingURL=Form.js.map