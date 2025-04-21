import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
export const DataListContent = (_a) => {
    var { className = '', children = null, id = '', isHidden = false, 'aria-label': ariaLabel, hasNoPadding = false, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    rowid = '' } = _a, props = __rest(_a, ["className", "children", "id", "isHidden", 'aria-label', "hasNoPadding", "rowid"]);
    return (React.createElement("section", Object.assign({ id: id, className: css(styles.dataListExpandableContent, className), hidden: isHidden, "aria-label": ariaLabel }, props),
        React.createElement("div", { className: css(styles.dataListExpandableContentBody, hasNoPadding && styles.modifiers.noPadding) }, children)));
};
DataListContent.displayName = 'DataListContent';
//# sourceMappingURL=DataListContent.js.map