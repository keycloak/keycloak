import core, { nullObj, boolObj, octObj, intObj, hexObj, nanObj, expObj, floatObj } from './core';
import failsafe from './failsafe';
import json from './json';
import yaml11 from './yaml-1.1';
import map from './failsafe/map';
import seq from './failsafe/seq';
import binary from './yaml-1.1/binary';
import omap from './yaml-1.1/omap';
import pairs from './yaml-1.1/pairs';
import set from './yaml-1.1/set';
import { floatTime, intTime, timestamp } from './yaml-1.1/timestamp';
export var schemas = {
  core: core,
  failsafe: failsafe,
  json: json,
  yaml11: yaml11
};
export var tags = {
  binary: binary,
  bool: boolObj,
  float: floatObj,
  floatExp: expObj,
  floatNaN: nanObj,
  floatTime: floatTime,
  int: intObj,
  intHex: hexObj,
  intOct: octObj,
  intTime: intTime,
  map: map,
  null: nullObj,
  omap: omap,
  pairs: pairs,
  seq: seq,
  set: set,
  timestamp: timestamp
};