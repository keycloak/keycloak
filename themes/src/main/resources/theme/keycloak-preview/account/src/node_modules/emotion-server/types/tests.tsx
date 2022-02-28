import * as React from 'react';
import { renderToNodeStream, renderToString } from 'react-dom/server';
import { extractCritical, renderStylesToNodeStream, renderStylesToString } from '../';

declare const element: React.ReactElement<any>;

const { html, css, ids } = extractCritical(renderToString(element));

renderStylesToString(renderToString(element));

renderToNodeStream(element).pipe(renderStylesToNodeStream());
