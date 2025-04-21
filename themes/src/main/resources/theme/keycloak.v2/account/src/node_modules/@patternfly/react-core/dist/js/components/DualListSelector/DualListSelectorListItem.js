"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DualListSelectorListItem = exports.DualListSelectorListItemBase = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const dual_list_selector_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DualListSelector/dual-list-selector"));
const react_styles_1 = require("@patternfly/react-styles");
const helpers_1 = require("../../helpers");
const grip_vertical_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/grip-vertical-icon'));
const Button_1 = require("../Button");
const DualListSelectorContext_1 = require("./DualListSelectorContext");
const DualListSelectorListItemBase = (_a) => {
    var { onOptionSelect, orderIndex, children, className, id = helpers_1.getUniqueId('dual-list-selector-list-item'), isSelected, innerRef, isDraggable = false, isDisabled, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    draggableButtonAriaLabel = 'Reorder option' } = _a, props = tslib_1.__rest(_a, ["onOptionSelect", "orderIndex", "children", "className", "id", "isSelected", "innerRef", "isDraggable", "isDisabled", "draggableButtonAriaLabel"]);
    const ref = innerRef || React.useRef(null);
    const { setFocusedOption } = React.useContext(DualListSelectorContext_1.DualListSelectorListContext);
    return (React.createElement("li", Object.assign({ className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorListItem, className, isDisabled && dual_list_selector_1.default.modifiers.disabled), key: orderIndex, onClick: isDisabled
            ? undefined
            : (e) => {
                setFocusedOption(id);
                onOptionSelect(e, id);
            }, onKeyDown: (e) => {
            if (e.key === ' ' || e.key === 'Enter') {
                document.activeElement.click();
                e.preventDefault();
            }
        }, "aria-selected": isSelected, id: id, ref: ref, role: "option", tabIndex: -1 }, props),
        React.createElement("div", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorListItemRow, isSelected && dual_list_selector_1.default.modifiers.selected) },
            isDraggable && !isDisabled && (React.createElement("div", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorDraggable) },
                React.createElement(Button_1.Button, { variant: Button_1.ButtonVariant.plain, component: "span" },
                    React.createElement(grip_vertical_icon_1.default, { style: { verticalAlign: '-0.3em' } })))),
            React.createElement("span", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorItem) },
                React.createElement("span", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorItemMain) },
                    React.createElement("span", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorItemText) }, children))))));
};
exports.DualListSelectorListItemBase = DualListSelectorListItemBase;
exports.DualListSelectorListItemBase.displayName = 'DualListSelectorListItemBase';
exports.DualListSelectorListItem = React.forwardRef((props, ref) => (React.createElement(exports.DualListSelectorListItemBase, Object.assign({ innerRef: ref }, props))));
exports.DualListSelectorListItem.displayName = 'DualListSelectorListItem';
//# sourceMappingURL=DualListSelectorListItem.js.map