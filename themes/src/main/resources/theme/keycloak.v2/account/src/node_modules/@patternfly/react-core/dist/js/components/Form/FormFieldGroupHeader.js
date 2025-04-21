"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.FormFieldGroupHeader = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const form_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Form/form"));
const react_styles_1 = require("@patternfly/react-styles");
const FormFieldGroupHeader = (_a) => {
    var { className, titleText, titleDescription, actions } = _a, props = tslib_1.__rest(_a, ["className", "titleText", "titleDescription", "actions"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(form_1.default.formFieldGroupHeader, className) }, props),
        React.createElement("div", { className: react_styles_1.css(form_1.default.formFieldGroupHeaderMain) },
            titleText && (React.createElement("div", { className: react_styles_1.css(form_1.default.formFieldGroupHeaderTitle) },
                React.createElement("div", { className: react_styles_1.css(form_1.default.formFieldGroupHeaderTitleText), id: titleText.id }, titleText.text))),
            titleDescription && React.createElement("div", { className: react_styles_1.css(form_1.default.formFieldGroupHeaderDescription) }, titleDescription)),
        React.createElement("div", { className: react_styles_1.css(form_1.default.formFieldGroupHeaderActions) }, actions && actions)));
};
exports.FormFieldGroupHeader = FormFieldGroupHeader;
exports.FormFieldGroupHeader.displayName = 'FormFieldGroupHeader';
//# sourceMappingURL=FormFieldGroupHeader.js.map