import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DualListSelector/dual-list-selector';
import { css } from '@patternfly/react-styles';
import { getUniqueId, handleArrows } from '../../helpers';
import { DualListSelectorList } from './DualListSelectorList';
import { DualListSelectorContext, DualListSelectorListContext } from './DualListSelectorContext';
export const DualListSelectorListWrapperBase = (_a) => {
    var { className, children, 'aria-labelledby': ariaLabelledBy, innerRef, options = [], selectedOptions = [], onOptionSelect, displayOption, id = getUniqueId('dual-list-selector-list'), isDisabled = false } = _a, props = __rest(_a, ["className", "children", 'aria-labelledby', "innerRef", "options", "selectedOptions", "onOptionSelect", "displayOption", "id", "isDisabled"]);
    const [focusedOption, setFocusedOption] = React.useState('');
    const menuRef = innerRef || React.useRef(null);
    const { isTree } = React.useContext(DualListSelectorContext);
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
        handleArrows(event, validOptions, (element) => activeElement.contains(element), (element) => {
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
    return (React.createElement("div", Object.assign({ className: css(styles.dualListSelectorMenu, className), ref: menuRef, tabIndex: 0 }, props),
        React.createElement(DualListSelectorListContext.Provider, { value: {
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
            } }, children ? children : React.createElement(DualListSelectorList, null))));
};
DualListSelectorListWrapperBase.displayName = 'DualListSelectorListWrapperBase';
export const DualListSelectorListWrapper = React.forwardRef((props, ref) => (React.createElement(DualListSelectorListWrapperBase, Object.assign({ innerRef: ref }, props))));
DualListSelectorListWrapper.displayName = 'DualListSelectorListWrapper';
//# sourceMappingURL=DualListSelectorListWrapper.js.map