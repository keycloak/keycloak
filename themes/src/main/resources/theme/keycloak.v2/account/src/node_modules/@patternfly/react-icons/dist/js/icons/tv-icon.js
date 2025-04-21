"use strict"
exports.__esModule = true;
exports.TvIconConfig = {
  name: 'TvIcon',
  height: 512,
  width: 640,
  svgPath: 'M592 0H48A48 48 0 0 0 0 48v320a48 48 0 0 0 48 48h240v32H112a16 16 0 0 0-16 16v32a16 16 0 0 0 16 16h416a16 16 0 0 0 16-16v-32a16 16 0 0 0-16-16H352v-32h240a48 48 0 0 0 48-48V48a48 48 0 0 0-48-48zm-16 352H64V64h512z',
  yOffset: 0,
  xOffset: 0,
};
exports.TvIcon = require('../createIcon').createIcon(exports.TvIconConfig);
exports["default"] = exports.TvIcon;