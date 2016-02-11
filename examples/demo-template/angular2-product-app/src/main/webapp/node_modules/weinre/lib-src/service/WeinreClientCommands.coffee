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

_ = require('underscore')

weinre             = require '../weinre'
utils              = require '../utils'
channelManager     = require '../channelManager'
serviceManager     = require '../serviceManager'
extensionManager   = require '../extensionManager'

WeinreClientEvents = serviceManager.get 'WeinreClientEvents'

#-------------------------------------------------------------------------------
module.exports = utils.registerClass class WeinreClientCommands

    #---------------------------------------------------------------------------
    registerClient: (channel, callbackId) ->
        if callbackId
            WeinreClientEvents.sendCallback(channel, callbackId, channel.description)        

        options = _.extend {}, utils.options
        for own key, val of options
            if typeof val in ['number', 'boolean']
                options[key] = "#{val}"
        
        options.version = weinre.getVersion()
        
        WeinreClientEvents.serverProperties(channel, options)

        clients = channelManager.getClientChannels(channel.id)
        WeinreClientEvents.clientRegistered(clients, channel.description)

    #---------------------------------------------------------------------------
    getTargets: (channel, callbackId) ->
        channels = channelManager.getTargetChannels(channel.id)
        result = _.pluck(channels, 'description')

        if callbackId
            WeinreClientEvents.sendCallback(channel, callbackId, [result])

    #---------------------------------------------------------------------------
    getClients: (channel, callbackId) ->
        channels = channelManager.getClientChannels(channel.id)
        result = _.pluck(channels, 'description')

        if callbackId
            WeinreClientEvents.sendCallback(channel, callbackId, [result])

    #---------------------------------------------------------------------------
    getExtensions: (channel, callbackId) ->
        result = for extension in extensionManager.extensions
            { startPage: "extensions/#{extension}/extension.html" }

        if callbackId
            WeinreClientEvents.sendCallback(channel, callbackId, [result])

    #---------------------------------------------------------------------------
    connectTarget: (channel, clientName, targetName, callbackId) ->
        client = channelManager.getChannel(clientName)
        return if !client

        target = channelManager.getChannel(targetName)
        return if !target

        channelManager.connectChannels(client, target)

        if callbackId
            WeinreClientEvents.sendCallback(channel, callbackId)
            
    #---------------------------------------------------------------------------
    disconnectTarget: (channel, clientName, callbackId) ->
        client = connectionManager.getClient(clientName)
        return if !client

        target = client.getConnectedTarget()
        return if !target

        connectionManager.disconnect(client, target)

        if callbackId
            WeinreClientEvents.sendCallback(channel, callbackId)
            
    #---------------------------------------------------------------------------
    logDebug: (channel, message, callbackId) ->
        utils.logVerbose "client #{channel.name}: #{message}"

        if callbackId
            WeinreClientEvents.sendCallback(channel, callbackId)
            
    #---------------------------------------------------------------------------
    logInfo: (channel, message, callbackId) ->
        utils.log "client #{channel.name}: #{message}"

        if callbackId
            WeinreClientEvents.sendCallback(channel, callbackId)
            
    #---------------------------------------------------------------------------
    logWarning: (channel, message, callbackId) ->
        utils.log "client #{channel.name}: #{message}"

        if callbackId
            WeinreClientEvents.sendCallback(channel, callbackId)
            
    #---------------------------------------------------------------------------
    logError: (channel, message, callbackId) ->
        utils.log "client #{channel.name}: #{message}"

        if callbackId
            WeinreClientEvents.sendCallback(channel, callbackId)
