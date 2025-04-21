"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DescriptionListDescription = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const description_list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DescriptionList/description-list"));
const react_styles_1 = require("@patternfly/react-styles");
const DescriptionListDescription = (_a) => {
    var { children = null, className } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("dd", Object.assign({ className: react_styles_1.css(description_list_1.default.descriptionListDescription, className) }, props),
        React.createElement("div", { className: 'pf-c-description-list__text' }, children)));
};
exports.DescriptionListDescription = DescriptionListDescription;
exports.DescriptionListDescription.displayName = 'DescriptionListDescription';
//# sourceMappingURL=DescriptionListDescription.js.map