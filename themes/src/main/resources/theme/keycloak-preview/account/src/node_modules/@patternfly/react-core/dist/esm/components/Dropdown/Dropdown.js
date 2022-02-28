import _pt from "prop-types";

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
import { DropdownContext } from './dropdownConstants';
import { DropdownWithContext } from './DropdownWithContext';
export const Dropdown = (_ref) => {
  let {
    onSelect,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref
  } = _ref,
      props = _objectWithoutProperties(_ref, ["onSelect", "ref"]);

  return React.createElement(DropdownContext.Provider, {
    value: {
      onSelect: event => onSelect && onSelect(event),
      toggleTextClass: styles.dropdownToggleText,
      toggleIconClass: styles.dropdownToggleIcon,
      menuClass: styles.dropdownMenu,
      itemClass: styles.dropdownMenuItem,
      toggleClass: styles.dropdownToggle,
      baseClass: styles.dropdown,
      baseComponent: 'div',
      sectionClass: styles.dropdownGroup,
      sectionTitleClass: styles.dropdownGroupTitle,
      sectionComponent: 'section',
      disabledClass: styles.modifiers.disabled,
      hoverClass: styles.modifiers.hover,
      separatorClass: styles.dropdownSeparator
    }
  }, React.createElement(DropdownWithContext, props));
};
Dropdown.propTypes = {
  children: _pt.node,
  className: _pt.string,
  dropdownItems: _pt.arrayOf(_pt.any),
  isOpen: _pt.bool,
  isPlain: _pt.bool,
  position: _pt.oneOfType([_pt.any, _pt.oneOf(['right']), _pt.oneOf(['left'])]),
  direction: _pt.oneOfType([_pt.any, _pt.oneOf(['up']), _pt.oneOf(['down'])]),
  isGrouped: _pt.bool,
  toggle: _pt.element.isRequired,
  onSelect: _pt.func,
  autoFocus: _pt.bool,
  ouiaComponentType: _pt.string
};
//# sourceMappingURL=Dropdown.js.map