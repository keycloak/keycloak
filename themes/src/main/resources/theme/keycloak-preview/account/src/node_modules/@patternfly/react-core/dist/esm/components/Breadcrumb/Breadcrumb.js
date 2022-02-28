import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Breadcrumb/breadcrumb';
import { css } from '@patternfly/react-styles';
import { withOuiaContext } from '../withOuia';

const Breadcrumb = (_ref) => {
  let {
    children = null,
    className = '',
    'aria-label': ariaLabel = 'Breadcrumb',
    ouiaContext = null,
    ouiaId = null
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "aria-label", "ouiaContext", "ouiaId"]);

  return React.createElement("nav", _extends({}, props, {
    "aria-label": ariaLabel,
    className: css(styles.breadcrumb, className)
  }, ouiaContext.isOuia && {
    'data-ouia-component-type': 'Breadcrumb',
    'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
  }), React.createElement("ol", {
    className: css(styles.breadcrumbList)
  }, children));
};

Breadcrumb.propTypes = {
  children: _pt.node,
  className: _pt.string,
  'aria-label': _pt.string
};
const BreadcrumbWithOuiaContext = withOuiaContext(Breadcrumb);
export { BreadcrumbWithOuiaContext as Breadcrumb };
//# sourceMappingURL=Breadcrumb.js.map