const ruleBoilerplate = (author, description) => `/**
 * @fileoverview ${description}
 * @author ${author}
 * @flow
 */

// ----------------------------------------------------------------------------
// Rule Definition
// ----------------------------------------------------------------------------

import type { JSXOpeningElement } from 'ast-types-flow';
import { generateObjSchema } from '../util/schemas';

const errorMessage = '';

const schema = generateObjSchema();

export default {
  meta: {
    docs: {},
    schema: [schema],
  },

  create: (context: ESLintContext) => ({
    JSXOpeningElement: (node: JSXOpeningElement) => {
      context.report({
        node,
        message: errorMessage,
      });
    },
  }),
};
`;

module.exports = ruleBoilerplate;
