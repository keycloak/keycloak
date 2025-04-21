"use strict"
exports.__esModule = true;
exports.PagerIconConfig = {
  name: 'PagerIcon',
  height: 512,
  width: 512,
  svgPath: 'M448 64H64a64 64 0 0 0-64 64v256a64 64 0 0 0 64 64h384a64 64 0 0 0 64-64V128a64 64 0 0 0-64-64zM160 368H80a16 16 0 0 1-16-16v-16a16 16 0 0 1 16-16h80zm128-16a16 16 0 0 1-16 16h-80v-48h80a16 16 0 0 1 16 16zm160-128a32 32 0 0 1-32 32H96a32 32 0 0 1-32-32v-64a32 32 0 0 1 32-32h320a32 32 0 0 1 32 32z',
  yOffset: 0,
  xOffset: 0,
};
exports.PagerIcon = require('../createIcon').createIcon(exports.PagerIconConfig);
exports["default"] = exports.PagerIcon;