"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AboutModalContainer = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const bullseye_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/layouts/Bullseye/bullseye"));
const helpers_1 = require("../../helpers");
const AboutModalBoxContent_1 = require("./AboutModalBoxContent");
const AboutModalBoxHeader_1 = require("./AboutModalBoxHeader");
const AboutModalBoxHero_1 = require("./AboutModalBoxHero");
const AboutModalBoxBrand_1 = require("./AboutModalBoxBrand");
const AboutModalBoxCloseButton_1 = require("./AboutModalBoxCloseButton");
const AboutModalBox_1 = require("./AboutModalBox");
const Backdrop_1 = require("../Backdrop/Backdrop");
const AboutModalContainer = (_a) => {
    var { children, className = '', isOpen = false, onClose = () => undefined, productName = '', trademark, brandImageSrc, brandImageAlt, backgroundImageSrc, closeButtonAriaLabel, aboutModalBoxHeaderId, aboutModalBoxContentId, disableFocusTrap = false } = _a, props = tslib_1.__rest(_a, ["children", "className", "isOpen", "onClose", "productName", "trademark", "brandImageSrc", "brandImageAlt", "backgroundImageSrc", "closeButtonAriaLabel", "aboutModalBoxHeaderId", "aboutModalBoxContentId", "disableFocusTrap"]);
    if (!isOpen) {
        return null;
    }
    return (React.createElement(Backdrop_1.Backdrop, null,
        React.createElement(helpers_1.FocusTrap, { active: !disableFocusTrap, focusTrapOptions: { clickOutsideDeactivates: true, tabbableOptions: { displayCheck: 'none' } }, className: react_styles_1.css(bullseye_1.default.bullseye) },
            React.createElement(AboutModalBox_1.AboutModalBox, { className: className, "aria-labelledby": aboutModalBoxHeaderId, "aria-describedby": aboutModalBoxContentId },
                React.createElement(AboutModalBoxBrand_1.AboutModalBoxBrand, { src: brandImageSrc, alt: brandImageAlt }),
                React.createElement(AboutModalBoxCloseButton_1.AboutModalBoxCloseButton, { "aria-label": closeButtonAriaLabel, onClose: onClose }),
                productName && React.createElement(AboutModalBoxHeader_1.AboutModalBoxHeader, { id: aboutModalBoxHeaderId, productName: productName }),
                React.createElement(AboutModalBoxContent_1.AboutModalBoxContent, Object.assign({ trademark: trademark, id: aboutModalBoxContentId, noAboutModalBoxContentContainer: false }, props), children),
                React.createElement(AboutModalBoxHero_1.AboutModalBoxHero, { backgroundImageSrc: backgroundImageSrc })))));
};
exports.AboutModalContainer = AboutModalContainer;
exports.AboutModalContainer.displayName = 'AboutModalContainer';
//# sourceMappingURL=AboutModalContainer.js.map