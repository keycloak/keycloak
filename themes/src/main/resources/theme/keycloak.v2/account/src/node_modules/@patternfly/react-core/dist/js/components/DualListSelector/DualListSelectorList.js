"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DualListSelectorList = void 0;
const tslib_1 = require("tslib");
const react_styles_1 = require("@patternfly/react-styles");
const dual_list_selector_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DualListSelector/dual-list-selector"));
const DualListSelectorListItem_1 = require("./DualListSelectorListItem");
const React = tslib_1.__importStar(require("react"));
const DualListSelectorContext_1 = require("./DualListSelectorContext");
const DualListSelectorList = (_a) => {
    var { children } = _a, props = tslib_1.__rest(_a, ["children"]);
    const { setFocusedOption, isTree, ariaLabelledBy, focusedOption, displayOption, selectedOptions, id, onOptionSelect, options, isDisabled } = React.useContext(DualListSelectorContext_1.DualListSelectorListContext);
    // only called when options are passed via options prop
    const onOptionClick = (e, index, id) => {
        setFocusedOption(id);
        onOptionSelect(e, index, id);
    };
    return (React.createElement("ul", Object.assign({ className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorList), role: isTree ? 'tree' : 'listbox', "aria-multiselectable": "true", "aria-labelledby": ariaLabelledBy, "aria-activedescendant": focusedOption, "aria-disabled": isDisabled ? 'true' : undefined }, props), options.length === 0
        ? children
        : options.map((option, index) => {
            if (displayOption(option)) {
                return (React.createElement(DualListSelectorListItem_1.DualListSelectorListItem, { key: index, isSelected: selectedOptions.indexOf(index) !== -1, id: `${id}-option-${index}`, onOptionSelect: (e, id) => onOptionClick(e, index, id), orderIndex: index, isDisabled: isDisabled }, option));
            }
            return;
        })));
};
exports.DualListSelectorList = DualListSelectorList;
exports.DualListSelectorList.displayName = 'DualListSelectorList';
//# sourceMappingURL=DualListSelectorList.js.map