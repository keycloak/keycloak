import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/AboutModalBox/about-modal-box';
import contentStyles from '@patternfly/react-styles/css/components/Content/content';
export const AboutModalBoxContent = (_a) => {
    var { children, className = '', trademark, id, noAboutModalBoxContentContainer = false } = _a, props = __rest(_a, ["children", "className", "trademark", "id", "noAboutModalBoxContentContainer"]);
    return (React.createElement("div", Object.assign({ className: css(styles.aboutModalBoxContent, className), id: id }, props),
        React.createElement("div", { className: css('pf-c-about-modal-box__body') }, noAboutModalBoxContentContainer ? children : React.createElement("div", { className: css(contentStyles.content) }, children)),
        React.createElement("p", { className: css(styles.aboutModalBoxStrapline) }, trademark)));
};
AboutModalBoxContent.displayName = 'AboutModalBoxContent';
//# sourceMappingURL=AboutModalBoxContent.js.map