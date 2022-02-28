import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Accordion/accordion';
import { AccordionContext } from './AccordionContext';
export const AccordionContent = (_ref) => {
  let {
    className = '',
    children = null,
    id = '',
    isHidden = false,
    isFixed = false,
    'aria-label': ariaLabel = '',
    component
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "children", "id", "isHidden", "isFixed", "aria-label", "component"]);

  return React.createElement(AccordionContext.Consumer, null, ({
    ContentContainer
  }) => {
    const Container = component || ContentContainer;
    return React.createElement(Container, _extends({
      id: id,
      className: css(styles.accordionExpandedContent, isFixed && styles.modifiers.fixed, !isHidden && styles.modifiers.expanded, className),
      hidden: isHidden,
      "aria-label": ariaLabel
    }, props), React.createElement("div", {
      className: css(styles.accordionExpandedContentBody)
    }, children));
  });
};
AccordionContent.propTypes = {
  children: _pt.node,
  className: _pt.string,
  id: _pt.string,
  isHidden: _pt.bool,
  isFixed: _pt.bool,
  'aria-label': _pt.string,
  component: _pt.any
};
//# sourceMappingURL=AccordionContent.js.map