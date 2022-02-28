/* eslint import/no-extraneous-dependencies: 0 */
const path = require('path')
const { createConfig, babel } = require('webpack-blocks')

module.exports = {
  title: 'react-dropzone',
  styleguideDir: path.join(__dirname, 'styleguide'),
  webpackConfig: createConfig([babel()]),
  exampleMode: 'expand',
  usageMode: 'expand',
  showSidebar: false,
  serverPort: 8080,
  compilerConfig: {
    transforms: { dangerousTaggedTemplateString: true },
    objectAssign: 'Object.assign'
  },
  sections: [
    {
      name: '',
      content: 'README.md'
    },
    {
      name: 'PropTypes',
      components: './src/index.js'
    },
    {
      name: 'Examples',
      context: {
        Dropzone: './src/index'
      },
      sections: [
        {
          name: 'Basic example',
          content: 'examples/Basic/Readme.md'
        },
        {
          name: 'Events',
          content: 'examples/Events/README.md'
        },
        {
          name: 'Styling Dropzone',
          content: 'examples/Styling/Readme.md'
        },
        {
          name: 'Accepting specific file types',
          content: 'examples/Accept/Readme.md'
        },
        {
          name: 'Previews',
          content: 'examples/Previews/Readme.md'
        },
        {
          name: 'Nested Dropzone',
          content: 'examples/Nesting/Readme.md'
        },
        {
          name: 'Opening File Dialog Programmatically',
          content: 'examples/FileDialog/Readme.md'
        },
        {
          name: 'Full Screen Dropzone',
          content: 'examples/Fullscreen/Readme.md'
        },
        {
          name: 'Extending Dropzone',
          context: {
            Dropzone: './src/index'
          },
          sections: [
            {
              name: 'Using third-party plugins',
              content: 'examples/PluginArchitecture/Readme.md'
            }
          ]
        }
      ]
    }
  ]
}
