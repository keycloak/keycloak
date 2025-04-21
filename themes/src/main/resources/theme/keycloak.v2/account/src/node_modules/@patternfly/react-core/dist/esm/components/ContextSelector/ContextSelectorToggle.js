import { __rest } from "tslib";
import * as React from 'react';
import CaretDownIcon from '@patternfly/react-icons/dist/esm/icons/caret-down-icon';
import styles from '@patternfly/react-styles/css/components/ContextSelector/context-selector';
import { css } from '@patternfly/react-styles';
import { KEY_CODES } from '../../helpers/constants';
export class ContextSelectorToggle extends React.Component {
    constructor() {
        super(...arguments);
        this.toggle = React.createRef();
        this.componentDidMount = () => {
            document.addEventListener('mousedown', this.onDocClick);
            document.addEventListener('touchstart', this.onDocClick);
            document.addEventListener('keydown', this.onEscPress);
        };
        this.componentWillUnmount = () => {
            document.removeEventListener('mousedown', this.onDocClick);
            document.removeEventListener('touchstart', this.onDocClick);
            document.removeEventListener('keydown', this.onEscPress);
        };
        this.onDocClick = (event) => {
            const { isOpen, parentRef, onToggle } = this.props;
            if (isOpen && (parentRef === null || parentRef === void 0 ? void 0 : parentRef.current) && !parentRef.current.contains(event.target)) {
                onToggle(null, false);
                this.toggle.current.focus();
            }
        };
        this.onEscPress = (event) => {
            const { isOpen, onToggle } = this.props;
            const keyCode = event.keyCode || event.which;
            if (isOpen && keyCode === KEY_CODES.ESCAPE_KEY) {
                onToggle(null, false);
                this.toggle.current.focus();
            }
        };
        this.onKeyDown = (event) => {
            const { isOpen, onToggle, onEnter } = this.props;
            if ((event.keyCode === KEY_CODES.TAB && !isOpen) || event.key !== KEY_CODES.ENTER) {
                return;
            }
            event.preventDefault();
            if ((event.keyCode === KEY_CODES.TAB || event.keyCode === KEY_CODES.ENTER || event.key !== KEY_CODES.SPACE) &&
                isOpen) {
                onToggle(null, !isOpen);
            }
            else if ((event.keyCode === KEY_CODES.ENTER || event.key === ' ') && !isOpen) {
                onToggle(null, !isOpen);
                onEnter();
            }
        };
    }
    render() {
        const _a = this.props, { className, toggleText, isOpen, onToggle, id, isPlain, isText, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        isActive, onEnter, parentRef } = _a, 
        /* eslint-enable @typescript-eslint/no-unused-vars */
        props = __rest(_a, ["className", "toggleText", "isOpen", "onToggle", "id", "isPlain", "isText", "isActive", "onEnter", "parentRef"]);
        return (React.createElement("button", Object.assign({}, props, { id: id, ref: this.toggle, className: css(styles.contextSelectorToggle, isActive && styles.modifiers.active, isPlain && styles.modifiers.plain, isText && styles.modifiers.text, className), type: "button", onClick: event => onToggle(event, !isOpen), "aria-expanded": isOpen, onKeyDown: this.onKeyDown }),
            React.createElement("span", { className: css(styles.contextSelectorToggleText) }, toggleText),
            React.createElement("span", { className: css(styles.contextSelectorToggleIcon) },
                React.createElement(CaretDownIcon, { "aria-hidden": true }))));
    }
}
ContextSelectorToggle.displayName = 'ContextSelectorToggle';
ContextSelectorToggle.defaultProps = {
    className: '',
    toggleText: '',
    isOpen: false,
    onEnter: () => undefined,
    parentRef: null,
    isActive: false,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onToggle: (event, value) => undefined
};
//# sourceMappingURL=ContextSelectorToggle.js.map