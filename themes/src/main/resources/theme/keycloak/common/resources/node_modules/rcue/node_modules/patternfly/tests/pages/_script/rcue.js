/* eslint-env node */
/* eslint no-console: 0 strict: 0 */
'use strict';

let fs = require('mz/fs'),
  fm = require('front-matter'),
  yaml = require('js-yaml'),
  _ = require('lodash'),
  engine = require('./liquid-engine');

function readTemplate (filename) {
  return fs.readFile(filename, 'utf8')
  .then(contents => {
    let markup = fm(contents);
    let page = markup.attributes;
    page.template = markup.body;
    return page;
  });
}

function renderTemplate (page, context) {
  return Promise.resolve(page.template)
  .then(template => {
    return engine.parse(template)
    .then(parsedTemplate => parsedTemplate.render(context))
    .then(renderedContent => {
      context.content = renderedContent;
      return context;
    });
  })
  .then(context => {
    let next;
    if (page.layout) {
      let layout = context.site._layouts[`${page.layout}.html`];
      _.merge(context.page, layout.page);
      next = renderTemplate(layout, context);
    } else {
      next = context;
    }
    return next;
  });
}

function readTemplateFolder (path) {
  return fs.readdir(path)
  // filter out files we don't want to process
  .then(filenames => {
    let promises = [];
    filenames = filenames.filter(filename => !filename.startsWith('_'));
    filenames.forEach(filename => {
      promises.push(
        fs.stat(`${path}/${filename}`)
        .then(stats => stats.isFile() ? filename : null)
      );
    });
    return Promise.all(promises);
  })
  .then(filenames => filenames.filter(filename => filename !== null))
  // Read the pages
  .then(filenames => {
    // filenames.splice(1, filenames.length);console.log(filenames)
    let promises = [];
    filenames.forEach(filename => {
      promises.push(
        readTemplate(`${path}/${filename}`)
        .then(page => {
          page.filename = filename;
          page.url = filename;
          return page;
        })
      );
    });
    return Promise.all(promises);
  });
}

function readSiteConfig () {
  return fs.readFile('tests/pages/_config.rcue.yml', 'utf8')
  .then(text => {
    return yaml.safeLoad(text);
  });
}

module.exports = {
  readTemplate: readTemplate,
  readSiteConfig: readSiteConfig,
  renderTemplate: renderTemplate,
  readTemplateFolder: readTemplateFolder
};
