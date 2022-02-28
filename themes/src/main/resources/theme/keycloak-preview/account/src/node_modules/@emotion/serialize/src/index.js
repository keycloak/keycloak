// @flow
import type {
  Interpolation,
  ScopedInsertableStyles,
  RegisteredCache
} from '@emotion/utils'
import hashString from '@emotion/hash'
import unitless from '@emotion/unitless'
import memoize from '@emotion/memoize'

let hyphenateRegex = /[A-Z]|^ms/g

let animationRegex = /_EMO_([^_]+?)_([^]*?)_EMO_/g

const processStyleName = memoize((styleName: string) =>
  styleName.replace(hyphenateRegex, '-$&').toLowerCase()
)

let processStyleValue = (key: string, value: string): string => {
  if (value == null || typeof value === 'boolean') {
    return ''
  }

  switch (key) {
    case 'animation':
    case 'animationName': {
      value = value.replace(animationRegex, (match, p1, p2) => {
        styles = p2 + styles
        return p1
      })
    }
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

function handleInterpolation(
  mergedProps: void | Object,
  registered: RegisteredCache,
  interpolation: Interpolation
): string | number {
  if (interpolation == null) {
    return ''
  }
  if (interpolation.__emotion_styles !== undefined) {
    if (
      process.env.NODE_ENV !== 'production' &&
      interpolation.toString() === 'NO_COMPONENT_SELECTOR'
    ) {
      throw new Error(
        'Component selectors can only be used in conjunction with babel-plugin-emotion.'
      )
    }
    return interpolation
  }

  switch (typeof interpolation) {
    case 'boolean': {
      return ''
    }
    case 'object': {
      if (interpolation.anim === 1) {
        styles = interpolation.styles + styles
        return interpolation.name
      }
      if (interpolation.styles !== undefined) {
        return interpolation.styles
      }

      return createStringFromObject(mergedProps, registered, interpolation)
    }
    case 'function': {
      if (mergedProps !== undefined) {
        return handleInterpolation(
          mergedProps,
          registered,
          // $FlowFixMe
          interpolation(mergedProps)
        )
      }
    }
    // eslint-disable-next-line no-fallthrough
    default: {
      const cached = registered[interpolation]
      return cached !== undefined ? cached : interpolation
    }
  }
}

function createStringFromObject(
  mergedProps: void | Object,
  registered: RegisteredCache,
  obj: { [key: string]: Interpolation }
): string {
  let string = ''

  if (Array.isArray(obj)) {
    for (let i = 0; i < obj.length; i++) {
      string += handleInterpolation(mergedProps, registered, obj[i])
    }
  } else {
    for (let key in obj) {
      if (typeof obj[key] !== 'object') {
        string += `${processStyleName(key)}:${processStyleValue(
          key,
          obj[key]
        )};`
      } else {
        if (
          key === 'NO_COMPONENT_SELECTOR' &&
          process.env.NODE_ENV !== 'production'
        ) {
          throw new Error(
            'Component selectors can only be used in conjunction with @emotion/babel-plugin-core.'
          )
        }
        if (
          Array.isArray(obj[key]) &&
          (typeof obj[key][0] === 'string' &&
            registered[obj[key][0]] === undefined)
        ) {
          obj[key].forEach(value => {
            string += `${processStyleName(key)}:${processStyleValue(
              key,
              value
            )};`
          })
        } else {
          string += `${key}{${handleInterpolation(
            mergedProps,
            registered,
            obj[key]
          )}}`
        }
      }
    }
  }

  return string
}

let labelPattern = /label:\s*([^\s;\n{]+)\s*;/g

// this is set to an empty string on each serializeStyles call
// it's declared in the module scope since we need to add to
// it in the middle of serialization to add styles from keyframes
let styles = ''

export const serializeStyles = function(
  registered: RegisteredCache,
  args: Array<Interpolation>,
  mergedProps: void | Object
): ScopedInsertableStyles {
  if (
    args.length === 1 &&
    typeof args[0] === 'object' &&
    args[0] !== null &&
    args[0].styles !== undefined
  ) {
    return args[0]
  }
  let stringMode = true
  styles = ''
  let identifierName = ''
  let strings = args[0]
  if (strings == null || strings.raw === undefined) {
    stringMode = false
    // we have to store this in a variable and then append it to styles since
    // styles could be modified in handleInterpolation and using += would mean
    // it would append the return value of handleInterpolation to the value before handleInterpolation is called
    let stringifiedInterpolation = handleInterpolation(
      mergedProps,
      registered,
      strings
    )
    styles += stringifiedInterpolation
  } else {
    styles += strings[0]
  }
  // we start at 1 since we've already handled the first arg
  for (let i = 1; i < args.length; i++) {
    // we have to store this in a variable and then append it to styles since
    // styles could be modified in handleInterpolation and using += would mean
    // it would append the return value of handleInterpolation to the value before handleInterpolation is called
    let stringifiedInterpolation = handleInterpolation(
      mergedProps,
      registered,
      args[i]
    )
    styles += stringifiedInterpolation
    if (stringMode) {
      styles += strings[i]
    }
  }

  styles = styles.replace(labelPattern, (match, p1: string) => {
    identifierName += `-${p1}`
    return ''
  })

  let name = hashString(styles) + identifierName

  return {
    name,
    styles
  }
}
