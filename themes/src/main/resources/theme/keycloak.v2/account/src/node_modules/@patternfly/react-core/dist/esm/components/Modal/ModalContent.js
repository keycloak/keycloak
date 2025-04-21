import { __rest } from "tslib";
import * as React from 'react';
import { FocusTrap } from '../../helpers';
import modalStyles from '@patternfly/react-styles/css/components/ModalBox/modal-box';
import bullsEyeStyles from '@patternfly/react-styles/css/layouts/Bullseye/bullseye';
import { css } from '@patternfly/react-styles';
import { getOUIAProps } from '../../helpers';
import { Backdrop } from '../Backdrop/Backdrop';
import { ModalBoxBody } from './ModalBoxBody';
import { ModalBoxCloseButton } from './ModalBoxCloseButton';
import { ModalBox } from './ModalBox';
import { ModalBoxFooter } from './ModalBoxFooter';
import { ModalBoxDescription } from './ModalBoxDescription';
import { ModalBoxHeader } from './ModalBoxHeader';
import { ModalBoxTitle, isVariantIcon } from './ModalBoxTitle';
export const ModalContent = (_a) => {
    var { children, className = '', isOpen = false, header = null, help = null, description = null, title = '', titleIconVariant = null, titleLabel = '', 'aria-label': ariaLabel = '', 'aria-describedby': ariaDescribedby, 'aria-labelledby': ariaLabelledby, bodyAriaLabel, bodyAriaRole, showClose = true, footer = null, actions = [], onClose = () => undefined, variant = 'default', position, positionOffset, width = -1, boxId, labelId, descriptorId, disableFocusTrap = false, hasNoBodyWrapper = false, ouiaId, ouiaSafe = true } = _a, props = __rest(_a, ["children", "className", "isOpen", "header", "help", "description", "title", "titleIconVariant", "titleLabel", 'aria-label', 'aria-describedby', 'aria-labelledby', "bodyAriaLabel", "bodyAriaRole", "showClose", "footer", "actions", "onClose", "variant", "position", "positionOffset", "width", "boxId", "labelId", "descriptorId", "disableFocusTrap", "hasNoBodyWrapper", "ouiaId", "ouiaSafe"]);
    if (!isOpen) {
        return null;
    }
    const modalBoxHeader = header ? (React.createElement(ModalBoxHeader, { help: help }, header)) : (title && (React.createElement(ModalBoxHeader, { help: help },
        React.createElement(ModalBoxTitle, { title: title, titleIconVariant: titleIconVariant, titleLabel: titleLabel, id: labelId }),
        description && React.createElement(ModalBoxDescription, { id: descriptorId }, description))));
    const modalBoxFooter = footer ? (React.createElement(ModalBoxFooter, null, footer)) : (actions.length > 0 && React.createElement(ModalBoxFooter, null, actions));
    const defaultModalBodyAriaRole = bodyAriaLabel ? 'region' : undefined;
    const modalBody = hasNoBodyWrapper ? (children) : (React.createElement(ModalBoxBody, Object.assign({ "aria-label": bodyAriaLabel, role: bodyAriaRole || defaultModalBodyAriaRole }, props, (!description && !ariaDescribedby && { id: descriptorId })), children));
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
    const modalBox = (React.createElement(ModalBox, Object.assign({ id: boxId, style: boxStyle, className: css(className, isVariantIcon(titleIconVariant) &&
            modalStyles.modifiers[titleIconVariant]), variant: variant, position: position, positionOffset: positionOffset, "aria-label": ariaLabel, "aria-labelledby": ariaLabelledbyFormatted(), "aria-describedby": ariaDescribedby || (hasNoBodyWrapper ? null : descriptorId) }, getOUIAProps(ModalContent.displayName, ouiaId, ouiaSafe)),
        showClose && React.createElement(ModalBoxCloseButton, { onClose: onClose, ouiaId: ouiaId }),
        modalBoxHeader,
        modalBody,
        modalBoxFooter));
    return (React.createElement(Backdrop, null,
        React.createElement(FocusTrap, { active: !disableFocusTrap, focusTrapOptions: { clickOutsideDeactivates: true, tabbableOptions: { displayCheck: 'none' } }, className: css(bullsEyeStyles.bullseye) }, modalBox)));
};
ModalContent.displayName = 'ModalContent';
//# sourceMappingURL=ModalContent.js.map