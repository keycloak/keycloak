import { stringifyString } from '../../stringify';
import { strOptions } from '../options';
export var resolveString = function resolveString(doc, node) {
  // on error, will return { str: string, errors: Error[] }
  var res = node.strValue;
  if (!res) return '';
  if (typeof res === 'string') return res;
  res.errors.forEach(function (error) {
    if (!error.source) error.source = node;
    doc.errors.push(error);
  });
  return res.str;
};
export default {
  identify: function identify(value) {
    return typeof value === 'string';
  },
  default: true,
  tag: 'tag:yaml.org,2002:str',
  resolve: resolveString,
  stringify: function stringify(item, ctx, onComment, onChompKeep) {
    ctx = Object.assign({
      actualString: true
    }, ctx);
    return stringifyString(item, ctx, onComment, onChompKeep);
  },
  options: strOptions
};