"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DescriptionListGroup = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const description_list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DescriptionList/description-list"));
const react_styles_1 = require("@patternfly/react-styles");
const DescriptionListGroup = (_a) => {
    var { className, children } = _a, props = tslib_1.__rest(_a, ["className", "children"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(description_list_1.default.descriptionListGroup, className) }, props), children));
};
exports.DescriptionListGroup = DescriptionListGroup;
exports.DescriptionListGroup.displayName = 'DescriptionListGroup';
//# sourceMappingURL=DescriptionListGroup.js.map