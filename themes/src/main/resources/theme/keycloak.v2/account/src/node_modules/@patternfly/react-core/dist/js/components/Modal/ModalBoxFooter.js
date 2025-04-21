"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ModalBoxFooter = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const modal_box_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/ModalBox/modal-box"));
const ModalBoxFooter = (_a) => {
    var { children = null, className = '' } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("footer", Object.assign({}, props, { className: react_styles_1.css(modal_box_1.default.modalBoxFooter, className) }), children));
};
exports.ModalBoxFooter = ModalBoxFooter;
exports.ModalBoxFooter.displayName = 'ModalBoxFooter';
//# sourceMappingURL=ModalBoxFooter.js.map