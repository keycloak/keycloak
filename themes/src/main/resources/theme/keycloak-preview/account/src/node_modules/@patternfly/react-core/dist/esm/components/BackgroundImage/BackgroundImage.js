import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css, StyleSheet } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/BackgroundImage/background-image';
/* eslint-disable camelcase */

import c_background_image_BackgroundImage from '@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage';
import c_background_image_BackgroundImage_2x from '@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_2x';
import c_background_image_BackgroundImage_sm from '@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_sm';
import c_background_image_BackgroundImage_sm_2x from '@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_sm_2x';
import c_background_image_BackgroundImage_lg from '@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_lg';
export let BackgroundImageSrc;

(function (BackgroundImageSrc) {
  BackgroundImageSrc["xs"] = "xs";
  BackgroundImageSrc["xs2x"] = "xs2x";
  BackgroundImageSrc["sm"] = "sm";
  BackgroundImageSrc["sm2x"] = "sm2x";
  BackgroundImageSrc["lg"] = "lg";
  BackgroundImageSrc["filter"] = "filter";
})(BackgroundImageSrc || (BackgroundImageSrc = {}));

const cssVariables = {
  [BackgroundImageSrc.xs]: c_background_image_BackgroundImage && c_background_image_BackgroundImage.name,
  [BackgroundImageSrc.xs2x]: c_background_image_BackgroundImage_2x && c_background_image_BackgroundImage_2x.name,
  [BackgroundImageSrc.sm]: c_background_image_BackgroundImage_sm && c_background_image_BackgroundImage_sm.name,
  [BackgroundImageSrc.sm2x]: c_background_image_BackgroundImage_sm_2x && c_background_image_BackgroundImage_sm_2x.name,
  [BackgroundImageSrc.lg]: c_background_image_BackgroundImage_lg && c_background_image_BackgroundImage_lg.name
};
export const BackgroundImage = (_ref) => {
  let {
    className = '',
    src
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "src"]);

  let srcMap = src; // Default string value to handle all sizes

  if (typeof src === 'string') {
    srcMap = {
      [BackgroundImageSrc.xs]: src,
      [BackgroundImageSrc.xs2x]: src,
      [BackgroundImageSrc.sm]: src,
      [BackgroundImageSrc.sm2x]: src,
      [BackgroundImageSrc.lg]: src,
      [BackgroundImageSrc.filter]: '' // unused

    };
  } // Build stylesheet string based on cssVariables


  let cssSheet = '';
  Object.keys(cssVariables).forEach(size => {
    cssSheet += `${cssVariables[size]}: url('${srcMap[size]}');`;
  }); // Create emotion stylesheet to inject new css

  const bgStyles = StyleSheet.create({
    bgOverrides: `&.pf-c-background-image {
      ${cssSheet}
    }`
  });
  return React.createElement("div", _extends({
    className: css(styles.backgroundImage, bgStyles.bgOverrides, className)
  }, props), React.createElement("svg", {
    xmlns: "http://www.w3.org/2000/svg",
    className: "pf-c-background-image__filter",
    width: "0",
    height: "0"
  }, React.createElement("filter", {
    id: "image_overlay"
  }, React.createElement("feColorMatrix", {
    type: "matrix",
    values: "1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 0 0 0 1 0"
  }), React.createElement("feComponentTransfer", {
    colorInterpolationFilters: "sRGB",
    result: "duotone"
  }, React.createElement("feFuncR", {
    type: "table",
    tableValues: "0.086274509803922 0.43921568627451"
  }), React.createElement("feFuncG", {
    type: "table",
    tableValues: "0.086274509803922 0.43921568627451"
  }), React.createElement("feFuncB", {
    type: "table",
    tableValues: "0.086274509803922 0.43921568627451"
  }), React.createElement("feFuncA", {
    type: "table",
    tableValues: "0 1"
  })))));
};
BackgroundImage.propTypes = {
  className: _pt.string,
  src: _pt.oneOfType([_pt.string, _pt.shape({
    xs: _pt.string.isRequired,
    xs2x: _pt.string.isRequired,
    sm: _pt.string.isRequired,
    sm2x: _pt.string.isRequired,
    lg: _pt.string.isRequired,
    filter: _pt.string
  })]).isRequired
};
//# sourceMappingURL=BackgroundImage.js.map