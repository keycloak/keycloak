"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.TextList = exports.TextListVariants = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
var TextListVariants;
(function (TextListVariants) {
    TextListVariants["ul"] = "ul";
    TextListVariants["ol"] = "ol";
    TextListVariants["dl"] = "dl";
})(TextListVariants = exports.TextListVariants || (exports.TextListVariants = {}));
const TextList = (_a) => {
    var { children = null, className = '', component = TextListVariants.ul } = _a, props = tslib_1.__rest(_a, ["children", "className", "component"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({}, props, { "data-pf-content": true, className: react_styles_1.css(className) }), children));
};
exports.TextList = TextList;
exports.TextList.displayName = 'TextList';
//# sourceMappingURL=TextList.js.map