import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { FocusTrap } from '../../helpers';
import titleStyles from '@patternfly/react-styles/css/components/Title/title';
import styles from '@patternfly/react-styles/css/layouts/Bullseye/bullseye';
import { css } from '@patternfly/react-styles';
import { Backdrop } from '../Backdrop/Backdrop';
import { ModalBoxBody } from './ModalBoxBody';
import { ModalBoxHeader } from './ModalBoxHeader';
import { ModalBoxCloseButton } from './ModalBoxCloseButton';
import { ModalBox } from './ModalBox';
import { ModalBoxFooter } from './ModalBoxFooter';
import { ModalBoxDescription } from './ModalBoxDescription';
export const ModalContent = (_ref) => {
  let {
    children,
    className = '',
    isOpen = false,
    header = null,
    description = null,
    title,
    hideTitle = false,
    showClose = true,
    footer = null,
    actions = [],
    isFooterLeftAligned = false,
    onClose = () => undefined,
    isLarge = false,
    isSmall = false,
    width = -1,
    ariaDescribedById = '',
    id = '',
    disableFocusTrap = false
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "isOpen", "header", "description", "title", "hideTitle", "showClose", "footer", "actions", "isFooterLeftAligned", "onClose", "isLarge", "isSmall", "width", "ariaDescribedById", "id", "disableFocusTrap"]);

  if (!isOpen) {
    return null;
  }

  const modalBoxHeader = header ? React.createElement("div", {
    className: css(titleStyles.title)
  }, header) : React.createElement(ModalBoxHeader, {
    hideTitle: hideTitle
  }, " ", title, " ");
  const modalBoxFooter = footer ? React.createElement(ModalBoxFooter, {
    isLeftAligned: isFooterLeftAligned
  }, footer) : actions.length > 0 && React.createElement(ModalBoxFooter, {
    isLeftAligned: isFooterLeftAligned
  }, actions);
  const boxStyle = width === -1 ? {} : {
    width
  };
  const modalBox = React.createElement(ModalBox, {
    style: boxStyle,
    className: className,
    isLarge: isLarge,
    isSmall: isSmall,
    title: title,
    id: ariaDescribedById || id
  }, showClose && React.createElement(ModalBoxCloseButton, {
    onClose: onClose
  }), modalBoxHeader, description && React.createElement(ModalBoxDescription, {
    id: id
  }, description), React.createElement(ModalBoxBody, _extends({}, props, !description && {
    id
  }), children), modalBoxFooter);
  return React.createElement(Backdrop, null, React.createElement(FocusTrap, {
    active: !disableFocusTrap,
    focusTrapOptions: {
      clickOutsideDeactivates: true
    },
    className: css(styles.bullseye)
  }, modalBox));
};
ModalContent.propTypes = {
  children: _pt.node.isRequired,
  className: _pt.string,
  isLarge: _pt.bool,
  isSmall: _pt.bool,
  isOpen: _pt.bool,
  header: _pt.node,
  description: _pt.node,
  title: _pt.string.isRequired,
  hideTitle: _pt.bool,
  showClose: _pt.bool,
  width: _pt.oneOfType([_pt.number, _pt.string]),
  footer: _pt.node,
  actions: _pt.any,
  isFooterLeftAligned: _pt.bool,
  onClose: _pt.func,
  ariaDescribedById: _pt.string,
  id: _pt.string.isRequired,
  disableFocusTrap: _pt.bool
};
//# sourceMappingURL=ModalContent.js.map