import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Page/page';
import { css } from '@patternfly/react-styles';
import { formatBreakpointMods } from '../../helpers/util';
import { PageContext } from '../Page/Page';
export const PageHeaderToolsGroup = (_a) => {
    var { children, className, visibility } = _a, props = __rest(_a, ["children", "className", "visibility"]);
    const { width, getBreakpoint } = React.useContext(PageContext);
    return (React.createElement("div", Object.assign({ className: css(styles.pageHeaderToolsGroup, formatBreakpointMods(visibility, styles, '', getBreakpoint(width)), className) }, props), children));
};
PageHeaderToolsGroup.displayName = 'PageHeaderToolsGroup';
//# sourceMappingURL=PageHeaderToolsGroup.js.map