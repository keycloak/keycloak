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
dumpingHandler = (request, response, uri) ->
    originalSend = response.send
    response.send = (body) ->
        dumpResponse(originalSend, body, request, response, uri)
    
    return if request.method != 'POST'
    
    utils.logVerbose '--------------------------------------------------'
    utils.logVerbose "#{request.method} #{uri} [request]"
    
    if _.isArray(request.body)
        for element in request.body
            utils.logVerbose "   #{enhance(JSON.parse(element))}"
    else
        utils.logVerbose "   #{enhance(request.body)}"

#-------------------------------------------------------------------------------
dumpResponse = (originalSend, body, request, response, uri) ->
    originalSend.call(response, body)

    return if request.method not in ['GET', 'POST']

    try 
        body = JSON.parse(body)
    catch e
        return

    return if _.isArray(body) && (body.length == 0)
        
    utils.logVerbose '--------------------------------------------------'
    utils.logVerbose "#{request.method} #{uri} #{response.statusCode} [response]"
    
    if _.isArray(body)
        for element in body
            utils.logVerbose "   #{enhance(JSON.parse(element))}"
    else
        utils.logVerbose "   #{enhance(body)}"

#-------------------------------------------------------------------------------
enhance = (object) ->
    if !object.interface || !object.method || !object.args 
        return JSON.stringify(object)
        
    signature = "#{object.interface}.#{object.method}"

    args = JSON.stringify(object.args)
    if args.length > 500
        args = "#{args.substr(0,50)}..."
    
    return "#{signature}(#{args})"

#-------------------------------------------------------------------------------
module.exports = dumpingHandler
