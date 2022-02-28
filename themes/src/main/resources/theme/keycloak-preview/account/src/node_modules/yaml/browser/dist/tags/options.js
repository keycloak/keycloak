import { Type } from '../constants';
export var binaryOptions = {
  defaultType: Type.BLOCK_LITERAL,
  lineWidth: 76
};
export var boolOptions = {
  trueStr: 'true',
  falseStr: 'false'
};
export var nullOptions = {
  nullStr: 'null'
};
export var strOptions = {
  defaultType: Type.PLAIN,
  doubleQuoted: {
    jsonEncoding: false,
    minMultiLineLength: 40
  },
  fold: {
    lineWidth: 80,
    minContentWidth: 20
  }
};