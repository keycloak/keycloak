import hashString from '@emotion/hash';
import unitless from '@emotion/unitless';
import memoize from '@emotion/memoize';

var hyphenateRegex = /[A-Z]|^ms/g;
var animationRegex = /_EMO_([^_]+?)_([^]*?)_EMO_/g;
var processStyleName = memoize(function (styleName) {
  return styleName.replace(hyphenateRegex, '-$&').toLowerCase();
});

var processStyleValue = function processStyleValue(key, value) {
  if (value == null || typeof value === 'boolean') {
    return '';
  }

  switch (key) {
    case 'animation':
    case 'animationName':
      {
        value = value.replace(animationRegex, function (match, p1, p2) {
          styles = p2 + styles;
          return p1;
        });
      }
  }

  if (unitless[key] !== 1 && key.charCodeAt(1) !== 45 && // custom properties
  !isNaN(value) && value !== 0) {
    return value + 'px';
  }

  return value;
};

if (process.env.NODE_ENV !== 'production') {
  var contentValuePattern = /(attr|calc|counters?|url)\(/;
  var contentValues = ['normal', 'none', 'counter', 'open-quote', 'close-quote', 'no-open-quote', 'no-close-quote', 'initial', 'inherit', 'unset'];
  var oldProcessStyleValue = processStyleValue;

  processStyleValue = function processStyleValue(key, value) {
    if (key === 'content') {
      if (typeof value !== 'string' || contentValues.indexOf(value) === -1 && !contentValuePattern.test(value) && (value.charAt(0) !== value.charAt(value.length - 1) || value.charAt(0) !== '"' && value.charAt(0) !== "'")) {
        console.error("You seem to be using a value for 'content' without quotes, try replacing it with `content: '\"" + value + "\"'`");
      }
    }

    return oldProcessStyleValue(key, value);
  };
}

function handleInterpolation(mergedProps, registered, interpolation) {
  if (interpolation == null) {
    return '';
  }

  if (interpolation.__emotion_styles !== undefined) {
    if (process.env.NODE_ENV !== 'production' && interpolation.toString() === 'NO_COMPONENT_SELECTOR') {
      throw new Error('Component selectors can only be used in conjunction with babel-plugin-emotion.');
    }

    return interpolation;
  }

  switch (typeof interpolation) {
    case 'boolean':
      {
        return '';
      }

    case 'object':
      {
        if (interpolation.anim === 1) {
          styles = interpolation.styles + styles;
          return interpolation.name;
        }

        if (interpolation.styles !== undefined) {
          return interpolation.styles;
        }

        return createStringFromObject(mergedProps, registered, interpolation);
      }

    case 'function':
      {
        if (mergedProps !== undefined) {
          return handleInterpolation(mergedProps, registered, // $FlowFixMe
          interpolation(mergedProps));
        }
      }
    // eslint-disable-next-line no-fallthrough

    default:
      {
        var cached = registered[interpolation];
        return cached !== undefined ? cached : interpolation;
      }
  }
}

function createStringFromObject(mergedProps, registered, obj) {
  var string = '';

  if (Array.isArray(obj)) {
    for (var i = 0; i < obj.length; i++) {
      string += handleInterpolation(mergedProps, registered, obj[i]);
    }
  } else {
    var _loop = function _loop(_key) {
      if (typeof obj[_key] !== 'object') {
        string += processStyleName(_key) + ":" + processStyleValue(_key, obj[_key]) + ";";
      } else {
        if (_key === 'NO_COMPONENT_SELECTOR' && process.env.NODE_ENV !== 'production') {
          throw new Error('Component selectors can only be used in conjunction with @emotion/babel-plugin-core.');
        }

        if (Array.isArray(obj[_key]) && typeof obj[_key][0] === 'string' && registered[obj[_key][0]] === undefined) {
          obj[_key].forEach(function (value) {
            string += processStyleName(_key) + ":" + processStyleValue(_key, value) + ";";
          });
        } else {
          string += _key + "{" + handleInterpolation(mergedProps, registered, obj[_key]) + "}";
        }
      }
    };

    for (var _key in obj) {
      _loop(_key);
    }
  }

  return string;
}

var labelPattern = /label:\s*([^\s;\n{]+)\s*;/g; // this is set to an empty string on each serializeStyles call
// it's declared in the module scope since we need to add to
// it in the middle of serialization to add styles from keyframes

var styles = '';
var serializeStyles = function serializeStyles(registered, args, mergedProps) {
  if (args.length === 1 && typeof args[0] === 'object' && args[0] !== null && args[0].styles !== undefined) {
    return args[0];
  }

  var stringMode = true;
  styles = '';
  var identifierName = '';
  var strings = args[0];

  if (strings == null || strings.raw === undefined) {
    stringMode = false; // we have to store this in a variable and then append it to styles since
    // styles could be modified in handleInterpolation and using += would mean
    // it would append the return value of handleInterpolation to the value before handleInterpolation is called

    var stringifiedInterpolation = handleInterpolation(mergedProps, registered, strings);
    styles += stringifiedInterpolation;
  } else {
    styles += strings[0];
  } // we start at 1 since we've already handled the first arg


  for (var i = 1; i < args.length; i++) {
    // we have to store this in a variable and then append it to styles since
    // styles could be modified in handleInterpolation and using += would mean
    // it would append the return value of handleInterpolation to the value before handleInterpolation is called
    var _stringifiedInterpolation = handleInterpolation(mergedProps, registered, args[i]);

    styles += _stringifiedInterpolation;

    if (stringMode) {
      styles += strings[i];
    }
  }

  styles = styles.replace(labelPattern, function (match, p1) {
    identifierName += "-" + p1;
    return '';
  });
  var name = hashString(styles) + identifierName;
  return {
    name: name,
    styles: styles
  };
};

export { serializeStyles };
