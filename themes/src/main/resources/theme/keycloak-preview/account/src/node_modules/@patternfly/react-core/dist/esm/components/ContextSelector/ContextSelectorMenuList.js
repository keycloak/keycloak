import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ContextSelector/context-selector';
import { css } from '@patternfly/react-styles';
export class ContextSelectorMenuList extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "refsCollection", []);

    _defineProperty(this, "sendRef", (index, ref) => {
      this.refsCollection[index] = ref;
    });

    _defineProperty(this, "render", () => {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const _this$props = this.props,
            {
        className,
        isOpen,
        children
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["className", "isOpen", "children"]);

      return React.createElement("ul", _extends({
        className: css(styles.contextSelectorMenuList, className),
        hidden: !isOpen,
        role: "menu"
      }, props), this.extendChildren());
    });
  }

  extendChildren() {
    return React.Children.map(this.props.children, (child, index) => React.cloneElement(child, {
      sendRef: this.sendRef,
      index
    }));
  }

}

_defineProperty(ContextSelectorMenuList, "propTypes", {
  children: _pt.node,
  className: _pt.string,
  isOpen: _pt.bool
});

_defineProperty(ContextSelectorMenuList, "defaultProps", {
  children: null,
  className: '',
  isOpen: true
});
//# sourceMappingURL=ContextSelectorMenuList.js.map