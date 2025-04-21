"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DescriptionListTerm = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const description_list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DescriptionList/description-list"));
const react_styles_1 = require("@patternfly/react-styles");
const DescriptionListTerm = (_a) => {
    var { children, className, icon } = _a, props = tslib_1.__rest(_a, ["children", "className", "icon"]);
    return (React.createElement("dt", Object.assign({ className: react_styles_1.css(description_list_1.default.descriptionListTerm, className) }, props),
        icon ? React.createElement("span", { className: react_styles_1.css(description_list_1.default.descriptionListTermIcon) }, icon) : null,
        React.createElement("span", { className: react_styles_1.css(description_list_1.default.descriptionListText) }, children)));
};
exports.DescriptionListTerm = DescriptionListTerm;
exports.DescriptionListTerm.displayName = 'DescriptionListTerm';
//# sourceMappingURL=DescriptionListTerm.js.map