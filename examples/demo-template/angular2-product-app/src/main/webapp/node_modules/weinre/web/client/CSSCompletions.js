/*
 * Copyright (C) 2010 Nikita Vasilyev. All rights reserved.
 * Copyright (C) 2010 Joseph Pecoraro. All rights reserved.
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

WebInspector.CSSCompletions = function(values, acceptEmptyPrefix)
{
    this._values = values.slice();
    this._values.sort();
    this._acceptEmptyPrefix = acceptEmptyPrefix;
}

WebInspector.CSSCompletions.prototype = {
    startsWith: function(prefix)
    {
        var firstIndex = this._firstIndexOfPrefix(prefix);
        if (firstIndex === -1)
            return [];

        var results = [];
        while (firstIndex < this._values.length && this._values[firstIndex].indexOf(prefix) === 0)
            results.push(this._values[firstIndex++]);
        return results;
    },

    firstStartsWith: function(prefix)
    {
        var foundIndex = this._firstIndexOfPrefix(prefix);
        return (foundIndex === -1 ? "" : this._values[foundIndex]);
    },

    _firstIndexOfPrefix: function(prefix)
    {
        if (!this._values.length)
            return -1;
        if (!prefix)
            return this._acceptEmptyPrefix ? 0 : -1;

        var maxIndex = this._values.length - 1;
        var minIndex = 0;
        var foundIndex;

        do {
            var middleIndex = (maxIndex + minIndex) >> 1;
            if (this._values[middleIndex].indexOf(prefix) === 0) {
                foundIndex = middleIndex;
                break;
            }
            if (this._values[middleIndex] < prefix)
                minIndex = middleIndex + 1;
            else
                maxIndex = middleIndex - 1;
        } while (minIndex <= maxIndex);

        if (foundIndex === undefined)
            return -1;

        while (foundIndex && this._values[foundIndex - 1].indexOf(prefix) === 0)
            foundIndex--;

        return foundIndex;
    },

    keySet: function()
    {
        return this._values.keySet();
    },

    next: function(str, prefix)
    {
        return this._closest(str, prefix, 1);
    },

    previous: function(str, prefix)
    {
        return this._closest(str, prefix, -1);
    },

    _closest: function(str, prefix, shift)
    {
        if (!str)
            return "";

        var index = this._values.indexOf(str);
        if (index === -1)
            return "";

        if (!prefix) {
            index = (index + this._values.length + shift) % this._values.length;
            return this._values[index];
        }

        var propertiesWithPrefix = this.startsWith(prefix);
        var j = propertiesWithPrefix.indexOf(str);
        j = (j + propertiesWithPrefix.length + shift) % propertiesWithPrefix.length;
        return propertiesWithPrefix[j];
    }
}
