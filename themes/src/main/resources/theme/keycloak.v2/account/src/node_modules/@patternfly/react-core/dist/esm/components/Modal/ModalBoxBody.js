import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/ModalBox/modal-box';
export const ModalBoxBody = (_a) => {
    var { children = null, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({}, props, { className: css(styles.modalBoxBody, className) }), children));
};
ModalBoxBody.displayName = 'ModalBoxBody';
//# sourceMappingURL=ModalBoxBody.js.map