"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.TextListItem = exports.TextListItemVariants = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
var TextListItemVariants;
(function (TextListItemVariants) {
    TextListItemVariants["li"] = "li";
    TextListItemVariants["dt"] = "dt";
    TextListItemVariants["dd"] = "dd";
})(TextListItemVariants = exports.TextListItemVariants || (exports.TextListItemVariants = {}));
const TextListItem = (_a) => {
    var { children = null, className = '', component = TextListItemVariants.li } = _a, props = tslib_1.__rest(_a, ["children", "className", "component"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({}, props, { "data-pf-content": true, className: react_styles_1.css(className) }), children));
};
exports.TextListItem = TextListItem;
exports.TextListItem.displayName = 'TextListItem';
//# sourceMappingURL=TextListItem.js.map