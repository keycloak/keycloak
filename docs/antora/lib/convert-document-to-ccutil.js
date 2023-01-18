'use strict'

const ospath = require('path')
const fs = require('fs')

function convertDocumentToCcutil (doc, buildConfig) {
  const {
    asciidoc: { attributes: baseAttributes } = { attributes: {} },
    contents: input,
    src: { component, version, basename, extname: docfilesuffix },
  } = doc
  const { dir } = buildConfig
  const docfile = `${version}@${component}::pdf$${basename}`
  const docname = basename.substr(0, basename.length - docfilesuffix.length)
  const convertAttributes = Object.assign({}, baseAttributes, {
    docfile,
    docfilesuffix,
    'docname@': docname,
    imagesdir: dir,
  })
  Object.assign(doc, { contents: null, extname: '-ccutil.adoc', mediaType: 'application/pdf' })
  const output = ospath.join(dir, doc.path)
  let splittedInput = input.toString().split('\n');
  // adding missing author
  splittedInput.splice(2, 0, 'Keycloak Project')
  // adding attributes to the file
  Object.entries(convertAttributes).forEach(
      ([name, val]) =>
          splittedInput.splice(4, 0, `:${name}: ${val}`)
  )
  return exportFile(splittedInput.join('\n'), output)
}

async function exportFile (input, output) {
  return new Promise((resolve) => {
    const path = output.substring(0, output.lastIndexOf('/'));
    if (!fs.existsSync(path)) {
      fs.mkdirSync(path, { recursive: true })
    }
    fs.writeFileSync(output, input)
    resolve(true)
  })
}

module.exports = convertDocumentToCcutil
