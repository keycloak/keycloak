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

_ = require 'underscore'

utils          = require './utils'
channelManager = require './channelManager'
messageHandler = require './messageHandler'
MessageQueue   = require './MessageQueue'

AnonymousId = 'anonymous'

#-------------------------------------------------------------------------------
module.exports = utils.registerClass class Channel
    
    #---------------------------------------------------------------------------
    constructor: (@pathPrefix, @id, @remoteAddress, @isClient) ->
        prefix         = if @isClient then 'c-' else 't-'
        @name          = "#{prefix}#{utils.getNextSequenceNumber()}"
        @messageQueue  = new MessageQueue
        @isClosed      = false
        @connections   = []
        @isTarget      = !@isClient
        @readTimeout   = utils.options.readTimeout * 1000
        
        @id = AnonymousId if !@id 
        
        @description = 
            channel:       @name
            id:            @id
            hostName:      @remoteAddress
            remoteAddress: @remoteAddress
        
        @updateLastRead()

        channelManager.created @

    #---------------------------------------------------------------------------
    close: () ->
        return if @isClosed
        
        channelManager.destroyed @
        
        @isClosed = true
        @messageQueue.shutdown()

    #---------------------------------------------------------------------------
    sendCallback: (intfName, callbackId, args...) ->
        return if !callbackId
        
        args.unshift callbackId
        
        @sendMessage intfName, 'sendCallback', args...
        
    #---------------------------------------------------------------------------
    sendMessage: (intfName, method, args...) ->

        message = genJSON
            interface: intfName
            method:    method
            args:      args
        
        @messageQueue.push message

    #---------------------------------------------------------------------------
    handleMessages: (messages) ->

        for message in messages
            message = parseJSON(message)
            continue if !message
            
            messageHandler.handleMessage @, message
        
    #---------------------------------------------------------------------------
    getMessages: (callback) ->
        @updateLastRead()
        return callback.call(null, null) if @isClosed
        
        @messageQueue.pullAll @readTimeout, callback
        
    #---------------------------------------------------------------------------
    updateLastRead: () ->
        @lastRead = (new Date).valueOf()

    #---------------------------------------------------------------------------
    toString: () ->
        connections = _.map(@connections, (val) -> val.name).join(',')
        "Channel(#{@name}, closed:#{@isClosed}, connections:[#{connections}])"

#-------------------------------------------------------------------------------
parseJSON = (message) ->
    try 
        return JSON.parse(message)
    catch e
        return null

#-------------------------------------------------------------------------------
genJSON = (message) ->
    try 
        return JSON.stringify(message)
    catch e
        return null
