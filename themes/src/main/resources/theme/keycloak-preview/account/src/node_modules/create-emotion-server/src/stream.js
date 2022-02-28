// @flow
import type { Emotion } from 'create-emotion'
import through from 'through'
import tokenize from 'html-tokenize'
import pipe from 'multipipe'

const createRenderStylesToNodeStream = (
  emotion: Emotion,
  nonceString: string
) => () => {
  let insed = {}
  const tokenStream = tokenize()

  const inlineStream = through(
    function write(thing) {
      let [type, data] = thing
      if (type === 'open') {
        let css = ''
        let ids = {}

        let match
        let fragment = data.toString()
        let regex = new RegExp(`${emotion.caches.key}-([a-zA-Z0-9-_]+)`, 'gm')
        while ((match = regex.exec(fragment)) !== null) {
          if (match !== null && insed[match[1]] === undefined) {
            ids[match[1]] = true
          }
        }
        Object.keys(emotion.caches.inserted).forEach(id => {
          if (
            emotion.caches.inserted[id] !== true &&
            insed[id] === undefined &&
            (ids[id] === true ||
              (emotion.caches.registered[`${emotion.caches.key}-${id}`] ===
                undefined &&
                (ids[id] = true)))
          ) {
            insed[id] = true
            // $FlowFixMe flow thinks emotion.caches.inserted[id] can be true even though it's checked earlier
            css += emotion.caches.inserted[id]
          }
        })

        if (css !== '') {
          this.queue(
            `<style data-emotion-${emotion.caches.key}="${Object.keys(ids).join(
              ' '
            )}"${nonceString}>${css}</style>`
          )
        }
      }
      this.queue(data)
    },
    function end() {
      this.queue(null)
    }
  )

  return pipe(
    tokenStream,
    inlineStream
  )
}

export default createRenderStylesToNodeStream
