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

utils = require './utils'

#-------------------------------------------------------------------------------
module.exports = utils.registerClass class MessageQueue

    #---------------------------------------------------------------------------
    constructor: () ->
        @messages     = []
        @closed       = false
        @callback     = null
        @timer        = null
        
        _.bindAll @, '_timerExpired', '_updated'
        
    #---------------------------------------------------------------------------
    shutdown: () ->
        return if @closed

        @closed = true

        clearTimeout @timer if @timer
        @callback.call(null, @messages) if @callback
        
        @callback = null
        @messages = null 
        @timer    = null
    
    #---------------------------------------------------------------------------
    push: (message) ->
        return if @closed
        
        @messages.push message
        process.nextTick @_updated

    #---------------------------------------------------------------------------
    pullAll: (timeout, callback) ->
        return callback.call(null, null) if @closed
        return callback.call(null, [])   if @callback
        
        if @messages.length
            callback.call(null, @messages)
            @messages = []
            return
        
        @callback = callback
        @timer    = setTimeout @_timerExpired, timeout

    #---------------------------------------------------------------------------
    _timerExpired: () ->
        @_updated()
        
    #---------------------------------------------------------------------------
    _updated: () ->
        return if @closed
        return if !@callback
        
        callback = @callback
        messages = @messages
        clearTimeout @timer if @timer
        
        @callback = null
        @messages = []
        @timer    = null

        callback.call(null, messages)      

