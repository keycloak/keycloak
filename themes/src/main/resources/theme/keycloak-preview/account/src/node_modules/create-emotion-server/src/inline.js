// @flow
import type { Emotion } from 'create-emotion'

function generateStyleTag(
  cssKey: string,
  ids: string,
  styles: string,
  nonceString: string
) {
  return `<style data-emotion-${cssKey}="${ids.substring(
    1
  )}"${nonceString}>${styles}</style>`
}

const createRenderStylesToString = (emotion: Emotion, nonceString: string) => (
  html: string
): string => {
  const { inserted, key: cssKey, registered } = emotion.caches
  const regex = new RegExp(`<|${cssKey}-([a-zA-Z0-9-_]+)`, 'gm')

  const seen = {}

  let result = ''
  let globalIds = ''
  let globalStyles = ''

  for (const id in inserted) {
    if (inserted.hasOwnProperty(id)) {
      const style = inserted[id]
      const key = `${cssKey}-${id}`
      if (style !== true && registered[key] === undefined) {
        globalStyles += style
        globalIds += ` ${id}`
      }
    }
  }

  if (globalStyles !== '') {
    result = generateStyleTag(cssKey, globalIds, globalStyles, nonceString)
  }

  let ids = ''
  let styles = ''
  let lastInsertionPoint = 0
  let match

  while ((match = regex.exec(html)) !== null) {
    // $FlowFixMe
    if (match[0] === '<') {
      if (ids !== '') {
        result += generateStyleTag(cssKey, ids, styles, nonceString)
        ids = ''
        styles = ''
      }
      // $FlowFixMe
      result += html.substring(lastInsertionPoint, match.index)
      // $FlowFixMe
      lastInsertionPoint = match.index
      continue
    }
    // $FlowFixMe
    const id = match[1]
    const style = inserted[id]
    if (style === true || seen[id]) {
      continue
    }

    seen[id] = true
    styles += style
    ids += ` ${id}`
  }

  result += html.substring(lastInsertionPoint)

  return result
}

export default createRenderStylesToString
