"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Brand = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const brand_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Brand/brand"));
const helpers_1 = require("../../helpers");
const Brand = (_a) => {
    var { className = '', src = '', alt, children, widths, heights, style } = _a, props = tslib_1.__rest(_a, ["className", "src", "alt", "children", "widths", "heights", "style"]);
    if (children !== undefined && widths !== undefined) {
        style = Object.assign(Object.assign({}, style), helpers_1.setBreakpointCssVars(widths, '--pf-c-brand--Width'));
    }
    if (children !== undefined && heights !== undefined) {
        style = Object.assign(Object.assign({}, style), helpers_1.setBreakpointCssVars(heights, '--pf-c-brand--Height'));
    }
    return (
    /** the brand component currently contains no styling the 'pf-c-brand' string will be used for the className */
    children !== undefined ? (React.createElement("picture", Object.assign({ className: react_styles_1.css(brand_1.default.brand, brand_1.default.modifiers.picture, className), style: style }, props),
        children,
        React.createElement("img", { src: src, alt: alt }))) : (React.createElement("img", Object.assign({}, props, { className: react_styles_1.css(brand_1.default.brand, className), src: src, alt: alt }))));
};
exports.Brand = Brand;
exports.Brand.displayName = 'Brand';
//# sourceMappingURL=Brand.js.map