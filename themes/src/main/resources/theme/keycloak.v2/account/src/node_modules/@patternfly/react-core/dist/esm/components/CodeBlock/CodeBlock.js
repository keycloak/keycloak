import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/CodeBlock/code-block';
import { css } from '@patternfly/react-styles';
export const CodeBlock = (_a) => {
    var { children = null, className, actions = null } = _a, props = __rest(_a, ["children", "className", "actions"]);
    return (React.createElement("div", Object.assign({ className: css(styles.codeBlock, className) }, props),
        React.createElement("div", { className: css(styles.codeBlockHeader) },
            React.createElement("div", { className: css(styles.codeBlockActions) }, actions && actions)),
        React.createElement("div", { className: css(styles.codeBlockContent) }, children)));
};
CodeBlock.displayName = 'CodeBlock';
//# sourceMappingURL=CodeBlock.js.map