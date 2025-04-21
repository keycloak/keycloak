"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DescriptionListTermHelpText = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const description_list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DescriptionList/description-list"));
const react_styles_1 = require("@patternfly/react-styles");
const DescriptionListTermHelpText = (_a) => {
    var { children, className } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("dt", Object.assign({ className: react_styles_1.css(description_list_1.default.descriptionListTerm, className) }, props), children));
};
exports.DescriptionListTermHelpText = DescriptionListTermHelpText;
exports.DescriptionListTermHelpText.displayName = 'DescriptionListTermHelpText';
//# sourceMappingURL=DescriptionListTermHelpText.js.map