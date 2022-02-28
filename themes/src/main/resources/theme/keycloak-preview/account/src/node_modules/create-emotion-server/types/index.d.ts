/// <reference types="node" />
// TypeScript Version: 2.3

import { Emotion } from 'create-emotion';

export interface EmotionServer {
  extractCritical(html: string): { html: string; ids: string[]; css: string; };
  renderStylesToString(html: string): string;
  renderStylesToNodeStream(): NodeJS.ReadWriteStream;
}

export default function createEmotionServer(emotion: Emotion): EmotionServer;
