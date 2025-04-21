"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MastheadBrand = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const masthead_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Masthead/masthead"));
const react_styles_1 = require("@patternfly/react-styles");
const MastheadBrand = (_a) => {
    var { children, className, component = 'a' } = _a, props = tslib_1.__rest(_a, ["children", "className", "component"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ className: react_styles_1.css(masthead_1.default.mastheadBrand, className), tabIndex: 0 }, props), children));
};
exports.MastheadBrand = MastheadBrand;
exports.MastheadBrand.displayName = 'MastheadBrand';
//# sourceMappingURL=MastheadBrand.js.map