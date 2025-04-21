"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SelectToggle = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const select_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Select/select"));
const button_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Button/button"));
const react_styles_1 = require("@patternfly/react-styles");
const caret_down_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/caret-down-icon'));
const selectConstants_1 = require("./selectConstants");
const util_1 = require("../../helpers/util");
const constants_1 = require("../../helpers/constants");
class SelectToggle extends React.Component {
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
                event.key === constants_1.KeyTypes.Tab &&
                (variant === selectConstants_1.SelectVariant.typeahead || variant === selectConstants_1.SelectVariant.typeaheadMulti)) {
                this.props.handleTypeaheadKeys('tab', event.shiftKey);
                event.preventDefault();
                return;
            }
            if (isOpen && event.key === constants_1.KeyTypes.Tab && hasFooter) {
                const tabbableItems = util_1.findTabbableElements(footerRef, selectConstants_1.SelectFooterTabbableItems);
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
                (event.key === constants_1.KeyTypes.Escape || event.key === constants_1.KeyTypes.Tab) &&
                (escFromToggle || escFromWithinMenu)) {
                onToggle(false, event);
                onClose();
                this.toggle.current.focus();
            }
        };
        this.onKeyDown = (event) => {
            const { isOpen, onToggle, variant, onClose, onEnter, handleTypeaheadKeys } = this.props;
            if (variant === selectConstants_1.SelectVariant.typeahead || variant === selectConstants_1.SelectVariant.typeaheadMulti) {
                if (event.key === constants_1.KeyTypes.ArrowDown || event.key === constants_1.KeyTypes.ArrowUp) {
                    handleTypeaheadKeys((event.key === constants_1.KeyTypes.ArrowDown && 'down') || (event.key === constants_1.KeyTypes.ArrowUp && 'up'));
                    event.preventDefault();
                }
                else if (event.key === constants_1.KeyTypes.Enter) {
                    if (isOpen) {
                        handleTypeaheadKeys('enter');
                    }
                    else {
                        onToggle(!isOpen, event);
                    }
                }
            }
            if (variant === selectConstants_1.SelectVariant.typeahead ||
                variant === selectConstants_1.SelectVariant.typeaheadMulti ||
                (event.key === constants_1.KeyTypes.Tab && !isOpen) ||
                (event.key !== constants_1.KeyTypes.Enter && event.key !== constants_1.KeyTypes.Space)) {
                return;
            }
            event.preventDefault();
            if ((event.key === constants_1.KeyTypes.Tab || event.key === constants_1.KeyTypes.Enter || event.key === constants_1.KeyTypes.Space) && isOpen) {
                onToggle(!isOpen, event);
                onClose();
                this.toggle.current.focus();
            }
            else if ((event.key === constants_1.KeyTypes.Enter || event.key === constants_1.KeyTypes.Space) && !isOpen) {
                onToggle(!isOpen, event);
                onEnter();
            }
        };
        const { variant } = props;
        const isTypeahead = variant === selectConstants_1.SelectVariant.typeahead || variant === selectConstants_1.SelectVariant.typeaheadMulti;
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
        const _a = this.props, { className, children, isOpen, isActive, isPlain, isDisabled, hasPlaceholderStyle, variant, onToggle, onEnter, onClose, onBlur, onClickTypeaheadToggleButton, handleTypeaheadKeys, moveFocusToLastMenuItem, parentRef, menuRef, id, type, hasClearButton, 'aria-labelledby': ariaLabelledBy, 'aria-label': ariaLabel, hasFooter, footerRef } = _a, props = tslib_1.__rest(_a, ["className", "children", "isOpen", "isActive", "isPlain", "isDisabled", "hasPlaceholderStyle", "variant", "onToggle", "onEnter", "onClose", "onBlur", "onClickTypeaheadToggleButton", "handleTypeaheadKeys", "moveFocusToLastMenuItem", "parentRef", "menuRef", "id", "type", "hasClearButton", 'aria-labelledby', 'aria-label', "hasFooter", "footerRef"]);
        /* eslint-enable @typescript-eslint/no-unused-vars */
        const isTypeahead = variant === selectConstants_1.SelectVariant.typeahead || variant === selectConstants_1.SelectVariant.typeaheadMulti || hasClearButton;
        const toggleProps = {
            id,
            'aria-labelledby': ariaLabelledBy,
            'aria-expanded': isOpen,
            'aria-haspopup': (variant !== selectConstants_1.SelectVariant.checkbox && 'listbox') || null
        };
        return (React.createElement(React.Fragment, null,
            !isTypeahead && (React.createElement("button", Object.assign({}, props, toggleProps, { ref: this.toggle, type: type, className: react_styles_1.css(select_1.default.selectToggle, hasPlaceholderStyle && select_1.default.modifiers.placeholder, isDisabled && select_1.default.modifiers.disabled, isPlain && select_1.default.modifiers.plain, isActive && select_1.default.modifiers.active, className), "aria-label": ariaLabel, onBlur: onBlur, 
                // eslint-disable-next-line @typescript-eslint/no-unused-vars
                onClick: event => {
                    onToggle(!isOpen, event);
                    if (isOpen) {
                        onClose();
                    }
                }, onKeyDown: this.onKeyDown, disabled: isDisabled }),
                children,
                React.createElement("span", { className: react_styles_1.css(select_1.default.selectToggleArrow) },
                    React.createElement(caret_down_icon_1.default, null)))),
            isTypeahead && (React.createElement("div", Object.assign({}, props, { ref: this.toggle, className: react_styles_1.css(select_1.default.selectToggle, hasPlaceholderStyle && select_1.default.modifiers.placeholder, isDisabled && select_1.default.modifiers.disabled, isPlain && select_1.default.modifiers.plain, isTypeahead && select_1.default.modifiers.typeahead, className), onBlur: onBlur, 
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
                React.createElement("button", Object.assign({}, toggleProps, { type: type, className: react_styles_1.css(button_1.default.button, select_1.default.selectToggleButton, select_1.default.modifiers.plain), "aria-label": ariaLabel, onClick: event => {
                        onToggle(!isOpen, event);
                        if (isOpen) {
                            onClose();
                        }
                        onClickTypeaheadToggleButton();
                    } }, ((variant === selectConstants_1.SelectVariant.typeahead || variant === selectConstants_1.SelectVariant.typeaheadMulti) && {
                    tabIndex: -1
                }), { disabled: isDisabled }),
                    React.createElement(caret_down_icon_1.default, { className: react_styles_1.css(select_1.default.selectToggleArrow) }))))));
    }
}
exports.SelectToggle = SelectToggle;
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