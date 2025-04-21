import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
import { DropdownContext } from './dropdownConstants';
import { css } from '@patternfly/react-styles';
import { KEY_CODES } from '../../helpers/constants';
const buttonVariantStyles = {
    default: '',
    primary: styles.modifiers.primary,
    secondary: styles.modifiers.secondary
};
export class Toggle extends React.Component {
    constructor() {
        super(...arguments);
        this.buttonRef = React.createRef();
        this.componentDidMount = () => {
            document.addEventListener('click', this.onDocClick);
            document.addEventListener('touchstart', this.onDocClick);
            document.addEventListener('keydown', this.onEscPress);
        };
        this.componentWillUnmount = () => {
            document.removeEventListener('click', this.onDocClick);
            document.removeEventListener('touchstart', this.onDocClick);
            document.removeEventListener('keydown', this.onEscPress);
        };
        this.onDocClick = (event) => {
            const { isOpen, parentRef, onToggle, getMenuRef } = this.props;
            const menuRef = getMenuRef && getMenuRef();
            const clickedOnToggle = parentRef && parentRef.current && parentRef.current.contains(event.target);
            const clickedWithinMenu = menuRef && menuRef.contains && menuRef.contains(event.target);
            if (isOpen && !(clickedOnToggle || clickedWithinMenu)) {
                onToggle(false, event);
            }
        };
        this.onEscPress = (event) => {
            const { parentRef, getMenuRef } = this.props;
            const keyCode = event.keyCode || event.which;
            const menuRef = getMenuRef && getMenuRef();
            const escFromToggle = parentRef && parentRef.current && parentRef.current.contains(event.target);
            const escFromWithinMenu = menuRef && menuRef.contains && menuRef.contains(event.target);
            if (this.props.isOpen &&
                (keyCode === KEY_CODES.ESCAPE_KEY || event.key === 'Tab') &&
                (escFromToggle || escFromWithinMenu)) {
                this.props.onToggle(false, event);
                this.buttonRef.current.focus();
            }
        };
        this.onKeyDown = (event) => {
            if (event.key === 'Tab' && !this.props.isOpen) {
                return;
            }
            if ((event.key === 'Tab' || event.key === 'Enter' || event.key === ' ') && this.props.isOpen) {
                if (!this.props.bubbleEvent) {
                    event.stopPropagation();
                }
                event.preventDefault();
                this.props.onToggle(!this.props.isOpen, event);
            }
            else if ((event.key === 'Enter' || event.key === ' ') && !this.props.isOpen) {
                if (!this.props.bubbleEvent) {
                    event.stopPropagation();
                }
                event.preventDefault();
                this.props.onToggle(!this.props.isOpen, event);
                this.props.onEnter();
            }
        };
    }
    render() {
        const _a = this.props, { className, children, isOpen, isDisabled, isPlain, isText, isPrimary, isSplitButton, toggleVariant, onToggle, 'aria-haspopup': ariaHasPopup, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        isActive, bubbleEvent, onEnter, parentRef, getMenuRef, 
        /* eslint-enable @typescript-eslint/no-unused-vars */
        id, type } = _a, props = __rest(_a, ["className", "children", "isOpen", "isDisabled", "isPlain", "isText", "isPrimary", "isSplitButton", "toggleVariant", "onToggle", 'aria-haspopup', "isActive", "bubbleEvent", "onEnter", "parentRef", "getMenuRef", "id", "type"]);
        return (React.createElement(DropdownContext.Consumer, null, ({ toggleClass }) => (React.createElement("button", Object.assign({}, props, { id: id, ref: this.buttonRef, className: css(isSplitButton ? styles.dropdownToggleButton : toggleClass || styles.dropdownToggle, isActive && styles.modifiers.active, isPlain && styles.modifiers.plain, isText && styles.modifiers.text, isPrimary && styles.modifiers.primary, buttonVariantStyles[toggleVariant], className), type: type || 'button', onClick: event => onToggle(!isOpen, event), "aria-expanded": isOpen, "aria-haspopup": ariaHasPopup, onKeyDown: event => this.onKeyDown(event), disabled: isDisabled }), children))));
    }
}
Toggle.displayName = 'Toggle';
Toggle.defaultProps = {
    className: '',
    isOpen: false,
    isActive: false,
    isDisabled: false,
    isPlain: false,
    isText: false,
    isPrimary: false,
    isSplitButton: false,
    onToggle: () => { },
    onEnter: () => { },
    bubbleEvent: false
};
//# sourceMappingURL=Toggle.js.map