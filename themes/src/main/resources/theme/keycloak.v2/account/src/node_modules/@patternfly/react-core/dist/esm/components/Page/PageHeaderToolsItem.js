import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Page/page';
import { css } from '@patternfly/react-styles';
import { formatBreakpointMods } from '../../helpers/util';
import { PageContext } from '../Page/Page';
export const PageHeaderToolsItem = (_a) => {
    var { children, id, className, visibility, isSelected } = _a, props = __rest(_a, ["children", "id", "className", "visibility", "isSelected"]);
    const { width, getBreakpoint } = React.useContext(PageContext);
    return (React.createElement("div", Object.assign({ className: css(styles.pageHeaderToolsItem, isSelected && styles.modifiers.selected, formatBreakpointMods(visibility, styles, '', getBreakpoint(width)), className), id: id }, props), children));
};
PageHeaderToolsItem.displayName = 'PageHeaderToolsItem';
//# sourceMappingURL=PageHeaderToolsItem.js.map