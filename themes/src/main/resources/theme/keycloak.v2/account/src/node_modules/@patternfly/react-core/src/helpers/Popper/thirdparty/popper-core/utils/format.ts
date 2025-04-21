// @ts-nocheck

/**
 * @param str
 * @param args
 */
export default function format(str: string, ...args: string[]) {
  return [...args].reduce((p, c) => p.replace(/%s/, c), str);
}
