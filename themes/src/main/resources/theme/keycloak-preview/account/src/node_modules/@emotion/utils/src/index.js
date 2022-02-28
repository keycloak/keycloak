// @flow
import type {
  RegisteredCache,
  CSSContextType,
  ScopedInsertableStyles
} from './types'

export const isBrowser = typeof document !== 'undefined'

export function getRegisteredStyles(
  registered: RegisteredCache,
  registeredStyles: string[],
  classNames: string
) {
  let rawClassName = ''

  classNames.split(' ').forEach(className => {
    if (registered[className] !== undefined) {
      registeredStyles.push(registered[className])
    } else {
      rawClassName += `${className} `
    }
  })
  return rawClassName
}

export const insertStyles = (
  context: CSSContextType,
  insertable: ScopedInsertableStyles,
  isStringTag: boolean
) => {
  if (
    // we only need to add the styles to the registered cache if the
    // class name could be used further down
    // the tree but if it's a string tag, we know it won't
    // so we don't have to add it to registered cache.
    // this improves memory usage since we can avoid storing the whole style string
    (isStringTag === false ||
      // we need to always store it if we're in compat mode and
      // in node since emotion-server relies on whether a style is in
      // the registered cache to know whether a style is global or not
      // also, note that this check will be dead code eliminated in the browser
      (isBrowser === false && context.compat !== undefined)) &&
    context.registered[`${context.key}-${insertable.name}`] === undefined
  ) {
    context.registered[`${context.key}-${insertable.name}`] = insertable.styles
  }
  if (context.inserted[insertable.name] === undefined) {
    let rules = context.stylis(
      `.${context.key}-${insertable.name}`,
      insertable.styles
    )
    context.inserted[insertable.name] = true

    if (isBrowser) {
      rules.forEach(context.sheet.insert, context.sheet)
    } else {
      let joinedRules = rules.join('')
      if (context.compat === undefined) {
        // in regular mode, we don't set the styles on the inserted cache
        // since we don't need to and that would be wasting memory
        // we return them so that they are rendered in a style tag
        return joinedRules
      } else {
        // in compat mode, we put the styles on the inserted cache so
        // that emotion-server can pull out the styles
        context.inserted[insertable.name] = joinedRules
      }
    }
  }
}

export * from './types'
