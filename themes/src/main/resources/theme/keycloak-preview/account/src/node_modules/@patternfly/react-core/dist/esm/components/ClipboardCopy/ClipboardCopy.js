import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ClipboardCopy/clipboard-copy';
import { css } from '@patternfly/react-styles';
import { TextInput } from '../TextInput';
import { TooltipPosition } from '../Tooltip';
import GenerateId from '../../helpers/GenerateId/GenerateId';
import { ClipboardCopyButton } from './ClipboardCopyButton';
import { ClipboardCopyToggle } from './ClipboardCopyToggle';
import { ClipboardCopyExpanded } from './ClipboardCopyExpanded';
export const clipboardCopyFunc = (event, text) => {
  const clipboard = event.currentTarget.parentElement;
  const el = document.createElement('input');
  el.value = text.toString();
  clipboard.appendChild(el);
  el.select();
  document.execCommand('copy');
  clipboard.removeChild(el);
};
export let ClipboardCopyVariant;

(function (ClipboardCopyVariant) {
  ClipboardCopyVariant["inline"] = "inline";
  ClipboardCopyVariant["expansion"] = "expansion";
})(ClipboardCopyVariant || (ClipboardCopyVariant = {}));

export class ClipboardCopy extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "timer", null);

    _defineProperty(this, "componentDidUpdate", (prevProps, prevState) => {
      if (prevProps.children !== this.props.children) {
        this.updateText(this.props.children);
      }
    });

    _defineProperty(this, "expandContent", _event => {
      this.setState(prevState => ({
        expanded: !prevState.expanded
      }));
    });

    _defineProperty(this, "updateText", text => {
      this.setState({
        text
      });
      this.props.onChange(text);
    });

    _defineProperty(this, "render", () => {
      const _this$props = this.props,
            {
        /* eslint-disable @typescript-eslint/no-unused-vars */
        isExpanded,
        onChange,
        // Don't pass to <div>

        /* eslint-enable @typescript-eslint/no-unused-vars */
        isReadOnly,
        isCode,
        exitDelay,
        maxWidth,
        entryDelay,
        switchDelay,
        onCopy,
        hoverTip,
        clickTip,
        textAriaLabel,
        toggleAriaLabel,
        variant,
        position,
        className
      } = _this$props,
            divProps = _objectWithoutProperties(_this$props, ["isExpanded", "onChange", "isReadOnly", "isCode", "exitDelay", "maxWidth", "entryDelay", "switchDelay", "onCopy", "hoverTip", "clickTip", "textAriaLabel", "toggleAriaLabel", "variant", "position", "className"]);

      const textIdPrefix = 'text-input-';
      const toggleIdPrefix = 'toggle-';
      const contentIdPrefix = 'content-';
      return React.createElement("div", _extends({
        className: css(styles.clipboardCopy, this.state.expanded && styles.modifiers.expanded, className)
      }, divProps), React.createElement(GenerateId, {
        prefix: ""
      }, id => React.createElement(React.Fragment, null, React.createElement("div", {
        className: css(styles.clipboardCopyGroup)
      }, variant === 'expansion' && React.createElement(ClipboardCopyToggle, {
        isExpanded: this.state.expanded,
        onClick: this.expandContent,
        id: `${toggleIdPrefix}-${id}`,
        textId: `${textIdPrefix}-${id}`,
        contentId: `${contentIdPrefix}-${id}`,
        "aria-label": toggleAriaLabel
      }), React.createElement(TextInput, {
        isReadOnly: isReadOnly || this.state.expanded,
        onChange: this.updateText,
        value: this.state.text,
        id: `text-input-${id}`,
        "aria-label": textAriaLabel
      }), React.createElement(ClipboardCopyButton, {
        exitDelay: exitDelay,
        entryDelay: entryDelay,
        maxWidth: maxWidth,
        position: position,
        id: `copy-button-${id}`,
        textId: `text-input-${id}`,
        "aria-label": hoverTip,
        onClick: event => {
          if (this.timer) {
            window.clearTimeout(this.timer);
            this.setState({
              copied: false
            });
          }

          onCopy(event, this.state.text);
          this.setState({
            copied: true
          }, () => {
            this.timer = window.setTimeout(() => {
              this.setState({
                copied: false
              });
              this.timer = null;
            }, switchDelay);
          });
        }
      }, this.state.copied ? clickTip : hoverTip)), this.state.expanded && React.createElement(ClipboardCopyExpanded, {
        isReadOnly: isReadOnly,
        isCode: isCode,
        id: `content-${id}`,
        onChange: this.updateText
      }, this.state.text))));
    });

    this.state = {
      text: this.props.children,
      expanded: this.props.isExpanded,
      copied: false
    };
  }

}

_defineProperty(ClipboardCopy, "propTypes", {
  className: _pt.string,
  hoverTip: _pt.string,
  clickTip: _pt.string,
  textAriaLabel: _pt.string,
  toggleAriaLabel: _pt.string,
  isReadOnly: _pt.bool,
  isExpanded: _pt.bool,
  isCode: _pt.bool,
  variant: _pt.oneOfType([_pt.any, _pt.oneOf(['inline']), _pt.oneOf(['expansion'])]),
  position: _pt.oneOfType([_pt.any, _pt.oneOf(['auto']), _pt.oneOf(['top']), _pt.oneOf(['bottom']), _pt.oneOf(['left']), _pt.oneOf(['right'])]),
  maxWidth: _pt.string,
  exitDelay: _pt.number,
  entryDelay: _pt.number,
  switchDelay: _pt.number,
  onCopy: _pt.func,
  onChange: _pt.func,
  children: _pt.node.isRequired
});

_defineProperty(ClipboardCopy, "defaultProps", {
  hoverTip: 'Copy to clipboard',
  clickTip: 'Successfully copied to clipboard!',
  isReadOnly: false,
  isExpanded: false,
  isCode: false,
  variant: 'inline',
  position: TooltipPosition.top,
  maxWidth: '150px',
  exitDelay: 1600,
  entryDelay: 100,
  switchDelay: 2000,
  onCopy: clipboardCopyFunc,
  onChange: () => undefined,
  textAriaLabel: 'Copyable input',
  toggleAriaLabel: 'Show content'
});
//# sourceMappingURL=ClipboardCopy.js.map