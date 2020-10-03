import path from 'path'
import { Configuration } from 'webpack'

interface CreateConfigOptions {
  minimize?: boolean
}

const packagesPath = path.join(__dirname, 'packages')
const classesPath = path.join(__dirname, 'target/classes')

function createConfig(options: CreateConfigOptions): Configuration {
  return {
    mode: 'production',
    entry: {
      keycloak: path.join(packagesPath, 'keycloak-js'),
      'keycloak-authz': path.join(packagesPath, 'keycloak-authz'),
    },
    output: {
      path: classesPath,
      filename: options.minimize ? '[name].min.js' : '[name].js',
    },
    optimization: {
      minimize: options.minimize,
    }
  }
}

export default [
  createConfig({ minimize: false }),
  createConfig({ minimize: true }),
]