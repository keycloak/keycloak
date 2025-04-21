"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ContextSelector = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const context_selector_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/ContextSelector/context-selector"));
const react_styles_1 = require("@patternfly/react-styles");
const search_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/search-icon'));
const ContextSelectorToggle_1 = require("./ContextSelectorToggle");
const ContextSelectorMenuList_1 = require("./ContextSelectorMenuList");
const contextSelectorConstants_1 = require("./contextSelectorConstants");
const Button_1 = require("../Button");
const TextInput_1 = require("../TextInput");
const InputGroup_1 = require("../InputGroup");
const constants_1 = require("../../helpers/constants");
const helpers_1 = require("../../helpers");
const Popper_1 = require("../../helpers/Popper/Popper");
const helpers_2 = require("../../helpers");
// seed for the aria-labelledby ID
let currentId = 0;
const newId = currentId++;
class ContextSelector extends React.Component {
    constructor(props) {
        super(props);
        this.parentRef = React.createRef();
        this.popperRef = React.createRef();
        this.onEnterPressed = (event) => {
            if (event.charCode === constants_1.KEY_CODES.ENTER) {
                this.props.onSearchButtonClick();
            }
        };
        this.state = {
            ouiaStateId: helpers_2.getDefaultOUIAId(ContextSelector.displayName)
        };
    }
    render() {
        const toggleId = `pf-context-selector-toggle-id-${newId}`;
        const screenReaderLabelId = `pf-context-selector-label-id-${newId}`;
        const searchButtonId = `pf-context-selector-search-button-id-${newId}`;
        const _a = this.props, { children, className, isOpen, isFullHeight, onToggle, onSelect, screenReaderLabel, toggleText, searchButtonAriaLabel, searchInputValue, onSearchInputChange, searchInputPlaceholder, onSearchButtonClick, menuAppendTo, ouiaId, ouiaSafe, isPlain, isText, footer, disableFocusTrap, isFlipEnabled } = _a, props = tslib_1.__rest(_a, ["children", "className", "isOpen", "isFullHeight", "onToggle", "onSelect", "screenReaderLabel", "toggleText", "searchButtonAriaLabel", "searchInputValue", "onSearchInputChange", "searchInputPlaceholder", "onSearchButtonClick", "menuAppendTo", "ouiaId", "ouiaSafe", "isPlain", "isText", "footer", "disableFocusTrap", "isFlipEnabled"]);
        const menuContainer = (React.createElement("div", Object.assign({ className: react_styles_1.css(context_selector_1.default.contextSelectorMenu) }, (isFlipEnabled && { style: { position: 'revert' } })), isOpen && (React.createElement(helpers_1.FocusTrap, { active: !disableFocusTrap, focusTrapOptions: { clickOutsideDeactivates: true, tabbableOptions: { displayCheck: 'none' } } },
            React.createElement("div", { className: react_styles_1.css(context_selector_1.default.contextSelectorMenuSearch) },
                React.createElement(InputGroup_1.InputGroup, null,
                    React.createElement(TextInput_1.TextInput, { value: searchInputValue, type: "search", placeholder: searchInputPlaceholder, onChange: onSearchInputChange, onKeyPress: this.onEnterPressed, "aria-labelledby": searchButtonId }),
                    React.createElement(Button_1.Button, { variant: Button_1.ButtonVariant.control, "aria-label": searchButtonAriaLabel, id: searchButtonId, onClick: onSearchButtonClick },
                        React.createElement(search_icon_1.default, { "aria-hidden": "true" })))),
            React.createElement(contextSelectorConstants_1.ContextSelectorContext.Provider, { value: { onSelect } },
                React.createElement(ContextSelectorMenuList_1.ContextSelectorMenuList, { isOpen: isOpen }, children)),
            footer))));
        const popperContainer = (React.createElement("div", Object.assign({ className: react_styles_1.css(context_selector_1.default.contextSelector, isOpen && context_selector_1.default.modifiers.expanded, className), ref: this.popperRef }, props), isOpen && menuContainer));
        const mainContainer = (React.createElement("div", Object.assign({ className: react_styles_1.css(context_selector_1.default.contextSelector, isOpen && context_selector_1.default.modifiers.expanded, isFullHeight && context_selector_1.default.modifiers.fullHeight, className), ref: this.parentRef }, helpers_2.getOUIAProps(ContextSelector.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe), props),
            screenReaderLabel && (React.createElement("span", { id: screenReaderLabelId, hidden: true }, screenReaderLabel)),
            React.createElement(ContextSelectorToggle_1.ContextSelectorToggle, { onToggle: onToggle, isOpen: isOpen, toggleText: toggleText, id: toggleId, parentRef: menuAppendTo === 'inline' ? this.parentRef : this.popperRef, "aria-labelledby": `${screenReaderLabelId} ${toggleId}`, isPlain: isPlain, isText: isText }),
            isOpen && menuAppendTo === 'inline' && menuContainer));
        const getParentElement = () => {
            if (this.parentRef && this.parentRef.current) {
                return this.parentRef.current.parentElement;
            }
            return null;
        };
        return menuAppendTo === 'inline' ? (mainContainer) : (React.createElement(Popper_1.Popper, { trigger: mainContainer, popper: popperContainer, appendTo: menuAppendTo === 'parent' ? getParentElement() : menuAppendTo, isVisible: isOpen }));
    }
}
exports.ContextSelector = ContextSelector;
ContextSelector.displayName = 'ContextSelector';
ContextSelector.defaultProps = {
    children: null,
    className: '',
    isOpen: false,
    onToggle: () => undefined,
    onSelect: () => undefined,
    screenReaderLabel: '',
    toggleText: '',
    searchButtonAriaLabel: 'Search menu items',
    searchInputValue: '',
    onSearchInputChange: () => undefined,
    searchInputPlaceholder: 'Search',
    onSearchButtonClick: () => undefined,
    menuAppendTo: 'inline',
    ouiaSafe: true,
    disableFocusTrap: false,
    footer: null,
    isPlain: false,
    isText: false,
    isFlipEnabled: false
};
//# sourceMappingURL=ContextSelector.js.map