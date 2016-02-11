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

_  = require 'underscore'

utils          = require './utils'
serviceManager = require './serviceManager'

WeinreClientEvents = null
WeinreTargetEvents = null

channelManager = null

#-------------------------------------------------------------------------------
utils.registerClass class ChannelManager

    #---------------------------------------------------------------------------
    constructor: () ->
        @channels  = {}

    #---------------------------------------------------------------------------
    initialize: ->

        WeinreClientEvents = serviceManager.get 'WeinreClientEvents'
        WeinreTargetEvents = serviceManager.get 'WeinreTargetEvents'

        if !WeinreClientEvents
            utils.exit 'WeinreClientEvents service not registered'

        if !WeinreTargetEvents
            utils.exit 'WeinreTargetEvents service not registered'

    #---------------------------------------------------------------------------
    created: (channel) ->
        @channels[channel.name] = channel

    #---------------------------------------------------------------------------
    destroyed: (channel) ->
        if channel.isClient
            for connection in channel.connections
                @disconnectChannels(channel, connection)
        else
            for connection in channel.connections
                @disconnectChannels(connection, channel)

        clients = @getClientChannels(channel.id)

        if channel.isClient
            WeinreClientEvents.clientUnregistered(clients, channel.name)
        else
            WeinreClientEvents.targetUnregistered(clients, channel.name)

        delete @channels[channel.name]

    #---------------------------------------------------------------------------
    getChannel: (name, remoteAddress) ->
        return null if !_.has(@channels, name)

        channel = @channels[name]

        return null if !channel

#        if remoteAddress
#            return null if channel.remoteAddress != remoteAddress

        channel

    #---------------------------------------------------------------------------
    connectChannels: (client, target) ->
        return if client.isClosed or target.isClosed

        if client.connections.length
            @disconnectChannels(client, client.connections[0])

        client.connections.push target
        target.connections.push client

        clients = @getClientChannels(client.id)

        WeinreClientEvents.connectionCreated(clients, client.name, target.name)
        WeinreTargetEvents.connectionCreated(target,  client.name, target.name)

    #---------------------------------------------------------------------------
    disconnectChannels: (client, target) ->

        clients = @getClientChannels(client.id)

        WeinreClientEvents.connectionDestroyed(clients, client.name, target.name)
        WeinreTargetEvents.connectionDestroyed(target,  client.name, target.name)

        client.connections = _.without(client.connections, target)
        target.connections = _.without(target.connections, client)

    #---------------------------------------------------------------------------
    getChannels: (id) ->
        if id?
            _.filter(@channels, (item) -> item.id == id)
        else
            _.values(@channels)

    #---------------------------------------------------------------------------
    getClientChannels: (id) ->
        _.filter(@channels, (item) -> item.isClient && item.id == id)

    #---------------------------------------------------------------------------
    getTargetChannels: (id) ->
        _.filter(@channels, (item) -> item.isTarget && item.id == id)

#-------------------------------------------------------------------------------
module.exports = new ChannelManager
