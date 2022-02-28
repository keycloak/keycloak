// @flow
import { addNamed } from '@babel/helper-module-imports'
import { addSourceMaps } from './source-map'
import type { BabelPath, EmotionBabelPluginPass } from './index'
import type { Types } from 'babel-flow-types'

export default function(
  path: BabelPath,
  state: EmotionBabelPluginPass,
  t: Types
) {
  let cssPath
  let classNamesPath

  path.get('attributes').forEach(openElPath => {
    if (t.isJSXSpreadAttribute(openElPath.node)) {
      return
    }

    const attrPath = openElPath.get('name')
    const name = attrPath.node.name

    if (name === state.importedNames.css) {
      cssPath = attrPath
    }

    if (name === 'className') {
      classNamesPath = attrPath
    }
  })

  if (!cssPath) return

  let cssPropValue = cssPath.container && cssPath.container.value
  let classNamesValue =
    classNamesPath && classNamesPath.container && classNamesPath.container.value

  if (t.isJSXExpressionContainer(cssPropValue)) {
    cssPropValue = cssPropValue.expression
  }

  let cssTemplateExpression
  if (t.isTemplateLiteral(cssPropValue)) {
    cssTemplateExpression = createCssTemplateExpression(cssPropValue)
  } else if (t.isStringLiteral(cssPropValue)) {
    cssTemplateExpression = createCssTemplateExpression(
      t.templateLiteral(
        [
          t.templateElement({
            raw: cssPropValue.value,
            cooked: cssPropValue.value
          })
        ],
        []
      )
    )
  } else {
    const args = state.opts.sourceMap
      ? [
          cssPropValue,
          t.stringLiteral(addSourceMaps(cssPath.node.loc.start, state))
        ]
      : [cssPropValue]
    cssTemplateExpression = t.callExpression(getCssIdentifer(), args)
  }
  if (
    !classNamesValue ||
    (t.isStringLiteral(classNamesValue) && !classNamesValue.value)
  ) {
    if (classNamesPath) classNamesPath.parentPath.remove()
    cssPath.parentPath.replaceWith(createClassNameAttr(cssTemplateExpression))
    return
  }

  cssPath.parentPath.remove()
  if (classNamesPath && classNamesPath.parentPath) {
    if (t.isJSXExpressionContainer(classNamesValue)) {
      const args = [
        add(
          cssTemplateExpression,
          add(t.stringLiteral(' '), classNamesValue.expression)
        )
      ]

      if (state.opts.sourceMap) {
        args.push(t.stringLiteral(addSourceMaps(cssPath.node.loc.start, state)))
      }

      classNamesPath.parentPath.replaceWith(
        createClassNameAttr(t.callExpression(getMergeIdentifier(), args))
      )
    } else {
      classNamesPath.parentPath.replaceWith(
        createClassNameAttr(
          add(
            cssTemplateExpression,
            t.stringLiteral(` ${classNamesValue.value || ''}`)
          )
        )
      )
    }
  }

  function add(a, b) {
    return t.binaryExpression('+', a, b)
  }

  function createClassNameAttr(expression) {
    return t.jSXAttribute(
      t.jSXIdentifier('className'),
      t.jSXExpressionContainer(expression)
    )
  }

  function getCssIdentifer() {
    if (state.opts.autoImportCssProp !== false) {
      const cssImport = addNamed(path, 'css', state.emotionImportPath)
      state.cssPropIdentifiers.push(cssImport)
      return cssImport
    } else {
      return t.identifier(state.importedNames.css)
    }
  }
  function getMergeIdentifier() {
    if (state.opts.autoImportCssProp !== false) {
      return addNamed(path, 'merge', state.emotionImportPath)
    } else {
      return t.identifier(state.importedNames.merge)
    }
  }
  function createCssTemplateExpression(templateLiteral) {
    return t.taggedTemplateExpression(getCssIdentifer(), templateLiteral)
  }
}
