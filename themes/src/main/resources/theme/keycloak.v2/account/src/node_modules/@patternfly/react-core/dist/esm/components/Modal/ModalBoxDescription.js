import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/ModalBox/modal-box';
export const ModalBoxDescription = (_a) => {
    var { children = null, className = '', id = '' } = _a, props = __rest(_a, ["children", "className", "id"]);
    return (React.createElement("div", Object.assign({}, props, { id: id, className: css(styles.modalBoxDescription, className) }), children));
};
ModalBoxDescription.displayName = 'ModalBoxDescription';
//# sourceMappingURL=ModalBoxDescription.js.map