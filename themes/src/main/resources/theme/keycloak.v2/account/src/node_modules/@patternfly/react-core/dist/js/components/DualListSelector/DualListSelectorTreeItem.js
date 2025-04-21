"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DualListSelectorTreeItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const dual_list_selector_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DualListSelector/dual-list-selector"));
const react_styles_1 = require("@patternfly/react-styles");
const Badge_1 = require("../Badge");
const angle_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-right-icon'));
const treeUtils_1 = require("./treeUtils");
const DualListSelectorContext_1 = require("./DualListSelectorContext");
const DualListSelectorTreeItemBase = (_a) => {
    var { onOptionCheck, children, className, id, text, defaultExpanded, hasBadge, isChecked, checkProps, badgeProps, itemData, isDisabled = false, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    useMemo } = _a, props = tslib_1.__rest(_a, ["onOptionCheck", "children", "className", "id", "text", "defaultExpanded", "hasBadge", "isChecked", "checkProps", "badgeProps", "itemData", "isDisabled", "useMemo"]);
    const ref = React.useRef(null);
    const [isExpanded, setIsExpanded] = React.useState(defaultExpanded || false);
    const { setFocusedOption } = React.useContext(DualListSelectorContext_1.DualListSelectorListContext);
    React.useEffect(() => {
        setIsExpanded(defaultExpanded);
    }, [defaultExpanded]);
    return (React.createElement("li", Object.assign({ className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorListItem, className, children && dual_list_selector_1.default.modifiers.expandable, isExpanded && dual_list_selector_1.default.modifiers.expanded, isDisabled && dual_list_selector_1.default.modifiers.disabled), id: id }, props, { "aria-selected": isChecked, role: "treeitem" }, (isExpanded && { 'aria-expanded': 'true' })),
        React.createElement("div", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorListItemRow, isChecked && dual_list_selector_1.default.modifiers.selected, dual_list_selector_1.default.modifiers.check) },
            React.createElement("div", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorItem), ref: ref, tabIndex: -1, onClick: isDisabled
                    ? undefined
                    : evt => {
                        onOptionCheck && onOptionCheck(evt, !isChecked, itemData);
                        setFocusedOption(id);
                    } },
                React.createElement("span", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorItemMain) },
                    children && (React.createElement("div", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorItemToggle), onClick: e => {
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
                        React.createElement("span", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorItemToggleIcon) },
                            React.createElement(angle_right_icon_1.default, { "aria-hidden": true })))),
                    React.createElement("span", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorItemCheck) },
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
                    React.createElement("span", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorItemText) }, text),
                    hasBadge && children && (React.createElement("span", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorItemCount) },
                        React.createElement(Badge_1.Badge, Object.assign({}, badgeProps), treeUtils_1.flattenTree(children.props.data).length)))))),
        isExpanded && children));
};
exports.DualListSelectorTreeItem = React.memo(DualListSelectorTreeItemBase, (prevProps, nextProps) => {
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
exports.DualListSelectorTreeItem.displayName = 'DualListSelectorTreeItem';
//# sourceMappingURL=DualListSelectorTreeItem.js.map