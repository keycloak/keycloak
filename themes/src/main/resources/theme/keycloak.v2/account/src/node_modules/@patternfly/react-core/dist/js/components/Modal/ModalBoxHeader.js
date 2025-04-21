"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ModalBoxHeader = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const modal_box_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/ModalBox/modal-box"));
const ModalBoxHeader = (_a) => {
    var { children = null, className = '', help = null } = _a, props = tslib_1.__rest(_a, ["children", "className", "help"]);
    return (React.createElement("header", Object.assign({ className: react_styles_1.css(modal_box_1.default.modalBoxHeader, help && modal_box_1.default.modifiers.help, className) }, props),
        help && (React.createElement(React.Fragment, null,
            React.createElement("div", { className: react_styles_1.css(modal_box_1.default.modalBoxHeaderMain) }, children),
            React.createElement("div", { className: "pf-c-modal-box__header-help" }, help))),
        !help && children));
};
exports.ModalBoxHeader = ModalBoxHeader;
exports.ModalBoxHeader.displayName = 'ModalBoxHeader';
//# sourceMappingURL=ModalBoxHeader.js.map