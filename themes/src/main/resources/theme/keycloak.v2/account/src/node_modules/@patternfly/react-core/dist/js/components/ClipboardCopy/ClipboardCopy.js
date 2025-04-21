"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ClipboardCopy = exports.ClipboardCopyVariant = exports.clipboardCopyFunc = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const clipboard_copy_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/ClipboardCopy/clipboard-copy"));
const react_styles_1 = require("@patternfly/react-styles");
const Popover_1 = require("../Popover");
const TextInput_1 = require("../TextInput");
const GenerateId_1 = require("../../helpers/GenerateId/GenerateId");
const ClipboardCopyButton_1 = require("./ClipboardCopyButton");
const ClipboardCopyToggle_1 = require("./ClipboardCopyToggle");
const ClipboardCopyExpanded_1 = require("./ClipboardCopyExpanded");
const clipboardCopyFunc = (event, text) => {
    const clipboard = event.currentTarget.parentElement;
    const el = document.createElement('textarea');
    el.value = text.toString();
    clipboard.appendChild(el);
    el.select();
    document.execCommand('copy');
    clipboard.removeChild(el);
};
exports.clipboardCopyFunc = clipboardCopyFunc;
var ClipboardCopyVariant;
(function (ClipboardCopyVariant) {
    ClipboardCopyVariant["inline"] = "inline";
    ClipboardCopyVariant["expansion"] = "expansion";
    ClipboardCopyVariant["inlineCompact"] = "inline-compact";
})(ClipboardCopyVariant = exports.ClipboardCopyVariant || (exports.ClipboardCopyVariant = {}));
class ClipboardCopy extends React.Component {
    constructor(props) {
        super(props);
        this.timer = null;
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        this.componentDidUpdate = (prevProps, prevState) => {
            if (prevProps.children !== this.props.children) {
                this.updateText(this.props.children);
            }
        };
        this.componentWillUnmount = () => {
            if (this.timer) {
                window.clearTimeout(this.timer);
            }
        };
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        this.expandContent = (_event) => {
            this.setState(prevState => ({
                expanded: !prevState.expanded
            }));
        };
        this.updateText = (text) => {
            this.setState({ text });
            this.props.onChange(text);
        };
        this.render = () => {
            const _a = this.props, { 
            /* eslint-disable @typescript-eslint/no-unused-vars */
            isExpanded, onChange, // Don't pass to <div>
            /* eslint-enable @typescript-eslint/no-unused-vars */
            isReadOnly, isCode, isBlock, exitDelay, maxWidth, entryDelay, switchDelay, onCopy, hoverTip, clickTip, textAriaLabel, toggleAriaLabel, variant, position, className, additionalActions } = _a, divProps = tslib_1.__rest(_a, ["isExpanded", "onChange", "isReadOnly", "isCode", "isBlock", "exitDelay", "maxWidth", "entryDelay", "switchDelay", "onCopy", "hoverTip", "clickTip", "textAriaLabel", "toggleAriaLabel", "variant", "position", "className", "additionalActions"]);
            const textIdPrefix = 'text-input-';
            const toggleIdPrefix = 'toggle-';
            const contentIdPrefix = 'content-';
            return (React.createElement("div", Object.assign({ className: react_styles_1.css(clipboard_copy_1.default.clipboardCopy, variant === 'inline-compact' && clipboard_copy_1.default.modifiers.inline, isBlock && clipboard_copy_1.default.modifiers.block, this.state.expanded && clipboard_copy_1.default.modifiers.expanded, className) }, divProps),
                variant === 'inline-compact' && (React.createElement(GenerateId_1.GenerateId, { prefix: "" }, id => (React.createElement(React.Fragment, null,
                    !isCode && (React.createElement("span", { className: react_styles_1.css(clipboard_copy_1.default.clipboardCopyText), id: `${textIdPrefix}${id}` }, this.state.text)),
                    isCode && (React.createElement("code", { className: react_styles_1.css(clipboard_copy_1.default.clipboardCopyText, clipboard_copy_1.default.modifiers.code), id: `${textIdPrefix}${id}` }, this.state.text)),
                    React.createElement("span", { className: react_styles_1.css(clipboard_copy_1.default.clipboardCopyActions) },
                        React.createElement("span", { className: react_styles_1.css(clipboard_copy_1.default.clipboardCopyActionsItem) },
                            React.createElement(ClipboardCopyButton_1.ClipboardCopyButton, { variant: "plain", exitDelay: exitDelay, entryDelay: entryDelay, maxWidth: maxWidth, position: position, id: `copy-button-${id}`, textId: `text-input-${id}`, "aria-label": hoverTip, onClick: (event) => {
                                    if (this.timer) {
                                        window.clearTimeout(this.timer);
                                        this.setState({ copied: false });
                                    }
                                    onCopy(event, this.state.text);
                                    this.setState({ copied: true }, () => {
                                        this.timer = window.setTimeout(() => {
                                            this.setState({ copied: false });
                                            this.timer = null;
                                        }, switchDelay);
                                    });
                                } }, this.state.copied ? clickTip : hoverTip)),
                        additionalActions && additionalActions))))),
                variant !== 'inline-compact' && (React.createElement(GenerateId_1.GenerateId, { prefix: "" }, id => (React.createElement(React.Fragment, null,
                    React.createElement("div", { className: react_styles_1.css(clipboard_copy_1.default.clipboardCopyGroup) },
                        variant === 'expansion' && (React.createElement(ClipboardCopyToggle_1.ClipboardCopyToggle, { isExpanded: this.state.expanded, onClick: this.expandContent, id: `${toggleIdPrefix}${id}`, textId: `${textIdPrefix}${id}`, contentId: `${contentIdPrefix}${id}`, "aria-label": toggleAriaLabel })),
                        React.createElement(TextInput_1.TextInput, { isReadOnly: isReadOnly || this.state.expanded, onChange: this.updateText, value: this.state.text, id: `text-input-${id}`, "aria-label": textAriaLabel }),
                        React.createElement(ClipboardCopyButton_1.ClipboardCopyButton, { exitDelay: exitDelay, entryDelay: entryDelay, maxWidth: maxWidth, position: position, id: `copy-button-${id}`, textId: `text-input-${id}`, "aria-label": hoverTip, onClick: (event) => {
                                if (this.timer) {
                                    window.clearTimeout(this.timer);
                                    this.setState({ copied: false });
                                }
                                onCopy(event, this.state.text);
                                this.setState({ copied: true }, () => {
                                    this.timer = window.setTimeout(() => {
                                        this.setState({ copied: false });
                                        this.timer = null;
                                    }, switchDelay);
                                });
                            } }, this.state.copied ? clickTip : hoverTip)),
                    this.state.expanded && (React.createElement(ClipboardCopyExpanded_1.ClipboardCopyExpanded, { isReadOnly: isReadOnly, isCode: isCode, id: `content-${id}`, onChange: this.updateText }, this.state.text))))))));
        };
        this.state = {
            text: Array.isArray(this.props.children)
                ? this.props.children.join('')
                : this.props.children,
            expanded: this.props.isExpanded,
            copied: false
        };
    }
}
exports.ClipboardCopy = ClipboardCopy;
ClipboardCopy.displayName = 'ClipboardCopy';
ClipboardCopy.defaultProps = {
    hoverTip: 'Copy to clipboard',
    clickTip: 'Successfully copied to clipboard!',
    isReadOnly: false,
    isExpanded: false,
    isCode: false,
    variant: 'inline',
    position: Popover_1.PopoverPosition.top,
    maxWidth: '150px',
    exitDelay: 1600,
    entryDelay: 300,
    switchDelay: 2000,
    onCopy: exports.clipboardCopyFunc,
    onChange: () => undefined,
    textAriaLabel: 'Copyable input',
    toggleAriaLabel: 'Show content',
    additionalActions: null
};
//# sourceMappingURL=ClipboardCopy.js.map