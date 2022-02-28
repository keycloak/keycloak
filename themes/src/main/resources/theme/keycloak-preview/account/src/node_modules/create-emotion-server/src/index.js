// @flow
import type { Emotion } from 'create-emotion'
import createExtractCritical from './extract-critical'
import createRenderStylesToString from './inline'
import createRenderStylesToStream from './stream'

export default function(emotion: Emotion) {
  const nonceString =
    emotion.caches.nonce !== undefined ? ` nonce="${emotion.caches.nonce}"` : ''
  return {
    extractCritical: createExtractCritical(emotion),
    renderStylesToString: createRenderStylesToString(emotion, nonceString),
    renderStylesToNodeStream: createRenderStylesToStream(emotion, nonceString)
  }
}
