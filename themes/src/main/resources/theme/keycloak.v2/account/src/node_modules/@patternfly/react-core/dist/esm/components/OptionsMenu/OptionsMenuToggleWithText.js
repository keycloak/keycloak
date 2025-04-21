import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import { KEY_CODES } from '../../helpers/constants';
import styles from '@patternfly/react-styles/css/components/OptionsMenu/options-menu';
export const OptionsMenuToggleWithText = (_a) => {
    var { parentId = '', toggleText, toggleTextClassName = '', toggleButtonContents, toggleButtonContentsClassName = '', onToggle = () => null, isOpen = false, isPlain = false, 
    /* eslint-disable @typescript-eslint/no-unused-vars */
    isText = true, isDisabled = false, 
    /* eslint-disable @typescript-eslint/no-unused-vars */
    isActive = false, 'aria-haspopup': ariaHasPopup, parentRef, 
    /* eslint-disable @typescript-eslint/no-unused-vars */
    getMenuRef, onEnter, 
    /* eslint-enable @typescript-eslint/no-unused-vars */
    'aria-label': ariaLabel = 'Options menu' } = _a, props = __rest(_a, ["parentId", "toggleText", "toggleTextClassName", "toggleButtonContents", "toggleButtonContentsClassName", "onToggle", "isOpen", "isPlain", "isText", "isDisabled", "isActive", 'aria-haspopup', "parentRef", "getMenuRef", "onEnter", 'aria-label']);
    const buttonRef = React.useRef();
    React.useEffect(() => {
        document.addEventListener('mousedown', onDocClick);
        document.addEventListener('touchstart', onDocClick);
        document.addEventListener('keydown', onEscPress);
        return () => {
            document.removeEventListener('mousedown', onDocClick);
            document.removeEventListener('touchstart', onDocClick);
            document.removeEventListener('keydown', onEscPress);
        };
    });
    const onDocClick = (event) => {
        if (isOpen && parentRef && parentRef.current && !parentRef.current.contains(event.target)) {
            onToggle(false);
            buttonRef.current.focus();
        }
    };
    const onKeyDown = (event) => {
        if (event.key === 'Tab' && !isOpen) {
            return;
        }
        event.preventDefault();
        if ((event.key === 'Enter' || event.key === ' ') && isOpen) {
            onToggle(!isOpen);
        }
        else if ((event.key === 'Enter' || event.key === ' ') && !isOpen) {
            onToggle(!isOpen);
            onEnter(event);
        }
    };
    const onEscPress = (event) => {
        const keyCode = event.keyCode || event.which;
        if (isOpen &&
            (keyCode === KEY_CODES.ESCAPE_KEY || event.key === 'Tab') &&
            parentRef &&
            parentRef.current &&
            parentRef.current.contains(event.target)) {
            onToggle(false);
            buttonRef.current.focus();
        }
    };
    return (React.createElement("div", Object.assign({ className: css(styles.optionsMenuToggle, styles.modifiers.text, isPlain && styles.modifiers.plain, isDisabled && styles.modifiers.disabled, isActive && styles.modifiers.active) }, props),
        React.createElement("span", { className: css(styles.optionsMenuToggleText, toggleTextClassName) }, toggleText),
        React.createElement("button", { className: css(styles.optionsMenuToggleButton, toggleButtonContentsClassName), id: `${parentId}-toggle`, "aria-haspopup": "listbox", "aria-label": ariaLabel, "aria-expanded": isOpen, ref: buttonRef, disabled: isDisabled, onClick: () => onToggle(!isOpen), onKeyDown: onKeyDown },
            React.createElement("span", { className: css(styles.optionsMenuToggleButtonIcon) }, toggleButtonContents))));
};
OptionsMenuToggleWithText.displayName = 'OptionsMenuToggleWithText';
//# sourceMappingURL=OptionsMenuToggleWithText.js.map