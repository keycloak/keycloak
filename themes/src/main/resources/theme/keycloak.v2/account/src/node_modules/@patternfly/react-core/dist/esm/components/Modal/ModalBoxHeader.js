import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/ModalBox/modal-box';
export const ModalBoxHeader = (_a) => {
    var { children = null, className = '', help = null } = _a, props = __rest(_a, ["children", "className", "help"]);
    return (React.createElement("header", Object.assign({ className: css(styles.modalBoxHeader, help && styles.modifiers.help, className) }, props),
        help && (React.createElement(React.Fragment, null,
            React.createElement("div", { className: css(styles.modalBoxHeaderMain) }, children),
            React.createElement("div", { className: "pf-c-modal-box__header-help" }, help))),
        !help && children));
};
ModalBoxHeader.displayName = 'ModalBoxHeader';
//# sourceMappingURL=ModalBoxHeader.js.map