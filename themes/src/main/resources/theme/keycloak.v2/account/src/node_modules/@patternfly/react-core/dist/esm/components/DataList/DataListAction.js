import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
import { formatBreakpointMods } from '../../helpers/util';
export const DataListAction = (_a) => {
    var { children, className, visibility, 
    /* eslint-disable @typescript-eslint/no-unused-vars */
    id, 'aria-label': ariaLabel, 'aria-labelledby': ariaLabelledBy, isPlainButtonAction } = _a, 
    /* eslint-enable @typescript-eslint/no-unused-vars */
    props = __rest(_a, ["children", "className", "visibility", "id", 'aria-label', 'aria-labelledby', "isPlainButtonAction"]);
    return (React.createElement("div", Object.assign({ className: css(styles.dataListItemAction, formatBreakpointMods(visibility, styles), className) }, props), isPlainButtonAction ? React.createElement("div", { className: css(styles.dataListAction) }, children) : children));
};
DataListAction.displayName = 'DataListAction';
//# sourceMappingURL=DataListAction.js.map