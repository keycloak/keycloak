import React, { useState, useEffect } from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/TreeView/tree-view';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import { Badge } from '../Badge';
import { GenerateId } from '../../helpers/GenerateId/GenerateId';
const TreeViewListItemBase = ({ name, title, id, isExpanded, defaultExpanded = false, children = null, onSelect, onCheck, hasCheck = false, checkProps = {
    checked: false
}, hasBadge = false, customBadgeContent, badgeProps = { isRead: true }, isCompact, activeItems = [], itemData, parentItem, icon, expandedIcon, action, compareItems, 
// eslint-disable-next-line @typescript-eslint/no-unused-vars
useMemo }) => {
    const [internalIsExpanded, setIsExpanded] = useState(defaultExpanded);
    useEffect(() => {
        if (isExpanded !== undefined && isExpanded !== null) {
            setIsExpanded(isExpanded);
        }
        else if (defaultExpanded !== undefined && defaultExpanded !== null) {
            setIsExpanded(internalIsExpanded || defaultExpanded);
        }
    }, [isExpanded, defaultExpanded]);
    const Component = hasCheck ? 'div' : 'button';
    const ToggleComponent = hasCheck ? 'button' : 'div';
    const renderToggle = (randomId) => (React.createElement(ToggleComponent, Object.assign({ className: css(styles.treeViewNodeToggle), onClick: () => {
            if (hasCheck) {
                setIsExpanded(!internalIsExpanded);
            }
        } }, (hasCheck && { 'aria-labelledby': `label-${randomId}` }), { tabIndex: -1 }),
        React.createElement("span", { className: css(styles.treeViewNodeToggleIcon) },
            React.createElement(AngleRightIcon, { "aria-hidden": "true" }))));
    const renderCheck = (randomId) => (React.createElement("span", { className: css(styles.treeViewNodeCheck) },
        React.createElement("input", Object.assign({ type: "checkbox", onChange: (evt) => onCheck && onCheck(evt, itemData, parentItem), onClick: (evt) => evt.stopPropagation(), ref: elem => elem && (elem.indeterminate = checkProps.checked === null) }, checkProps, { checked: checkProps.checked === null ? false : checkProps.checked, id: randomId, tabIndex: -1 }))));
    const iconRendered = (React.createElement("span", { className: css(styles.treeViewNodeIcon) },
        !internalIsExpanded && icon,
        internalIsExpanded && (expandedIcon || icon)));
    const renderNodeContent = (randomId) => {
        const content = (React.createElement(React.Fragment, null,
            isCompact && title && React.createElement("span", { className: css(styles.treeViewNodeTitle) }, title),
            hasCheck ? (React.createElement("label", { className: css(styles.treeViewNodeText), htmlFor: randomId, id: `label-${randomId}` }, name)) : (React.createElement("span", { className: css(styles.treeViewNodeText) }, name))));
        return isCompact ? React.createElement("div", { className: css(styles.treeViewNodeContent) }, content) : content;
    };
    const badgeRendered = (React.createElement(React.Fragment, null,
        hasBadge && children && (React.createElement("span", { className: css(styles.treeViewNodeCount) },
            React.createElement(Badge, Object.assign({}, badgeProps), customBadgeContent ? customBadgeContent : children.props.data.length))),
        hasBadge && !children && customBadgeContent !== undefined && (React.createElement("span", { className: css(styles.treeViewNodeCount) },
            React.createElement(Badge, Object.assign({}, badgeProps), customBadgeContent)))));
    return (React.createElement("li", Object.assign({ id: id, className: css(styles.treeViewListItem, internalIsExpanded && styles.modifiers.expanded) }, (internalIsExpanded && { 'aria-expanded': 'true' }), { role: "treeitem", tabIndex: -1 }),
        React.createElement("div", { className: css(styles.treeViewContent) },
            React.createElement(GenerateId, { prefix: "checkbox-id" }, randomId => (React.createElement(Component, { className: css(styles.treeViewNode, !children &&
                    activeItems &&
                    activeItems.length > 0 &&
                    activeItems.some(item => compareItems && item && compareItems(item, itemData))
                    ? styles.modifiers.current
                    : ''), onClick: (evt) => {
                    if (!hasCheck) {
                        onSelect && onSelect(evt, itemData, parentItem);
                        if (children && evt.isDefaultPrevented() !== true) {
                            setIsExpanded(!internalIsExpanded);
                        }
                    }
                }, tabIndex: -1 },
                React.createElement("div", { className: css(styles.treeViewNodeContainer) },
                    children && renderToggle(randomId),
                    hasCheck && renderCheck(randomId),
                    icon && iconRendered,
                    renderNodeContent(randomId),
                    badgeRendered)))),
            action && React.createElement("div", { className: css(styles.treeViewAction) }, action)),
        internalIsExpanded && children));
};
export const TreeViewListItem = React.memo(TreeViewListItemBase, (prevProps, nextProps) => {
    if (!nextProps.useMemo) {
        return false;
    }
    const prevIncludes = prevProps.activeItems &&
        prevProps.activeItems.length > 0 &&
        prevProps.activeItems.some(item => prevProps.compareItems && item && prevProps.compareItems(item, prevProps.itemData));
    const nextIncludes = nextProps.activeItems &&
        nextProps.activeItems.length > 0 &&
        nextProps.activeItems.some(item => nextProps.compareItems && item && nextProps.compareItems(item, nextProps.itemData));
    if (prevIncludes || nextIncludes) {
        return false;
    }
    if (prevProps.name !== nextProps.name ||
        prevProps.title !== nextProps.title ||
        prevProps.id !== nextProps.id ||
        prevProps.isExpanded !== nextProps.isExpanded ||
        prevProps.defaultExpanded !== nextProps.defaultExpanded ||
        prevProps.onSelect !== nextProps.onSelect ||
        prevProps.onCheck !== nextProps.onCheck ||
        prevProps.hasCheck !== nextProps.hasCheck ||
        prevProps.checkProps !== nextProps.checkProps ||
        prevProps.hasBadge !== nextProps.hasBadge ||
        prevProps.customBadgeContent !== nextProps.customBadgeContent ||
        prevProps.badgeProps !== nextProps.badgeProps ||
        prevProps.isCompact !== nextProps.isCompact ||
        prevProps.icon !== nextProps.icon ||
        prevProps.expandedIcon !== nextProps.expandedIcon ||
        prevProps.action !== nextProps.action ||
        prevProps.parentItem !== nextProps.parentItem ||
        prevProps.itemData !== nextProps.itemData) {
        return false;
    }
    return true;
});
TreeViewListItem.displayName = 'TreeViewListItem';
//# sourceMappingURL=TreeViewListItem.js.map