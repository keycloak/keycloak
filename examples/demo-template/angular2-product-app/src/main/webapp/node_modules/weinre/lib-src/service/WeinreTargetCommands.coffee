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

utils              = require '../utils'
channelManager     = require '../channelManager'
serviceManager     = require '../serviceManager'

WeinreClientEvents = serviceManager.get 'WeinreClientEvents'
WeinreTargetEvents = serviceManager.get 'WeinreTargetEvents'

#-------------------------------------------------------------------------------
module.exports = utils.registerClass class WeinreTargetCommands

    #---------------------------------------------------------------------------
    registerTarget: (channel, url, callbackId) -> 
        channel.description.url = url

        clients = channelManager.getClientChannels(channel.id)
        WeinreClientEvents.targetRegistered(clients, channel.description)
    
        if callbackId
            WeinreTargetEvents.sendCallback(channel, callbackId, channel.description)

    #---------------------------------------------------------------------------
    sendClientCallback: (channel, clientCallbackId, args, callbackId) ->

        # the channel to send the callback to is embedded in the callbackId
        callbackChannel = getCallbackChannel(clientCallbackId)
        if !callbackChannel
            return main.warn "#{@constructor.name}.sendClientCallback() sent with invalid callbackId: #{clientCallbackId}"

        callbackChannel = channelManager.getChannel(callbackChannel)
        if !callbackChannel
            # indication that channel was closed; this message may generate a lot of noise
            return main.warn "#{@constructor.name}.sendClientCallback() unable to find channel : #{clientCallbackId}"

        WeinreClientEvents.sendCallback(callbackChannel, clientCallbackId, args)
            
        if callbackId
            WeinreTargetEvents.sendCallback(channel, callbackId, description)

    #---------------------------------------------------------------------------
    logDebug: (channel, message, callbackId) ->
        utils.logVerbose "target #{channel.name}: #{message}"

        if callbackId
            WeinreTargetEvents.sendCallback(channel, callbackId, description)

    #---------------------------------------------------------------------------
    logInfo: (channel, message, callbackId) ->
        utils.log "target #{channel.name}: #{message}"

        if callbackId
            WeinreTargetEvents.sendCallback(channel, callbackId, description)

    #---------------------------------------------------------------------------
    logWarning: (channel, message, callbackId) ->
        utils.log "target #{channel.name}: #{message}"

        if callbackId
            WeinreTargetEvents.sendCallback(channel, callbackId, description)

    #---------------------------------------------------------------------------
    logError: (channel, message, callbackId) ->
        utils.log "target #{channel.name}: #{message}"

        if callbackId
            WeinreTargetEvents.sendCallback(channel, callbackId, description)

#---------------------------------------------------------------------------
getCallbackChannel = (callbackId) ->
    callbackId = callbackId.toString()
    callbackId.split('::')[0]

