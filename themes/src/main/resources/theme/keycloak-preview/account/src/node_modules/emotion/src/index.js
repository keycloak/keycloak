// @flow
import createEmotion from 'create-emotion'

const context = typeof global !== 'undefined' ? global : {}

export const {
  flush,
  hydrate,
  cx,
  merge,
  getRegisteredStyles,
  injectGlobal,
  keyframes,
  css,
  sheet,
  caches
} = createEmotion(context)
