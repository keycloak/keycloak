import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/layouts/Bullseye/bullseye';
import { FocusTrap } from '../../helpers';
import { AboutModalBoxContent } from './AboutModalBoxContent';
import { AboutModalBoxHeader } from './AboutModalBoxHeader';
import { AboutModalBoxHero } from './AboutModalBoxHero';
import { AboutModalBoxBrand } from './AboutModalBoxBrand';
import { AboutModalBoxCloseButton } from './AboutModalBoxCloseButton';
import { AboutModalBox } from './AboutModalBox';
import { Backdrop } from '../Backdrop/Backdrop';
export const AboutModalContainer = (_ref) => {
  let {
    children,
    className = '',
    isOpen = false,
    onClose = () => undefined,
    productName = '',
    trademark,
    brandImageSrc,
    brandImageAlt,
    backgroundImageSrc,
    ariaLabelledbyId,
    ariaDescribedById,
    closeButtonAriaLabel
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "isOpen", "onClose", "productName", "trademark", "brandImageSrc", "brandImageAlt", "backgroundImageSrc", "ariaLabelledbyId", "ariaDescribedById", "closeButtonAriaLabel"]);

  if (!isOpen) {
    return null;
  }

  return React.createElement(Backdrop, null, React.createElement(FocusTrap, {
    focusTrapOptions: {
      clickOutsideDeactivates: true
    },
    className: css(styles.bullseye)
  }, React.createElement(AboutModalBox, {
    className: className,
    "aria-labelledby": ariaLabelledbyId,
    "aria-describedby": ariaDescribedById
  }, React.createElement(AboutModalBoxBrand, {
    src: brandImageSrc,
    alt: brandImageAlt
  }), React.createElement(AboutModalBoxCloseButton, {
    "aria-label": closeButtonAriaLabel,
    onClose: onClose
  }), productName && React.createElement(AboutModalBoxHeader, {
    id: ariaLabelledbyId,
    productName: productName
  }), React.createElement(AboutModalBoxContent, _extends({
    trademark: trademark,
    id: ariaDescribedById,
    noAboutModalBoxContentContainer: false
  }, props), children), React.createElement(AboutModalBoxHero, {
    backgroundImageSrc: backgroundImageSrc
  }))));
};
AboutModalContainer.propTypes = {
  children: _pt.node.isRequired,
  className: _pt.string,
  isOpen: _pt.bool,
  onClose: _pt.func,
  productName: _pt.string,
  trademark: _pt.string,
  brandImageSrc: _pt.string.isRequired,
  brandImageAlt: _pt.string.isRequired,
  backgroundImageSrc: _pt.string,
  ariaLabelledbyId: _pt.string.isRequired,
  ariaDescribedById: _pt.string.isRequired,
  closeButtonAriaLabel: _pt.string
};
//# sourceMappingURL=AboutModalContainer.js.map