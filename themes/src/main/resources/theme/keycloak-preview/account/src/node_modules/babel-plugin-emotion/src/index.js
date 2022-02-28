// @flow
import fs from 'fs'
import nodePath from 'path'
import findRoot from 'find-root'
import mkdirp from 'mkdirp'
import { touchSync } from 'touch'
import { addSideEffect } from '@babel/helper-module-imports'
import {
  getIdentifierName,
  getName,
  createRawStringFromTemplateLiteral,
  getLabel,
  appendStringToExpressions
} from './babel-utils'
import type {
  Node,
  Identifier,
  BabelPluginPass,
  Types,
  Babel
} from 'babel-flow-types'
import hashString from '@emotion/hash'
import Stylis from '@emotion/stylis'
import memoize from '@emotion/memoize'
import { addSourceMaps } from './source-map'

import cssProps from './css-prop'
import { getExpressionsFromTemplateLiteral } from '@emotion/babel-utils'
import emotionMacro from './macro'
import styledMacro from './macro-styled'

export const macros = {
  emotion: emotionMacro,
  styled: styledMacro
}

export type BabelPath = any

export function hashArray(arr: Array<string>) {
  return hashString(arr.join(''))
}

const staticStylis = new Stylis({ keyframe: false })

export function hoistPureArgs(path: BabelPath) {
  const args = path.get('arguments')

  if (args && Array.isArray(args)) {
    args.forEach(arg => {
      if (!arg.isIdentifier() && arg.isPure()) {
        arg.hoist()
      }
    })
  }
}

type ImportedNames = {
  css: string,
  keyframes: string,
  injectGlobal: string,
  styled: string,
  merge: string
}

export type EmotionBabelPluginPass = BabelPluginPass & {
  extractStatic: boolean,
  insertStaticRules: (rules: Array<string>) => void,
  emotionImportPath: string,
  staticRules: Array<string>,
  cssPropIdentifiers: Array<Identifier>,
  importedNames: ImportedNames,
  count: number,
  opts: any
}

export function replaceCssWithCallExpression(
  path: BabelPath,
  identifier: Identifier,
  state: EmotionBabelPluginPass,
  t: Types,
  staticCSSSrcCreator: (
    src: string,
    name: string,
    hash: string
  ) => string = src => src,
  removePath: boolean = false,
  staticCSSSelectorCreator: (name: string, hash: string) => string = (
    name,
    hash
  ) => `.${name}-${hash}`
) {
  try {
    let { hash, src } = createRawStringFromTemplateLiteral(path.node.quasi)
    const identifierName = getIdentifierName(path, t)
    const name = getName(identifierName, 'css')

    if (state.extractStatic && !path.node.quasi.expressions.length) {
      const staticCSSRules = staticStylis(
        staticCSSSelectorCreator(name, hash),
        staticCSSSrcCreator(src, name, hash)
      )
      state.insertStaticRules([staticCSSRules])
      if (!removePath) {
        return path.replaceWith(t.stringLiteral(`${name}-${hash}`))
      }
      return path.replaceWith(t.identifier('undefined'))
    }

    if (!removePath) {
      path.addComment('leading', '#__PURE__')
    }

    let stringToAppend = ''
    if (state.opts.sourceMap === true && path.node.quasi.loc !== undefined) {
      stringToAppend += addSourceMaps(path.node.quasi.loc.start, state)
    }

    const label = getLabel(
      identifierName,
      state.opts.autoLabel,
      state.opts.labelFormat,
      state.file.opts.filename
    )

    if (label) {
      stringToAppend += `label:${label};`
    }

    path.replaceWith(
      t.callExpression(
        identifier,
        appendStringToExpressions(
          getExpressionsFromTemplateLiteral(path.node.quasi, t),
          stringToAppend,
          t
        )
      )
    )

    if (state.opts.hoist) {
      hoistPureArgs(path)
    }

    return
  } catch (e) {
    if (path) {
      throw path.buildCodeFrameError(e)
    }

    throw e
  }
}

const unsafeRequire = require

const getPackageRootPath = memoize(filename => findRoot(filename))

function buildTargetObjectProperty(path, state, t) {
  if (state.count === undefined) {
    state.count = 0
  }

  const filename = state.file.opts.filename

  // normalize the file path to ignore folder structure
  // outside the current node project and arch-specific delimiters
  let moduleName = ''
  let rootPath = filename

  try {
    rootPath = getPackageRootPath(filename)
    moduleName = unsafeRequire(rootPath + '/package.json').name
  } catch (err) {}

  const finalPath = filename === rootPath ? '' : filename.slice(rootPath.length)

  const positionInFile = state.count++

  const stuffToHash = [moduleName]

  if (finalPath) {
    stuffToHash.push(nodePath.normalize(finalPath))
  } else {
    stuffToHash.push(state.file.code)
  }

  const stableClassName = `e${hashArray(stuffToHash)}${positionInFile}`

  return t.objectProperty(
    t.identifier('target'),
    t.stringLiteral(stableClassName)
  )
}

const buildFinalOptions = (t, options, ...newProps) => {
  let existingProperties = []

  if (options && !t.isObjectExpression(options)) {
    console.warn(
      "Second argument to a styled call is not an object, it's going to be removed."
    )
  } else if (options) {
    // $FlowFixMe
    existingProperties = options.properties
  }

  return t.objectExpression([
    ...existingProperties,
    ...newProps.filter(Boolean)
  ])
}

export function buildStyledCallExpression(
  identifier: Identifier,
  args: Node[],
  path: BabelPath,
  state: EmotionBabelPluginPass,
  isCallExpression: boolean,
  t: Types
) {
  // unpacking "manually" to prevent array out of bounds access (deopt)
  const tag = args[0]
  const options = args.length >= 2 ? args[1] : null
  const restArgs = args.slice(2)

  const identifierName = getIdentifierName(path, t)

  const targetProperty = buildTargetObjectProperty(path, state, t)

  if (state.extractStatic && !path.node.quasi.expressions.length) {
    const { hash, src } = createRawStringFromTemplateLiteral(path.node.quasi)
    const staticClassName = `css-${hash}`
    const staticCSSRules = staticStylis(`.${staticClassName}`, src)

    state.insertStaticRules([staticCSSRules])

    const finalOptions = buildFinalOptions(
      t,
      options,
      t.objectProperty(t.identifier('e'), t.stringLiteral(staticClassName)),
      targetProperty
    )

    return t.callExpression(
      // $FlowFixMe
      t.callExpression(identifier, [tag, finalOptions, ...restArgs]),
      []
    )
  }

  path.addComment('leading', '#__PURE__')

  let stringToAppend = ''

  if (state.opts.sourceMap === true && path.node.quasi.loc !== undefined) {
    stringToAppend += addSourceMaps(path.node.quasi.loc.start, state)
  }

  let labelProperty

  const label = getLabel(
    identifierName,
    state.opts.autoLabel,
    state.opts.labelFormat,
    state.file.opts.filename
  )

  if (label) {
    labelProperty = t.objectProperty(
      t.identifier('label'),
      t.stringLiteral(label)
    )
  }

  const finalOptions = buildFinalOptions(
    t,
    options,
    labelProperty,
    targetProperty
  )

  let styledCall =
    t.isStringLiteral(tag) &&
    !isCallExpression &&
    // $FlowFixMe
    tag.value[0] !== tag.value[0].toLowerCase()
      ? // $FlowFixMe
        t.memberExpression(identifier, t.identifier(tag.value))
      : // $FlowFixMe
        t.callExpression(identifier, [tag, finalOptions, ...restArgs])

  return t.callExpression(
    styledCall,
    appendStringToExpressions(
      getExpressionsFromTemplateLiteral(path.node.quasi, t),
      stringToAppend,
      t
    )
  )
}

export function buildStyledObjectCallExpression(
  path: BabelPath,
  state: EmotionBabelPluginPass,
  identifier: Identifier,
  t: Types
) {
  const targetProperty = buildTargetObjectProperty(path, state, t)
  const identifierName = getIdentifierName(path, t)

  const tag = t.isCallExpression(path.node.callee)
    ? path.node.callee.arguments[0]
    : t.stringLiteral(path.node.callee.property.name)
  let isCallExpression = t.isCallExpression(path.node.callee)
  let styledOptions = null
  let restStyledArgs = []
  if (t.isCallExpression(path.node.callee)) {
    const styledArgs = path.node.callee.arguments

    if (styledArgs.length >= 2) {
      styledOptions = styledArgs[1]
    }

    restStyledArgs = styledArgs.slice(2)
  }

  let args = path.node.arguments
  if (state.opts.sourceMap === true && path.node.loc !== undefined) {
    args.push(t.stringLiteral(addSourceMaps(path.node.loc.start, state)))
  }

  const label = getLabel(
    identifierName,
    state.opts.autoLabel,
    state.opts.labelFormat,
    state.file.opts.filename
  )

  const labelProperty = label
    ? t.objectProperty(t.identifier('label'), t.stringLiteral(label))
    : null

  path.addComment('leading', '#__PURE__')

  let styledCall =
    t.isStringLiteral(tag) &&
    !isCallExpression &&
    tag.value[0] !== tag.value[0].toLowerCase()
      ? t.memberExpression(identifier, t.identifier(tag.value))
      : t.callExpression(identifier, [
          tag,
          buildFinalOptions(t, styledOptions, targetProperty, labelProperty),
          ...restStyledArgs
        ])

  return t.callExpression(styledCall, args)
}

const visited = Symbol('visited')

const defaultImportedNames: ImportedNames = {
  styled: 'styled',
  css: 'css',
  keyframes: 'keyframes',
  injectGlobal: 'injectGlobal',
  merge: 'merge'
}

const importedNameKeys = Object.keys(defaultImportedNames).map(
  key => (key === 'styled' ? 'default' : key)
)

const defaultEmotionPaths = [
  'emotion',
  'react-emotion',
  'preact-emotion',
  '@emotion/primitives'
]

function getRelativePath(filepath: string, absoluteInstancePath: string) {
  let relativePath = nodePath.relative(
    nodePath.dirname(filepath),
    absoluteInstancePath
  )

  return relativePath.charAt(0) === '.' ? relativePath : `./${relativePath}`
}

function getAbsolutePath(instancePath: string, rootPath: string) {
  if (instancePath.charAt(0) === '.') {
    let absoluteInstancePath = nodePath.resolve(rootPath, instancePath)
    return absoluteInstancePath
  }
  return false
}

function getInstancePathToImport(instancePath: string, filepath: string) {
  let absolutePath = getAbsolutePath(instancePath, process.cwd())
  if (absolutePath === false) {
    return instancePath
  }
  return getRelativePath(filepath, absolutePath)
}

function getInstancePathToCompare(instancePath: string, rootPath: string) {
  let absolutePath = getAbsolutePath(instancePath, rootPath)
  if (absolutePath === false) {
    return instancePath
  }
  return absolutePath
}

let warnedAboutExtractStatic = false

export default function(babel: Babel) {
  const { types: t } = babel

  return {
    name: 'emotion', // not required
    inherits: require('babel-plugin-syntax-jsx'),
    visitor: {
      Program: {
        enter(path: BabelPath, state: EmotionBabelPluginPass) {
          const hasFilepath =
            path.hub.file.opts.filename &&
            path.hub.file.opts.filename !== 'unknown'
          state.emotionImportPath = 'emotion'
          if (state.opts.primaryInstance !== undefined) {
            state.emotionImportPath = getInstancePathToImport(
              state.opts.primaryInstance,
              path.hub.file.opts.filename
            )
          }

          state.importedNames = {
            ...defaultImportedNames,
            ...state.opts.importedNames
          }

          const imports = []

          let isModule = false

          for (const node of path.node.body) {
            if (t.isModuleDeclaration(node)) {
              isModule = true
              break
            }
          }

          if (isModule) {
            path.traverse({
              ImportDeclaration: {
                exit(path) {
                  const { node } = path

                  const imported = []
                  const specifiers = []

                  imports.push({
                    source: node.source.value,
                    imported,
                    specifiers
                  })

                  for (const specifier of path.get('specifiers')) {
                    const local = specifier.node.local.name

                    if (specifier.isImportDefaultSpecifier()) {
                      imported.push('default')
                      specifiers.push({
                        kind: 'named',
                        imported: 'default',
                        local
                      })
                    }

                    if (specifier.isImportSpecifier()) {
                      const importedName = specifier.node.imported.name
                      imported.push(importedName)
                      specifiers.push({
                        kind: 'named',
                        imported: importedName,
                        local
                      })
                    }
                  }
                }
              }
            })
          }
          const emotionPaths = defaultEmotionPaths.concat(
            (state.opts.instances || []).map(instancePath =>
              getInstancePathToCompare(instancePath, process.cwd())
            )
          )
          let dirname = hasFilepath
            ? nodePath.dirname(path.hub.file.opts.filename)
            : ''
          imports.forEach(({ source, imported, specifiers }) => {
            if (
              emotionPaths.indexOf(
                getInstancePathToCompare(source, dirname)
              ) !== -1
            ) {
              const importedNames = specifiers
                .filter(v => importedNameKeys.indexOf(v.imported) !== -1)
                .reduce(
                  (acc, { imported, local }) => ({
                    ...acc,
                    [imported === 'default' ? 'styled' : imported]: local
                  }),
                  defaultImportedNames
                )
              state.importedNames = {
                ...importedNames,
                ...state.opts.importedNames
              }
            }
          })
          state.cssPropIdentifiers = []
          if (state.opts.extractStatic && !warnedAboutExtractStatic) {
            console.warn(
              'extractStatic is deprecated and will be removed in emotion@10. We recommend disabling extractStatic or using other libraries like linaria or css-literal-loader'
            )
            // lots of cli tools write to the same line so
            // this moves to the next line so the warning doesn't get removed
            console.log('')
            warnedAboutExtractStatic = true
          }
          state.extractStatic =
            // path.hub.file.opts.filename !== 'unknown' ||
            state.opts.extractStatic

          state.staticRules = []

          state.insertStaticRules = function(staticRules) {
            state.staticRules.push(...staticRules)
          }
        },
        exit(path: BabelPath, state: EmotionBabelPluginPass) {
          if (state.staticRules.length !== 0) {
            const toWrite = state.staticRules.join('\n').trim()
            let cssFilename = path.hub.file.opts.generatorOpts
              ? path.hub.file.opts.generatorOpts.sourceFileName
              : path.hub.file.opts.sourceFileName
            let cssFileOnDisk
            let importPath

            const cssFilenameArr = cssFilename.split('.')
            // remove the extension
            cssFilenameArr.pop()
            // add emotion.css as an extension
            cssFilenameArr.push('emotion.css')

            cssFilename = cssFilenameArr.join('.')

            if (state.opts.outputDir) {
              const relativeToSourceDir = nodePath.relative(
                nodePath.dirname(cssFilename),
                state.opts.outputDir
              )
              importPath = nodePath.join(relativeToSourceDir, cssFilename)
              cssFileOnDisk = nodePath.resolve(cssFilename, '..', importPath)
            } else {
              importPath = `./${nodePath.basename(cssFilename)}`
              cssFileOnDisk = nodePath.resolve(cssFilename)
            }

            const exists = fs.existsSync(cssFileOnDisk)
            addSideEffect(path, importPath)
            if (
              exists ? fs.readFileSync(cssFileOnDisk, 'utf8') !== toWrite : true
            ) {
              if (!exists) {
                if (state.opts.outputDir) {
                  mkdirp.sync(nodePath.dirname(cssFileOnDisk))
                }

                touchSync(cssFileOnDisk)
              }
              fs.writeFileSync(cssFileOnDisk, toWrite)
            }
          }
        }
      },
      JSXOpeningElement(path: BabelPath, state: EmotionBabelPluginPass) {
        cssProps(path, state, t)
        if (state.opts.hoist) {
          path.traverse({
            CallExpression(callExprPath) {
              if (
                callExprPath.node.callee.name === state.importedNames.css ||
                state.cssPropIdentifiers.indexOf(callExprPath.node.callee) !==
                  -1
              ) {
                hoistPureArgs(callExprPath)
              }
            }
          })
        }
      },
      CallExpression: {
        enter(path: BabelPath, state: EmotionBabelPluginPass) {
          // $FlowFixMe
          if (path[visited]) {
            return
          }
          try {
            if (t.isIdentifier(path.node.callee)) {
              switch (path.node.callee.name) {
                case state.importedNames.css:
                case state.importedNames.keyframes: {
                  path.addComment('leading', '#__PURE__')
                  const label = getLabel(
                    getIdentifierName(path, t),
                    state.opts.autoLabel,
                    state.opts.labelFormat,
                    state.file.opts.filename
                  )
                  if (label) {
                    path.node.arguments.push(t.stringLiteral(`label:${label};`))
                  }
                }
                // eslint-disable-next-line no-fallthrough
                case state.importedNames.injectGlobal:
                  if (
                    state.opts.sourceMap === true &&
                    path.node.loc !== undefined
                  ) {
                    path.node.arguments.push(
                      t.stringLiteral(addSourceMaps(path.node.loc.start, state))
                    )
                  }
              }
            }

            if (
              (t.isCallExpression(path.node.callee) &&
                path.node.callee.callee.name === state.importedNames.styled) ||
              (t.isMemberExpression(path.node.callee) &&
                t.isIdentifier(path.node.callee.object) &&
                path.node.callee.object.name === state.importedNames.styled)
            ) {
              const identifier = t.isCallExpression(path.node.callee)
                ? path.node.callee.callee
                : path.node.callee.object
              path.replaceWith(
                buildStyledObjectCallExpression(path, state, identifier, t)
              )

              if (state.opts.hoist) {
                hoistPureArgs(path)
              }
            }
          } catch (e) {
            throw path.buildCodeFrameError(e)
          }
          // $FlowFixMe
          path[visited] = true
        },
        exit(path: BabelPath, state: EmotionBabelPluginPass) {
          try {
            if (
              path.node.callee &&
              path.node.callee.property &&
              path.node.callee.property.name === 'withComponent'
            ) {
              if (path.node.arguments.length === 1) {
                path.node.arguments.push(
                  t.objectExpression([
                    buildTargetObjectProperty(path, state, t)
                  ])
                )
              }
            }
          } catch (e) {
            throw path.buildCodeFrameError(e)
          }
        }
      },
      TaggedTemplateExpression(path: BabelPath, state: EmotionBabelPluginPass) {
        // $FlowFixMe
        if (path[visited]) {
          return
        }
        // $FlowFixMe
        path[visited] = true
        if (
          // styled.h1`color:${color};`
          t.isMemberExpression(path.node.tag) &&
          path.node.tag.object.name === state.importedNames.styled
        ) {
          path.replaceWith(
            buildStyledCallExpression(
              path.node.tag.object,
              [t.stringLiteral(path.node.tag.property.name)],
              path,
              state,
              false,
              t
            )
          )
        } else if (
          // styled('h1')`color:${color};`
          t.isCallExpression(path.node.tag) &&
          path.node.tag.callee.name === state.importedNames.styled
        ) {
          path.replaceWith(
            buildStyledCallExpression(
              path.node.tag.callee,
              path.node.tag.arguments,
              path,
              state,
              true,
              t
            )
          )
        } else if (t.isIdentifier(path.node.tag)) {
          if (
            path.node.tag.name === state.importedNames.css ||
            state.cssPropIdentifiers.indexOf(path.node.tag) !== -1
          ) {
            replaceCssWithCallExpression(path, path.node.tag, state, t)
          } else if (path.node.tag.name === state.importedNames.keyframes) {
            replaceCssWithCallExpression(
              path,
              path.node.tag,
              state,
              t,
              (src, name, hash) => `@keyframes ${name}-${hash} { ${src} }`,
              false,
              () => ''
            )
          } else if (path.node.tag.name === state.importedNames.injectGlobal) {
            replaceCssWithCallExpression(
              path,
              path.node.tag,
              state,
              t,
              undefined,
              true,
              () => ''
            )
          }
        }
      }
    }
  }
}
