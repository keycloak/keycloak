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

utils             = require './utils'
channelManager    = require './channelManager'
serviceManager    = require './serviceManager'

#-------------------------------------------------------------------------------
utils.registerClass class MessageHandler

    #---------------------------------------------------------------------------
    handleMessage: (channel, message) ->
        @_serviceMethodInvoker(channel, message.interface, message.method, message.args)

    #---------------------------------------------------------------------------
    _serviceMethodInvoker: (channel, intfName, method, args) ->
        methodSignature = "#{intfName}.#{method}()"
        # utils.logVerbose "MessageHandler._serviceMethodInvoker(#{methodSignature})"

        service = serviceManager.get(intfName)
        
        if !service
            return @_redirectToConnections(channel, intfName, method, args)

        args = args.slice()
        args.unshift channel
        
        try 
            service[method].apply(service, args)
            
        catch e
            utils.log "error running service method #{methodSignature}: #{e}"
            utils.log "stack:\n#{e.stack}"

    #---------------------------------------------------------------------------
    _redirectToConnections: (channel, intfName, method, args) ->
        # utils.logVerbose "MessageHandler._redirectToConnections(#{channel.name}, #{intfName}, #{method})"

        for connection in channel.connections
            connection.sendMessage(intfName, method, args...)
            
#-------------------------------------------------------------------------------
module.exports = new MessageHandler
