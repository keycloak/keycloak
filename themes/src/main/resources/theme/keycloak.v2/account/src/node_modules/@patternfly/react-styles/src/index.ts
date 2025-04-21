/** Joins args into a className string
 *
 * @param {any} args list of objects, string, or arrays to reduce
 */
export function css(...args: any): string {
  // Adapted from https://github.com/JedWatson/classnames/blob/master/index.js
  const classes = [] as string[];
  const hasOwn = {}.hasOwnProperty;

  args.filter(Boolean).forEach((arg: any) => {
    const argType = typeof arg;

    if (argType === 'string' || argType === 'number') {
      classes.push(arg);
    } else if (Array.isArray(arg) && arg.length) {
      const inner = css(...(arg as any));
      if (inner) {
        classes.push(inner);
      }
    } else if (argType === 'object') {
      for (const key in arg) {
        if (hasOwn.call(arg, key) && arg[key]) {
          classes.push(key);
        }
      }
    }
  });

  return classes.join(' ');
}
