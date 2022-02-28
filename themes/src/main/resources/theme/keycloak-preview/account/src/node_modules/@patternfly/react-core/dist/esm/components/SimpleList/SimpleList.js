import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/SimpleList/simple-list';
import { SimpleListGroup } from './SimpleListGroup';
export const SimpleListContext = React.createContext({});
export class SimpleList extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "state", {
      currentRef: null
    });

    _defineProperty(this, "handleCurrentUpdate", (newCurrentRef, itemProps) => {
      this.setState({
        currentRef: newCurrentRef
      });
      const {
        onSelect
      } = this.props;
      onSelect && onSelect(newCurrentRef, itemProps);
    });
  }

  componentDidMount() {
    if (!SimpleList.hasWarnBeta && process.env.NODE_ENV !== 'production') {
      // eslint-disable-next-line no-console
      console.warn('This component is in beta and subject to change.');
      SimpleList.hasWarnBeta = true;
    }
  }

  render() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const _this$props = this.props,
          {
      children,
      className,
      onSelect
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["children", "className", "onSelect"]);

    let isGrouped = false;

    if (children) {
      isGrouped = React.Children.toArray(children)[0].type === SimpleListGroup;
    }

    return React.createElement(SimpleListContext.Provider, {
      value: {
        currentRef: this.state.currentRef,
        updateCurrentRef: this.handleCurrentUpdate
      }
    }, React.createElement("div", _extends({
      className: css(styles.simpleList, className)
    }, props, isGrouped && {
      role: 'list'
    }), isGrouped && children, !isGrouped && React.createElement("ul", null, children)));
  }

}

_defineProperty(SimpleList, "propTypes", {
  children: _pt.node,
  className: _pt.string,
  onSelect: _pt.func
});

_defineProperty(SimpleList, "hasWarnBeta", false);

_defineProperty(SimpleList, "defaultProps", {
  children: null,
  className: ''
});
//# sourceMappingURL=SimpleList.js.map