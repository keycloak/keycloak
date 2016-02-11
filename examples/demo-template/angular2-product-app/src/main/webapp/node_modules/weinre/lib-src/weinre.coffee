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
net  = require 'net'
dns  = require 'dns'
path = require 'path'

_       = require 'underscore'
express = require 'express'

utils              = require './utils'
jsonBodyParser     = require './jsonBodyParser'
HttpChannelHandler = require './HttpChannelHandler'
dumpingHandler     = require './dumpingHandler'
channelManager     = require './channelManager'
serviceManager     = require './serviceManager'

#-------------------------------------------------------------------------------
exports.run = (options) ->
    processOptions(options, run2)

#-------------------------------------------------------------------------------
run2 = ->
    options = utils.options
    
    serviceManager.registerProxyClass 'WeinreClientEvents'
    serviceManager.registerProxyClass 'WeinreTargetEvents'
    serviceManager.registerLocalClass 'WeinreClientCommands'
    serviceManager.registerLocalClass 'WeinreTargetCommands'
    
    startDeathWatcher options.deathTimeout
    
    startServer()    

#-------------------------------------------------------------------------------
processOptions = (options, cb) ->
    options.httpPort     = utils.ensureInteger( options.httpPort,     'the value of the option httpPort is not a number')
    options.boundHost    = utils.ensureString(  options.boundHost,    'the value of the option boundHost is not a string')
    options.verbose      = utils.ensureBoolean( options.verbose,      'the value of the option verbose is not a boolean')
    options.debug        = utils.ensureBoolean( options.debug,        'the value of the option debug is not a boolean')
    options.readTimeout  = utils.ensureInteger( options.readTimeout,  'the value of the option readTimeout is not a number')
    options.deathTimeout = utils.ensureInteger( options.deathTimeout, 'the value of the option deathTimeout is not a number')

    options.verbose = true if options.debug
    
    options.staticWebDir = getStaticWebDir()
    
    utils.logVerbose "pid:                 #{process.pid}"
    utils.logVerbose "version:             #{getVersion()}"
    utils.logVerbose "node versions:"
    
    names   = _.keys(process.versions)
    reducer = (memo, name) -> Math.max(memo, name.length)
    nameLen = _.reduce(names, reducer, 0)
    
    for name in names
        utils.logVerbose "   #{utils.alignLeft(name, nameLen)}: #{process.versions[name]}"
    
    utils.logVerbose "options:"
    utils.logVerbose "   httpPort:     #{options.httpPort}"
    utils.logVerbose "   boundHost:    #{options.boundHost}"
    utils.logVerbose "   verbose:      #{options.verbose}"
    utils.logVerbose "   debug:        #{options.debug}"
    utils.logVerbose "   readTimeout:  #{options.readTimeout}"
    utils.logVerbose "   deathTimeout: #{options.deathTimeout}"

    utils.setOptions options

    checkHost options.boundHost, (err) ->
        if err
            utils.exit "unable to resolve boundHost address: #{options.boundHost}"

        cb()

#-------------------------------------------------------------------------------
checkHost = (hostName, cb) ->
    return cb() if hostName == '-all-'
    return cb() if hostName == 'localhost'
    
    return cb() if net.isIP(hostName)
    
    dns.lookup hostName, cb

#-------------------------------------------------------------------------------
deathTimeout = null

#-------------------------------------------------------------------------------
startDeathWatcher = (timeout) ->
    deathTimeout = utils.options.deathTimeout * 1000
    
    setInterval checkForDeath, 1000

#-------------------------------------------------------------------------------
checkForDeath = ->
    now = (new Date).valueOf()
    for channel in channelManager.getChannels()
        if now - channel.lastRead > deathTimeout
            channel.close()

#-------------------------------------------------------------------------------
startServer = () ->
    options = utils.options

    clientHandler = new HttpChannelHandler('/ws/client')
    targetHandler = new HttpChannelHandler('/ws/target')
    
    channelManager.initialize()
    
    favIcon = "#{options.staticWebDir}/images/weinre-icon-32x32.png"

    staticCacheOptions =
        maxObjects: 500
        maxLength:  32 * 1024 * 1024
        
    app = express.createServer()
    
    app.on 'error', (error) ->
        utils.exit "error running server: #{error}"

    app.use express.favicon(favIcon)

    app.use jsonBodyParser()

    app.all /^\/ws\/client(.*)/, (request, response, next) ->
        uri = request.params[0]
        uri = '/' if uri == ''
        
        dumpingHandler(request, response, uri) if options.debug
        clientHandler.handle(request, response, uri)

    app.all /^\/ws\/target(.*)/, (request, response, next) ->
        uri = request.params[0]
        uri = '/' if uri == ''

        dumpingHandler(request, response, uri) if options.debug
        targetHandler.handle(request, response, uri)

    app.use express.errorHandler(dumpExceptions: true)

    app.use express.staticCache(staticCacheOptions)
    app.use express.static(options.staticWebDir)
    
    if options.boundHost == '-all-'
        utils.log "starting server at http://localhost:#{options.httpPort}"
        app.listen options.httpPort
        
    else
        utils.log "starting server at http://#{options.boundHost}:#{options.httpPort}"
        app.listen options.httpPort, options.boundHost

#-------------------------------------------------------------------------------
getStaticWebDir = () ->
    webDir = path.normalize path.join(__dirname,'../web')
    return webDir if utils.fileExistsSync webDir
    
    utils.exit 'unable to find static files to serve in #{webDir}; did you do a build?'
    
#-------------------------------------------------------------------------------
Version = null
getVersion = exports.getVersion = () ->
    return Version if Version 

    packageJsonName  = path.join(path.dirname(fs.realpathSync(__filename)), '../package.json')

    json = fs.readFileSync(packageJsonName, 'utf8')
    values = JSON.parse(json)

    Version = values.version
    return Version
