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

#-------------------------------------------------------------------------------
jsonBodyParser = -> 
    return (request, response, next) -> parseBodyAsJSON(request, response, next)
        
#-------------------------------------------------------------------------------
parseBodyAsJSON = (request, response, next) ->

    return next() if request.body
    
    request.body = {}
    
    return next() if request.method != 'POST'
    
    request.setEncoding 'utf8'
    
    buffer = ''
    request.on 'data', (chunk) -> buffer += chunk
    request.on 'end', ->
        return next() if '' == buffer

        try 
            request.body = JSON.parse(buffer)
            next()
        catch e
            next(e)

#-------------------------------------------------------------------------------
module.exports = jsonBodyParser
