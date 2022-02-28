// @flow
import {
  buildStyledCallExpression,
  buildStyledObjectCallExpression
} from './index'
import { buildMacroRuntimeNode, omit } from './babel-utils'
import emotionMacro from './macro'
import { createMacro } from 'babel-plugin-macros'

export default createMacro(macro)

function macro(options) {
  const {
    references,
    state,
    babel: { types: t }
  } = options
  let referencesWithoutDefault = references
  if (references.default) {
    referencesWithoutDefault = omit(references, key => key !== 'default')
    references.default.reverse().forEach(styledReference => {
      const path = styledReference.parentPath.parentPath
      const runtimeNode = buildMacroRuntimeNode(
        styledReference,
        state,
        'default',
        t
      )
      if (t.isTemplateLiteral(path.node.quasi)) {
        if (t.isMemberExpression(path.node.tag)) {
          path.replaceWith(
            buildStyledCallExpression(
              runtimeNode,
              [t.stringLiteral(path.node.tag.property.name)],
              path,
              state,
              false,
              t
            )
          )
        } else if (t.isCallExpression(path.node.tag)) {
          path.replaceWith(
            buildStyledCallExpression(
              runtimeNode,
              path.node.tag.arguments,
              path,
              state,
              true,
              t
            )
          )
        }
      } else if (
        t.isCallExpression(path) &&
        (t.isCallExpression(path.node.callee) ||
          t.isIdentifier(path.node.callee.object))
      ) {
        path.replaceWith(
          buildStyledObjectCallExpression(path, state, runtimeNode, t)
        )
      }
    })
  }
  emotionMacro({ ...options, references: referencesWithoutDefault })
}
