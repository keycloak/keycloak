/**
 * @fileoverview Prevent variables used in JSX to be marked as unused
 * @author Yannick Croissant
 */

'use strict';

const docsUrl = require('../util/docsUrl');

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

const isTagNameRe = /^[a-z]/;
const isTagName = (name) => isTagNameRe.test(name);

module.exports = {
  meta: {
    docs: {
      description: 'Prevent variables used in JSX to be marked as unused',
      category: 'Best Practices',
      recommended: true,
      url: docsUrl('jsx-uses-vars'),
    },
    schema: [],
  },

  create(context) {
    return {
      JSXOpeningElement(node) {
        let name;
        if (node.name.namespace) {
          // <Foo:Bar>
          return;
        }
        if (node.name.name) {
          // <Foo>
          name = node.name.name;
          // Exclude lowercase tag names like <div>
          if (isTagName(name)) {
            return;
          }
        } else if (node.name.object) {
          // <Foo...Bar>
          let parent = node.name.object;
          while (parent.object) {
            parent = parent.object;
          }
          name = parent.name;
        } else {
          return;
        }

        context.markVariableAsUsed(name);
      },

    };
  },
};
