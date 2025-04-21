import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/AboutModalBox/about-modal-box';
import { Title } from '../Title';
export const AboutModalBoxHeader = (_a) => {
    var { className = '', productName = '', id } = _a, props = __rest(_a, ["className", "productName", "id"]);
    return (React.createElement("div", Object.assign({ className: css(styles.aboutModalBoxHeader, className) }, props),
        React.createElement(Title, { headingLevel: "h1", size: "4xl", id: id }, productName)));
};
AboutModalBoxHeader.displayName = 'AboutModalBoxHeader';
//# sourceMappingURL=AboutModalBoxHeader.js.map