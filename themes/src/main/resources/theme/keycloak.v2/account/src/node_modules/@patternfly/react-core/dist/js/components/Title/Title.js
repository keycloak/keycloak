"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Title = exports.TitleSizes = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const title_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Title/title"));
const helpers_1 = require("../../helpers");
var TitleSizes;
(function (TitleSizes) {
    TitleSizes["md"] = "md";
    TitleSizes["lg"] = "lg";
    TitleSizes["xl"] = "xl";
    TitleSizes["2xl"] = "2xl";
    TitleSizes["3xl"] = "3xl";
    TitleSizes["4xl"] = "4xl";
})(TitleSizes = exports.TitleSizes || (exports.TitleSizes = {}));
var headingLevelSizeMap;
(function (headingLevelSizeMap) {
    headingLevelSizeMap["h1"] = "2xl";
    headingLevelSizeMap["h2"] = "xl";
    headingLevelSizeMap["h3"] = "lg";
    headingLevelSizeMap["h4"] = "md";
    headingLevelSizeMap["h5"] = "md";
    headingLevelSizeMap["h6"] = "md";
})(headingLevelSizeMap || (headingLevelSizeMap = {}));
const Title = (_a) => {
    var { className = '', children = '', headingLevel: HeadingLevel, size = headingLevelSizeMap[HeadingLevel], ouiaId, ouiaSafe = true } = _a, props = tslib_1.__rest(_a, ["className", "children", "headingLevel", "size", "ouiaId", "ouiaSafe"]);
    const ouiaProps = helpers_1.useOUIAProps(exports.Title.displayName, ouiaId, ouiaSafe);
    return (React.createElement(HeadingLevel, Object.assign({}, ouiaProps, props, { className: react_styles_1.css(title_1.default.title, size && title_1.default.modifiers[size], className) }), children));
};
exports.Title = Title;
exports.Title.displayName = 'Title';
//# sourceMappingURL=Title.js.map