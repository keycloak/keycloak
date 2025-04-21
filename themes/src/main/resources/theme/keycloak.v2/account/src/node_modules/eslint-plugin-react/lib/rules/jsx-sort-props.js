/**
 * @fileoverview Enforce props alphabetical sorting
 * @author Ilya Volodin, Yannick Croissant
 */

'use strict';

const propName = require('jsx-ast-utils/propName');
const includes = require('array-includes');
const docsUrl = require('../util/docsUrl');
const jsxUtil = require('../util/jsx');
const report = require('../util/report');

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

function isCallbackPropName(name) {
  return /^on[A-Z]/.test(name);
}

function isMultilineProp(node) {
  return node.loc.start.line !== node.loc.end.line;
}

const messages = {
  noUnreservedProps: 'A customized reserved first list must only contain a subset of React reserved props. Remove: {{unreservedWords}}',
  listIsEmpty: 'A customized reserved first list must not be empty',
  listReservedPropsFirst: 'Reserved props must be listed before all other props',
  listCallbacksLast: 'Callbacks must be listed after all other props',
  listShorthandFirst: 'Shorthand props must be listed before all other props',
  listShorthandLast: 'Shorthand props must be listed after all other props',
  listMultilineFirst: 'Multiline props must be listed before all other props',
  listMultilineLast: 'Multiline props must be listed after all other props',
  sortPropsByAlpha: 'Props should be sorted alphabetically',
};

const RESERVED_PROPS_LIST = [
  'children',
  'dangerouslySetInnerHTML',
  'key',
  'ref',
];

function isReservedPropName(name, list) {
  return list.indexOf(name) >= 0;
}

function contextCompare(a, b, options) {
  let aProp = propName(a);
  let bProp = propName(b);

  if (options.reservedFirst) {
    const aIsReserved = isReservedPropName(aProp, options.reservedList);
    const bIsReserved = isReservedPropName(bProp, options.reservedList);
    if (aIsReserved && !bIsReserved) {
      return -1;
    }
    if (!aIsReserved && bIsReserved) {
      return 1;
    }
  }

  if (options.callbacksLast) {
    const aIsCallback = isCallbackPropName(aProp);
    const bIsCallback = isCallbackPropName(bProp);
    if (aIsCallback && !bIsCallback) {
      return 1;
    }
    if (!aIsCallback && bIsCallback) {
      return -1;
    }
  }

  if (options.shorthandFirst || options.shorthandLast) {
    const shorthandSign = options.shorthandFirst ? -1 : 1;
    if (!a.value && b.value) {
      return shorthandSign;
    }
    if (a.value && !b.value) {
      return -shorthandSign;
    }
  }

  if (options.multiline !== 'ignore') {
    const multilineSign = options.multiline === 'first' ? -1 : 1;
    const aIsMultiline = isMultilineProp(a);
    const bIsMultiline = isMultilineProp(b);
    if (aIsMultiline && !bIsMultiline) {
      return multilineSign;
    }
    if (!aIsMultiline && bIsMultiline) {
      return -multilineSign;
    }
  }

  if (options.noSortAlphabetically) {
    return 0;
  }

  const actualLocale = options.locale === 'auto' ? undefined : options.locale;

  if (options.ignoreCase) {
    aProp = aProp.toLowerCase();
    bProp = bProp.toLowerCase();
    return aProp.localeCompare(bProp, actualLocale);
  }
  if (aProp === bProp) {
    return 0;
  }
  if (options.locale === 'auto') {
    return aProp < bProp ? -1 : 1;
  }
  return aProp.localeCompare(bProp, actualLocale);
}

/**
 * Create an array of arrays where each subarray is composed of attributes
 * that are considered sortable.
 * @param {Array<JSXSpreadAttribute|JSXAttribute>} attributes
 * @return {Array<Array<JSXAttribute>>}
 */
function getGroupsOfSortableAttributes(attributes) {
  const sortableAttributeGroups = [];
  let groupCount = 0;
  for (let i = 0; i < attributes.length; i++) {
    const lastAttr = attributes[i - 1];
    // If we have no groups or if the last attribute was JSXSpreadAttribute
    // then we start a new group. Append attributes to the group until we
    // come across another JSXSpreadAttribute or exhaust the array.
    if (
      !lastAttr
      || (lastAttr.type === 'JSXSpreadAttribute'
        && attributes[i].type !== 'JSXSpreadAttribute')
    ) {
      groupCount += 1;
      sortableAttributeGroups[groupCount - 1] = [];
    }
    if (attributes[i].type !== 'JSXSpreadAttribute') {
      sortableAttributeGroups[groupCount - 1].push(attributes[i]);
    }
  }
  return sortableAttributeGroups;
}

const generateFixerFunction = (node, context, reservedList) => {
  const sourceCode = context.getSourceCode();
  const attributes = node.attributes.slice(0);
  const configuration = context.options[0] || {};
  const ignoreCase = configuration.ignoreCase || false;
  const callbacksLast = configuration.callbacksLast || false;
  const shorthandFirst = configuration.shorthandFirst || false;
  const shorthandLast = configuration.shorthandLast || false;
  const multiline = configuration.multiline || 'ignore';
  const noSortAlphabetically = configuration.noSortAlphabetically || false;
  const reservedFirst = configuration.reservedFirst || false;
  const locale = configuration.locale || 'auto';

  // Sort props according to the context. Only supports ignoreCase.
  // Since we cannot safely move JSXSpreadAttribute (due to potential variable overrides),
  // we only consider groups of sortable attributes.
  const options = {
    ignoreCase,
    callbacksLast,
    shorthandFirst,
    shorthandLast,
    multiline,
    noSortAlphabetically,
    reservedFirst,
    reservedList,
    locale,
  };
  const sortableAttributeGroups = getGroupsOfSortableAttributes(attributes);
  const sortedAttributeGroups = sortableAttributeGroups
    .slice(0)
    .map((group) => group.slice(0).sort((a, b) => contextCompare(a, b, options)));

  return function fixFunction(fixer) {
    const fixers = [];
    let source = sourceCode.getText();

    // Replace each unsorted attribute with the sorted one.
    sortableAttributeGroups.forEach((sortableGroup, ii) => {
      sortableGroup.forEach((attr, jj) => {
        const sortedAttr = sortedAttributeGroups[ii][jj];
        const sortedAttrText = sourceCode.getText(sortedAttr);
        fixers.push({
          range: [attr.range[0], attr.range[1]],
          text: sortedAttrText,
        });
      });
    });

    fixers.sort((a, b) => b.range[0] - a.range[0]);

    const rangeStart = fixers[fixers.length - 1].range[0];
    const rangeEnd = fixers[0].range[1];

    fixers.forEach((fix) => {
      source = `${source.substr(0, fix.range[0])}${fix.text}${source.substr(fix.range[1])}`;
    });

    return fixer.replaceTextRange([rangeStart, rangeEnd], source.substr(rangeStart, rangeEnd - rangeStart));
  };
};

/**
 * Checks if the `reservedFirst` option is valid
 * @param {Object} context The context of the rule
 * @param {Boolean|Array<String>} reservedFirst The `reservedFirst` option
 * @return {Function|undefined} If an error is detected, a function to generate the error message, otherwise, `undefined`
 */
// eslint-disable-next-line consistent-return
function validateReservedFirstConfig(context, reservedFirst) {
  if (reservedFirst) {
    if (Array.isArray(reservedFirst)) {
      // Only allow a subset of reserved words in customized lists
      const nonReservedWords = reservedFirst.filter((word) => !isReservedPropName(
        word,
        RESERVED_PROPS_LIST
      ));

      if (reservedFirst.length === 0) {
        return function Report(decl) {
          report(context, messages.listIsEmpty, 'listIsEmpty', {
            node: decl,
          });
        };
      }
      if (nonReservedWords.length > 0) {
        return function Report(decl) {
          report(context, messages.noUnreservedProps, 'noUnreservedProps', {
            node: decl,
            data: {
              unreservedWords: nonReservedWords.toString(),
            },
          });
        };
      }
    }
  }
}

const reportedNodeAttributes = new WeakMap();
/**
 * Check if the current node attribute has already been reported with the same error type
 * if that's the case then we don't report a new error
 * otherwise we report the error
 * @param {Object} nodeAttribute The node attribute to be reported
 * @param {string} errorType The error type to be reported
 * @param {Object} node The parent node for the node attribute
 * @param {Object} context The context of the rule
 * @param {Array<String>} reservedList The list of reserved props
 */
function reportNodeAttribute(nodeAttribute, errorType, node, context, reservedList) {
  const errors = reportedNodeAttributes.get(nodeAttribute) || [];

  if (includes(errors, errorType)) {
    return;
  }

  errors.push(errorType);

  reportedNodeAttributes.set(nodeAttribute, errors);

  report(context, messages[errorType], errorType, {
    node: nodeAttribute.name,
    fix: generateFixerFunction(node, context, reservedList),
  });
}

module.exports = {
  meta: {
    docs: {
      description: 'Enforce props alphabetical sorting',
      category: 'Stylistic Issues',
      recommended: false,
      url: docsUrl('jsx-sort-props'),
    },
    fixable: 'code',

    messages,

    schema: [{
      type: 'object',
      properties: {
        // Whether callbacks (prefixed with "on") should be listed at the very end,
        // after all other props. Supersedes shorthandLast.
        callbacksLast: {
          type: 'boolean',
        },
        // Whether shorthand properties (without a value) should be listed first
        shorthandFirst: {
          type: 'boolean',
        },
        // Whether shorthand properties (without a value) should be listed last
        shorthandLast: {
          type: 'boolean',
        },
        // Whether multiline properties should be listed first or last
        multiline: {
          enum: ['ignore', 'first', 'last'],
          default: 'ignore',
        },
        ignoreCase: {
          type: 'boolean',
        },
        // Whether alphabetical sorting should be enforced
        noSortAlphabetically: {
          type: 'boolean',
        },
        reservedFirst: {
          type: ['array', 'boolean'],
        },
        locale: {
          type: 'string',
          default: 'auto',
        },
      },
      additionalProperties: false,
    }],
  },

  create(context) {
    const configuration = context.options[0] || {};
    const ignoreCase = configuration.ignoreCase || false;
    const callbacksLast = configuration.callbacksLast || false;
    const shorthandFirst = configuration.shorthandFirst || false;
    const shorthandLast = configuration.shorthandLast || false;
    const multiline = configuration.multiline || 'ignore';
    const noSortAlphabetically = configuration.noSortAlphabetically || false;
    const reservedFirst = configuration.reservedFirst || false;
    const reservedFirstError = validateReservedFirstConfig(context, reservedFirst);
    let reservedList = Array.isArray(reservedFirst) ? reservedFirst : RESERVED_PROPS_LIST;
    const locale = configuration.locale || 'auto';

    return {
      JSXOpeningElement(node) {
        // `dangerouslySetInnerHTML` is only "reserved" on DOM components
        if (reservedFirst && !jsxUtil.isDOMComponent(node)) {
          reservedList = reservedList.filter((prop) => prop !== 'dangerouslySetInnerHTML');
        }

        node.attributes.reduce((memo, decl, idx, attrs) => {
          if (decl.type === 'JSXSpreadAttribute') {
            return attrs[idx + 1];
          }

          let previousPropName = propName(memo);
          let currentPropName = propName(decl);
          const previousValue = memo.value;
          const currentValue = decl.value;
          const previousIsCallback = isCallbackPropName(previousPropName);
          const currentIsCallback = isCallbackPropName(currentPropName);
          const previousIsMultiline = isMultilineProp(memo);
          const currentIsMultiline = isMultilineProp(decl);

          if (ignoreCase) {
            previousPropName = previousPropName.toLowerCase();
            currentPropName = currentPropName.toLowerCase();
          }

          if (reservedFirst) {
            if (reservedFirstError) {
              reservedFirstError(decl);
              return memo;
            }

            const previousIsReserved = isReservedPropName(previousPropName, reservedList);
            const currentIsReserved = isReservedPropName(currentPropName, reservedList);

            if (previousIsReserved && !currentIsReserved) {
              return decl;
            }
            if (!previousIsReserved && currentIsReserved) {
              reportNodeAttribute(decl, 'listReservedPropsFirst', node, context, reservedList);

              return memo;
            }
          }

          if (callbacksLast) {
            if (!previousIsCallback && currentIsCallback) {
              // Entering the callback prop section
              return decl;
            }
            if (previousIsCallback && !currentIsCallback) {
              // Encountered a non-callback prop after a callback prop
              reportNodeAttribute(memo, 'listCallbacksLast', node, context, reservedList);

              return memo;
            }
          }

          if (shorthandFirst) {
            if (currentValue && !previousValue) {
              return decl;
            }
            if (!currentValue && previousValue) {
              reportNodeAttribute(decl, 'listShorthandFirst', node, context, reservedList);

              return memo;
            }
          }

          if (shorthandLast) {
            if (!currentValue && previousValue) {
              return decl;
            }
            if (currentValue && !previousValue) {
              reportNodeAttribute(memo, 'listShorthandLast', node, context, reservedList);

              return memo;
            }
          }

          if (multiline === 'first') {
            if (previousIsMultiline && !currentIsMultiline) {
              // Exiting the multiline prop section
              return decl;
            }
            if (!previousIsMultiline && currentIsMultiline) {
              // Encountered a non-multiline prop before a multiline prop
              reportNodeAttribute(decl, 'listMultilineFirst', node, context, reservedList);

              return memo;
            }
          }

          if (multiline === 'last') {
            if (!previousIsMultiline && currentIsMultiline) {
              // Entering the multiline prop section
              return decl;
            }
            if (previousIsMultiline && !currentIsMultiline) {
              // Encountered a non-multiline prop after a multiline prop
              reportNodeAttribute(memo, 'listMultilineLast', node, context, reservedList);

              return memo;
            }
          }

          if (
            !noSortAlphabetically
            && (
              (ignoreCase || locale !== 'auto')
                ? previousPropName.localeCompare(currentPropName, locale === 'auto' ? undefined : locale) > 0
                : previousPropName > currentPropName
            )
          ) {
            reportNodeAttribute(decl, 'sortPropsByAlpha', node, context, reservedList);

            return memo;
          }

          return decl;
        }, node.attributes[0]);
      },
    };
  },
};
