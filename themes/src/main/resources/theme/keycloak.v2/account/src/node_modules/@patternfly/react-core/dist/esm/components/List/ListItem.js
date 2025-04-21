import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/List/list';
import { css } from '@patternfly/react-styles';
export const ListItem = (_a) => {
    var { icon = null, children = null } = _a, props = __rest(_a, ["icon", "children"]);
    return (React.createElement("li", Object.assign({ className: css(icon && styles.listItem) }, props),
        icon && React.createElement("span", { className: css(styles.listItemIcon) }, icon),
        children));
};
ListItem.displayName = 'ListItem';
//# sourceMappingURL=ListItem.js.map