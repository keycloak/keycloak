"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DualListSelectorTree = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const dual_list_selector_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DualListSelector/dual-list-selector"));
const DualListSelectorTreeItem_1 = require("./DualListSelectorTreeItem");
const DualListSelectorTree = (_a) => {
    var { data, hasBadges = false, isNested = false, defaultAllExpanded = false, onOptionCheck, isDisabled = false } = _a, props = tslib_1.__rest(_a, ["data", "hasBadges", "isNested", "defaultAllExpanded", "onOptionCheck", "isDisabled"]);
    const dataToRender = typeof data === 'function' ? data() : data;
    const tree = dataToRender.map(item => (React.createElement(DualListSelectorTreeItem_1.DualListSelectorTreeItem, Object.assign({ key: item.id, text: item.text, id: item.id, defaultExpanded: item.defaultExpanded !== undefined ? item.defaultExpanded : defaultAllExpanded, onOptionCheck: onOptionCheck, isChecked: item.isChecked, checkProps: item.checkProps, hasBadge: item.hasBadge !== undefined ? item.hasBadge : hasBadges, badgeProps: item.badgeProps, itemData: item, isDisabled: isDisabled, useMemo: true }, (item.children && {
        children: (React.createElement(exports.DualListSelectorTree, { isNested: true, data: item.children, hasBadges: hasBadges, defaultAllExpanded: defaultAllExpanded, onOptionCheck: onOptionCheck, isDisabled: isDisabled }))
    })))));
    return isNested ? (React.createElement("ul", Object.assign({ className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorList), role: "group" }, props), tree)) : (React.createElement(React.Fragment, null, tree));
};
exports.DualListSelectorTree = DualListSelectorTree;
exports.DualListSelectorTree.displayName = 'DualListSelectorTree';
//# sourceMappingURL=DualListSelectorTree.js.map