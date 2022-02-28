// @flow
import type { Emotion } from 'create-emotion'

const createExtractCritical = (emotion: Emotion) => (html: string) => {
  // parse out ids from html
  // reconstruct css/rules/cache to pass
  let RGX = new RegExp(`${emotion.caches.key}-([a-zA-Z0-9-_]+)`, 'gm')

  let o = { html, ids: [], css: '' }
  let match
  let ids = {}
  while ((match = RGX.exec(html)) !== null) {
    // $FlowFixMe
    if (ids[match[1]] === undefined) {
      // $FlowFixMe
      ids[match[1]] = true
    }
  }

  o.ids = Object.keys(emotion.caches.inserted).filter(id => {
    if (
      (ids[id] === true ||
        emotion.caches.registered[`${emotion.caches.key}-${id}`] ===
          undefined) &&
      emotion.caches.inserted[id] !== true
    ) {
      o.css += emotion.caches.inserted[id]
      return true
    }
  })

  return o
}

export default createExtractCritical
