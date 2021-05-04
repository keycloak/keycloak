import path from 'path'
import TerserPlugin from 'terser-webpack-plugin'
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
        import: [
          'promise-polyfill/src/polyfill',
          path.join(packagesPath, 'keycloak-js')
        ],
        library: {
          type: 'umd',
          name: 'Keycloak',
          export: 'default',
        }
      },
      'keycloak-authz': {
        import: path.join(packagesPath, 'keycloak-authz'),
        library: {
          type: 'umd',
          name: 'KeycloakAuthorization',
          export: 'default',
        }
      },
    },
    output: {
      path: classesPath,
      filename: options.minimize ? '[name].min.js' : '[name].js',
    },
    module: {
      rules: [{
        test: /\.(t|j)s$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: [
              ['@babel/preset-env'],
            ]
          }
        }
      }]
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
