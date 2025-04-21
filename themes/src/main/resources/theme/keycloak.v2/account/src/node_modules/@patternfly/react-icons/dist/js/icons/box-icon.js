"use strict"
exports.__esModule = true;
exports.BoxIconConfig = {
  name: 'BoxIcon',
  height: 512,
  width: 512,
  svgPath: 'M509.5 184.6L458.9 32.8C452.4 13.2 434.1 0 413.4 0H272v192h238.7c-.4-2.5-.4-5-1.2-7.4zM240 0H98.6c-20.7 0-39 13.2-45.5 32.8L2.5 184.6c-.8 2.4-.8 4.9-1.2 7.4H240V0zM0 224v240c0 26.5 21.5 48 48 48h416c26.5 0 48-21.5 48-48V224H0z',
  yOffset: 0,
  xOffset: 0,
};
exports.BoxIcon = require('../createIcon').createIcon(exports.BoxIconConfig);
exports["default"] = exports.BoxIcon;