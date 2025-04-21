/**
 * @fileoverview Prevent usage of unknown DOM property
 * @author Yannick Croissant
 */

'use strict';

const has = require('object.hasown/polyfill')();
const docsUrl = require('../util/docsUrl');
const testReactVersion = require('../util/version').testReactVersion;
const report = require('../util/report');

// ------------------------------------------------------------------------------
// Constants
// ------------------------------------------------------------------------------

const DEFAULTS = {
  ignore: [],
};

const DOM_ATTRIBUTE_NAMES = {
  'accept-charset': 'acceptCharset',
  class: 'className',
  for: 'htmlFor',
  'http-equiv': 'httpEquiv',
  crossorigin: 'crossOrigin',
};

const ATTRIBUTE_TAGS_MAP = {
  // image is required for SVG support, all other tags are HTML.
  crossOrigin: ['script', 'img', 'video', 'audio', 'link', 'image'],
};

const SVGDOM_ATTRIBUTE_NAMES = {
  'accent-height': 'accentHeight',
  'alignment-baseline': 'alignmentBaseline',
  'arabic-form': 'arabicForm',
  'baseline-shift': 'baselineShift',
  'cap-height': 'capHeight',
  'clip-path': 'clipPath',
  'clip-rule': 'clipRule',
  'color-interpolation': 'colorInterpolation',
  'color-interpolation-filters': 'colorInterpolationFilters',
  'color-profile': 'colorProfile',
  'color-rendering': 'colorRendering',
  'dominant-baseline': 'dominantBaseline',
  'enable-background': 'enableBackground',
  'fill-opacity': 'fillOpacity',
  'fill-rule': 'fillRule',
  'flood-color': 'floodColor',
  'flood-opacity': 'floodOpacity',
  'font-family': 'fontFamily',
  'font-size': 'fontSize',
  'font-size-adjust': 'fontSizeAdjust',
  'font-stretch': 'fontStretch',
  'font-style': 'fontStyle',
  'font-variant': 'fontVariant',
  'font-weight': 'fontWeight',
  'glyph-name': 'glyphName',
  'glyph-orientation-horizontal': 'glyphOrientationHorizontal',
  'glyph-orientation-vertical': 'glyphOrientationVertical',
  'horiz-adv-x': 'horizAdvX',
  'horiz-origin-x': 'horizOriginX',
  'image-rendering': 'imageRendering',
  'letter-spacing': 'letterSpacing',
  'lighting-color': 'lightingColor',
  'marker-end': 'markerEnd',
  'marker-mid': 'markerMid',
  'marker-start': 'markerStart',
  'overline-position': 'overlinePosition',
  'overline-thickness': 'overlineThickness',
  'paint-order': 'paintOrder',
  'panose-1': 'panose1',
  'pointer-events': 'pointerEvents',
  'rendering-intent': 'renderingIntent',
  'shape-rendering': 'shapeRendering',
  'stop-color': 'stopColor',
  'stop-opacity': 'stopOpacity',
  'strikethrough-position': 'strikethroughPosition',
  'strikethrough-thickness': 'strikethroughThickness',
  'stroke-dasharray': 'strokeDasharray',
  'stroke-dashoffset': 'strokeDashoffset',
  'stroke-linecap': 'strokeLinecap',
  'stroke-linejoin': 'strokeLinejoin',
  'stroke-miterlimit': 'strokeMiterlimit',
  'stroke-opacity': 'strokeOpacity',
  'stroke-width': 'strokeWidth',
  'text-anchor': 'textAnchor',
  'text-decoration': 'textDecoration',
  'text-rendering': 'textRendering',
  'underline-position': 'underlinePosition',
  'underline-thickness': 'underlineThickness',
  'unicode-bidi': 'unicodeBidi',
  'unicode-range': 'unicodeRange',
  'units-per-em': 'unitsPerEm',
  'v-alphabetic': 'vAlphabetic',
  'v-hanging': 'vHanging',
  'v-ideographic': 'vIdeographic',
  'v-mathematical': 'vMathematical',
  'vector-effect': 'vectorEffect',
  'vert-adv-y': 'vertAdvY',
  'vert-origin-x': 'vertOriginX',
  'vert-origin-y': 'vertOriginY',
  'word-spacing': 'wordSpacing',
  'writing-mode': 'writingMode',
  'x-height': 'xHeight',
  'xlink:actuate': 'xlinkActuate',
  'xlink:arcrole': 'xlinkArcrole',
  'xlink:href': 'xlinkHref',
  'xlink:role': 'xlinkRole',
  'xlink:show': 'xlinkShow',
  'xlink:title': 'xlinkTitle',
  'xlink:type': 'xlinkType',
  'xml:base': 'xmlBase',
  'xml:lang': 'xmlLang',
  'xml:space': 'xmlSpace',
};

const DOM_PROPERTY_NAMES = [
  // Standard
  'acceptCharset', 'accessKey', 'allowFullScreen', 'autoComplete', 'autoFocus', 'autoPlay',
  'cellPadding', 'cellSpacing', 'classID', 'className', 'colSpan', 'contentEditable', 'contextMenu',
  'dateTime', 'encType', 'formAction', 'formEncType', 'formMethod', 'formNoValidate', 'formTarget',
  'frameBorder', 'hrefLang', 'htmlFor', 'httpEquiv', 'inputMode', 'keyParams', 'keyType', 'marginHeight', 'marginWidth',
  'maxLength', 'mediaGroup', 'minLength', 'noValidate', 'onAnimationEnd', 'onAnimationIteration', 'onAnimationStart',
  'onBlur', 'onChange', 'onClick', 'onContextMenu', 'onCopy', 'onCompositionEnd', 'onCompositionStart',
  'onCompositionUpdate', 'onCut', 'onDoubleClick', 'onDrag', 'onDragEnd', 'onDragEnter', 'onDragExit', 'onDragLeave',
  'onError', 'onFocus', 'onInput', 'onKeyDown', 'onKeyPress', 'onKeyUp', 'onLoad', 'onWheel', 'onDragOver',
  'onDragStart', 'onDrop', 'onMouseDown', 'onMouseEnter', 'onMouseLeave', 'onMouseMove', 'onMouseOut', 'onMouseOver',
  'onMouseUp', 'onPaste', 'onScroll', 'onSelect', 'onSubmit', 'onTransitionEnd', 'radioGroup', 'readOnly', 'rowSpan',
  'spellCheck', 'srcDoc', 'srcLang', 'srcSet', 'tabIndex', 'useMap',
  // Non standard
  'autoCapitalize', 'autoCorrect',
  'autoSave',
  'itemProp', 'itemScope', 'itemType', 'itemRef', 'itemID',
];
function getDOMPropertyNames(context) {
  // this was removed in React v16.1+, see https://github.com/facebook/react/pull/10823
  if (!testReactVersion(context, '>= 16.1.0')) {
    return ['allowTransparency'].concat(DOM_PROPERTY_NAMES);
  }
  return DOM_PROPERTY_NAMES;
}

// ------------------------------------------------------------------------------
// Helpers
// ------------------------------------------------------------------------------

/**
 * Checks if a node matches the JSX tag convention. This also checks if a node
 * is extended as a webcomponent using the attribute "is".
 * @param {Object} node - JSX element being tested.
 * @returns {boolean} Whether or not the node name match the JSX tag convention.
 */
const tagConvention = /^[a-z][^-]*$/;
function isTagName(node) {
  if (tagConvention.test(node.parent.name.name)) {
    // https://www.w3.org/TR/custom-elements/#type-extension-semantics
    return !node.parent.attributes.some((attrNode) => (
      attrNode.type === 'JSXAttribute'
        && attrNode.name.type === 'JSXIdentifier'
        && attrNode.name.name === 'is'
    ));
  }
  return false;
}

/**
 * Extracts the tag name for the JSXAttribute
 * @param {JSXAttribute} node - JSXAttribute being tested.
 * @returns {String|null} tag name
 */
function getTagName(node) {
  if (node && node.parent && node.parent.name && node.parent.name) {
    return node.parent.name.name;
  }
  return null;
}

/**
 * Test wether the tag name for the JSXAttribute is
 * something like <Foo.bar />
 * @param {JSXAttribute} node - JSXAttribute being tested.
 * @returns {Boolean} result
 */
function tagNameHasDot(node) {
  return !!(
    node.parent
    && node.parent.name
    && node.parent.name.type === 'JSXMemberExpression'
  );
}

/**
 * Get the standard name of the attribute.
 * @param {String} name - Name of the attribute.
 * @param {String} context - eslint context
 * @returns {String | undefined} The standard name of the attribute, or undefined if no standard name was found.
 */
function getStandardName(name, context) {
  if (has(DOM_ATTRIBUTE_NAMES, name)) {
    return DOM_ATTRIBUTE_NAMES[name];
  }
  if (has(SVGDOM_ATTRIBUTE_NAMES, name)) {
    return SVGDOM_ATTRIBUTE_NAMES[name];
  }
  const names = getDOMPropertyNames(context);
  // Let's find a possible attribute match with a case-insensitive search.
  return names.find((element) => element.toLowerCase() === name.toLowerCase());
}

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

const messages = {
  invalidPropOnTag: 'Invalid property \'{{name}}\' found on tag \'{{tagName}}\', but it is only allowed on: {{allowedTags}}',
  unknownProp: 'Unknown property \'{{name}}\' found, use \'{{standardName}}\' instead',
};

module.exports = {
  meta: {
    docs: {
      description: 'Prevent usage of unknown DOM property',
      category: 'Possible Errors',
      recommended: true,
      url: docsUrl('no-unknown-property'),
    },
    fixable: 'code',

    messages,

    schema: [{
      type: 'object',
      properties: {
        ignore: {
          type: 'array',
          items: {
            type: 'string',
          },
        },
      },
      additionalProperties: false,
    }],
  },

  create(context) {
    function getIgnoreConfig() {
      return (context.options[0] && context.options[0].ignore) || DEFAULTS.ignore;
    }

    return {
      JSXAttribute(node) {
        const ignoreNames = getIgnoreConfig();
        const name = context.getSourceCode().getText(node.name);
        if (ignoreNames.indexOf(name) >= 0) {
          return;
        }

        // Ignore tags like <Foo.bar />
        if (tagNameHasDot(node)) {
          return;
        }

        const tagName = getTagName(node);

        // 1. Some attributes are allowed on some tags only.
        const allowedTags = has(ATTRIBUTE_TAGS_MAP, name) ? ATTRIBUTE_TAGS_MAP[name] : null;
        if (tagName && allowedTags && /[^A-Z]/.test(tagName.charAt(0)) && allowedTags.indexOf(tagName) === -1) {
          report(context, messages.invalidPropOnTag, 'invalidPropOnTag', {
            node,
            data: {
              name,
              tagName,
              allowedTags: allowedTags.join(', '),
            },
          });
        }

        // 2. Otherwise, we'll try to find if the attribute is a close version
        // of what we should normally have with React. If yes, we'll report an
        // error. We don't want to report if the input attribute name is the
        // standard name though!
        const standardName = getStandardName(name, context);
        if (!isTagName(node) || !standardName || standardName === name) {
          return;
        }
        report(context, messages.unknownProp, 'unknownProp', {
          node,
          data: {
            name,
            standardName,
          },
          fix(fixer) {
            return fixer.replaceText(node.name, standardName);
          },
        });
      },
    };
  },
};
