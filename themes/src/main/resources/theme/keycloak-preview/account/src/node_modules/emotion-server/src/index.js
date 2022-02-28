// @flow
import createEmotionServer from 'create-emotion-server'
import * as emotion from 'emotion'

export const {
  extractCritical,
  renderStylesToString,
  renderStylesToNodeStream
} = createEmotionServer(emotion)
