import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Form/form';
import { ASTERISK } from '../../helpers/htmlConstants';
import { FormContext } from './FormContext';
import { css, getModifier } from '@patternfly/react-styles';
import { ValidatedOptions } from '../../helpers/constants';
export const FormGroup = (_ref) => {
  let {
    children = null,
    className = '',
    label,
    isRequired = false,
    isValid = true,
    validated = 'default',
    isInline = false,
    helperText,
    helperTextInvalid,
    fieldId
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "label", "isRequired", "isValid", "validated", "isInline", "helperText", "helperTextInvalid", "fieldId"]);

  const validHelperText = React.createElement("div", {
    className: css(styles.formHelperText, validated === ValidatedOptions.success && styles.modifiers.success),
    id: `${fieldId}-helper`,
    "aria-live": "polite"
  }, helperText);
  const inValidHelperText = React.createElement("div", {
    className: css(styles.formHelperText, styles.modifiers.error),
    id: `${fieldId}-helper`,
    "aria-live": "polite"
  }, helperTextInvalid);
  return React.createElement(FormContext.Consumer, null, ({
    isHorizontal
  }) => React.createElement("div", _extends({}, props, {
    className: css(styles.formGroup, isInline ? getModifier(styles, 'inline', className) : className)
  }), label && React.createElement("label", {
    className: css(styles.formLabel),
    htmlFor: fieldId
  }, React.createElement("span", {
    className: css(styles.formLabelText)
  }, label), isRequired && React.createElement("span", {
    className: css(styles.formLabelRequired),
    "aria-hidden": "true"
  }, ASTERISK)), isHorizontal ? React.createElement("div", {
    className: css(styles.formHorizontalGroup)
  }, children) : children, (!isValid || validated === ValidatedOptions.error) && helperTextInvalid ? inValidHelperText : validated !== ValidatedOptions.error && helperText ? validHelperText : ''));
};
FormGroup.propTypes = {
  children: _pt.node,
  className: _pt.string,
  label: _pt.node,
  isRequired: _pt.bool,
  isValid: _pt.bool,
  validated: _pt.oneOf(['success', 'error', 'default']),
  isInline: _pt.bool,
  helperText: _pt.node,
  helperTextInvalid: _pt.node,
  fieldId: _pt.string.isRequired
};
//# sourceMappingURL=FormGroup.js.map