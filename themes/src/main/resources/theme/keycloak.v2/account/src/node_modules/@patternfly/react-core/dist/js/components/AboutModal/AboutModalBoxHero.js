"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AboutModalBoxHero = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const about_modal_box_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/AboutModalBox/about-modal-box"));
// eslint-disable-next-line camelcase
const c_about_modal_box__hero_sm_BackgroundImage_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/c_about_modal_box__hero_sm_BackgroundImage'));
const AboutModalBoxHero = (_a) => {
    var { className, backgroundImageSrc } = _a, props = tslib_1.__rest(_a, ["className", "backgroundImageSrc"]);
    return (React.createElement("div", Object.assign({ style: 
        /* eslint-disable camelcase */
        backgroundImageSrc !== ''
            ? { [c_about_modal_box__hero_sm_BackgroundImage_1.default.name]: `url(${backgroundImageSrc})` }
            : {}, className: react_styles_1.css(about_modal_box_1.default.aboutModalBoxHero, className) }, props)));
};
exports.AboutModalBoxHero = AboutModalBoxHero;
exports.AboutModalBoxHero.displayName = 'AboutModalBoxHero';
//# sourceMappingURL=AboutModalBoxHero.js.map