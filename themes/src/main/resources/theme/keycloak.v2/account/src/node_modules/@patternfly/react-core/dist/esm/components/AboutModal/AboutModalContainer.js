import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/layouts/Bullseye/bullseye';
import { FocusTrap } from '../../helpers';
import { AboutModalBoxContent } from './AboutModalBoxContent';
import { AboutModalBoxHeader } from './AboutModalBoxHeader';
import { AboutModalBoxHero } from './AboutModalBoxHero';
import { AboutModalBoxBrand } from './AboutModalBoxBrand';
import { AboutModalBoxCloseButton } from './AboutModalBoxCloseButton';
import { AboutModalBox } from './AboutModalBox';
import { Backdrop } from '../Backdrop/Backdrop';
export const AboutModalContainer = (_a) => {
    var { children, className = '', isOpen = false, onClose = () => undefined, productName = '', trademark, brandImageSrc, brandImageAlt, backgroundImageSrc, closeButtonAriaLabel, aboutModalBoxHeaderId, aboutModalBoxContentId, disableFocusTrap = false } = _a, props = __rest(_a, ["children", "className", "isOpen", "onClose", "productName", "trademark", "brandImageSrc", "brandImageAlt", "backgroundImageSrc", "closeButtonAriaLabel", "aboutModalBoxHeaderId", "aboutModalBoxContentId", "disableFocusTrap"]);
    if (!isOpen) {
        return null;
    }
    return (React.createElement(Backdrop, null,
        React.createElement(FocusTrap, { active: !disableFocusTrap, focusTrapOptions: { clickOutsideDeactivates: true, tabbableOptions: { displayCheck: 'none' } }, className: css(styles.bullseye) },
            React.createElement(AboutModalBox, { className: className, "aria-labelledby": aboutModalBoxHeaderId, "aria-describedby": aboutModalBoxContentId },
                React.createElement(AboutModalBoxBrand, { src: brandImageSrc, alt: brandImageAlt }),
                React.createElement(AboutModalBoxCloseButton, { "aria-label": closeButtonAriaLabel, onClose: onClose }),
                productName && React.createElement(AboutModalBoxHeader, { id: aboutModalBoxHeaderId, productName: productName }),
                React.createElement(AboutModalBoxContent, Object.assign({ trademark: trademark, id: aboutModalBoxContentId, noAboutModalBoxContentContainer: false }, props), children),
                React.createElement(AboutModalBoxHero, { backgroundImageSrc: backgroundImageSrc })))));
};
AboutModalContainer.displayName = 'AboutModalContainer';
//# sourceMappingURL=AboutModalContainer.js.map