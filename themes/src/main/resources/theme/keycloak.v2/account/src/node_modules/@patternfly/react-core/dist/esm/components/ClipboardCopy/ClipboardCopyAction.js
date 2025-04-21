import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ClipboardCopy/clipboard-copy';
import { css } from '@patternfly/react-styles';
export const ClipboardCopyAction = (_a) => {
    var { children = null, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("span", Object.assign({ className: css(styles.clipboardCopyActionsItem, className) }, props), children));
};
ClipboardCopyAction.displayName = 'ClipboardCopyAction';
//# sourceMappingURL=ClipboardCopyAction.js.map