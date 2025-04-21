/**
 * @fileoverview Prevent direct mutation of this.state
 * @author David Petersen
 * @author Nicolas Fernandez <@burabure>
 */

'use strict';

const Components = require('../util/Components');
const componentUtil = require('../util/componentUtil');
const docsUrl = require('../util/docsUrl');
const report = require('../util/report');

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

const messages = {
  noDirectMutation: 'Do not mutate state directly. Use setState().',
};

module.exports = {
  meta: {
    docs: {
      description: 'Prevent direct mutation of this.state',
      category: 'Possible Errors',
      recommended: true,
      url: docsUrl('no-direct-mutation-state'),
    },

    messages,
  },

  create: Components.detect((context, components, utils) => {
    /**
     * Checks if the component is valid
     * @param {Object} component The component to process
     * @returns {Boolean} True if the component is valid, false if not.
     */
    function isValid(component) {
      return Boolean(component && !component.mutateSetState);
    }

    /**
     * Reports undeclared proptypes for a given component
     * @param {Object} component The component to process
     */
    function reportMutations(component) {
      let mutation;
      for (let i = 0, j = component.mutations.length; i < j; i++) {
        mutation = component.mutations[i];
        report(context, messages.noDirectMutation, 'noDirectMutation', {
          node: mutation,
        });
      }
    }

    /**
     * Walks throughs the MemberExpression to the top-most property.
     * @param {Object} node The node to process
     * @returns {Object} The outer-most MemberExpression
     */
    function getOuterMemberExpression(node) {
      while (node.object && node.object.property) {
        node = node.object;
      }
      return node;
    }

    /**
     * Determine if we should currently ignore assignments in this component.
     * @param {?Object} component The component to process
     * @returns {Boolean} True if we should skip assignment checks.
     */
    function shouldIgnoreComponent(component) {
      return !component || (component.inConstructor && !component.inCallExpression);
    }

    // --------------------------------------------------------------------------
    // Public
    // --------------------------------------------------------------------------
    return {
      MethodDefinition(node) {
        if (node.kind === 'constructor') {
          components.set(node, {
            inConstructor: true,
          });
        }
      },

      CallExpression(node) {
        components.set(node, {
          inCallExpression: true,
        });
      },

      AssignmentExpression(node) {
        const component = components.get(utils.getParentComponent());
        if (shouldIgnoreComponent(component) || !node.left || !node.left.object) {
          return;
        }
        const item = getOuterMemberExpression(node.left);
        if (componentUtil.isStateMemberExpression(item)) {
          const mutations = (component && component.mutations) || [];
          mutations.push(node.left.object);
          components.set(node, {
            mutateSetState: true,
            mutations,
          });
        }
      },

      UpdateExpression(node) {
        const component = components.get(utils.getParentComponent());
        if (shouldIgnoreComponent(component) || node.argument.type !== 'MemberExpression') {
          return;
        }
        const item = getOuterMemberExpression(node.argument);
        if (componentUtil.isStateMemberExpression(item)) {
          const mutations = (component && component.mutations) || [];
          mutations.push(item);
          components.set(node, {
            mutateSetState: true,
            mutations,
          });
        }
      },

      'CallExpression:exit'(node) {
        components.set(node, {
          inCallExpression: false,
        });
      },

      'MethodDefinition:exit'(node) {
        if (node.kind === 'constructor') {
          components.set(node, {
            inConstructor: false,
          });
        }
      },

      'Program:exit'() {
        const list = components.list();

        Object.keys(list).forEach((key) => {
          if (!isValid(list[key])) {
            reportMutations(list[key]);
          }
        });
      },
    };
  }),
};
