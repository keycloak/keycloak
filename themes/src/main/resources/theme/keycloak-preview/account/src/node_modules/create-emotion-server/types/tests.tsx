import { Emotion } from 'create-emotion';

import createEmotionServer from '../';

declare const emotion: Emotion;

const emotionServer = createEmotionServer(emotion);

const { html, css, ids } = emotionServer.extractCritical("<div></div>");

emotionServer.renderStylesToString("<div></div>");

declare const stream: NodeJS.ReadableStream;

stream.pipe(emotionServer.renderStylesToNodeStream());
