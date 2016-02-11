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

path = require 'path'
fs   = require 'fs'

_ = require 'underscore'

utils = require './utils'

Services = {}

#-------------------------------------------------------------------------------
utils.registerClass class ServiceManager
    
    #---------------------------------------------------------------------------
    constructor: ->
        @services = {}

    #---------------------------------------------------------------------------
    get: (name) ->
        return @services[name] if _.has(@services, name)
        
        null

    #---------------------------------------------------------------------------
    registerLocalClass: (name) ->

        serviceClass = null
        try
            serviceClass = require "./service/#{name}"
        catch e
            utils.log "local service class not found: #{name}"
            throw e

        @services[name] = new serviceClass
    
    #---------------------------------------------------------------------------
    registerProxyClass: (name) ->
    
        intf = getServiceInterface(name)
        
        if !intf
            utils.exit "proxy service class not found: #{name}"
            
        if intf.name != name
            utils.exit "proxy interface '#{intf.name}' loaded when '#{name}' requested"
            
        service = {}
        
        for method in intf.methods
            service[method.name] = getMethodProxy(name, method.name)    
            
        @services[name] = service


#-------------------------------------------------------------------------------
getMethodProxy = (intfName, methodName) ->
    (channels, args...) ->
        channels = [channels] if !_.isArray(channels) 
        
        for channel in channels
            channel.sendMessage(intfName, methodName, args...)
    
#-------------------------------------------------------------------------------
getServiceInterface = (name) ->
    jsonName = "#{name}.json"
    fileName = path.join utils.options.staticWebDir, 'interfaces', jsonName
    
    return null if !utils.fileExistsSync(fileName) 
    
    contents = fs.readFileSync(fileName, 'utf8')
    
    serviceInterface = JSON.parse(contents)
    
    return serviceInterface.interfaces[0]

#-------------------------------------------------------------------------------
module.exports = new ServiceManager