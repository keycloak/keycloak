import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/AboutModalBox/about-modal-box';
import { Button } from '../Button';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';
export const AboutModalBoxCloseButton = (_a) => {
    var { className = '', onClose = () => undefined, 'aria-label': ariaLabel = 'Close Dialog' } = _a, props = __rest(_a, ["className", "onClose", 'aria-label']);
    return (React.createElement("div", Object.assign({ className: css(styles.aboutModalBoxClose, className) }, props),
        React.createElement(Button, { variant: "plain", onClick: onClose, "aria-label": ariaLabel },
            React.createElement(TimesIcon, null))));
};
AboutModalBoxCloseButton.displayName = 'AboutModalBoxCloseButton';
//# sourceMappingURL=AboutModalBoxCloseButton.js.map