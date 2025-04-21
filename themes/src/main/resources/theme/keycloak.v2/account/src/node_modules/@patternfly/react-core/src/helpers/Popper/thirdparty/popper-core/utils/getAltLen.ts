// @ts-nocheck

/**
 * @param len
 */
export default function getAltLen(len: 'width' | 'height'): 'width' | 'height' {
  return len === 'width' ? 'height' : 'width';
}
