import _pt from "prop-types";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/OptionsMenu/options-menu';
import { css } from '@patternfly/react-styles';
import { fillTemplate } from '../../helpers';
import { DropdownToggle } from '../Dropdown';
let toggleId = 0;
export const OptionsToggle = ({
  itemsTitle = 'items',
  optionsToggle = 'Select',
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  itemsPerPageTitle = 'Items per page',
  firstIndex = 0,
  lastIndex = 0,
  itemCount = 0,
  widgetId = '',
  showToggle = true,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onToggle = _isOpen => undefined,
  isOpen = false,
  isDisabled = false,
  parentRef = null,
  toggleTemplate: ToggleTemplate = '',
  onEnter = null
}) => React.createElement("div", {
  className: css(styles.optionsMenuToggle, isDisabled && styles.modifiers.disabled, styles.modifiers.plain, styles.modifiers.text)
}, showToggle && React.createElement(React.Fragment, null, React.createElement("span", {
  className: css(styles.optionsMenuToggleText)
}, typeof ToggleTemplate === 'string' ? fillTemplate(ToggleTemplate, {
  firstIndex,
  lastIndex,
  itemCount,
  itemsTitle
}) : React.createElement(ToggleTemplate, {
  firstIndex: firstIndex,
  lastIndex: lastIndex,
  itemCount: itemCount,
  itemsTitle: itemsTitle
})), React.createElement(DropdownToggle, {
  onEnter: onEnter,
  "aria-label": optionsToggle,
  onToggle: onToggle,
  isDisabled: isDisabled || itemCount <= 0,
  isOpen: isOpen,
  id: `${widgetId}-toggle-${toggleId++}`,
  className: styles.optionsMenuToggleButton,
  parentRef: parentRef
})));
OptionsToggle.propTypes = {
  itemsTitle: _pt.string,
  optionsToggle: _pt.string,
  itemsPerPageTitle: _pt.string,
  firstIndex: _pt.number,
  lastIndex: _pt.number,
  itemCount: _pt.number,
  widgetId: _pt.string,
  showToggle: _pt.bool,
  onToggle: _pt.func,
  isOpen: _pt.bool,
  isDisabled: _pt.bool,
  parentRef: _pt.any,
  toggleTemplate: _pt.oneOfType([_pt.func, _pt.string]),
  onEnter: _pt.func
};
//# sourceMappingURL=OptionsToggle.js.map