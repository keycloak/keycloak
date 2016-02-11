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

fs   = require 'fs'
path = require 'path'

utils = exports

utils.Program = Program = path.basename process.argv[1]

SequenceNumberMax = 100 * 1024 * 1024
SequenceNumber    = 0

#-------------------------------------------------------------------------------
utils.getNextSequenceNumber = (g) -> 
    SequenceNumber++
    
    if SequenceNumber > SequenceNumberMax
        SequenceNumber = 0
        
    SequenceNumber

#-------------------------------------------------------------------------------
utils.trim = (string) -> 
    string.replace(/(^\s+)|(\s+$)/g,'')

#-------------------------------------------------------------------------------
utils.log = log = (message) ->
    date    = new Date()
    time    = date.toISOString()
    console.log "#{time} #{Program}: #{message}"

#-------------------------------------------------------------------------------
utils.logVerbose = (message) ->
    return if !utils?.options?.verbose

    log message

#-------------------------------------------------------------------------------
utils.logDebug = (message) ->
    return if !utils?.options?.debug

    log message

#-------------------------------------------------------------------------------
utils.exit = (message) ->
    log message
    process.exit 1

#-------------------------------------------------------------------------------
utils.pitch = (message) ->
    log message
    throw message

#-------------------------------------------------------------------------------
utils.setOptions = (options) ->
    utils.options = options
    
#-------------------------------------------------------------------------------
utils.ensureInteger = (value, message) ->
    newValue = parseInt value
    
    if isNaN newValue
        utils.exit "#{message}: '#{value}'"
    
    newValue    
    
#-------------------------------------------------------------------------------
utils.ensureString = (value, message) ->
    
    if typeof value != 'string'
        utils.exit "#{message}: '#{value}'"
    
    value    
    
#-------------------------------------------------------------------------------
utils.ensureBoolean = (value, message) ->
    uValue = value.toString().toUpperCase()

    newValue = null    
    switch uValue
        when 'TRUE'  then newValue = true
        when 'FALSE' then newValue = false
    
    if typeof(newValue) != 'boolean'
        utils.exit "#{message}: '#{value}'"
    
    newValue

#-------------------------------------------------------------------------------
utils.setNamesForClass = (aClass) ->

    for own key, val of aClass
        if typeof(val) is "function"
            val.signature   = "#{aClass.name}::#{key}"
            val.displayName = val.signature
            val.name        = val.signature

    for own key, val of aClass.prototype
        if typeof(val) is "function"
            val.signature   = "#{aClass.name}.#{key}"
            val.displayName = val.signature
            val.name        = val.signature

#-------------------------------------------------------------------------------
utils.registerClass = (aClass) ->
    utils.setNamesForClass(aClass)
    aClass

#-------------------------------------------------------------------------------
utils.alignLeft = (string, length) ->
    while string.length < length
        string = "#{string} "
        
    string

#-------------------------------------------------------------------------------
utils.alignRight = (string, length) ->
    while string.length < length
        string = " #{string}"

    string

#-------------------------------------------------------------------------------
utils.fileExistsSync = (name) ->
    
    if fs.existsSync
        return fs.existsSync name
        
    return path.existsSync(name)

#-------------------------------------------------------------------------------
Error.prepareStackTrace = (error, structuredStackTrace) ->
    result = []
    result.push "---------------------------------------------------------"
    result.push "error: #{error}"
    result.push "---------------------------------------------------------"
    result.push "stack: "

    longestFile = 0
    longestLine = 0
    
    for callSite in structuredStackTrace
        file = callSite.getFileName()
        line = callSite.getLineNumber()

        file = path.basename(file)
        line = "#{line}"
        
        if file.length > longestFile
            longestFile = file.length
    
        if line.length > longestLine
            longestLine = line.length
    
    for callSite in structuredStackTrace
        func = callSite.getFunction()
        file = callSite.getFileName()
        line = callSite.getLineNumber()

        file = path.basename(file)
        line = "#{line}"
        
        file = utils.alignRight(file, longestFile)
        line = utils.alignLeft( line, longestLine)
        
        funcName = func.displayName ||
                   func.name || 
                   callSite.getFunctionName()
                   callSite.getMethodName()
                   '???'
        
        if funcName == "Module._compile"
            result.pop()
            result.pop()
            break
            
        result.push "   #{file}:#{line} - #{funcName}()"
        
    result.join "\n"
