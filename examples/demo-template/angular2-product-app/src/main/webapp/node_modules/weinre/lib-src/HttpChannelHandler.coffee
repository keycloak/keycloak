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
Channel        = require './Channel'
channelManager = require './channelManager'

#-------------------------------------------------------------------------------
module.exports = utils.registerClass class HttpChannelHandler

    #---------------------------------------------------------------------------
    constructor: (@pathPrefix) ->
    
        if @pathPrefix == '/ws/client'
            @isClient = true
            
        else if @pathPrefix == '/ws/target'
            @isClient = false
            
        else
            utils.pitch "invalid pathPrefix: #{@pathPrefix}"
            
        @isTarget = !@isClient
        
    #---------------------------------------------------------------------------
    handle: (request, response, uri) ->
    
        setCORSHeaders  request, response
        setCacheHeaders request, response

        #-----------------
        
        # * #{pathPrefix}a
        if uri[0] != '/'
            return handleError(request, response, 404)
            
        #-----------------
        
        if uri == '/'
        
            # OPTIONS #{pathPrefix}/
            if request.method == 'OPTIONS'
                return handleOptions(request, response)

            # POST #{pathPrefix}/
            if request.method == 'POST'
                return handleCreate(@pathPrefix, @isClient, request, response)
                
            # * #{pathPrefix}/
            return handleError(request, response, 405)
            
        #-----------------
            
        parts = uri.split('/')
        
        # * #{pathPrefix}/x/y
        if parts.length > 2
            return handleError(request, response, 404)

        #-----------------
        
        channelName = parts[1]
        
        # OPTIONS #{pathPrefix}/x
        if request.method == 'OPTIONS'
            return handleOptions(request, response)

        # GET #{pathPrefix}/x
        if request.method == 'GET'
            return handleGet(request, response, channelName)
        
        # POST #{pathPrefix}/x
        if request.method == 'POST'
            return handlePost(request, response, channelName)
        
        # anything else
        return handleError(request, response, 405)

#-------------------------------------------------------------------------------
handleCreate = (pathPrefix, isClient, request, response) ->
    id = request.body?.id
    
    remoteAddress = request.connection?.remoteAddress || ""
    
    channel = new Channel(pathPrefix, id, remoteAddress, isClient)
    
    response.contentType 'application/json'
    response.send JSON.stringify
        channel: channel.name
        id:      channel.id

#-------------------------------------------------------------------------------
handleGet = (request, response, channelName) ->
    remoteAddress = request.connection?.remoteAddress || ""
    channel       = channelManager.getChannel(channelName, remoteAddress)
    return handleError(request, response, 404) if !channel
    
    channel.getMessages (messages) => 
        return handleError(request, response, 404) if channel.isClosed
        return handleError(request, response, 404) if !messages
        
        response.contentType 'application/json'
        response.send JSON.stringify(messages)

#-------------------------------------------------------------------------------
handlePost = (request, response, channelName) ->
    remoteAddress = request.connection?.remoteAddress || ""
    channel       = channelManager.getChannel(channelName, remoteAddress)
    return handleError(request, response, 404) if !channel
    
    channel.handleMessages(request.body)
    response.send('')

#-------------------------------------------------------------------------------
handleOptions = (request, response) ->
    response.send('')

#-------------------------------------------------------------------------------
handleError = (request, response, status) ->
    response.send(status)

#-------------------------------------------------------------------------------
setCORSHeaders = (request, response) ->
    origin = request.header 'Origin'
    return if !origin
    
    response.header 'Access-Control-Allow-Origin',  origin
    response.header 'Access-Control-Max-Age',       '600'
    response.header 'Access-Control-Allow-Methods', 'GET, POST'

#-------------------------------------------------------------------------------
setCacheHeaders = (request, response) ->
    response.header 'Pragma',        'no-cache'
    response.header 'Expires',       '0'
    response.header 'Cache-Control', 'no-cache'
    response.header 'Cache-Control', 'no-store'
