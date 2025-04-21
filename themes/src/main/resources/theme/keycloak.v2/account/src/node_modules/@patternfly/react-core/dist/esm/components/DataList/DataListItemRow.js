import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
export const DataListItemRow = (_a) => {
    var { children, className = '', rowid = '', wrapModifier = null } = _a, props = __rest(_a, ["children", "className", "rowid", "wrapModifier"]);
    return (React.createElement("div", Object.assign({ className: css(styles.dataListItemRow, className, wrapModifier && styles.modifiers[wrapModifier]) }, props), React.Children.map(children, child => React.isValidElement(child) &&
        React.cloneElement(child, {
            rowid
        }))));
};
DataListItemRow.displayName = 'DataListItemRow';
//# sourceMappingURL=DataListItemRow.js.map