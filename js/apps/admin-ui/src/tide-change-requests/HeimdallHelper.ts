export async function importHeimdall() {
  try {
    const path = new URL('../../tide-modules/modules/heimdall', import.meta.url).href;
    return await import(/* @vite-ignore */ path);
  } catch (err) {
    console.warn('Heimdall not available – loading fallback.');
    return null;
  }
}

