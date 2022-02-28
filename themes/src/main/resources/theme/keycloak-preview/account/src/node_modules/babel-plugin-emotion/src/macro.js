// @flow
import { replaceCssWithCallExpression } from './index'
import { buildMacroRuntimeNode, addRuntimeImports } from './babel-utils'
import { createMacro } from 'babel-plugin-macros'

export default createMacro(macro)

function macro({ references, state, babel: { types: t } }) {
  Object.keys(references).forEach(referenceKey => {
    let isPure = true
    switch (referenceKey) {
      case 'injectGlobal': {
        isPure = false
      }
      // eslint-disable-next-line no-fallthrough
      case 'css':
      case 'keyframes': {
        references[referenceKey].reverse().forEach(reference => {
          const path = reference.parentPath
          const runtimeNode = buildMacroRuntimeNode(
            reference,
            state,
            referenceKey,
            t
          )
          if (t.isTaggedTemplateExpression(path)) {
            replaceCssWithCallExpression(
              path,
              runtimeNode,
              state,
              t,
              undefined,
              !isPure
            )
          } else {
            if (isPure) {
              path.addComment('leading', '#__PURE__')
            }
            reference.replaceWith(runtimeNode)
          }
        })
        break
      }
      default: {
        references[referenceKey].reverse().forEach(reference => {
          reference.replaceWith(
            buildMacroRuntimeNode(reference, state, referenceKey, t)
          )
        })
      }
    }
  })
  addRuntimeImports(state, t)
}
