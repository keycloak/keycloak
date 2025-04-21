"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.TreeViewListItem = void 0;
const tslib_1 = require("tslib");
const react_1 = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const tree_view_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/TreeView/tree-view"));
const angle_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-right-icon'));
const Badge_1 = require("../Badge");
const GenerateId_1 = require("../../helpers/GenerateId/GenerateId");
const TreeViewListItemBase = ({ name, title, id, isExpanded, defaultExpanded = false, children = null, onSelect, onCheck, hasCheck = false, checkProps = {
    checked: false
}, hasBadge = false, customBadgeContent, badgeProps = { isRead: true }, isCompact, activeItems = [], itemData, parentItem, icon, expandedIcon, action, compareItems, 
// eslint-disable-next-line @typescript-eslint/no-unused-vars
useMemo }) => {
    const [internalIsExpanded, setIsExpanded] = react_1.useState(defaultExpanded);
    react_1.useEffect(() => {
        if (isExpanded !== undefined && isExpanded !== null) {
            setIsExpanded(isExpanded);
        }
        else if (defaultExpanded !== undefined && defaultExpanded !== null) {
            setIsExpanded(internalIsExpanded || defaultExpanded);
        }
    }, [isExpanded, defaultExpanded]);
    const Component = hasCheck ? 'div' : 'button';
    const ToggleComponent = hasCheck ? 'button' : 'div';
    const renderToggle = (randomId) => (react_1.default.createElement(ToggleComponent, Object.assign({ className: react_styles_1.css(tree_view_1.default.treeViewNodeToggle), onClick: () => {
            if (hasCheck) {
                setIsExpanded(!internalIsExpanded);
            }
        } }, (hasCheck && { 'aria-labelledby': `label-${randomId}` }), { tabIndex: -1 }),
        react_1.default.createElement("span", { className: react_styles_1.css(tree_view_1.default.treeViewNodeToggleIcon) },
            react_1.default.createElement(angle_right_icon_1.default, { "aria-hidden": "true" }))));
    const renderCheck = (randomId) => (react_1.default.createElement("span", { className: react_styles_1.css(tree_view_1.default.treeViewNodeCheck) },
        react_1.default.createElement("input", Object.assign({ type: "checkbox", onChange: (evt) => onCheck && onCheck(evt, itemData, parentItem), onClick: (evt) => evt.stopPropagation(), ref: elem => elem && (elem.indeterminate = checkProps.checked === null) }, checkProps, { checked: checkProps.checked === null ? false : checkProps.checked, id: randomId, tabIndex: -1 }))));
    const iconRendered = (react_1.default.createElement("span", { className: react_styles_1.css(tree_view_1.default.treeViewNodeIcon) },
        !internalIsExpanded && icon,
        internalIsExpanded && (expandedIcon || icon)));
    const renderNodeContent = (randomId) => {
        const content = (react_1.default.createElement(react_1.default.Fragment, null,
            isCompact && title && react_1.default.createElement("span", { className: react_styles_1.css(tree_view_1.default.treeViewNodeTitle) }, title),
            hasCheck ? (react_1.default.createElement("label", { className: react_styles_1.css(tree_view_1.default.treeViewNodeText), htmlFor: randomId, id: `label-${randomId}` }, name)) : (react_1.default.createElement("span", { className: react_styles_1.css(tree_view_1.default.treeViewNodeText) }, name))));
        return isCompact ? react_1.default.createElement("div", { className: react_styles_1.css(tree_view_1.default.treeViewNodeContent) }, content) : content;
    };
    const badgeRendered = (react_1.default.createElement(react_1.default.Fragment, null,
        hasBadge && children && (react_1.default.createElement("span", { className: react_styles_1.css(tree_view_1.default.treeViewNodeCount) },
            react_1.default.createElement(Badge_1.Badge, Object.assign({}, badgeProps), customBadgeContent ? customBadgeContent : children.props.data.length))),
        hasBadge && !children && customBadgeContent !== undefined && (react_1.default.createElement("span", { className: react_styles_1.css(tree_view_1.default.treeViewNodeCount) },
            react_1.default.createElement(Badge_1.Badge, Object.assign({}, badgeProps), customBadgeContent)))));
    return (react_1.default.createElement("li", Object.assign({ id: id, className: react_styles_1.css(tree_view_1.default.treeViewListItem, internalIsExpanded && tree_view_1.default.modifiers.expanded) }, (internalIsExpanded && { 'aria-expanded': 'true' }), { role: "treeitem", tabIndex: -1 }),
        react_1.default.createElement("div", { className: react_styles_1.css(tree_view_1.default.treeViewContent) },
            react_1.default.createElement(GenerateId_1.GenerateId, { prefix: "checkbox-id" }, randomId => (react_1.default.createElement(Component, { className: react_styles_1.css(tree_view_1.default.treeViewNode, !children &&
                    activeItems &&
                    activeItems.length > 0 &&
                    activeItems.some(item => compareItems && item && compareItems(item, itemData))
                    ? tree_view_1.default.modifiers.current
                    : ''), onClick: (evt) => {
                    if (!hasCheck) {
                        onSelect && onSelect(evt, itemData, parentItem);
                        if (children && evt.isDefaultPrevented() !== true) {
                            setIsExpanded(!internalIsExpanded);
                        }
                    }
                }, tabIndex: -1 },
                react_1.default.createElement("div", { className: react_styles_1.css(tree_view_1.default.treeViewNodeContainer) },
                    children && renderToggle(randomId),
                    hasCheck && renderCheck(randomId),
                    icon && iconRendered,
                    renderNodeContent(randomId),
                    badgeRendered)))),
            action && react_1.default.createElement("div", { className: react_styles_1.css(tree_view_1.default.treeViewAction) }, action)),
        internalIsExpanded && children));
};
exports.TreeViewListItem = react_1.default.memo(TreeViewListItemBase, (prevProps, nextProps) => {
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
exports.TreeViewListItem.displayName = 'TreeViewListItem';
//# sourceMappingURL=TreeViewListItem.js.map