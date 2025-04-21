"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AboutModalBoxCloseButton = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const about_modal_box_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/AboutModalBox/about-modal-box"));
const Button_1 = require("../Button");
const times_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/times-icon'));
const AboutModalBoxCloseButton = (_a) => {
    var { className = '', onClose = () => undefined, 'aria-label': ariaLabel = 'Close Dialog' } = _a, props = tslib_1.__rest(_a, ["className", "onClose", 'aria-label']);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(about_modal_box_1.default.aboutModalBoxClose, className) }, props),
        React.createElement(Button_1.Button, { variant: "plain", onClick: onClose, "aria-label": ariaLabel },
            React.createElement(times_icon_1.default, null))));
};
exports.AboutModalBoxCloseButton = AboutModalBoxCloseButton;
exports.AboutModalBoxCloseButton.displayName = 'AboutModalBoxCloseButton';
//# sourceMappingURL=AboutModalBoxCloseButton.js.map