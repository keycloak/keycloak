#-------------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#-------------------------------------------------------------------------------

fs   = require 'fs'
path = require 'path'

_     = require 'underscore'
nopt  = require 'nopt'

utils  = require './utils'
weinre = require './weinre'

optionDefaults =
    httpPort:     8080
    boundHost:    'localhost'
    verbose:      false
    debug:        false
    readTimeout:  5

#-------------------------------------------------------------------------------
exports.run = ->

    knownOpts =
        httpPort:     Number
        boundHost:    String
        verbose:      Boolean
        debug:        Boolean
        readTimeout:  Number
        deathTimeout: Number
        help:         Boolean

    shortHands =
        '?':  ['--help']
        'h':  ['--help']

    nopt.invalidHandler = printNoptError
    parsedOpts = nopt(knownOpts, shortHands, process.argv, 2)

    #----

    printHelp() if parsedOpts.help

    args = parsedOpts.argv.remain

    printHelp() if args.length != 0

    #----

    delete parsedOpts.argv
    opts = _.extend {}, optionDefaults, getDotWeinreServerProperties(), parsedOpts

    if !opts.deathTimeout?
        opts.deathTimeout = 3 * opts.readTimeout

    utils.setOptions opts

    weinre.run opts

#-------------------------------------------------------------------------------
printNoptError = (key, val, types) ->
    utils.exit "error with option '#{key}', value '#{val}'"

#-------------------------------------------------------------------------------
printHelp = () ->
    version = weinre.getVersion()

    console.error """
usage:   #{utils.Program} [options]
version: #{version}

options:
    --httpPort     port to run the http server on        default: #{optionDefaults.httpPort}
    --boundHost    ip address to bind the server to      default: #{optionDefaults.boundHost}
    --verbose      print more diagnostics                default: #{optionDefaults.verbose}
    --debug        print even more diagnostics           default: #{optionDefaults.debug}
    --readTimeout  seconds to wait for a client message  default: #{optionDefaults.readTimeout}
    --deathTimeout seconds to wait to kill client        default: 3*readTimeout

--boundHost can be an ip address, hostname, or -all-, where -all-
means binding to all ip address on the current machine'

for more info see: http://people.apache.org/~pmuellr/weinre/
"""
    process.exit()

#-------------------------------------------------------------------------------
getDotWeinreServerProperties = () ->
    properties = {}

    fileName = replaceTilde '~/.weinre/server.properties'
    return properties if !utils.fileExistsSync(fileName)

    contents = fs.readFileSync(fileName, 'utf8')
    lines    = contents.split('\n')

    for line in lines
        line = line.replace(/#.*/,'')
        match = line.match /\s*(\w+)\s*:\s*(.+)\s*/
        continue if !match

        key = utils.trim match[1]
        val = utils.trim match[2]

        properties[key] = val

    properties

#-------------------------------------------------------------------------------
replaceTilde = (fileName) ->
    fileName.replace('~', getTildeReplacement())

#-------------------------------------------------------------------------------
getTildeReplacement = () ->
    process.env["HOME"] || process.env["USERPROFILE"] || '.'
