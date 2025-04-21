"use strict"
exports.__esModule = true;
exports.EggIconConfig = {
  name: 'EggIcon',
  height: 512,
  width: 384,
  svgPath: 'M192 0C86 0 0 214 0 320s86 192 192 192 192-86 192-192S298 0 192 0z',
  yOffset: 0,
  xOffset: 0,
};
exports.EggIcon = require('../createIcon').createIcon(exports.EggIconConfig);
exports["default"] = exports.EggIcon;