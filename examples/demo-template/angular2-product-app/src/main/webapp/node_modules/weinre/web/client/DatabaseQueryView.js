/*
 * Copyright (C) 2008 Apple Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

WebInspector.DatabaseQueryView = function(database)
{
    WebInspector.View.call(this);

    this.database = database;

    this.element.addStyleClass("storage-view");
    this.element.addStyleClass("query");
    this.element.addStyleClass("monospace");
    this.element.tabIndex = 0;

    this.element.addEventListener("selectstart", this._selectStart.bind(this), false);

    this.promptElement = document.createElement("div");
    this.promptElement.className = "database-query-prompt";
    this.promptElement.appendChild(document.createElement("br"));
    this.promptElement.addEventListener("keydown", this._promptKeyDown.bind(this), true);
    this.element.appendChild(this.promptElement);

    this.prompt = new WebInspector.TextPrompt(this.promptElement, this.completions.bind(this), " ");
}

WebInspector.DatabaseQueryView.prototype = {
    show: function(parentElement)
    {
        WebInspector.View.prototype.show.call(this, parentElement);

        function moveBackIfOutside()
        {
            if (!this.prompt.isCaretInsidePrompt() && window.getSelection().isCollapsed)
                this.prompt.moveCaretToEndOfPrompt();
        }

        setTimeout(moveBackIfOutside.bind(this), 0);
    },

    completions: function(wordRange, bestMatchOnly, completionsReadyCallback)
    {
        var prefix = wordRange.toString().toLowerCase();
        if (!prefix.length)
            return;

        var results = [];

        function accumulateMatches(textArray)
        {
            if (bestMatchOnly && results.length)
                return;
            for (var i = 0; i < textArray.length; ++i) {
                var text = textArray[i].toLowerCase();
                if (text.length < prefix.length)
                    continue;
                if (text.indexOf(prefix) !== 0)
                    continue;
                results.push(textArray[i]);
                if (bestMatchOnly)
                    return;
            }
        }
        
        function tableNamesCallback(tableNames)
        {
            accumulateMatches(tableNames.map(function(name) { return name + " " }));
            accumulateMatches(["SELECT ", "FROM ", "WHERE ", "LIMIT ", "DELETE FROM ", "CREATE ", "DROP ", "TABLE ", "INDEX ", "UPDATE ", "INSERT INTO ", "VALUES ("]);

            completionsReadyCallback(results);
        }
        this.database.getTableNames(tableNamesCallback);
    },

    _promptKeyDown: function(event)
    {
        if (isEnterKey(event)) {
            this._enterKeyPressed(event);
            return;
        }
    },

    _selectStart: function(event)
    {
        if (this._selectionTimeout)
            clearTimeout(this._selectionTimeout);

        this.prompt.clearAutoComplete();

        function moveBackIfOutside()
        {
            delete this._selectionTimeout;
            if (!this.prompt.isCaretInsidePrompt() && window.getSelection().isCollapsed)
                this.prompt.moveCaretToEndOfPrompt();
            this.prompt.autoCompleteSoon();
        }

        this._selectionTimeout = setTimeout(moveBackIfOutside.bind(this), 100);
    },

    _enterKeyPressed: function(event)
    {
        event.preventDefault();
        event.stopPropagation();

        this.prompt.clearAutoComplete(true);

        var query = this.prompt.text;
        if (!query.length)
            return;

        this.prompt.history.push(query);
        this.prompt.historyOffset = 0;
        this.prompt.text = "";

        this.database.executeSql(query, this._queryFinished.bind(this, query), this._queryError.bind(this, query));
    },

    _queryFinished: function(query, columnNames, values)
    {
        var dataGrid = WebInspector.panels.resources.dataGridForResult(columnNames, values);
        var trimmedQuery = query.trim();

        if (dataGrid) {
            dataGrid.element.addStyleClass("inline");
            this._appendQueryResult(trimmedQuery, dataGrid.element);
            dataGrid.autoSizeColumns(5);            
        }

        if (trimmedQuery.match(/^create /i) || trimmedQuery.match(/^drop table /i))
            WebInspector.panels.resources.updateDatabaseTables(this.database);
    },

    _queryError: function(query, error)
    {
        if (error.message)
            var message = error.message;
        else if (error.code == 2)
            var message = WebInspector.UIString("Database no longer has expected version.");
        else
            var message = WebInspector.UIString("An unexpected error %s occurred.", error.code);

        this._appendQueryResult(query, message, "error");
    },

    _appendQueryResult: function(query, result, resultClassName)
    {
        var element = document.createElement("div");
        element.className = "database-user-query";

        var commandTextElement = document.createElement("span");
        commandTextElement.className = "database-query-text";
        commandTextElement.textContent = query;
        element.appendChild(commandTextElement);

        var resultElement = document.createElement("div");
        resultElement.className = "database-query-result";

        if (resultClassName)
            resultElement.addStyleClass(resultClassName);

        if (typeof result === "string" || result instanceof String)
            resultElement.textContent = result;
        else if (result && result.nodeName)
            resultElement.appendChild(result);

        if (resultElement.childNodes.length)
            element.appendChild(resultElement);

        this.element.insertBefore(element, this.promptElement);
        this.promptElement.scrollIntoView(false);
    }
}

WebInspector.DatabaseQueryView.prototype.__proto__ = WebInspector.View.prototype;
