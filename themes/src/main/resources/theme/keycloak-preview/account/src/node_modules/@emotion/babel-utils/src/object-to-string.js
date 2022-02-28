// @flow
import { serializeStyles } from '@emotion/serialize'

// to anyone looking at this, this isn't intended to simplify every single case
// it's meant to simplify the most common cases so i don't want to make it especially complex
// also, this will be unnecessary when prepack is ready
export function simplifyObject(node: *, t: Object) {
  let bailout = false
  let finalString = ''
  node.properties.forEach(property => {
    if (bailout) {
      return
    }
    if (
      property.computed ||
      (!t.isIdentifier(property.key) && !t.isStringLiteral(property.key)) ||
      (!t.isStringLiteral(property.value) &&
        !t.isNumericLiteral(property.value) &&
        !t.isObjectExpression(property.value))
    ) {
      bailout = true
    }

    let key = property.key.name || property.key.value
    if (key === 'styles') {
      bailout = true
      return
    }
    if (t.isObjectExpression(property.value)) {
      let simplifiedChild = simplifyObject(property.value, t)
      if (!t.isStringLiteral(simplifiedChild)) {
        bailout = true
        return
      }
      finalString += `${key}{${simplifiedChild.value}}`
      return
    }
    let value = property.value.value

    finalString += serializeStyles({}, [{ [key]: value }]).styles
  })
  return bailout ? node : t.stringLiteral(finalString)
}
