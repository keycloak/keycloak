'use strict'

const { runCommand } = (() => {
  try {
    return require('@antora/assembler')
  } catch {
    return require('../../assembler')
  }
})()
const ospath = require('path')

// Q: how can we simplify this to avoid redundant code?
// Q: rename to convertDocumentToPDF?
function convertDocumentToPdf (doc, buildConfig) {
  const {
    asciidoc: { attributes: baseAttributes } = { attributes: {} },
    contents: input,
    src: { component, version, basename, extname: docfilesuffix },
  } = doc
  const { command = 'asciidoctor-pdf', cwd, dir } = buildConfig
  const docfile = `${version}@${component}::pdf$${basename}`
  const docname = basename.substr(0, basename.length - docfilesuffix.length)
  const convertAttributes = Object.assign({}, baseAttributes, {
    docfile,
    docfilesuffix,
    'docname@': docname,
    imagesdir: dir,
  })
  Object.assign(doc, { contents: null, extname: '.pdf', mediaType: 'application/pdf' })
  const argv = Object.entries(convertAttributes).reduce(
    (accum, [name, val]) =>
      accum.push('-a', val ? `${name}=${val}` : val === '' ? name : `!${name}${val === false ? '=@' : ''}`) && accum,
    []
  )
  const output = ospath.join(dir, doc.path)
  argv.push('-o', output)
  // Q: should runCommand accept outputFlag and automatically append to argv?
  return runCommand(command, argv, { cwd, input, output }).then((contents) =>
    true // SKIP PDF UPLOAD // Object.assign(doc, { contents, out: { path: doc.path } })
  )
}

module.exports = convertDocumentToPdf
