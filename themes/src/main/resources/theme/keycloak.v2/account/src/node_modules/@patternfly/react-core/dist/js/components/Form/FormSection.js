"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.FormSection = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const form_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Form/form"));
const react_styles_1 = require("@patternfly/react-styles");
const GenerateId_1 = require("../../helpers/GenerateId/GenerateId");
const FormSection = (_a) => {
    var { className = '', children, title = '', titleElement: TitleElement = 'div' } = _a, props = tslib_1.__rest(_a, ["className", "children", "title", "titleElement"]);
    return (React.createElement(GenerateId_1.GenerateId, { prefix: "pf-form-section-title" }, sectionId => (React.createElement("section", Object.assign({ className: react_styles_1.css(form_1.default.formSection, className), role: "group" }, (title && { 'aria-labelledby': sectionId }), props),
        title && (React.createElement(TitleElement, { id: sectionId, className: react_styles_1.css(form_1.default.formSectionTitle, className) }, title)),
        children))));
};
exports.FormSection = FormSection;
exports.FormSection.displayName = 'FormSection';
//# sourceMappingURL=FormSection.js.map