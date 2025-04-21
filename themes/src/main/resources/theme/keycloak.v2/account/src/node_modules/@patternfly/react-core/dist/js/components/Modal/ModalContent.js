"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ModalContent = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const helpers_1 = require("../../helpers");
const modal_box_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/ModalBox/modal-box"));
const bullseye_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/layouts/Bullseye/bullseye"));
const react_styles_1 = require("@patternfly/react-styles");
const helpers_2 = require("../../helpers");
const Backdrop_1 = require("../Backdrop/Backdrop");
const ModalBoxBody_1 = require("./ModalBoxBody");
const ModalBoxCloseButton_1 = require("./ModalBoxCloseButton");
const ModalBox_1 = require("./ModalBox");
const ModalBoxFooter_1 = require("./ModalBoxFooter");
const ModalBoxDescription_1 = require("./ModalBoxDescription");
const ModalBoxHeader_1 = require("./ModalBoxHeader");
const ModalBoxTitle_1 = require("./ModalBoxTitle");
const ModalContent = (_a) => {
    var { children, className = '', isOpen = false, header = null, help = null, description = null, title = '', titleIconVariant = null, titleLabel = '', 'aria-label': ariaLabel = '', 'aria-describedby': ariaDescribedby, 'aria-labelledby': ariaLabelledby, bodyAriaLabel, bodyAriaRole, showClose = true, footer = null, actions = [], onClose = () => undefined, variant = 'default', position, positionOffset, width = -1, boxId, labelId, descriptorId, disableFocusTrap = false, hasNoBodyWrapper = false, ouiaId, ouiaSafe = true } = _a, props = tslib_1.__rest(_a, ["children", "className", "isOpen", "header", "help", "description", "title", "titleIconVariant", "titleLabel", 'aria-label', 'aria-describedby', 'aria-labelledby', "bodyAriaLabel", "bodyAriaRole", "showClose", "footer", "actions", "onClose", "variant", "position", "positionOffset", "width", "boxId", "labelId", "descriptorId", "disableFocusTrap", "hasNoBodyWrapper", "ouiaId", "ouiaSafe"]);
    if (!isOpen) {
        return null;
    }
    const modalBoxHeader = header ? (React.createElement(ModalBoxHeader_1.ModalBoxHeader, { help: help }, header)) : (title && (React.createElement(ModalBoxHeader_1.ModalBoxHeader, { help: help },
        React.createElement(ModalBoxTitle_1.ModalBoxTitle, { title: title, titleIconVariant: titleIconVariant, titleLabel: titleLabel, id: labelId }),
        description && React.createElement(ModalBoxDescription_1.ModalBoxDescription, { id: descriptorId }, description))));
    const modalBoxFooter = footer ? (React.createElement(ModalBoxFooter_1.ModalBoxFooter, null, footer)) : (actions.length > 0 && React.createElement(ModalBoxFooter_1.ModalBoxFooter, null, actions));
    const defaultModalBodyAriaRole = bodyAriaLabel ? 'region' : undefined;
    const modalBody = hasNoBodyWrapper ? (children) : (React.createElement(ModalBoxBody_1.ModalBoxBody, Object.assign({ "aria-label": bodyAriaLabel, role: bodyAriaRole || defaultModalBodyAriaRole }, props, (!description && !ariaDescribedby && { id: descriptorId })), children));
    const boxStyle = width === -1 ? {} : { width };
    const ariaLabelledbyFormatted = () => {
        if (ariaLabelledby === null) {
            return null;
        }
        const idRefList = [];
        if ((ariaLabel && boxId) !== '') {
            idRefList.push(ariaLabel && boxId);
        }
        if (ariaLabelledby) {
            idRefList.push(ariaLabelledby);
        }
        if (title) {
            idRefList.push(labelId);
        }
        return idRefList.join(' ');
    };
    const modalBox = (React.createElement(ModalBox_1.ModalBox, Object.assign({ id: boxId, style: boxStyle, className: react_styles_1.css(className, ModalBoxTitle_1.isVariantIcon(titleIconVariant) &&
            modal_box_1.default.modifiers[titleIconVariant]), variant: variant, position: position, positionOffset: positionOffset, "aria-label": ariaLabel, "aria-labelledby": ariaLabelledbyFormatted(), "aria-describedby": ariaDescribedby || (hasNoBodyWrapper ? null : descriptorId) }, helpers_2.getOUIAProps(exports.ModalContent.displayName, ouiaId, ouiaSafe)),
        showClose && React.createElement(ModalBoxCloseButton_1.ModalBoxCloseButton, { onClose: onClose, ouiaId: ouiaId }),
        modalBoxHeader,
        modalBody,
        modalBoxFooter));
    return (React.createElement(Backdrop_1.Backdrop, null,
        React.createElement(helpers_1.FocusTrap, { active: !disableFocusTrap, focusTrapOptions: { clickOutsideDeactivates: true, tabbableOptions: { displayCheck: 'none' } }, className: react_styles_1.css(bullseye_1.default.bullseye) }, modalBox)));
};
exports.ModalContent = ModalContent;
exports.ModalContent.displayName = 'ModalContent';
//# sourceMappingURL=ModalContent.js.map