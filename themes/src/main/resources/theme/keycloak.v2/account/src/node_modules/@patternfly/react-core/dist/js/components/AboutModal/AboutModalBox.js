"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AboutModalBox = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const about_modal_box_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/AboutModalBox/about-modal-box"));
const AboutModalBox = (_a) => {
    var { children, className = '' } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({ role: "dialog", "aria-modal": "true", className: react_styles_1.css(about_modal_box_1.default.aboutModalBox, className) }, props), children));
};
exports.AboutModalBox = AboutModalBox;
exports.AboutModalBox.displayName = 'AboutModalBox';
//# sourceMappingURL=AboutModalBox.js.map