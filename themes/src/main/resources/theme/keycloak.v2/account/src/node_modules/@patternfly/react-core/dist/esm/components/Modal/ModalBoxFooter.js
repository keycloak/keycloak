import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/ModalBox/modal-box';
export const ModalBoxFooter = (_a) => {
    var { children = null, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("footer", Object.assign({}, props, { className: css(styles.modalBoxFooter, className) }), children));
};
ModalBoxFooter.displayName = 'ModalBoxFooter';
//# sourceMappingURL=ModalBoxFooter.js.map