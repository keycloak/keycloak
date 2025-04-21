"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Split = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const split_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/layouts/Split/split"));
const react_styles_1 = require("@patternfly/react-styles");
const Split = (_a) => {
    var { hasGutter = false, isWrappable = false, className = '', children = null, component = 'div' } = _a, props = tslib_1.__rest(_a, ["hasGutter", "isWrappable", "className", "children", "component"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({}, props, { className: react_styles_1.css(split_1.default.split, hasGutter && split_1.default.modifiers.gutter, isWrappable && split_1.default.modifiers.wrap, className) }), children));
};
exports.Split = Split;
exports.Split.displayName = 'Split';
//# sourceMappingURL=Split.js.map