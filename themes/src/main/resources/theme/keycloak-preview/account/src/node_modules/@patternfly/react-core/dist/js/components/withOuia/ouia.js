"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.getUniqueId = exports.generateOUIAId = exports.isOUIAEnvironment = void 0;

var isOUIAEnvironment = function isOUIAEnvironment() {
  try {
    return typeof window !== 'undefined' && window.localStorage && window.localStorage.getItem('ouia:enabled') && window.localStorage['ouia:enabled'].toLowerCase() === 'true' || false;
  } catch (exception) {
    return false;
  }
};

exports.isOUIAEnvironment = isOUIAEnvironment;

var generateOUIAId = function generateOUIAId() {
  return typeof window !== 'undefined' && window.localStorage['ouia-generate-id'] && window.localStorage['ouia-generate-id'].toLowerCase() === 'true' || false;
};

exports.generateOUIAId = generateOUIAId;
var id = 0;

var getUniqueId = function getUniqueId() {
  return id++;
};

exports.getUniqueId = getUniqueId;
//# sourceMappingURL=ouia.js.map