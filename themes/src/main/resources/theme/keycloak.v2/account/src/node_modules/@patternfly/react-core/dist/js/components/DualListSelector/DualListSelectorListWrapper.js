"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DualListSelectorListWrapper = exports.DualListSelectorListWrapperBase = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const dual_list_selector_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DualListSelector/dual-list-selector"));
const react_styles_1 = require("@patternfly/react-styles");
const helpers_1 = require("../../helpers");
const DualListSelectorList_1 = require("./DualListSelectorList");
const DualListSelectorContext_1 = require("./DualListSelectorContext");
const DualListSelectorListWrapperBase = (_a) => {
    var { className, children, 'aria-labelledby': ariaLabelledBy, innerRef, options = [], selectedOptions = [], onOptionSelect, displayOption, id = helpers_1.getUniqueId('dual-list-selector-list'), isDisabled = false } = _a, props = tslib_1.__rest(_a, ["className", "children", 'aria-labelledby', "innerRef", "options", "selectedOptions", "onOptionSelect", "displayOption", "id", "isDisabled"]);
    const [focusedOption, setFocusedOption] = React.useState('');
    const menuRef = innerRef || React.useRef(null);
    const { isTree } = React.useContext(DualListSelectorContext_1.DualListSelectorContext);
    // sets up keyboard focus handling for the dual list selector menu child of the pane. This keyboard
    // handling is applied whether the pane is dynamically built or passed via the children prop.
    const handleKeys = (event) => {
        if (!menuRef.current ||
            (menuRef.current !== event.target.closest('.pf-c-dual-list-selector__menu') &&
                !Array.from(menuRef.current.getElementsByClassName('pf-c-dual-list-selector__menu')).includes(event.target.closest('.pf-c-dual-list-selector__menu')))) {
            return;
        }
        event.stopImmediatePropagation();
        const validOptions = isTree
            ? Array.from(menuRef.current.querySelectorAll('.pf-c-dual-list-selector__item-toggle, .pf-c-dual-list-selector__item-check > input'))
            : Array.from(menuRef.current.getElementsByTagName('LI')).filter(el => !el.classList.contains('pf-m-disabled'));
        const activeElement = document.activeElement;
        helpers_1.handleArrows(event, validOptions, (element) => activeElement.contains(element), (element) => {
            if (element.classList.contains('.pf-c-dual-list-selector__list-item')) {
                setFocusedOption(element.id);
            }
            else {
                setFocusedOption(element.closest('.pf-c-dual-list-selector__list-item').id);
            }
            return element;
        }, ['.pf-c-dual-list-selector__item-toggle', '.pf-c-dual-list-selector__item-check > input'], undefined, false, false, false);
    };
    React.useEffect(() => {
        window.addEventListener('keydown', handleKeys);
        return () => {
            window.removeEventListener('keydown', handleKeys);
        };
    }, [menuRef.current]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorMenu, className), ref: menuRef, tabIndex: 0 }, props),
        React.createElement(DualListSelectorContext_1.DualListSelectorListContext.Provider, { value: {
                setFocusedOption,
                isTree,
                focusedOption,
                ariaLabelledBy,
                displayOption,
                selectedOptions,
                id,
                options,
                onOptionSelect,
                isDisabled
            } }, children ? children : React.createElement(DualListSelectorList_1.DualListSelectorList, null))));
};
exports.DualListSelectorListWrapperBase = DualListSelectorListWrapperBase;
exports.DualListSelectorListWrapperBase.displayName = 'DualListSelectorListWrapperBase';
exports.DualListSelectorListWrapper = React.forwardRef((props, ref) => (React.createElement(exports.DualListSelectorListWrapperBase, Object.assign({ innerRef: ref }, props))));
exports.DualListSelectorListWrapper.displayName = 'DualListSelectorListWrapper';
//# sourceMappingURL=DualListSelectorListWrapper.js.map