import { renderStylesToString } from 'emotion-server';

/**
 * @param {Function} renderFn - Render function
 */
export function renderStatic(renderFn) {
  const html = renderStylesToString(renderFn());
  return { html };
}
