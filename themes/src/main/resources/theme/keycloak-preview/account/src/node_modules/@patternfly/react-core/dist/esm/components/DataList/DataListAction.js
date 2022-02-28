import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(source, true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(source).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import { css, pickProperties } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
const visibilityModifiers = pickProperties(styles.modifiers, ['hidden', 'hiddenOnSm', 'hiddenOnMd', 'hiddenOnLg', 'hiddenOnXl', 'hiddenOn_2xl', 'visibleOnSm', 'visibleOnMd', 'visibleOnLg', 'visibleOnXl', 'visibleOn_2xl']); // eslint-disable-next-line @typescript-eslint/interface-name-prefix

export const DataListActionVisibility = Object.keys(visibilityModifiers).map(key => [key.replace('_2xl', '2Xl'), visibilityModifiers[key]]).reduce((acc, curr) => _objectSpread({}, acc, {
  [curr[0]]: curr[1]
}), {});
export class DataListAction extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "onToggle", isOpen => {
      this.setState({
        isOpen
      });
    });

    _defineProperty(this, "onSelect", event => {
      this.setState(prevState => ({
        isOpen: !prevState.isOpen
      }));
    });

    this.state = {
      isOpen: false
    };
  }

  render() {
    const _this$props = this.props,
          {
      children,
      className,

      /* eslint-disable @typescript-eslint/no-unused-vars */
      id,
      'aria-label': ariaLabel,
      'aria-labelledby': ariaLabelledBy
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["children", "className", "id", "aria-label", "aria-labelledby"]);

    return React.createElement("div", _extends({
      className: css(styles.dataListItemAction, className)
    }, props), children);
  }

}

_defineProperty(DataListAction, "propTypes", {
  children: _pt.node.isRequired,
  className: _pt.string,
  id: _pt.string.isRequired,
  'aria-labelledby': _pt.string.isRequired,
  'aria-label': _pt.string.isRequired
});

_defineProperty(DataListAction, "defaultProps", {
  className: ''
});
//# sourceMappingURL=DataListAction.js.map