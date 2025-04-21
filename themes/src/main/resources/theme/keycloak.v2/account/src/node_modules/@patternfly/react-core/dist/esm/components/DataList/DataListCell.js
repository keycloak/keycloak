import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
export const DataListCell = (_a) => {
    var { children = null, className = '', width = 1, isFilled = true, alignRight = false, isIcon = false, wrapModifier = null } = _a, props = __rest(_a, ["children", "className", "width", "isFilled", "alignRight", "isIcon", "wrapModifier"]);
    return (React.createElement("div", Object.assign({ className: css(styles.dataListCell, width > 1 && styles.modifiers[`flex_${width}`], !isFilled && styles.modifiers.noFill, alignRight && styles.modifiers.alignRight, isIcon && styles.modifiers.icon, className, wrapModifier && styles.modifiers[wrapModifier]) }, props), children));
};
DataListCell.displayName = 'DataListCell';
//# sourceMappingURL=DataListCell.js.map