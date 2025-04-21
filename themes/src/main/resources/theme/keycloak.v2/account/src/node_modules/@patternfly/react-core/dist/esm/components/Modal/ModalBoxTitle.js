import { __rest } from "tslib";
import * as React from 'react';
import modalStyles from '@patternfly/react-styles/css/components/ModalBox/modal-box';
import { css } from '@patternfly/react-styles';
import accessibleStyles from '@patternfly/react-styles/css/utilities/Accessibility/accessibility';
import { capitalize } from '../../helpers';
import { Tooltip } from '../Tooltip';
import CheckCircleIcon from '@patternfly/react-icons/dist/esm/icons/check-circle-icon';
import ExclamationCircleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-circle-icon';
import ExclamationTriangleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-triangle-icon';
import InfoCircleIcon from '@patternfly/react-icons/dist/esm/icons/info-circle-icon';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';
import { useIsomorphicLayoutEffect } from '../../helpers';
export const isVariantIcon = (icon) => ['success', 'danger', 'warning', 'info', 'default'].includes(icon);
export const ModalBoxTitle = (_a) => {
    var { className = '', id, title, titleIconVariant, titleLabel = '' } = _a, props = __rest(_a, ["className", "id", "title", "titleIconVariant", "titleLabel"]);
    const [hasTooltip, setHasTooltip] = React.useState(false);
    const h1 = React.useRef();
    const label = titleLabel || (isVariantIcon(titleIconVariant) ? `${capitalize(titleIconVariant)} alert:` : titleLabel);
    const variantIcons = {
        success: React.createElement(CheckCircleIcon, null),
        danger: React.createElement(ExclamationCircleIcon, null),
        warning: React.createElement(ExclamationTriangleIcon, null),
        info: React.createElement(InfoCircleIcon, null),
        default: React.createElement(BellIcon, null)
    };
    const CustomIcon = !isVariantIcon(titleIconVariant) && titleIconVariant;
    useIsomorphicLayoutEffect(() => {
        setHasTooltip(h1.current && h1.current.offsetWidth < h1.current.scrollWidth);
    }, []);
    const content = (React.createElement("h1", Object.assign({ id: id, ref: h1, className: css(modalStyles.modalBoxTitle, titleIconVariant && modalStyles.modifiers.icon, className) }, props),
        titleIconVariant && (React.createElement("span", { className: css(modalStyles.modalBoxTitleIcon) }, isVariantIcon(titleIconVariant) ? variantIcons[titleIconVariant] : React.createElement(CustomIcon, null))),
        label && React.createElement("span", { className: css(accessibleStyles.screenReader) }, label),
        React.createElement("span", { className: css(modalStyles.modalBoxTitleText) }, title)));
    return hasTooltip ? React.createElement(Tooltip, { content: title }, content) : content;
};
ModalBoxTitle.displayName = 'ModalBoxTitle';
//# sourceMappingURL=ModalBoxTitle.js.map