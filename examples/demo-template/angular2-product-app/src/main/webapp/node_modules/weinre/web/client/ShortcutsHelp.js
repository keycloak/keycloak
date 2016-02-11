/*
 * Copyright (C) 2010 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

WebInspector.ShortcutsHelp = function()
{
    this._sections = {};
}

WebInspector.ShortcutsHelp.prototype = {
    section: function(name)
    {
        var section = this._sections[name];
        if (!section)
            this._sections[name] = section = new WebInspector.ShortcutsSection(name);
        return section;
    },

    show: function()
    {
        if (!this._helpScreen) {
            this._helpScreen = new WebInspector.HelpScreen(WebInspector.UIString("Keyboard Shortcuts"));
            this._buildTable(this._helpScreen.contentElement, 2);
        }
        this._helpScreen.show();
    },

    _buildTable: function(parent, nColumns)
    {
        var height = 0;
        var orderedSections = [];
        for (var section in this._sections) {
            height += this._sections[section]._height;
            orderedSections.push(this._sections[section])
        }
        function compareSections(a, b)
        {
            return a.order - b.order;
        }
        orderedSections = orderedSections.sort(compareSections);

        var wrapAfter = height / nColumns;
        var table = document.createElement("table");
        table.className = "help-table";
        var row = table.createChild("tr");

        // This manual layout ugliness should be gone once WebKit implements
        // pagination hints for CSS columns (break-inside etc).
        for (var section = 0; section < orderedSections.length;) {
            var td = row.createChild("td");
            td.style.width = (100 / nColumns) + "%";
            var column = td.createChild("table");
            for (var columnHeight = 0;
                columnHeight < wrapAfter && section < orderedSections.length;
                columnHeight += orderedSections[section]._height, section++) {
                orderedSections[section].renderSection(column);
            }
        }
        parent.appendChild(table);
    }
};

WebInspector.shortcutsHelp = new WebInspector.ShortcutsHelp();

WebInspector.ShortcutsSection = function(name)
{
    this.name = name;
    this._lines = [];
    this.order = ++WebInspector.ShortcutsSection._sequenceNumber;
};

WebInspector.ShortcutsSection._sequenceNumber = 0;

WebInspector.ShortcutsSection.prototype = {
    addKey: function(key, description)
    {
        this.addLine(this._renderKey(key), description);
    },

    addRelatedKeys: function(keys, description)
    {
        this.addLine(this._renderSequence(keys,"/"), description);
    },

    addAlternateKeys: function(keys, description)
    {
        this.addLine(this._renderSequence(keys,WebInspector.UIString("or")), description);
    },

    addLine: function(htmlKey, description)
    {
        this._lines.push({ key: htmlKey, text: description })
    },

    renderSection: function(parent)
    {
        this._renderHeader(parent);

        for (var line = 0; line < this._lines.length; ++line) {
            var tr = parent.createChild("tr");
            tr.createChild("td", "help-key-cell").innerHTML = this._lines[line].key + " : ";
            tr.createChild("td").innerText = this._lines[line].text;
        }
    },

    _renderHeader: function(parent)
    {
        var trHead = parent.createChild("tr");
 
        trHead.createChild("th");
        trHead.createChild("th").innerText = this.name;
    },

    _renderSequence: function(sequence, delimiter)
    {
        var delimiterHtml = '<span class="help-key-delimiter">' + delimiter.escapeHTML() + '</span>'
        return sequence.map(this._renderKey).join(delimiterHtml);
    },

    _renderKey: function(key)
    {
        function renderLabel(label)
        {
            return '<span class="help-key monospace">' + label.escapeHTML() + '</span>';
        }
        return key.split(" + ").map(renderLabel).join('<span class="help-combine-keys">+</span>');
    },

    get _height()
    {
        return this._lines.length + 2; // add some space for header
    }
};
