"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AboutModalBoxHeader = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const about_modal_box_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/AboutModalBox/about-modal-box"));
const Title_1 = require("../Title");
const AboutModalBoxHeader = (_a) => {
    var { className = '', productName = '', id } = _a, props = tslib_1.__rest(_a, ["className", "productName", "id"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(about_modal_box_1.default.aboutModalBoxHeader, className) }, props),
        React.createElement(Title_1.Title, { headingLevel: "h1", size: "4xl", id: id }, productName)));
};
exports.AboutModalBoxHeader = AboutModalBoxHeader;
exports.AboutModalBoxHeader.displayName = 'AboutModalBoxHeader';
//# sourceMappingURL=AboutModalBoxHeader.js.map