import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/SimpleList/simple-list';
export const SimpleListGroup = (_a) => {
    var { children = null, className = '', title = '', titleClassName = '', id = '' } = _a, props = __rest(_a, ["children", "className", "title", "titleClassName", "id"]);
    return (React.createElement("section", Object.assign({ className: css(styles.simpleListSection) }, props),
        React.createElement("h2", { id: id, className: css(styles.simpleListTitle, titleClassName), "aria-hidden": "true" }, title),
        React.createElement("ul", { className: css(className), "aria-labelledby": id }, children)));
};
SimpleListGroup.displayName = 'SimpleListGroup';
//# sourceMappingURL=SimpleListGroup.js.map