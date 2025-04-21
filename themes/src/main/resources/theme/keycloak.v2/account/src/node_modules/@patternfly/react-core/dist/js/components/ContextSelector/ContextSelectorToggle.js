"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ContextSelectorToggle = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const caret_down_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/caret-down-icon'));
const context_selector_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/ContextSelector/context-selector"));
const react_styles_1 = require("@patternfly/react-styles");
const constants_1 = require("../../helpers/constants");
class ContextSelectorToggle extends React.Component {
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
            if (isOpen && keyCode === constants_1.KEY_CODES.ESCAPE_KEY) {
                onToggle(null, false);
                this.toggle.current.focus();
            }
        };
        this.onKeyDown = (event) => {
            const { isOpen, onToggle, onEnter } = this.props;
            if ((event.keyCode === constants_1.KEY_CODES.TAB && !isOpen) || event.key !== constants_1.KEY_CODES.ENTER) {
                return;
            }
            event.preventDefault();
            if ((event.keyCode === constants_1.KEY_CODES.TAB || event.keyCode === constants_1.KEY_CODES.ENTER || event.key !== constants_1.KEY_CODES.SPACE) &&
                isOpen) {
                onToggle(null, !isOpen);
            }
            else if ((event.keyCode === constants_1.KEY_CODES.ENTER || event.key === ' ') && !isOpen) {
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
        props = tslib_1.__rest(_a, ["className", "toggleText", "isOpen", "onToggle", "id", "isPlain", "isText", "isActive", "onEnter", "parentRef"]);
        return (React.createElement("button", Object.assign({}, props, { id: id, ref: this.toggle, className: react_styles_1.css(context_selector_1.default.contextSelectorToggle, isActive && context_selector_1.default.modifiers.active, isPlain && context_selector_1.default.modifiers.plain, isText && context_selector_1.default.modifiers.text, className), type: "button", onClick: event => onToggle(event, !isOpen), "aria-expanded": isOpen, onKeyDown: this.onKeyDown }),
            React.createElement("span", { className: react_styles_1.css(context_selector_1.default.contextSelectorToggleText) }, toggleText),
            React.createElement("span", { className: react_styles_1.css(context_selector_1.default.contextSelectorToggleIcon) },
                React.createElement(caret_down_icon_1.default, { "aria-hidden": true }))));
    }
}
exports.ContextSelectorToggle = ContextSelectorToggle;
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