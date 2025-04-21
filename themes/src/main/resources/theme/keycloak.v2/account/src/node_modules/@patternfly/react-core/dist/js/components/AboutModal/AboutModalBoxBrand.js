"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AboutModalBoxBrand = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const about_modal_box_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/AboutModalBox/about-modal-box"));
const AboutModalBoxBrand = (_a) => {
    var { className = '', src = '', alt } = _a, props = tslib_1.__rest(_a, ["className", "src", "alt"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(about_modal_box_1.default.aboutModalBoxBrand, className) }, props),
        React.createElement("img", { className: react_styles_1.css(about_modal_box_1.default.aboutModalBoxBrandImage), src: src, alt: alt })));
};
exports.AboutModalBoxBrand = AboutModalBoxBrand;
exports.AboutModalBoxBrand.displayName = 'AboutModalBoxBrand';
//# sourceMappingURL=AboutModalBoxBrand.js.map