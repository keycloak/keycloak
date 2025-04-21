import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/TreeView/tree-view';
import formStyles from '@patternfly/react-styles/css/components/FormControl/form-control';
export const TreeViewSearch = (_a) => {
    var { className, onSearch, id, name, 'aria-label': ariaLabel } = _a, props = __rest(_a, ["className", "onSearch", "id", "name", 'aria-label']);
    return (React.createElement("div", { className: css(styles.treeViewSearch, className) },
        React.createElement("input", Object.assign({ className: css(formStyles.formControl, formStyles.modifiers.search), onChange: onSearch, id: id, name: name, "aria-label": ariaLabel, type: "search" }, props))));
};
TreeViewSearch.displayName = 'TreeViewSearch';
//# sourceMappingURL=TreeViewSearch.js.map