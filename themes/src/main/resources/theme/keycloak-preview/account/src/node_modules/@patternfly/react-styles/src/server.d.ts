export interface StaticRenderResult {
  html: string;
}

export function renderStatic(renderFn: () => string): StaticRenderResult;
