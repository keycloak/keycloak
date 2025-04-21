"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DescriptionListTermHelpTextButton = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const description_list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DescriptionList/description-list"));
const react_styles_1 = require("@patternfly/react-styles");
const DescriptionListTermHelpTextButton = (_a) => {
    var { children, className } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("span", Object.assign({ className: react_styles_1.css(className, description_list_1.default.descriptionListText, description_list_1.default.modifiers.helpText), role: "button", type: "button", tabIndex: 0 }, props), children));
};
exports.DescriptionListTermHelpTextButton = DescriptionListTermHelpTextButton;
exports.DescriptionListTermHelpTextButton.displayName = 'DescriptionListTermHelpTextButton';
//# sourceMappingURL=DescriptionListTermHelpTextButton.js.map