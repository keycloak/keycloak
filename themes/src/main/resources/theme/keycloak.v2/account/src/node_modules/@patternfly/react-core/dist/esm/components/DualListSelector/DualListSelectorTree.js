import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DualListSelector/dual-list-selector';
import { DualListSelectorTreeItem } from './DualListSelectorTreeItem';
export const DualListSelectorTree = (_a) => {
    var { data, hasBadges = false, isNested = false, defaultAllExpanded = false, onOptionCheck, isDisabled = false } = _a, props = __rest(_a, ["data", "hasBadges", "isNested", "defaultAllExpanded", "onOptionCheck", "isDisabled"]);
    const dataToRender = typeof data === 'function' ? data() : data;
    const tree = dataToRender.map(item => (React.createElement(DualListSelectorTreeItem, Object.assign({ key: item.id, text: item.text, id: item.id, defaultExpanded: item.defaultExpanded !== undefined ? item.defaultExpanded : defaultAllExpanded, onOptionCheck: onOptionCheck, isChecked: item.isChecked, checkProps: item.checkProps, hasBadge: item.hasBadge !== undefined ? item.hasBadge : hasBadges, badgeProps: item.badgeProps, itemData: item, isDisabled: isDisabled, useMemo: true }, (item.children && {
        children: (React.createElement(DualListSelectorTree, { isNested: true, data: item.children, hasBadges: hasBadges, defaultAllExpanded: defaultAllExpanded, onOptionCheck: onOptionCheck, isDisabled: isDisabled }))
    })))));
    return isNested ? (React.createElement("ul", Object.assign({ className: css(styles.dualListSelectorList), role: "group" }, props), tree)) : (React.createElement(React.Fragment, null, tree));
};
DualListSelectorTree.displayName = 'DualListSelectorTree';
//# sourceMappingURL=DualListSelectorTree.js.map