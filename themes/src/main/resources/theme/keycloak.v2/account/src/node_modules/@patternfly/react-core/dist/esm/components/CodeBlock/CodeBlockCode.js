import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/CodeBlock/code-block';
import { css } from '@patternfly/react-styles';
export const CodeBlockCode = (_a) => {
    var { children = null, className, codeClassName } = _a, props = __rest(_a, ["children", "className", "codeClassName"]);
    return (React.createElement("pre", Object.assign({ className: css(styles.codeBlockPre, className) }, props),
        React.createElement("code", { className: css(styles.codeBlockCode, codeClassName) }, children)));
};
CodeBlockCode.displayName = 'CodeBlockCode';
//# sourceMappingURL=CodeBlockCode.js.map