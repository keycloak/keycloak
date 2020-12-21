import * as path from 'path'
import * as TerserPlugin from 'terser-webpack-plugin'
import { Configuration } from 'webpack'

interface CreateConfigOptions {
  minimize: boolean
}

const packagesPath = path.join(__dirname, 'packages')
const classesPath = path.join(__dirname, 'target/classes')

function createConfig(options: CreateConfigOptions): Configuration {
  return {
    mode: 'production',
    entry: {
      keycloak: {
        import: path.join(packagesPath, 'keycloak-js'),
        library: {
          type: 'umd',
          name: 'Keycloak',
        }
      },
      'keycloak-authz': {
        import: path.join(packagesPath, 'keycloak-authz'),
        library: {
          type: 'umd',
          name: 'KeycloakAuthorization',
        }
      },
    },
    output: {
      path: classesPath,
      filename: options.minimize ? '[name].min.js' : '[name].js',
    },
    devtool: options.minimize ? 'source-map' : false,
    optimization: {
      minimize: options.minimize,
      minimizer: [new TerserPlugin({
        extractComments: false,
      })],
    }
  }
}

export default [
  createConfig({ minimize: false }),
  createConfig({ minimize: true }),
]
