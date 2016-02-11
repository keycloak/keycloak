/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

"use strict";

//------------------------------------------------------------------------------
var started = false

var buttonStartStuff
var buttonClearOutput
var outputElement
var storageIndex = 0
var db
var otherDB

// set the id based on the hash
var hash = location.href.split("#")[1]
if (!hash) hash = "anonymous"
window.WeinreServerId = hash

//------------------------------------------------------------------------------
function onLoad() {
    if (!buttonStartStuff)       buttonStartStuff       = document.getElementById("button-start-stuff")
    if (!buttonClearOutput)      buttonClearOutput      = document.getElementById("button-clear-output")
    if (!outputElement)          outputElement          = document.getElementById("output")

    buttonStartStuff.addEventListener("click", function() {
        lastClickTime = new Date().toString()
        if (db) db.transaction(addClick)

        openTheOtherDatabase()

        if (!started) {
            buttonStartStuff.value = "stop stuff"
            startStuff()
        }
        else {
            buttonStartStuff.value = "start stuff"
            stopStuff()
        }
        started = !started
    })

    buttonClearOutput.addEventListener("click", function() {
        outputElement.innerHTML = ""
    })

    openTheDatabase()
}

//------------------------------------------------------------------------------
var interval

function startStuff() {
    if (window.localStorage)   window.localStorage.clear()
    if (window.sessionStorage) window.sessionStorage.clear()

    storageIndex = 0

    interval = setInterval(intervalStuff, 1000)
}

function stopStuff() {
    clearInterval(interval)
}

//------------------------------------------------------------------------------
function intervalStuff() {

    var message = "doing interval stuff at " + new Date()

    // add a timeout
    setTimeout(function() { console.log(message)}, 333)

    // add a timeline marker
    console.markTimeline(message)

    // write to local- and sessionStorage
    if (window.localStorage) {
        var smessage = message + " (local)"
        window.localStorage.setItem(  "item-" + storageIndex, smessage)
    }

    if (window.sessionStorage) {
        var smessage = message + " (session)"
        window.sessionStorage.setItem("item-" + storageIndex, smessage)
    }
    storageIndex++

    // write the message to the page
    output(message)

    // do an XHR
    var xhr = new XMLHttpRequest()
    // xhr.addEventListener("readystatechange", function() {logXhr(this)})
    xhr.open("GET", "../target/target-script.js", true)
    xhr.send()
}

//------------------------------------------------------------------------------
function sqlSuccess(tx, resultSet) {
    console.log("SQL Success!")
}

//------------------------------------------------------------------------------
function sqlError(tx, error) {
    console.log("SQL Error " + error.code + ": " + error.message)
}

//------------------------------------------------------------------------------
var lastClickTime

function addClick(tx) {
    var sql = "insert into clicks (date) values (?)"
    tx.executeSql(sql, [lastClickTime], null, sqlError)
}

//------------------------------------------------------------------------------
function clearDatabase(tx, resultSet) {
    var sql = "delete from clicks"
    tx.executeSql(sql, null, null, sqlError);
}

//------------------------------------------------------------------------------
function createDatabase(tx) {
    var schema = "clicks (id integer primary key, date text)"
    var sql = "create table if not exists " + schema

    tx.executeSql(sql, null, clearDatabase, sqlError);
}

//------------------------------------------------------------------------------
function createDatabase_other(tx) {
    var schema = "clicks_other (id integer primary key, other text)"
    var sql = "create table if not exists " + schema

    tx.executeSql(sql, null, null, sqlError);
}

//------------------------------------------------------------------------------
function openTheDatabase() {
    if (window.openDatabase) {
        db = window.openDatabase("clicks_db", "1.0", "clicks_db", 8192)
        db.transaction(createDatabase)
    }
}

//------------------------------------------------------------------------------
function openTheOtherDatabase() {
    if (otherDB) return

    if (window.openDatabase) {
        otherDB = window.openDatabase("clicks_other_db", "1.0", "clicks_other_db", 8192)
        otherDB.transaction(createDatabase_other)
    }
}

//------------------------------------------------------------------------------
function output(string) {
    var element = document.createElement("div")
    element.innerHTML = string
    outputElement.appendChild(element)
}

//------------------------------------------------------------------------------
function logXhr(xhr) {
    console.log("xhr: readyState: " + xhr.readyState)
}

