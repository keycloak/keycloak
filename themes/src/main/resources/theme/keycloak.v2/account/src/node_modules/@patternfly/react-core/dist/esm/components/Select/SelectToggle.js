import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Select/select';
import buttonStyles from '@patternfly/react-styles/css/components/Button/button';
import { css } from '@patternfly/react-styles';
import CaretDownIcon from '@patternfly/react-icons/dist/esm/icons/caret-down-icon';
import { SelectVariant, SelectFooterTabbableItems } from './selectConstants';
import { findTabbableElements } from '../../helpers/util';
import { KeyTypes } from '../../helpers/constants';
export class SelectToggle extends React.Component {
    constructor(props) {
        super(props);
        this.onDocClick = (event) => {
            const { parentRef, menuRef, footerRef, isOpen, onToggle, onClose } = this.props;
            const clickedOnToggle = parentRef && parentRef.current && parentRef.current.contains(event.target);
            const clickedWithinMenu = menuRef && menuRef.current && menuRef.current.contains && menuRef.current.contains(event.target);
            const clickedWithinFooter = footerRef && footerRef.current && footerRef.current.contains && footerRef.current.contains(event.target);
            if (isOpen && !(clickedOnToggle || clickedWithinMenu || clickedWithinFooter)) {
                onToggle(false, event);
                onClose();
            }
        };
        this.handleGlobalKeys = (event) => {
            const { parentRef, menuRef, hasFooter, footerRef, isOpen, variant, onToggle, onClose, moveFocusToLastMenuItem } = this.props;
            const escFromToggle = parentRef && parentRef.current && parentRef.current.contains(event.target);
            const escFromWithinMenu = menuRef && menuRef.current && menuRef.current.contains && menuRef.current.contains(event.target);
            if (isOpen &&
                event.key === KeyTypes.Tab &&
                (variant === SelectVariant.typeahead || variant === SelectVariant.typeaheadMulti)) {
                this.props.handleTypeaheadKeys('tab', event.shiftKey);
                event.preventDefault();
                return;
            }
            if (isOpen && event.key === KeyTypes.Tab && hasFooter) {
                const tabbableItems = findTabbableElements(footerRef, SelectFooterTabbableItems);
                // If no tabbable item in footer close select
                if (tabbableItems.length <= 0) {
                    onToggle(false, event);
                    onClose();
                    this.toggle.current.focus();
                    return;
                }
                else {
                    // if current element is not in footer, tab to first tabbable element in footer, or close if shift clicked
                    const currentElementIndex = tabbableItems.findIndex((item) => item === document.activeElement);
                    if (currentElementIndex === -1) {
                        if (event.shiftKey) {
                            if (variant !== 'checkbox') {
                                // only close non checkbox variation on shift clicked
                                onToggle(false, event);
                                onClose();
                                this.toggle.current.focus();
                            }
                        }
                        else {
                            // tab to footer
                            tabbableItems[0].focus();
                            return;
                        }
                    }
                    // Current element is in footer.
                    if (event.shiftKey) {
                        // Move focus back to menu if current tab index is 0
                        if (currentElementIndex === 0) {
                            moveFocusToLastMenuItem();
                            event.preventDefault();
                        }
                        return;
                    }
                    // Tab to next element in footer or close if there are none
                    if (currentElementIndex + 1 < tabbableItems.length) {
                        tabbableItems[currentElementIndex + 1].focus();
                    }
                    else {
                        // no more footer items close menu
                        onToggle(false, event);
                        onClose();
                        this.toggle.current.focus();
                    }
                    event.preventDefault();
                    return;
                }
            }
            if (isOpen &&
                (event.key === KeyTypes.Escape || event.key === KeyTypes.Tab) &&
                (escFromToggle || escFromWithinMenu)) {
                onToggle(false, event);
                onClose();
                this.toggle.current.focus();
            }
        };
        this.onKeyDown = (event) => {
            const { isOpen, onToggle, variant, onClose, onEnter, handleTypeaheadKeys } = this.props;
            if (variant === SelectVariant.typeahead || variant === SelectVariant.typeaheadMulti) {
                if (event.key === KeyTypes.ArrowDown || event.key === KeyTypes.ArrowUp) {
                    handleTypeaheadKeys((event.key === KeyTypes.ArrowDown && 'down') || (event.key === KeyTypes.ArrowUp && 'up'));
                    event.preventDefault();
                }
                else if (event.key === KeyTypes.Enter) {
                    if (isOpen) {
                        handleTypeaheadKeys('enter');
                    }
                    else {
                        onToggle(!isOpen, event);
                    }
                }
            }
            if (variant === SelectVariant.typeahead ||
                variant === SelectVariant.typeaheadMulti ||
                (event.key === KeyTypes.Tab && !isOpen) ||
                (event.key !== KeyTypes.Enter && event.key !== KeyTypes.Space)) {
                return;
            }
            event.preventDefault();
            if ((event.key === KeyTypes.Tab || event.key === KeyTypes.Enter || event.key === KeyTypes.Space) && isOpen) {
                onToggle(!isOpen, event);
                onClose();
                this.toggle.current.focus();
            }
            else if ((event.key === KeyTypes.Enter || event.key === KeyTypes.Space) && !isOpen) {
                onToggle(!isOpen, event);
                onEnter();
            }
        };
        const { variant } = props;
        const isTypeahead = variant === SelectVariant.typeahead || variant === SelectVariant.typeaheadMulti;
        this.toggle = isTypeahead ? React.createRef() : React.createRef();
    }
    componentDidMount() {
        document.addEventListener('click', this.onDocClick, { capture: true });
        document.addEventListener('touchstart', this.onDocClick);
        document.addEventListener('keydown', this.handleGlobalKeys);
    }
    componentWillUnmount() {
        document.removeEventListener('click', this.onDocClick);
        document.removeEventListener('touchstart', this.onDocClick);
        document.removeEventListener('keydown', this.handleGlobalKeys);
    }
    render() {
        /* eslint-disable @typescript-eslint/no-unused-vars */
        const _a = this.props, { className, children, isOpen, isActive, isPlain, isDisabled, hasPlaceholderStyle, variant, onToggle, onEnter, onClose, onBlur, onClickTypeaheadToggleButton, handleTypeaheadKeys, moveFocusToLastMenuItem, parentRef, menuRef, id, type, hasClearButton, 'aria-labelledby': ariaLabelledBy, 'aria-label': ariaLabel, hasFooter, footerRef } = _a, props = __rest(_a, ["className", "children", "isOpen", "isActive", "isPlain", "isDisabled", "hasPlaceholderStyle", "variant", "onToggle", "onEnter", "onClose", "onBlur", "onClickTypeaheadToggleButton", "handleTypeaheadKeys", "moveFocusToLastMenuItem", "parentRef", "menuRef", "id", "type", "hasClearButton", 'aria-labelledby', 'aria-label', "hasFooter", "footerRef"]);
        /* eslint-enable @typescript-eslint/no-unused-vars */
        const isTypeahead = variant === SelectVariant.typeahead || variant === SelectVariant.typeaheadMulti || hasClearButton;
        const toggleProps = {
            id,
            'aria-labelledby': ariaLabelledBy,
            'aria-expanded': isOpen,
            'aria-haspopup': (variant !== SelectVariant.checkbox && 'listbox') || null
        };
        return (React.createElement(React.Fragment, null,
            !isTypeahead && (React.createElement("button", Object.assign({}, props, toggleProps, { ref: this.toggle, type: type, className: css(styles.selectToggle, hasPlaceholderStyle && styles.modifiers.placeholder, isDisabled && styles.modifiers.disabled, isPlain && styles.modifiers.plain, isActive && styles.modifiers.active, className), "aria-label": ariaLabel, onBlur: onBlur, 
                // eslint-disable-next-line @typescript-eslint/no-unused-vars
                onClick: event => {
                    onToggle(!isOpen, event);
                    if (isOpen) {
                        onClose();
                    }
                }, onKeyDown: this.onKeyDown, disabled: isDisabled }),
                children,
                React.createElement("span", { className: css(styles.selectToggleArrow) },
                    React.createElement(CaretDownIcon, null)))),
            isTypeahead && (React.createElement("div", Object.assign({}, props, { ref: this.toggle, className: css(styles.selectToggle, hasPlaceholderStyle && styles.modifiers.placeholder, isDisabled && styles.modifiers.disabled, isPlain && styles.modifiers.plain, isTypeahead && styles.modifiers.typeahead, className), onBlur: onBlur, 
                // eslint-disable-next-line @typescript-eslint/no-unused-vars
                onClick: event => {
                    if (!isDisabled) {
                        onToggle(!isOpen, event);
                        if (isOpen) {
                            onClose();
                        }
                    }
                }, onKeyDown: this.onKeyDown }),
                children,
                React.createElement("button", Object.assign({}, toggleProps, { type: type, className: css(buttonStyles.button, styles.selectToggleButton, styles.modifiers.plain), "aria-label": ariaLabel, onClick: event => {
                        onToggle(!isOpen, event);
                        if (isOpen) {
                            onClose();
                        }
                        onClickTypeaheadToggleButton();
                    } }, ((variant === SelectVariant.typeahead || variant === SelectVariant.typeaheadMulti) && {
                    tabIndex: -1
                }), { disabled: isDisabled }),
                    React.createElement(CaretDownIcon, { className: css(styles.selectToggleArrow) }))))));
    }
}
SelectToggle.displayName = 'SelectToggle';
SelectToggle.defaultProps = {
    className: '',
    isOpen: false,
    isActive: false,
    isPlain: false,
    isDisabled: false,
    hasPlaceholderStyle: false,
    hasClearButton: false,
    hasFooter: false,
    variant: 'single',
    'aria-labelledby': '',
    'aria-label': '',
    type: 'button',
    onToggle: () => { },
    onEnter: () => { },
    onClose: () => { },
    onClickTypeaheadToggleButton: () => { }
};
//# sourceMappingURL=SelectToggle.js.map