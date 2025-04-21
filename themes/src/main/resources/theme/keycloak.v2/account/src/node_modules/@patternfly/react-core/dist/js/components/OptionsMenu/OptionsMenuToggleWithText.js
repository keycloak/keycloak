"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.OptionsMenuToggleWithText = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const constants_1 = require("../../helpers/constants");
const options_menu_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/OptionsMenu/options-menu"));
const OptionsMenuToggleWithText = (_a) => {
    var { parentId = '', toggleText, toggleTextClassName = '', toggleButtonContents, toggleButtonContentsClassName = '', onToggle = () => null, isOpen = false, isPlain = false, 
    /* eslint-disable @typescript-eslint/no-unused-vars */
    isText = true, isDisabled = false, 
    /* eslint-disable @typescript-eslint/no-unused-vars */
    isActive = false, 'aria-haspopup': ariaHasPopup, parentRef, 
    /* eslint-disable @typescript-eslint/no-unused-vars */
    getMenuRef, onEnter, 
    /* eslint-enable @typescript-eslint/no-unused-vars */
    'aria-label': ariaLabel = 'Options menu' } = _a, props = tslib_1.__rest(_a, ["parentId", "toggleText", "toggleTextClassName", "toggleButtonContents", "toggleButtonContentsClassName", "onToggle", "isOpen", "isPlain", "isText", "isDisabled", "isActive", 'aria-haspopup', "parentRef", "getMenuRef", "onEnter", 'aria-label']);
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
            (keyCode === constants_1.KEY_CODES.ESCAPE_KEY || event.key === 'Tab') &&
            parentRef &&
            parentRef.current &&
            parentRef.current.contains(event.target)) {
            onToggle(false);
            buttonRef.current.focus();
        }
    };
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(options_menu_1.default.optionsMenuToggle, options_menu_1.default.modifiers.text, isPlain && options_menu_1.default.modifiers.plain, isDisabled && options_menu_1.default.modifiers.disabled, isActive && options_menu_1.default.modifiers.active) }, props),
        React.createElement("span", { className: react_styles_1.css(options_menu_1.default.optionsMenuToggleText, toggleTextClassName) }, toggleText),
        React.createElement("button", { className: react_styles_1.css(options_menu_1.default.optionsMenuToggleButton, toggleButtonContentsClassName), id: `${parentId}-toggle`, "aria-haspopup": "listbox", "aria-label": ariaLabel, "aria-expanded": isOpen, ref: buttonRef, disabled: isDisabled, onClick: () => onToggle(!isOpen), onKeyDown: onKeyDown },
            React.createElement("span", { className: react_styles_1.css(options_menu_1.default.optionsMenuToggleButtonIcon) }, toggleButtonContents))));
};
exports.OptionsMenuToggleWithText = OptionsMenuToggleWithText;
exports.OptionsMenuToggleWithText.displayName = 'OptionsMenuToggleWithText';
//# sourceMappingURL=OptionsMenuToggleWithText.js.map