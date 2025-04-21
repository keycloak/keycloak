"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Text = exports.TextVariants = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const content_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Content/content"));
const helpers_1 = require("../../helpers");
var TextVariants;
(function (TextVariants) {
    TextVariants["h1"] = "h1";
    TextVariants["h2"] = "h2";
    TextVariants["h3"] = "h3";
    TextVariants["h4"] = "h4";
    TextVariants["h5"] = "h5";
    TextVariants["h6"] = "h6";
    TextVariants["p"] = "p";
    TextVariants["a"] = "a";
    TextVariants["small"] = "small";
    TextVariants["blockquote"] = "blockquote";
    TextVariants["pre"] = "pre";
})(TextVariants = exports.TextVariants || (exports.TextVariants = {}));
const Text = (_a) => {
    var { children = null, className = '', component = TextVariants.p, isVisitedLink = false, ouiaId, ouiaSafe = true } = _a, props = tslib_1.__rest(_a, ["children", "className", "component", "isVisitedLink", "ouiaId", "ouiaSafe"]);
    const Component = component;
    const ouiaProps = helpers_1.useOUIAProps(exports.Text.displayName, ouiaId, ouiaSafe);
    return (React.createElement(Component, Object.assign({}, ouiaProps, props, { "data-pf-content": true, className: react_styles_1.css(isVisitedLink && component === TextVariants.a && content_1.default.modifiers.visited, className) }), children));
};
exports.Text = Text;
exports.Text.displayName = 'Text';
//# sourceMappingURL=Text.js.map