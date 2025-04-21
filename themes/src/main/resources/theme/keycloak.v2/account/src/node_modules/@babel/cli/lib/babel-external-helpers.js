"use strict";

function _commander() {
  const data = require("commander");

  _commander = function () {
    return data;
  };

  return data;
}

function _core() {
  const data = require("@babel/core");

  _core = function () {
    return data;
  };

  return data;
}

function collect(value, previousValue) {
  if (typeof value !== "string") return previousValue;
  const values = value.split(",");

  if (previousValue) {
    previousValue.push(...values);
    return previousValue;
  }

  return values;
}

_commander().option("-l, --whitelist [whitelist]", "Whitelist of helpers to ONLY include", collect);

_commander().option("-t, --output-type [type]", "Type of output (global|umd|var)", "global");

_commander().usage("[options]");

_commander().parse(process.argv);

console.log((0, _core().buildExternalHelpers)(_commander().whitelist, _commander().outputType));