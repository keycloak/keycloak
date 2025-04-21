/**
 * @fileoverview Prevent usage of setState
 * @author Mark Dalgleish
 */

'use strict';

const Components = require('../util/Components');
const docsUrl = require('../util/docsUrl');
const report = require('../util/report');

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

const messages = {
  noSetState: 'Do not use setState',
};

module.exports = {
  meta: {
    docs: {
      description: 'Prevent usage of setState',
      category: 'Stylistic Issues',
      recommended: false,
      url: docsUrl('no-set-state'),
    },

    messages,

    schema: [],
  },

  create: Components.detect((context, components, utils) => {
    /**
     * Checks if the component is valid
     * @param {Object} component The component to process
     * @returns {Boolean} True if the component is valid, false if not.
     */
    function isValid(component) {
      return Boolean(component && !component.useSetState);
    }

    /**
     * Reports usages of setState for a given component
     * @param {Object} component The component to process
     */
    function reportSetStateUsages(component) {
      let setStateUsage;
      for (let i = 0, j = component.setStateUsages.length; i < j; i++) {
        setStateUsage = component.setStateUsages[i];
        report(context, messages.noSetState, 'noSetState', {
          node: setStateUsage,
        });
      }
    }

    return {
      CallExpression(node) {
        const callee = node.callee;
        if (
          callee.type !== 'MemberExpression'
          || callee.object.type !== 'ThisExpression'
          || callee.property.name !== 'setState'
        ) {
          return;
        }
        const component = components.get(utils.getParentComponent());
        const setStateUsages = (component && component.setStateUsages) || [];
        setStateUsages.push(callee);
        components.set(node, {
          useSetState: true,
          setStateUsages,
        });
      },

      'Program:exit'() {
        const list = components.list();
        Object.keys(list).filter((component) => !isValid(list[component])).forEach((component) => {
          reportSetStateUsages(list[component]);
        });
      },
    };
  }),
};
