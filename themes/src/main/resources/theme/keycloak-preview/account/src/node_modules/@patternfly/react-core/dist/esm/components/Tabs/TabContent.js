import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';

const TabContentBase = (_ref) => {
  let {
    id,
    activeKey,
    'aria-label': ariaLabel,
    child,
    children,
    className,
    eventKey,
    innerRef
  } = _ref,
      props = _objectWithoutProperties(_ref, ["id", "activeKey", "aria-label", "child", "children", "className", "eventKey", "innerRef"]);

  if (children || child) {
    let labelledBy;

    if (ariaLabel) {
      labelledBy = null;
    } else {
      labelledBy = children ? `pf-tab-${eventKey}-${id}` : `pf-tab-${child.props.eventKey}-${id}`;
    }

    return React.createElement("section", _extends({
      ref: innerRef,
      hidden: children ? null : child.props.eventKey !== activeKey,
      className: children ? css('pf-c-tab-content', className) : css('pf-c-tab-content', child.props.className),
      id: children ? id : `pf-tab-section-${child.props.eventKey}-${id}`,
      "aria-label": ariaLabel,
      "aria-labelledby": labelledBy,
      role: "tabpanel",
      tabIndex: 0
    }, props), children || child.props.children);
  }

  return null;
};

TabContentBase.propTypes = {
  children: _pt.any,
  child: _pt.element,
  className: _pt.string,
  activeKey: _pt.oneOfType([_pt.number, _pt.string]),
  eventKey: _pt.oneOfType([_pt.number, _pt.string]),
  innerRef: _pt.oneOfType([_pt.string, _pt.func, _pt.object]),
  id: _pt.string.isRequired,
  'aria-label': _pt.string
};
export const TabContent = React.forwardRef((props, ref) => React.createElement(TabContentBase, _extends({}, props, {
  innerRef: ref
})));
//# sourceMappingURL=TabContent.js.map