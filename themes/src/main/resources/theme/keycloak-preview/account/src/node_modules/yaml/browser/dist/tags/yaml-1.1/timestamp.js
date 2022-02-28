import { stringifyNumber } from '../../stringify';

var parseSexagesimal = function parseSexagesimal(sign, parts) {
  var n = parts.split(':').reduce(function (n, p) {
    return n * 60 + Number(p);
  }, 0);
  return sign === '-' ? -n : n;
}; // hhhh:mm:ss.sss


var stringifySexagesimal = function stringifySexagesimal(_ref) {
  var value = _ref.value;
  if (isNaN(value) || !isFinite(value)) return stringifyNumber(value);
  var sign = '';

  if (value < 0) {
    sign = '-';
    value = Math.abs(value);
  }

  var parts = [value % 60]; // seconds, including ms

  if (value < 60) {
    parts.unshift(0); // at least one : is required
  } else {
    value = Math.round((value - parts[0]) / 60);
    parts.unshift(value % 60); // minutes

    if (value >= 60) {
      value = Math.round((value - parts[0]) / 60);
      parts.unshift(value); // hours
    }
  }

  return sign + parts.map(function (n) {
    return n < 10 ? '0' + String(n) : String(n);
  }).join(':').replace(/000000\d*$/, '') // % 60 may introduce error
  ;
};

export var intTime = {
  identify: function identify(value) {
    return typeof value === 'number';
  },
  default: true,
  tag: 'tag:yaml.org,2002:int',
  format: 'TIME',
  test: /^([-+]?)([0-9][0-9_]*(?::[0-5]?[0-9])+)$/,
  resolve: function resolve(str, sign, parts) {
    return parseSexagesimal(sign, parts.replace(/_/g, ''));
  },
  stringify: stringifySexagesimal
};
export var floatTime = {
  identify: function identify(value) {
    return typeof value === 'number';
  },
  default: true,
  tag: 'tag:yaml.org,2002:float',
  format: 'TIME',
  test: /^([-+]?)([0-9][0-9_]*(?::[0-5]?[0-9])+\.[0-9_]*)$/,
  resolve: function resolve(str, sign, parts) {
    return parseSexagesimal(sign, parts.replace(/_/g, ''));
  },
  stringify: stringifySexagesimal
};
export var timestamp = {
  identify: function identify(value) {
    return value instanceof Date;
  },
  default: true,
  tag: 'tag:yaml.org,2002:timestamp',
  // If the time zone is omitted, the timestamp is assumed to be specified in UTC. The time part
  // may be omitted altogether, resulting in a date format. In such a case, the time part is
  // assumed to be 00:00:00Z (start of day, UTC).
  test: RegExp('^(?:' + '([0-9]{4})-([0-9]{1,2})-([0-9]{1,2})' + // YYYY-Mm-Dd
  '(?:(?:t|T|[ \\t]+)' + // t | T | whitespace
  '([0-9]{1,2}):([0-9]{1,2}):([0-9]{1,2}(\\.[0-9]+)?)' + // Hh:Mm:Ss(.ss)?
  '(?:[ \\t]*(Z|[-+][012]?[0-9](?::[0-9]{2})?))?' + // Z | +5 | -03:30
  ')?' + ')$'),
  resolve: function resolve(str, year, month, day, hour, minute, second, millisec, tz) {
    if (millisec) millisec = (millisec + '00').substr(1, 3);
    var date = Date.UTC(year, month - 1, day, hour || 0, minute || 0, second || 0, millisec || 0);

    if (tz && tz !== 'Z') {
      var d = parseSexagesimal(tz[0], tz.slice(1));
      if (Math.abs(d) < 30) d *= 60;
      date -= 60000 * d;
    }

    return new Date(date);
  },
  stringify: function stringify(_ref2) {
    var value = _ref2.value;
    return value.toISOString().replace(/((T00:00)?:00)?\.000Z$/, '');
  }
};