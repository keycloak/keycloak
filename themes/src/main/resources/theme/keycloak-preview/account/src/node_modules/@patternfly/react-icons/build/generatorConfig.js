const path = require('path');

const templatesDir = path.resolve(__dirname, './templates');
const srcDir = path.resolve(__dirname, '../src');
const iconsDir = path.join(srcDir, './icons');

const escapeFilePath = filePath => filePath.replace(/\\/g, '\\$&');

module.exports = plop => {
  plop.setGenerator('icons', {
    prompts: [],
    actions(data) {
      const actions = [];
      data.icons.forEach(icon => {
        actions.push({
          type: 'add',
          force: true,
          data: icon,
          path: escapeFilePath(path.join(iconsDir, './{{id}}.js')),
          templateFile: path.join(templatesDir, 'iconFile.hbs')
        });

        actions.push({
          type: 'add',
          force: true,
          data: icon,
          path: escapeFilePath(path.join(iconsDir, './{{id}}.d.ts')),
          templateFile: path.join(templatesDir, 'iconFileTS.hbs')
        });
      });

      actions.push({
        type: 'add',
        force: true,
        path: escapeFilePath(path.join(srcDir, './index.js')),
        templateFile: path.join(templatesDir, 'mainBarrelFile.hbs')
      });

      actions.push({
        type: 'add',
        force: true,
        path: escapeFilePath(path.join(srcDir, './index.d.ts')),
        templateFile: path.join(templatesDir, 'mainBarrelFileTS.hbs')
      });

      return actions;
    }
  });
};
