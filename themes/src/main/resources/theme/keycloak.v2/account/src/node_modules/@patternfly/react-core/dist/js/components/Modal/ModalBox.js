"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ModalBox = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const modal_box_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/ModalBox/modal-box"));
const c_modal_box_m_align_top_spacer_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/c_modal_box_m_align_top_spacer'));
const ModalBox = (_a) => {
    var { children, className = '', variant = 'default', position, positionOffset, 'aria-labelledby': ariaLabelledby, 'aria-label': ariaLabel = '', 'aria-describedby': ariaDescribedby, style } = _a, props = tslib_1.__rest(_a, ["children", "className", "variant", "position", "positionOffset", 'aria-labelledby', 'aria-label', 'aria-describedby', "style"]);
    if (positionOffset) {
        style = style || {};
        style[c_modal_box_m_align_top_spacer_1.default.name] = positionOffset;
    }
    return (React.createElement("div", Object.assign({}, props, { role: "dialog", "aria-label": ariaLabel || null, "aria-labelledby": ariaLabelledby || null, "aria-describedby": ariaDescribedby, "aria-modal": "true", className: react_styles_1.css(modal_box_1.default.modalBox, className, position === 'top' && modal_box_1.default.modifiers.alignTop, variant === 'large' && modal_box_1.default.modifiers.lg, variant === 'small' && modal_box_1.default.modifiers.sm, variant === 'medium' && modal_box_1.default.modifiers.md), style: style }), children));
};
exports.ModalBox = ModalBox;
exports.ModalBox.displayName = 'ModalBox';
//# sourceMappingURL=ModalBox.js.map