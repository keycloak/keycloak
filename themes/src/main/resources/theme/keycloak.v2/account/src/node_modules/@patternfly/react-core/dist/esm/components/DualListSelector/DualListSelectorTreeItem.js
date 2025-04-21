import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DualListSelector/dual-list-selector';
import { css } from '@patternfly/react-styles';
import { Badge } from '../Badge';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import { flattenTree } from './treeUtils';
import { DualListSelectorListContext } from './DualListSelectorContext';
const DualListSelectorTreeItemBase = (_a) => {
    var { onOptionCheck, children, className, id, text, defaultExpanded, hasBadge, isChecked, checkProps, badgeProps, itemData, isDisabled = false, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    useMemo } = _a, props = __rest(_a, ["onOptionCheck", "children", "className", "id", "text", "defaultExpanded", "hasBadge", "isChecked", "checkProps", "badgeProps", "itemData", "isDisabled", "useMemo"]);
    const ref = React.useRef(null);
    const [isExpanded, setIsExpanded] = React.useState(defaultExpanded || false);
    const { setFocusedOption } = React.useContext(DualListSelectorListContext);
    React.useEffect(() => {
        setIsExpanded(defaultExpanded);
    }, [defaultExpanded]);
    return (React.createElement("li", Object.assign({ className: css(styles.dualListSelectorListItem, className, children && styles.modifiers.expandable, isExpanded && styles.modifiers.expanded, isDisabled && styles.modifiers.disabled), id: id }, props, { "aria-selected": isChecked, role: "treeitem" }, (isExpanded && { 'aria-expanded': 'true' })),
        React.createElement("div", { className: css(styles.dualListSelectorListItemRow, isChecked && styles.modifiers.selected, styles.modifiers.check) },
            React.createElement("div", { className: css(styles.dualListSelectorItem), ref: ref, tabIndex: -1, onClick: isDisabled
                    ? undefined
                    : evt => {
                        onOptionCheck && onOptionCheck(evt, !isChecked, itemData);
                        setFocusedOption(id);
                    } },
                React.createElement("span", { className: css(styles.dualListSelectorItemMain) },
                    children && (React.createElement("div", { className: css(styles.dualListSelectorItemToggle), onClick: e => {
                            if (children) {
                                setIsExpanded(!isExpanded);
                            }
                            e.stopPropagation();
                        }, onKeyDown: (e) => {
                            if (e.key === ' ' || e.key === 'Enter') {
                                document.activeElement.click();
                                e.preventDefault();
                            }
                        }, tabIndex: -1 },
                        React.createElement("span", { className: css(styles.dualListSelectorItemToggleIcon) },
                            React.createElement(AngleRightIcon, { "aria-hidden": true })))),
                    React.createElement("span", { className: css(styles.dualListSelectorItemCheck) },
                        React.createElement("input", Object.assign({ type: "checkbox", onChange: (evt) => {
                                onOptionCheck && onOptionCheck(evt, !isChecked, itemData);
                                setFocusedOption(id);
                            }, onClick: (evt) => evt.stopPropagation(), onKeyDown: (e) => {
                                if (e.key === ' ' || e.key === 'Enter') {
                                    onOptionCheck && onOptionCheck(e, !isChecked, itemData);
                                    setFocusedOption(id);
                                    e.preventDefault();
                                }
                            }, ref: elem => elem && (elem.indeterminate = isChecked === null), checked: isChecked || false, tabIndex: -1 }, checkProps))),
                    React.createElement("span", { className: css(styles.dualListSelectorItemText) }, text),
                    hasBadge && children && (React.createElement("span", { className: css(styles.dualListSelectorItemCount) },
                        React.createElement(Badge, Object.assign({}, badgeProps), flattenTree(children.props.data).length)))))),
        isExpanded && children));
};
export const DualListSelectorTreeItem = React.memo(DualListSelectorTreeItemBase, (prevProps, nextProps) => {
    if (!nextProps.useMemo) {
        return false;
    }
    if (prevProps.className !== nextProps.className ||
        prevProps.text !== nextProps.text ||
        prevProps.id !== nextProps.id ||
        prevProps.defaultExpanded !== nextProps.defaultExpanded ||
        prevProps.checkProps !== nextProps.checkProps ||
        prevProps.hasBadge !== nextProps.hasBadge ||
        prevProps.badgeProps !== nextProps.badgeProps ||
        prevProps.isChecked !== nextProps.isChecked ||
        prevProps.itemData !== nextProps.itemData) {
        return false;
    }
    return true;
});
DualListSelectorTreeItem.displayName = 'DualListSelectorTreeItem';
//# sourceMappingURL=DualListSelectorTreeItem.js.map