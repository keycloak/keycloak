import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
export const DataListItemCells = (_a) => {
    var { className = '', dataListCells, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    rowid = '' } = _a, props = __rest(_a, ["className", "dataListCells", "rowid"]);
    return (React.createElement("div", Object.assign({ className: css(styles.dataListItemContent, className) }, props), dataListCells));
};
DataListItemCells.displayName = 'DataListItemCells';
//# sourceMappingURL=DataListItemCells.js.map