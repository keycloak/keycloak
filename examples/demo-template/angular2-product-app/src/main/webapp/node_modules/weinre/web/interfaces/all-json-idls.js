modjewel.require('weinre/common/Weinre').addIDLs([
    {
        "interfaces": [
            {
                "extendedAttributes": {
                    "Conditional": "INSPECTOR"
                }, 
                "name": "InjectedScriptHost", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "clearConsoleMessages", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "copyText", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "DOMString", 
                                    "name": "string"
                                }, 
                                "name": "text"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "originalName": "DOMObject", 
                            "name": "any"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "Custom": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }
                        ], 
                        "name": "nodeForId"
                    }, 
                    {
                        "returns": {
                            "name": "int"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "Custom": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "DOMObject", 
                                    "name": "any"
                                }, 
                                "name": "node"
                            }, 
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "withChildren"
                            }, 
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "selectInUI"
                            }
                        ], 
                        "name": "pushNodePathToFrontend"
                    }, 
                    {
                        "returns": {
                            "originalName": "long", 
                            "name": "int"
                        }, 
                        "callbackParameters": [], 
                        "name": "inspectedNode", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "unsigned", 
                                    "name": "int"
                                }, 
                                "name": "num"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "originalName": "DOMObject", 
                            "name": "any"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "Custom": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "DOMObject", 
                                    "name": "any"
                                }, 
                                "name": "object"
                            }
                        ], 
                        "name": "internalConstructorName"
                    }, 
                    {
                        "returns": {
                            "originalName": "DOMObject", 
                            "name": "any"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "Custom": true
                        }, 
                        "parameters": [], 
                        "name": "currentCallFrame"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "Custom": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "DOMObject", 
                                    "name": "any"
                                }, 
                                "name": "database"
                            }
                        ], 
                        "name": "selectDatabase"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "Custom": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "DOMObject", 
                                    "name": "any"
                                }, 
                                "name": "storage"
                            }
                        ], 
                        "name": "selectDOMStorage"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "didCreateWorker", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "id"
                            }, 
                            {
                                "type": {
                                    "originalName": "DOMString", 
                                    "name": "string"
                                }, 
                                "name": "url"
                            }, 
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "isFakeWorker"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "didDestroyWorker", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "id"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "originalName": "long", 
                            "name": "int"
                        }, 
                        "callbackParameters": [], 
                        "name": "nextWorkerId", 
                        "parameters": []
                    }
                ]
            }
        ], 
        "name": "core"
    }, 
    {
        "interfaces": [
            {
                "extendedAttributes": {
                    "Conditional": "INSPECTOR"
                }, 
                "name": "Inspector", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "addScriptToEvaluateOnLoad", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "scriptSource"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "removeAllScriptsToEvaluateOnLoad", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "reloadPage", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "ignoreCache"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "populateScriptObjects", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "openInInspectedWindow", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "url"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "newState", 
                                "out": true
                            }
                        ], 
                        "name": "setSearchingForNode", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "enabled"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "didEvaluateForTestInFrontend", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "testCallId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "jsonResult"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "highlightDOMNode", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "hideDOMNodeHighlight", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "highlightFrame", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "unsigned", 
                                    "name": "int"
                                }, 
                                "name": "frameId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "hideFrameHighlight", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "setUserAgentOverride", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "userAgent"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "any", 
                                    "rank": 1
                                }, 
                                "name": "cookies", 
                                "out": true
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "cookiesString", 
                                "out": true
                            }
                        ], 
                        "name": "getCookies", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "deleteCookie", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "cookieName"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "domain"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "startTimelineProfiler", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "stopTimelineProfiler", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "enableDebugger", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "disableDebugger", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "enableProfiler", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "disableProfiler", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "startProfiling", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "stopProfiling", 
                        "parameters": []
                    }
                ]
            }, 
            {
                "extendedAttributes": {
                    "Conditional": "INSPECTOR"
                }, 
                "name": "Runtime", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "result", 
                                "out": true
                            }
                        ], 
                        "name": "evaluate", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "expression"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "objectGroup"
                            }, 
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "includeCommandLineAPI"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "result", 
                                "out": true
                            }
                        ], 
                        "name": "getCompletions", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "expression"
                            }, 
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "includeCommandLineAPI"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "result", 
                                "out": true
                            }
                        ], 
                        "name": "getProperties", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "objectId"
                            }, 
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "ignoreHasOwnProperty"
                            }, 
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "abbreviate"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "result", 
                                "out": true
                            }
                        ], 
                        "name": "setPropertyValue", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "objectId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "propertyName"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "expression"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "releaseWrapperObjectGroup", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "injectedScriptId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "objectGroup"
                            }
                        ]
                    }
                ]
            }, 
            {
                "extendedAttributes": {
                    "Conditional": "INSPECTOR"
                }, 
                "name": "InjectedScript", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "result", 
                                "out": true
                            }
                        ], 
                        "name": "evaluateOnSelf", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "functionBody"
                            }, 
                            {
                                "type": {
                                    "name": "any", 
                                    "rank": 1
                                }, 
                                "name": "argumentsArray"
                            }
                        ]
                    }
                ]
            }, 
            {
                "extendedAttributes": {
                    "Conditional": "INSPECTOR"
                }, 
                "name": "Console", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "newState", 
                                "out": true
                            }
                        ], 
                        "name": "setConsoleMessagesEnabled", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "enabled"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "clearConsoleMessages", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "setMonitoringXHREnabled", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "enabled"
                            }
                        ]
                    }
                ]
            }, 
            {
                "extendedAttributes": {
                    "Conditional": "INSPECTOR"
                }, 
                "name": "Network", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "resources", 
                                "out": true
                            }
                        ], 
                        "name": "cachedResources", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "success", 
                                "out": true
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "content", 
                                "out": true
                            }
                        ], 
                        "name": "resourceContent", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "unsigned", 
                                    "name": "int"
                                }, 
                                "name": "frameId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "url"
                            }, 
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "base64Encode"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "setExtraHeaders", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "headers"
                            }
                        ]
                    }
                ]
            }, 
            {
                "extendedAttributes": {
                    "Conditional": "INSPECTOR"
                }, 
                "name": "Database", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "any", 
                                    "rank": 1
                                }, 
                                "name": "tableNames", 
                                "out": true
                            }
                        ], 
                        "name": "getDatabaseTableNames", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "databaseId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "success", 
                                "out": true
                            }, 
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "transactionId", 
                                "out": true
                            }
                        ], 
                        "name": "executeSQL", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "databaseId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "query"
                            }
                        ]
                    }
                ]
            }, 
            {
                "extendedAttributes": {
                    "Conditional": "INSPECTOR"
                }, 
                "name": "DOMStorage", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "any", 
                                    "rank": 1
                                }, 
                                "name": "entries", 
                                "out": true
                            }
                        ], 
                        "name": "getDOMStorageEntries", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "storageId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "success", 
                                "out": true
                            }
                        ], 
                        "name": "setDOMStorageItem", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "storageId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "key"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "value"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "success", 
                                "out": true
                            }
                        ], 
                        "name": "removeDOMStorageItem", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "storageId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "key"
                            }
                        ]
                    }
                ]
            }, 
            {
                "extendedAttributes": {
                    "Conditional": "INSPECTOR"
                }, 
                "name": "ApplicationCache", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "applicationCaches", 
                                "out": true
                            }
                        ], 
                        "name": "getApplicationCaches", 
                        "parameters": []
                    }
                ]
            }, 
            {
                "extendedAttributes": {
                    "Conditional": "INSPECTOR"
                }, 
                "name": "DOM", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "getChildNodes", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "success", 
                                "out": true
                            }
                        ], 
                        "name": "setAttribute", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "elementId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "name"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "value"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "success", 
                                "out": true
                            }
                        ], 
                        "name": "removeAttribute", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "elementId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "name"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "success", 
                                "out": true
                            }
                        ], 
                        "name": "setTextNodeValue", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "value"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "outNodeId", 
                                "out": true
                            }, 
                            {
                                "type": {
                                    "name": "any", 
                                    "rank": 1
                                }, 
                                "name": "listenersArray", 
                                "out": true
                            }
                        ], 
                        "name": "getEventListenersForNode", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "copyNode", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "outNodeId", 
                                "out": true
                            }
                        ], 
                        "name": "removeNode", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "outNodeId", 
                                "out": true
                            }
                        ], 
                        "name": "changeTagName", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "newTagName"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "outerHTML", 
                                "out": true
                            }
                        ], 
                        "name": "getOuterHTML", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "outNodeId", 
                                "out": true
                            }
                        ], 
                        "name": "setOuterHTML", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "outerHTML"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "addInspectedNode", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "performSearch", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "query"
                            }, 
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "runSynchronously"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "searchCanceled", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId", 
                                "out": true
                            }
                        ], 
                        "name": "pushNodeByPathToFrontend", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "path"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "result", 
                                "out": true
                            }
                        ], 
                        "name": "resolveNode", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "result", 
                                "out": true
                            }
                        ], 
                        "name": "getNodeProperties", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }, 
                            {
                                "type": {
                                    "name": "any", 
                                    "rank": 1
                                }, 
                                "name": "propertiesArray"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "result", 
                                "out": true
                            }
                        ], 
                        "name": "getNodePrototypes", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "result", 
                                "out": true
                            }
                        ], 
                        "name": "pushNodeToFrontend", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "objectId"
                            }
                        ]
                    }
                ]
            }, 
            {
                "extendedAttributes": {
                    "Conditional": "INSPECTOR"
                }, 
                "name": "CSS", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "styles", 
                                "out": true
                            }
                        ], 
                        "name": "getStylesForNode", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "style", 
                                "out": true
                            }
                        ], 
                        "name": "getComputedStyleForNode", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "style", 
                                "out": true
                            }
                        ], 
                        "name": "getInlineStyleForNode", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "any", 
                                    "rank": 1
                                }, 
                                "name": "styleSheetIds", 
                                "out": true
                            }
                        ], 
                        "name": "getAllStyles", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "styleSheet", 
                                "out": true
                            }
                        ], 
                        "name": "getStyleSheet", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "styleSheetId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "url", 
                                "out": true
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "text", 
                                "out": true
                            }
                        ], 
                        "name": "getStyleSheetText", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "styleSheetId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "success", 
                                "out": true
                            }
                        ], 
                        "name": "setStyleSheetText", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "styleSheetId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "text"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "style", 
                                "out": true
                            }
                        ], 
                        "name": "setPropertyText", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "styleId"
                            }, 
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "propertyIndex"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "text"
                            }, 
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "overwrite"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "style", 
                                "out": true
                            }
                        ], 
                        "name": "toggleProperty", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "styleId"
                            }, 
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "propertyIndex"
                            }, 
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "disable"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "rule", 
                                "out": true
                            }
                        ], 
                        "name": "setRuleSelector", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "ruleId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "selector"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "rule", 
                                "out": true
                            }
                        ], 
                        "name": "addRule", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "contextNodeId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "selector"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "any", 
                                    "rank": 1
                                }, 
                                "name": "cssProperties", 
                                "out": true
                            }
                        ], 
                        "name": "getSupportedCSSProperties", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "any", 
                                    "rank": 1
                                }, 
                                "name": "result", 
                                "out": true
                            }
                        ], 
                        "name": "querySelectorAll", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "documentId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "selector"
                            }
                        ]
                    }
                ]
            }, 
            {
                "extendedAttributes": {
                    "Conditional": "INSPECTOR"
                }, 
                "name": "Timeline", 
                "methods": []
            }, 
            {
                "extendedAttributes": {
                    "Conditional": "INSPECTOR"
                }, 
                "name": "Debugger", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "activateBreakpoints", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "deactivateBreakpoints", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "breakpointId", 
                                "out": true
                            }, 
                            {
                                "type": {
                                    "name": "any", 
                                    "rank": 1
                                }, 
                                "name": "locations", 
                                "out": true
                            }
                        ], 
                        "name": "setJavaScriptBreakpoint", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "url"
                            }, 
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "lineNumber"
                            }, 
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "columnNumber"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "condition"
                            }, 
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "enabled"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "breakpointId", 
                                "out": true
                            }, 
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "actualLineNumber", 
                                "out": true
                            }, 
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "actualColumnNumber", 
                                "out": true
                            }
                        ], 
                        "name": "setJavaScriptBreakpointBySourceId", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "sourceId"
                            }, 
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "lineNumber"
                            }, 
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "columnNumber"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "condition"
                            }, 
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "enabled"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "removeJavaScriptBreakpoint", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "breakpointId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "continueToLocation", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "sourceId"
                            }, 
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "lineNumber"
                            }, 
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "columnNumber"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "stepOver", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "stepInto", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "stepOut", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "pause", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "resume", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "success", 
                                "out": true
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "result", 
                                "out": true
                            }, 
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "newCallFrames", 
                                "out": true
                            }
                        ], 
                        "name": "editScriptSource", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "sourceID"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "newContent"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "scriptSource", 
                                "out": true
                            }
                        ], 
                        "name": "getScriptSource", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "sourceID"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "newState", 
                                "out": true
                            }
                        ], 
                        "name": "setPauseOnExceptionsState", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "pauseOnExceptionsState"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "result", 
                                "out": true
                            }
                        ], 
                        "name": "evaluateOnCallFrame", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "callFrameId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "expression"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "objectGroup"
                            }, 
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "includeCommandLineAPI"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "result", 
                                "out": true
                            }
                        ], 
                        "name": "getCompletionsOnCallFrame", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "callFrameId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "expression"
                            }, 
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "includeCommandLineAPI"
                            }
                        ]
                    }
                ]
            }, 
            {
                "extendedAttributes": {
                    "Conditional": "INSPECTOR"
                }, 
                "name": "BrowserDebugger", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "setAllBrowserBreakpoints", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "breakpoints"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "setDOMBreakpoint", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }, 
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "type"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "removeDOMBreakpoint", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }, 
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "type"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "setEventListenerBreakpoint", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "eventName"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "removeEventListenerBreakpoint", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "eventName"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "setXHRBreakpoint", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "url"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "removeXHRBreakpoint", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "url"
                            }
                        ]
                    }
                ]
            }, 
            {
                "extendedAttributes": {
                    "Conditional": "INSPECTOR"
                }, 
                "name": "Profiler", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "any", 
                                    "rank": 1
                                }, 
                                "name": "headers", 
                                "out": true
                            }
                        ], 
                        "name": "getProfileHeaders", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "profile", 
                                "out": true
                            }
                        ], 
                        "name": "getProfile", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "type"
                            }, 
                            {
                                "type": {
                                    "originalName": "unsigned", 
                                    "name": "int"
                                }, 
                                "name": "uid"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "removeProfile", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "type"
                            }, 
                            {
                                "type": {
                                    "originalName": "unsigned", 
                                    "name": "int"
                                }, 
                                "name": "uid"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "clearProfiles", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "takeHeapSnapshot", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "detailed"
                            }
                        ]
                    }
                ]
            }, 
            {
                "name": "InspectorNotify", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [], 
                        "name": "frontendReused"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "name": "any", 
                                    "rank": 1
                                }, 
                                "name": "nodeIds"
                            }
                        ], 
                        "name": "addNodesToSearchResult"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [], 
                        "name": "bringToFront"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [], 
                        "name": "disconnectFromBackend"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "url"
                            }
                        ], 
                        "name": "inspectedURLChanged"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "double", 
                                    "name": "float"
                                }, 
                                "name": "time"
                            }
                        ], 
                        "name": "domContentEventFired"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "double", 
                                    "name": "float"
                                }, 
                                "name": "time"
                            }
                        ], 
                        "name": "loadEventFired"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [], 
                        "name": "reset"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "panel"
                            }
                        ], 
                        "name": "showPanel"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "testCallId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "script"
                            }
                        ], 
                        "name": "evaluateForTestInFrontend"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "nodeId"
                            }
                        ], 
                        "name": "updateFocusedNode"
                    }
                ]
            }, 
            {
                "name": "ConsoleNotify", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "messageObj"
                            }
                        ], 
                        "name": "addConsoleMessage"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "unsigned", 
                                    "name": "int"
                                }, 
                                "name": "count"
                            }
                        ], 
                        "name": "updateConsoleMessageExpiredCount"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "unsigned", 
                                    "name": "int"
                                }, 
                                "name": "count"
                            }
                        ], 
                        "name": "updateConsoleMessageRepeatCount"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [], 
                        "name": "consoleMessagesCleared"
                    }
                ]
            }, 
            {
                "name": "NetworkNotify", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "unsigned", 
                                    "name": "int"
                                }, 
                                "name": "frameId"
                            }
                        ], 
                        "name": "frameDetachedFromParent"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "identifier"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "url"
                            }, 
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "loader"
                            }, 
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "callStack"
                            }
                        ], 
                        "name": "identifierForInitialRequest"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "identifier"
                            }, 
                            {
                                "type": {
                                    "originalName": "double", 
                                    "name": "float"
                                }, 
                                "name": "time"
                            }, 
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "request"
                            }, 
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "redirectResponse"
                            }
                        ], 
                        "name": "willSendRequest"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "identifier"
                            }
                        ], 
                        "name": "markResourceAsCached"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "identifier"
                            }, 
                            {
                                "type": {
                                    "originalName": "double", 
                                    "name": "float"
                                }, 
                                "name": "time"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "resourceType"
                            }, 
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "response"
                            }
                        ], 
                        "name": "didReceiveResponse"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "identifier"
                            }, 
                            {
                                "type": {
                                    "originalName": "double", 
                                    "name": "float"
                                }, 
                                "name": "time"
                            }, 
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "lengthReceived"
                            }
                        ], 
                        "name": "didReceiveContentLength"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "identifier"
                            }, 
                            {
                                "type": {
                                    "originalName": "double", 
                                    "name": "float"
                                }, 
                                "name": "finishTime"
                            }
                        ], 
                        "name": "didFinishLoading"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "identifier"
                            }, 
                            {
                                "type": {
                                    "originalName": "double", 
                                    "name": "float"
                                }, 
                                "name": "time"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "localizedDescription"
                            }
                        ], 
                        "name": "didFailLoading"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "double", 
                                    "name": "float"
                                }, 
                                "name": "time"
                            }, 
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "resource"
                            }
                        ], 
                        "name": "didLoadResourceFromMemoryCache"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "identifier"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "sourceString"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "type"
                            }
                        ], 
                        "name": "setInitialContent"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "frame"
                            }, 
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "loader"
                            }
                        ], 
                        "name": "didCommitLoadForFrame"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "unsigned", 
                                    "name": "int"
                                }, 
                                "name": "identifier"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "requestURL"
                            }
                        ], 
                        "name": "didCreateWebSocket"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "unsigned", 
                                    "name": "int"
                                }, 
                                "name": "identifier"
                            }, 
                            {
                                "type": {
                                    "originalName": "double", 
                                    "name": "float"
                                }, 
                                "name": "time"
                            }, 
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "request"
                            }
                        ], 
                        "name": "willSendWebSocketHandshakeRequest"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "unsigned", 
                                    "name": "int"
                                }, 
                                "name": "identifier"
                            }, 
                            {
                                "type": {
                                    "originalName": "double", 
                                    "name": "float"
                                }, 
                                "name": "time"
                            }, 
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "response"
                            }
                        ], 
                        "name": "didReceiveWebSocketHandshakeResponse"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "unsigned", 
                                    "name": "int"
                                }, 
                                "name": "identifier"
                            }, 
                            {
                                "type": {
                                    "originalName": "double", 
                                    "name": "float"
                                }, 
                                "name": "time"
                            }
                        ], 
                        "name": "didCloseWebSocket"
                    }
                ]
            }, 
            {
                "name": "DatabaseNotify", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "database"
                            }
                        ], 
                        "name": "addDatabase"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "databaseId"
                            }
                        ], 
                        "name": "selectDatabase"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "transactionId"
                            }, 
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "columnNames"
                            }, 
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "values"
                            }
                        ], 
                        "name": "sqlTransactionSucceeded"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "transactionId"
                            }, 
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "sqlError"
                            }
                        ], 
                        "name": "sqlTransactionFailed"
                    }
                ]
            }, 
            {
                "name": "DOMStorageNotify", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "storage"
                            }
                        ], 
                        "name": "addDOMStorage"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "storageId"
                            }
                        ], 
                        "name": "updateDOMStorage"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "storageId"
                            }
                        ], 
                        "name": "selectDOMStorage"
                    }
                ]
            }, 
            {
                "name": "ApplicationCacheNotify", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "status"
                            }
                        ], 
                        "name": "updateApplicationCacheStatus"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "isNowOnline"
                            }
                        ], 
                        "name": "updateNetworkState"
                    }
                ]
            }, 
            {
                "name": "DOMNotify", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Value", 
                                    "name": "any"
                                }, 
                                "name": "root"
                            }
                        ], 
                        "name": "setDocument"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "id"
                            }, 
                            {
                                "type": {
                                    "name": "any", 
                                    "rank": 1
                                }, 
                                "name": "attributes"
                            }
                        ], 
                        "name": "attributesUpdated"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "id"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "newValue"
                            }
                        ], 
                        "name": "characterDataModified"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "parentId"
                            }, 
                            {
                                "type": {
                                    "name": "any", 
                                    "rank": 1
                                }, 
                                "name": "nodes"
                            }
                        ], 
                        "name": "setChildNodes"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "root"
                            }
                        ], 
                        "name": "setDetachedRoot"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "id"
                            }, 
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "newValue"
                            }
                        ], 
                        "name": "childNodeCountUpdated"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "parentId"
                            }, 
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "prevId"
                            }, 
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "node"
                            }
                        ], 
                        "name": "childNodeInserted"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "parentId"
                            }, 
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "id"
                            }
                        ], 
                        "name": "childNodeRemoved"
                    }
                ]
            }, 
            {
                "name": "TimelineNotify", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [], 
                        "name": "timelineProfilerWasStarted"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [], 
                        "name": "timelineProfilerWasStopped"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "record"
                            }
                        ], 
                        "name": "addRecordToTimeline"
                    }
                ]
            }, 
            {
                "name": "DebuggerNotify", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [], 
                        "name": "debuggerWasEnabled"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [], 
                        "name": "debuggerWasDisabled"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "sourceID"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "url"
                            }, 
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "lineOffset"
                            }, 
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "columnOffset"
                            }, 
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "length"
                            }, 
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "scriptWorldType"
                            }
                        ], 
                        "name": "parsedScriptSource"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "url"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "data"
                            }, 
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "firstLine"
                            }, 
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "errorLine"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "errorMessage"
                            }
                        ], 
                        "name": "failedToParseScriptSource"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "breakpointId"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "sourceId"
                            }, 
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "lineNumber"
                            }, 
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "columnNumber"
                            }
                        ], 
                        "name": "breakpointResolved"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "details"
                            }
                        ], 
                        "name": "pausedScript"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [], 
                        "name": "resumedScript"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "id"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "url"
                            }, 
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "isShared"
                            }
                        ], 
                        "name": "didCreateWorker"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "long", 
                                    "name": "int"
                                }, 
                                "name": "id"
                            }
                        ], 
                        "name": "didDestroyWorker"
                    }
                ]
            }, 
            {
                "name": "ProfilerNotify", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [], 
                        "name": "profilerWasEnabled"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [], 
                        "name": "profilerWasDisabled"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "header"
                            }
                        ], 
                        "name": "addProfileHeader"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "unsigned", 
                                    "name": "int"
                                }, 
                                "name": "uid"
                            }, 
                            {
                                "type": {
                                    "originalName": "String", 
                                    "name": "string"
                                }, 
                                "name": "chunk"
                            }
                        ], 
                        "name": "addHeapSnapshotChunk"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "unsigned", 
                                    "name": "int"
                                }, 
                                "name": "uid"
                            }
                        ], 
                        "name": "finishHeapSnapshot"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "name": "boolean"
                                }, 
                                "name": "isProfiling"
                            }
                        ], 
                        "name": "setRecordingProfile"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [], 
                        "name": "resetProfiles"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "notify": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "done"
                            }, 
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "total"
                            }
                        ], 
                        "name": "reportHeapSnapshotProgress"
                    }
                ]
            }
        ], 
        "name": "core"
    }, 
    {
        "interfaces": [
            {
                "extendedAttributes": {
                    "Conditional": "INSPECTOR"
                }, 
                "name": "InspectorFrontendHost", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "loaded", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "closeWindow", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "disconnectFromBackend", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "bringToFront", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "inspectedURLChanged", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "DOMString", 
                                    "name": "string"
                                }, 
                                "name": "newURL"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "requestAttachWindow", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "requestDetachWindow", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "setAttachedWindowHeight", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "unsigned", 
                                    "name": "int"
                                }, 
                                "name": "height"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "moveWindowBy", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "float"
                                }, 
                                "name": "x"
                            }, 
                            {
                                "type": {
                                    "name": "float"
                                }, 
                                "name": "y"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "setExtensionAPI", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "DOMString", 
                                    "name": "string"
                                }, 
                                "name": "script"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "originalName": "DOMString", 
                            "name": "string"
                        }, 
                        "callbackParameters": [], 
                        "name": "localizedStringsURL", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "originalName": "DOMString", 
                            "name": "string"
                        }, 
                        "callbackParameters": [], 
                        "name": "hiddenPanels", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "copyText", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "DOMString", 
                                    "name": "string"
                                }, 
                                "name": "text"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "originalName": "DOMString", 
                            "name": "string"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "Custom": true
                        }, 
                        "parameters": [], 
                        "name": "platform"
                    }, 
                    {
                        "returns": {
                            "originalName": "DOMString", 
                            "name": "string"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "Custom": true
                        }, 
                        "parameters": [], 
                        "name": "port"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "extendedAttributes": {
                            "Custom": true
                        }, 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "MouseEvent", 
                                    "name": "any"
                                }, 
                                "name": "event"
                            }, 
                            {
                                "type": {
                                    "originalName": "DOMObject", 
                                    "name": "any"
                                }, 
                                "name": "items"
                            }
                        ], 
                        "name": "showContextMenu"
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "sendMessageToBackend", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "DOMString", 
                                    "name": "string"
                                }, 
                                "name": "message"
                            }
                        ]
                    }
                ]
            }
        ], 
        "name": "core"
    }, 
    {
        "interfaces": [
            {
                "name": "WeinreClientCommands", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "clientId", 
                                "out": true
                            }
                        ], 
                        "name": "registerClient", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any", 
                                    "rank": 1
                                }, 
                                "name": "targets", 
                                "out": true
                            }
                        ], 
                        "name": "getTargets", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any", 
                                    "rank": 1
                                }, 
                                "name": "clients", 
                                "out": true
                            }
                        ], 
                        "name": "getClients", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "connectTarget", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "clientId"
                            }, 
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "targetId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "disconnectTarget", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "clientId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "string", 
                                    "rank": 1
                                }, 
                                "name": "extensions", 
                                "out": true
                            }
                        ], 
                        "name": "getExtensions", 
                        "parameters": []
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "logDebug", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "message"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "logInfo", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "message"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "logWarning", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "message"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "logError", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "message"
                            }
                        ]
                    }
                ]
            }
        ], 
        "name": "weinre"
    }, 
    {
        "interfaces": [
            {
                "name": "WeinreClientEvents", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "clientRegistered", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "client"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "targetRegistered", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "target"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "clientUnregistered", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "clientId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "targetUnregistered", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "targetId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "connectionCreated", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "clientId"
                            }, 
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "targetId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "connectionDestroyed", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "clientId"
                            }, 
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "targetId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "sendCallback", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "callbackId"
                            }, 
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "result"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "serverProperties", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "properties"
                            }
                        ]
                    }
                ]
            }
        ], 
        "name": "weinre"
    }, 
    {
        "interfaces": [
            {
                "name": "WeinreExtraClientCommands", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any", 
                                    "rank": 1
                                }, 
                                "name": "databaseRecords", 
                                "out": true
                            }
                        ], 
                        "name": "getDatabases", 
                        "parameters": []
                    }
                ]
            }
        ], 
        "name": "weinre"
    }, 
    {
        "interfaces": [
            {
                "name": "WeinreExtraTargetEvents", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "databaseOpened", 
                        "parameters": [
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "databaseRecord"
                            }
                        ]
                    }
                ]
            }
        ], 
        "name": "weinre"
    }, 
    {
        "interfaces": [
            {
                "name": "WeinreTargetCommands", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "targetId", 
                                "out": true
                            }
                        ], 
                        "name": "registerTarget", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "int"
                                }, 
                                "name": "url"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "sendClientCallback", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "callbackId"
                            }, 
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "args"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "logDebug", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "message"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "logInfo", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "message"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "logWarning", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "message"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "logError", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "message"
                            }
                        ]
                    }
                ]
            }
        ], 
        "name": "weinre"
    }, 
    {
        "interfaces": [
            {
                "name": "WeinreTargetEvents", 
                "methods": [
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "connectionCreated", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "clientId"
                            }, 
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "targetId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "connectionDestroyed", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "clientId"
                            }, 
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "targetId"
                            }
                        ]
                    }, 
                    {
                        "returns": {
                            "name": "void"
                        }, 
                        "callbackParameters": [], 
                        "name": "sendCallback", 
                        "parameters": [
                            {
                                "type": {
                                    "name": "string"
                                }, 
                                "name": "callbackId"
                            }, 
                            {
                                "type": {
                                    "originalName": "Object", 
                                    "name": "any"
                                }, 
                                "name": "result"
                            }
                        ]
                    }
                ]
            }
        ], 
        "name": "weinre"
    }
])