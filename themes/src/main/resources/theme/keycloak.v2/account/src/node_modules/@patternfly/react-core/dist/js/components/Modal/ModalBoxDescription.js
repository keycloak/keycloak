"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ModalBoxDescription = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const modal_box_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/ModalBox/modal-box"));
const ModalBoxDescription = (_a) => {
    var { children = null, className = '', id = '' } = _a, props = tslib_1.__rest(_a, ["children", "className", "id"]);
    return (React.createElement("div", Object.assign({}, props, { id: id, className: react_styles_1.css(modal_box_1.default.modalBoxDescription, className) }), children));
};
exports.ModalBoxDescription = ModalBoxDescription;
exports.ModalBoxDescription.displayName = 'ModalBoxDescription';
//# sourceMappingURL=ModalBoxDescription.js.map