import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Form/form';
import { css } from '@patternfly/react-styles';
import { FormContext } from './FormContext';
export const ActionGroup = (_ref) => {
  let {
    children = null,
    className = ''
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className"]);

  const customClassName = css(styles.formGroup, styles.modifiers.action, className);
  const classesHorizontal = css(styles.formHorizontalGroup);
  const formActionsComponent = React.createElement("div", {
    className: css(styles.formActions)
  }, children);
  return React.createElement(FormContext.Consumer, null, ({
    isHorizontal
  }) => React.createElement("div", _extends({}, props, {
    className: customClassName
  }), isHorizontal ? React.createElement("div", {
    className: classesHorizontal
  }, formActionsComponent) : formActionsComponent));
};
ActionGroup.propTypes = {
  children: _pt.node,
  className: _pt.string
};
//# sourceMappingURL=ActionGroup.js.map