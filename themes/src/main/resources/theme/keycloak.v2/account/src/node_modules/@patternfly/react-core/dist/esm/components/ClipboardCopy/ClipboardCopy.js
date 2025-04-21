import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ClipboardCopy/clipboard-copy';
import { css } from '@patternfly/react-styles';
import { PopoverPosition } from '../Popover';
import { TextInput } from '../TextInput';
import { GenerateId } from '../../helpers/GenerateId/GenerateId';
import { ClipboardCopyButton } from './ClipboardCopyButton';
import { ClipboardCopyToggle } from './ClipboardCopyToggle';
import { ClipboardCopyExpanded } from './ClipboardCopyExpanded';
export const clipboardCopyFunc = (event, text) => {
    const clipboard = event.currentTarget.parentElement;
    const el = document.createElement('textarea');
    el.value = text.toString();
    clipboard.appendChild(el);
    el.select();
    document.execCommand('copy');
    clipboard.removeChild(el);
};
export var ClipboardCopyVariant;
(function (ClipboardCopyVariant) {
    ClipboardCopyVariant["inline"] = "inline";
    ClipboardCopyVariant["expansion"] = "expansion";
    ClipboardCopyVariant["inlineCompact"] = "inline-compact";
})(ClipboardCopyVariant || (ClipboardCopyVariant = {}));
export class ClipboardCopy extends React.Component {
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
            isReadOnly, isCode, isBlock, exitDelay, maxWidth, entryDelay, switchDelay, onCopy, hoverTip, clickTip, textAriaLabel, toggleAriaLabel, variant, position, className, additionalActions } = _a, divProps = __rest(_a, ["isExpanded", "onChange", "isReadOnly", "isCode", "isBlock", "exitDelay", "maxWidth", "entryDelay", "switchDelay", "onCopy", "hoverTip", "clickTip", "textAriaLabel", "toggleAriaLabel", "variant", "position", "className", "additionalActions"]);
            const textIdPrefix = 'text-input-';
            const toggleIdPrefix = 'toggle-';
            const contentIdPrefix = 'content-';
            return (React.createElement("div", Object.assign({ className: css(styles.clipboardCopy, variant === 'inline-compact' && styles.modifiers.inline, isBlock && styles.modifiers.block, this.state.expanded && styles.modifiers.expanded, className) }, divProps),
                variant === 'inline-compact' && (React.createElement(GenerateId, { prefix: "" }, id => (React.createElement(React.Fragment, null,
                    !isCode && (React.createElement("span", { className: css(styles.clipboardCopyText), id: `${textIdPrefix}${id}` }, this.state.text)),
                    isCode && (React.createElement("code", { className: css(styles.clipboardCopyText, styles.modifiers.code), id: `${textIdPrefix}${id}` }, this.state.text)),
                    React.createElement("span", { className: css(styles.clipboardCopyActions) },
                        React.createElement("span", { className: css(styles.clipboardCopyActionsItem) },
                            React.createElement(ClipboardCopyButton, { variant: "plain", exitDelay: exitDelay, entryDelay: entryDelay, maxWidth: maxWidth, position: position, id: `copy-button-${id}`, textId: `text-input-${id}`, "aria-label": hoverTip, onClick: (event) => {
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
                variant !== 'inline-compact' && (React.createElement(GenerateId, { prefix: "" }, id => (React.createElement(React.Fragment, null,
                    React.createElement("div", { className: css(styles.clipboardCopyGroup) },
                        variant === 'expansion' && (React.createElement(ClipboardCopyToggle, { isExpanded: this.state.expanded, onClick: this.expandContent, id: `${toggleIdPrefix}${id}`, textId: `${textIdPrefix}${id}`, contentId: `${contentIdPrefix}${id}`, "aria-label": toggleAriaLabel })),
                        React.createElement(TextInput, { isReadOnly: isReadOnly || this.state.expanded, onChange: this.updateText, value: this.state.text, id: `text-input-${id}`, "aria-label": textAriaLabel }),
                        React.createElement(ClipboardCopyButton, { exitDelay: exitDelay, entryDelay: entryDelay, maxWidth: maxWidth, position: position, id: `copy-button-${id}`, textId: `text-input-${id}`, "aria-label": hoverTip, onClick: (event) => {
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
                    this.state.expanded && (React.createElement(ClipboardCopyExpanded, { isReadOnly: isReadOnly, isCode: isCode, id: `content-${id}`, onChange: this.updateText }, this.state.text))))))));
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
ClipboardCopy.displayName = 'ClipboardCopy';
ClipboardCopy.defaultProps = {
    hoverTip: 'Copy to clipboard',
    clickTip: 'Successfully copied to clipboard!',
    isReadOnly: false,
    isExpanded: false,
    isCode: false,
    variant: 'inline',
    position: PopoverPosition.top,
    maxWidth: '150px',
    exitDelay: 1600,
    entryDelay: 300,
    switchDelay: 2000,
    onCopy: clipboardCopyFunc,
    onChange: () => undefined,
    textAriaLabel: 'Copyable input',
    toggleAriaLabel: 'Show content',
    additionalActions: null
};
//# sourceMappingURL=ClipboardCopy.js.map