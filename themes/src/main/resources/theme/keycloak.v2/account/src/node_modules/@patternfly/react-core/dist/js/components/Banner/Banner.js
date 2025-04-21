"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Banner = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const banner_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Banner/banner"));
const react_styles_1 = require("@patternfly/react-styles");
const Banner = (_a) => {
    var { children, className, variant = 'default', screenReaderText, isSticky = false } = _a, props = tslib_1.__rest(_a, ["children", "className", "variant", "screenReaderText", "isSticky"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(banner_1.default.banner, banner_1.default.modifiers[variant], isSticky && banner_1.default.modifiers.sticky, className) }, props),
        children,
        React.createElement("span", { className: "pf-u-screen-reader" }, screenReaderText || `${variant} banner`)));
};
exports.Banner = Banner;
exports.Banner.displayName = 'Banner';
//# sourceMappingURL=Banner.js.map