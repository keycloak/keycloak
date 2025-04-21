"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ModalBoxBody = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const modal_box_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/ModalBox/modal-box"));
const ModalBoxBody = (_a) => {
    var { children = null, className = '' } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(modal_box_1.default.modalBoxBody, className) }), children));
};
exports.ModalBoxBody = ModalBoxBody;
exports.ModalBoxBody.displayName = 'ModalBoxBody';
//# sourceMappingURL=ModalBoxBody.js.map