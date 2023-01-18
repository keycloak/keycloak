'use strict'

const convertDocumentToPdf = require('./convert-document-to-ccutil')
const { assembleContent } = (() => {
  try {
    return require('@antora/assembler')
  } catch {
    return require('../../assembler')
  }
})()

module.exports.register = function () {
  //this.on('beforeProcess', ({ asciidocConfig }) => {
  //  asciidocConfig.keepSource = true
  //})
  this.on('contentClassified', ({ contentCatalog }) => {
    contentCatalog.getPages((page) => {
      if (!page.out) return
      page.src.contents = page.contents
      page.src = new Proxy(page.src, { deleteProperty: (o, p) => (p === 'contents' ? true : delete o[p]) })
    })
  })
  this.on('beforePublish', ({ playbook, contentCatalog, siteCatalog }) =>
    assembleContent.call(this, playbook, contentCatalog, convertDocumentToPdf, { siteCatalog })
  )
}
