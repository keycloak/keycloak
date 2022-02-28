import { reservedES3, reservedESnext } from './reserved'

// from https://werxltd.com/wp/2010/05/13/javascript-implementation-of-javas-string-hashcode-method/
function hashCode(str) {
  let hash = 0
  for (let i = 0; i < str.length; ++i) {
    const char = str.charCodeAt(i)
    hash = (hash << 5) - hash + char
    hash |= 0 // Convert to 32bit integer
  }
  return hash
}

/**
 * Sanitize a string for use as an identifier name
 *
 * Replaces invalid character sequences with _ and may add a _ prefix if the
 * resulting name would conflict with a JavaScript reserved name.
 *
 * @param {string} key The desired identifier name
 * @param {boolean} unique Append a hash of the key to the result
 * @returns {string}
 */
export function identifier(key, unique) {
  if (unique) key += ' ' + hashCode(key).toString(36)
  const id = key.trim().replace(/\W+/g, '_')
  return reservedES3[id] || reservedESnext[id] || /^\d/.test(id) ? '_' + id : id
}

/**
 * Sanitize a string for use as a property name
 *
 * By default uses `obj.key` notation, falling back to `obj["key"]` if the key
 * contains invalid characters or is an ECMAScript 3rd Edition reserved word
 * (required by IE8).
 *
 * @param {string} [obj] If empty, returns only the possibly quoted key
 * @param {string} key The property name
 * @returns {string}
 */
export function property(obj, key) {
  if (/^[A-Z_$][0-9A-Z_$]*$/i.test(key) && !reservedES3[key]) {
    return obj ? obj + '.' + key : key
  } else {
    const jkey = JSON.stringify(key)
    return obj ? obj + '[' + jkey + ']' : jkey
  }
}
