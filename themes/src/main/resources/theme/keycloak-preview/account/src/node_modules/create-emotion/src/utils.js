// @flow
import memoize from '@emotion/memoize'
import unitless from '@emotion/unitless'

const hyphenateRegex = /[A-Z]|^ms/g

export const processStyleName: (styleName: string) => string = memoize(
  (styleName: string) => styleName.replace(hyphenateRegex, '-$&').toLowerCase()
)

export let processStyleValue = (key: string, value: string): string => {
  if (value == null || typeof value === 'boolean') {
    return ''
  }

  if (
    unitless[key] !== 1 &&
    key.charCodeAt(1) !== 45 && // custom properties
    !isNaN(value) &&
    value !== 0
  ) {
    return value + 'px'
  }
  return value
}

if (process.env.NODE_ENV !== 'production') {
  let contentValuePattern = /(attr|calc|counters?|url)\(/
  let contentValues = [
    'normal',
    'none',
    'counter',
    'open-quote',
    'close-quote',
    'no-open-quote',
    'no-close-quote',
    'initial',
    'inherit',
    'unset'
  ]
  let oldProcessStyleValue = processStyleValue
  processStyleValue = (key: string, value: string) => {
    if (key === 'content') {
      if (
        typeof value !== 'string' ||
        (contentValues.indexOf(value) === -1 &&
          !contentValuePattern.test(value) &&
          (value.charAt(0) !== value.charAt(value.length - 1) ||
            (value.charAt(0) !== '"' && value.charAt(0) !== "'")))
      ) {
        console.error(
          `You seem to be using a value for 'content' without quotes, try replacing it with \`content: '"${value}"'\``
        )
      }
    }
    return oldProcessStyleValue(key, value)
  }
}

export type ClassNameArg =
  | string
  | boolean
  | (() => ClassNameArg)
  | { [key: string]: boolean }
  | Array<ClassNameArg>

export const classnames = (args: Array<ClassNameArg>): string => {
  let len = args.length
  let i = 0
  let cls = ''
  for (; i < len; i++) {
    let arg = args[i]
    if (arg == null) continue

    let toAdd
    switch (typeof arg) {
      case 'boolean':
        break
      case 'function':
        if (process.env.NODE_ENV !== 'production') {
          console.error(
            'Passing functions to cx is deprecated and will be removed in the next major version of Emotion.\n' +
              'Please call the function before passing it to cx.'
          )
        }
        toAdd = classnames([arg()])
        break
      case 'object': {
        if (Array.isArray(arg)) {
          toAdd = classnames(arg)
        } else {
          toAdd = ''
          for (const k in arg) {
            if (arg[k] && k) {
              toAdd && (toAdd += ' ')
              toAdd += k
            }
          }
        }
        break
      }
      default: {
        toAdd = arg
      }
    }
    if (toAdd) {
      cls && (cls += ' ')
      cls += toAdd
    }
  }
  return cls
}

export const isBrowser = typeof document !== 'undefined'

export type PrefixOption =
  | boolean
  | ((key: string, value: string, context: 1 | 2 | 3) => boolean)
