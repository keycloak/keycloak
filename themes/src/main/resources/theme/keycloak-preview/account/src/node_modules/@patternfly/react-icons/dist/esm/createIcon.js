function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import React from 'react';
import { getSize, propTypes, defaultProps } from './common';
let currentId = 0;

const createIcon = iconDefinition => {
  const viewBox = [iconDefinition.xOffset || 0, iconDefinition.yOffset || 0, iconDefinition.width, iconDefinition.height].join(' ');
  const transform = iconDefinition.transform;

  class Icon extends React.Component {
    constructor(...args) {
      super(...args);

      _defineProperty(this, "id", `icon-title-${currentId++}`);
    }

    render() {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const _this$props = this.props,
            {
        size,
        color,
        title,
        noStyle,
        noVerticalAlign
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["size", "color", "title", "noStyle", "noVerticalAlign"]);

      const hasTitle = Boolean(title);
      const heightWidth = getSize(size);
      const baseAlign = -0.125 * Number.parseFloat(heightWidth);
      const style = noVerticalAlign ? null : {
        verticalAlign: `${baseAlign}em`
      };
      return React.createElement("svg", _extends({
        style: style,
        fill: color,
        height: heightWidth,
        width: heightWidth,
        viewBox: viewBox,
        "aria-labelledby": hasTitle ? this.id : null,
        "aria-hidden": hasTitle ? null : true,
        role: "img"
      }, props), hasTitle && React.createElement("title", {
        id: this.id
      }, title), React.createElement("path", {
        d: iconDefinition.svgPath,
        transform: transform
      }));
    }

  }

  _defineProperty(Icon, "displayName", iconDefinition.name);

  _defineProperty(Icon, "propTypes", propTypes);

  _defineProperty(Icon, "defaultProps", defaultProps);

  return Icon;
};

export default createIcon;
//# sourceMappingURL=createIcon.js.map