/*
 * Copyright (C) 2009 Apple Inc.  All rights reserved.
 * Copyright (C) 2009 Joseph Pecoraro
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1.  Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 * 2.  Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 3.  Neither the name of Apple Computer, Inc. ("Apple") nor the names of
 *     its contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE AND ITS CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

WebInspector.Color = function(str)
{
    this.value = str;
    this._parse();
}

WebInspector.Color.prototype = {
    get shorthex()
    {
        if ("_short" in this)
            return this._short;

        if (!this.simple)
            return null;

        var hex = this.hex;
        if (hex.charAt(0) === hex.charAt(1) && hex.charAt(2) === hex.charAt(3) && hex.charAt(4) === hex.charAt(5))
            this._short = hex.charAt(0) + hex.charAt(2) + hex.charAt(4);
        else
            this._short = hex;

        return this._short;
    },

    get hex()
    {
        if (!this.simple)
            return null;

        return this._hex;
    },

    set hex(x)
    {
        this._hex = x;
    },

    get rgb()
    {
        if ("_rgb" in this)
            return this._rgb;

        if (this.simple)
            this._rgb = this._hexToRGB(this.hex);
        else {
            var rgba = this.rgba;
            this._rgb = [rgba[0], rgba[1], rgba[2]];
        }

        return this._rgb;
    },

    set rgb(x)
    {
        this._rgb = x;
    },

    get hsl()
    {
        if ("_hsl" in this)
            return this._hsl;

        this._hsl = this._rgbToHSL(this.rgb);
        return this._hsl;
    },

    set hsl(x)
    {
        this._hsl = x;
    },

    get nickname()
    {
        if (typeof this._nickname !== "undefined") // would be set on parse if there was a nickname
            return this._nickname;
        else
            return null;
    },

    set nickname(x)
    {
        this._nickname = x;
    },

    get rgba()
    {
        return this._rgba;
    },

    set rgba(x)
    {
        this._rgba = x;
    },

    get hsla()
    {
        return this._hsla;
    },

    set hsla(x)
    {
        this._hsla = x;
    },

    hasShortHex: function()
    {
        var shorthex = this.shorthex;
        return (shorthex && shorthex.length === 3);
    },

    toString: function(format)
    {
        if (!format)
            format = this.format;

        switch (format) {
            case "original":
                return this.value;
            case "rgb":
                return "rgb(" + this.rgb.join(", ") + ")";
            case "rgba":
                return "rgba(" + this.rgba.join(", ") + ")";
            case "hsl":
                var hsl = this.hsl;
                return "hsl(" + hsl[0] + ", " + hsl[1] + "%, " + hsl[2] + "%)";
            case "hsla":
                var hsla = this.hsla;
                return "hsla(" + hsla[0] + ", " + hsla[1] + "%, " + hsla[2] + "%, " + hsla[3] + ")";
            case "hex":
                return "#" + this.hex;
            case "shorthex":
                return "#" + this.shorthex;
            case "nickname":
                return this.nickname;
        }

        throw "invalid color format";
    },

    _rgbToHex: function(rgb)
    {
        var r = parseInt(rgb[0]).toString(16);
        var g = parseInt(rgb[1]).toString(16);
        var b = parseInt(rgb[2]).toString(16);
        if (r.length === 1)
            r = "0" + r;
        if (g.length === 1)
            g = "0" + g;
        if (b.length === 1)
            b = "0" + b;

        return (r + g + b).toUpperCase();
    },

    _hexToRGB: function(hex)
    {
        var r = parseInt(hex.substring(0,2), 16);
        var g = parseInt(hex.substring(2,4), 16);
        var b = parseInt(hex.substring(4,6), 16);

        return [r, g, b];
    },

    _rgbToHSL: function(rgb)
    {
        var r = parseInt(rgb[0]) / 255;
        var g = parseInt(rgb[1]) / 255;
        var b = parseInt(rgb[2]) / 255;
        var max = Math.max(r, g, b);
        var min = Math.min(r, g, b);
        var diff = max - min;
        var add = max + min;

        if (min === max)
            var h = 0;
        else if (r === max)
            var h = ((60 * (g - b) / diff) + 360) % 360;
        else if (g === max)
            var h = (60 * (b - r) / diff) + 120;
        else
            var h = (60 * (r - g) / diff) + 240;

        var l = 0.5 * add;

        if (l === 0)
            var s = 0;
        else if (l === 1)
            var s = 1;
        else if (l <= 0.5)
            var s = diff / add;
        else
            var s = diff / (2 - add);

        h = Math.round(h);
        s = Math.round(s*100);
        l = Math.round(l*100);

        return [h, s, l];
    },

    _hslToRGB: function(hsl)
    {
        var h = parseFloat(hsl[0]) / 360;
        var s = parseFloat(hsl[1]) / 100;
        var l = parseFloat(hsl[2]) / 100;

        if (l <= 0.5)
            var q = l * (1 + s);
        else
            var q = l + s - (l * s);

        var p = 2 * l - q;

        var tr = h + (1 / 3);
        var tg = h;
        var tb = h - (1 / 3);

        var r = Math.round(hueToRGB(p, q, tr) * 255);
        var g = Math.round(hueToRGB(p, q, tg) * 255);
        var b = Math.round(hueToRGB(p, q, tb) * 255);
        return [r, g, b];

        function hueToRGB(p, q, h) {
            if (h < 0)
                h += 1;
            else if (h > 1)
                h -= 1;

            if ((h * 6) < 1)
                return p + (q - p) * h * 6;
            else if ((h * 2) < 1)
                return q;
            else if ((h * 3) < 2)
                return p + (q - p) * ((2 / 3) - h) * 6;
            else
                return p;
        }
    },

    _rgbaToHSLA: function(rgba)
    {
        var alpha = rgba[3];
        var hsl = this._rgbToHSL(rgba)
        hsl.push(alpha);
        return hsl;
    },

    _hslaToRGBA: function(hsla)
    {
        var alpha = hsla[3];
        var rgb = this._hslToRGB(hsla);
        rgb.push(alpha);
        return rgb;
    },

    _parse: function()
    {
        // Special Values - Advanced but Must Be Parsed First - transparent
        var value = this.value.toLowerCase().replace(/%|\s+/g, "");
        if (value in WebInspector.Color.AdvancedNickNames) {
            this.format = "nickname";
            var set = WebInspector.Color.AdvancedNickNames[value];
            this.simple = false;
            this.rgba = set[0];
            this.hsla = set[1];
            this.nickname = set[2];
            this.alpha = set[0][3];
            return;
        }

        // Simple - #hex, rgb(), nickname, hsl()
        var simple = /^(?:#([0-9a-f]{3,6})|rgb\(([^)]+)\)|(\w+)|hsl\(([^)]+)\))$/i;
        var match = this.value.match(simple);
        if (match) {
            this.simple = true;

            if (match[1]) { // hex
                var hex = match[1].toUpperCase();
                if (hex.length === 3) {
                    this.format = "shorthex";
                    this.hex = hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
                } else {
                    this.format = "hex";
                    this.hex = hex;
                }
            } else if (match[2]) { // rgb
                this.format = "rgb";
                var rgb = match[2].split(/\s*,\s*/);
                this.rgb = rgb;
                this.hex = this._rgbToHex(rgb);
            } else if (match[3]) { // nickname
                var nickname = match[3].toLowerCase();
                if (nickname in WebInspector.Color.Nicknames) {
                    this.format = "nickname";
                    this.hex = WebInspector.Color.Nicknames[nickname];
                } else // unknown name
                    throw "unknown color name";
            } else if (match[4]) { // hsl
                this.format = "hsl";
                var hsl = match[4].replace(/%/g, "").split(/\s*,\s*/);
                this.hsl = hsl;
                this.rgb = this._hslToRGB(hsl);
                this.hex = this._rgbToHex(this.rgb);
            }

            // Fill in the values if this is a known hex color
            var hex = this.hex;
            if (hex && hex in WebInspector.Color.HexTable) {
                var set = WebInspector.Color.HexTable[hex];
                this.rgb = set[0];
                this.hsl = set[1];
                this.nickname = set[2];
            }

            return;
        }

        // Advanced - rgba(), hsla()
        var advanced = /^(?:rgba\(([^)]+)\)|hsla\(([^)]+)\))$/;
        match = this.value.match(advanced);
        if (match) {
            this.simple = false;
            if (match[1]) { // rgba
                this.format = "rgba";
                this.rgba = match[1].split(/\s*,\s*/);
                this.hsla = this._rgbaToHSLA(this.rgba);
                this.alpha = this.rgba[3];
            } else if (match[2]) { // hsla
                this.format = "hsla";
                this.hsla = match[2].replace(/%/g, "").split(/\s*,\s*/);
                this.rgba = this._hslaToRGBA(this.hsla);
                this.alpha = this.hsla[3];
            }

            return;
        }

        // Could not parse as a valid color
        throw "could not parse color";
    }
}

// Simple Values: [rgb, hsl, nickname]
WebInspector.Color.HexTable = {
    "000000": [[0, 0, 0], [0, 0, 0], "black"],
    "000080": [[0, 0, 128], [240, 100, 25], "navy"],
    "00008B": [[0, 0, 139], [240, 100, 27], "darkBlue"],
    "0000CD": [[0, 0, 205], [240, 100, 40], "mediumBlue"],
    "0000FF": [[0, 0, 255], [240, 100, 50], "blue"],
    "006400": [[0, 100, 0], [120, 100, 20], "darkGreen"],
    "008000": [[0, 128, 0], [120, 100, 25], "green"],
    "008080": [[0, 128, 128], [180, 100, 25], "teal"],
    "008B8B": [[0, 139, 139], [180, 100, 27], "darkCyan"],
    "00BFFF": [[0, 191, 255], [195, 100, 50], "deepSkyBlue"],
    "00CED1": [[0, 206, 209], [181, 100, 41], "darkTurquoise"],
    "00FA9A": [[0, 250, 154], [157, 100, 49], "mediumSpringGreen"],
    "00FF00": [[0, 255, 0], [120, 100, 50], "lime"],
    "00FF7F": [[0, 255, 127], [150, 100, 50], "springGreen"],
    "00FFFF": [[0, 255, 255], [180, 100, 50], "cyan"],
    "191970": [[25, 25, 112], [240, 64, 27], "midnightBlue"],
    "1E90FF": [[30, 144, 255], [210, 100, 56], "dodgerBlue"],
    "20B2AA": [[32, 178, 170], [177, 70, 41], "lightSeaGreen"],
    "228B22": [[34, 139, 34], [120, 61, 34], "forestGreen"],
    "2E8B57": [[46, 139, 87], [146, 50, 36], "seaGreen"],
    "2F4F4F": [[47, 79, 79], [180, 25, 25], "darkSlateGray"],
    "32CD32": [[50, 205, 50], [120, 61, 50], "limeGreen"],
    "3CB371": [[60, 179, 113], [147, 50, 47], "mediumSeaGreen"],
    "40E0D0": [[64, 224, 208], [174, 72, 56], "turquoise"],
    "4169E1": [[65, 105, 225], [225, 73, 57], "royalBlue"],
    "4682B4": [[70, 130, 180], [207, 44, 49], "steelBlue"],
    "483D8B": [[72, 61, 139], [248, 39, 39], "darkSlateBlue"],
    "48D1CC": [[72, 209, 204], [178, 60, 55], "mediumTurquoise"],
    "4B0082": [[75, 0, 130], [275, 100, 25], "indigo"],
    "556B2F": [[85, 107, 47], [82, 39, 30], "darkOliveGreen"],
    "5F9EA0": [[95, 158, 160], [182, 25, 50], "cadetBlue"],
    "6495ED": [[100, 149, 237], [219, 79, 66], "cornflowerBlue"],
    "66CDAA": [[102, 205, 170], [160, 51, 60], "mediumAquaMarine"],
    "696969": [[105, 105, 105], [0, 0, 41], "dimGray"],
    "6A5ACD": [[106, 90, 205], [248, 53, 58], "slateBlue"],
    "6B8E23": [[107, 142, 35], [80, 60, 35], "oliveDrab"],
    "708090": [[112, 128, 144], [210, 13, 50], "slateGray"],
    "778899": [[119, 136, 153], [210, 14, 53], "lightSlateGray"],
    "7B68EE": [[123, 104, 238], [249, 80, 67], "mediumSlateBlue"],
    "7CFC00": [[124, 252, 0], [90, 100, 49], "lawnGreen"],
    "7FFF00": [[127, 255, 0], [90, 100, 50], "chartreuse"],
    "7FFFD4": [[127, 255, 212], [160, 100, 75], "aquamarine"],
    "800000": [[128, 0, 0], [0, 100, 25], "maroon"],
    "800080": [[128, 0, 128], [300, 100, 25], "purple"],
    "808000": [[128, 128, 0], [60, 100, 25], "olive"],
    "808080": [[128, 128, 128], [0, 0, 50], "gray"],
    "87CEEB": [[135, 206, 235], [197, 71, 73], "skyBlue"],
    "87CEFA": [[135, 206, 250], [203, 92, 75], "lightSkyBlue"],
    "8A2BE2": [[138, 43, 226], [271, 76, 53], "blueViolet"],
    "8B0000": [[139, 0, 0], [0, 100, 27], "darkRed"],
    "8B008B": [[139, 0, 139], [300, 100, 27], "darkMagenta"],
    "8B4513": [[139, 69, 19], [25, 76, 31], "saddleBrown"],
    "8FBC8F": [[143, 188, 143], [120, 25, 65], "darkSeaGreen"],
    "90EE90": [[144, 238, 144], [120, 73, 75], "lightGreen"],
    "9370D8": [[147, 112, 219], [260, 60, 65], "mediumPurple"],
    "9400D3": [[148, 0, 211], [282, 100, 41], "darkViolet"],
    "98FB98": [[152, 251, 152], [120, 93, 79], "paleGreen"],
    "9932CC": [[153, 50, 204], [280, 61, 50], "darkOrchid"],
    "9ACD32": [[154, 205, 50], [80, 61, 50], "yellowGreen"],
    "A0522D": [[160, 82, 45], [19, 56, 40], "sienna"],
    "A52A2A": [[165, 42, 42], [0, 59, 41], "brown"],
    "A9A9A9": [[169, 169, 169], [0, 0, 66], "darkGray"],
    "ADD8E6": [[173, 216, 230], [195, 53, 79], "lightBlue"],
    "ADFF2F": [[173, 255, 47], [84, 100, 59], "greenYellow"],
    "AFEEEE": [[175, 238, 238], [180, 65, 81], "paleTurquoise"],
    "B0C4DE": [[176, 196, 222], [214, 41, 78], "lightSteelBlue"],
    "B0E0E6": [[176, 224, 230], [187, 52, 80], "powderBlue"],
    "B22222": [[178, 34, 34], [0, 68, 42], "fireBrick"],
    "B8860B": [[184, 134, 11], [43, 89, 38], "darkGoldenrod"],
    "BA55D3": [[186, 85, 211], [288, 59, 58], "mediumOrchid"],
    "BC8F8F": [[188, 143, 143], [0, 25, 65], "rosyBrown"],
    "BDB76B": [[189, 183, 107], [56, 38, 58], "darkKhaki"],
    "C0C0C0": [[192, 192, 192], [0, 0, 75], "silver"],
    "C71585": [[199, 21, 133], [322, 81, 43], "mediumVioletRed"],
    "CD5C5C": [[205, 92, 92], [0, 53, 58], "indianRed"],
    "CD853F": [[205, 133, 63], [30, 59, 53], "peru"],
    "D2691E": [[210, 105, 30], [25, 75, 47], "chocolate"],
    "D2B48C": [[210, 180, 140], [34, 44, 69], "tan"],
    "D3D3D3": [[211, 211, 211], [0, 0, 83], "lightGrey"],
    "D87093": [[219, 112, 147], [340, 60, 65], "paleVioletRed"],
    "D8BFD8": [[216, 191, 216], [300, 24, 80], "thistle"],
    "DA70D6": [[218, 112, 214], [302, 59, 65], "orchid"],
    "DAA520": [[218, 165, 32], [43, 74, 49], "goldenrod"],
    "DC143C": [[237, 164, 61], [35, 83, 58], "crimson"],
    "DCDCDC": [[220, 220, 220], [0, 0, 86], "gainsboro"],
    "DDA0DD": [[221, 160, 221], [300, 47, 75], "plum"],
    "DEB887": [[222, 184, 135], [34, 57, 70], "burlyWood"],
    "E0FFFF": [[224, 255, 255], [180, 100, 94], "lightCyan"],
    "E6E6FA": [[230, 230, 250], [240, 67, 94], "lavender"],
    "E9967A": [[233, 150, 122], [15, 72, 70], "darkSalmon"],
    "EE82EE": [[238, 130, 238], [300, 76, 72], "violet"],
    "EEE8AA": [[238, 232, 170], [55, 67, 80], "paleGoldenrod"],
    "F08080": [[240, 128, 128], [0, 79, 72], "lightCoral"],
    "F0E68C": [[240, 230, 140], [54, 77, 75], "khaki"],
    "F0F8FF": [[240, 248, 255], [208, 100, 97], "aliceBlue"],
    "F0FFF0": [[240, 255, 240], [120, 100, 97], "honeyDew"],
    "F0FFFF": [[240, 255, 255], [180, 100, 97], "azure"],
    "F4A460": [[244, 164, 96], [28, 87, 67], "sandyBrown"],
    "F5DEB3": [[245, 222, 179], [39, 77, 83], "wheat"],
    "F5F5DC": [[245, 245, 220], [60, 56, 91], "beige"],
    "F5F5F5": [[245, 245, 245], [0, 0, 96], "whiteSmoke"],
    "F5FFFA": [[245, 255, 250], [150, 100, 98], "mintCream"],
    "F8F8FF": [[248, 248, 255], [240, 100, 99], "ghostWhite"],
    "FA8072": [[250, 128, 114], [6, 93, 71], "salmon"],
    "FAEBD7": [[250, 235, 215], [34, 78, 91], "antiqueWhite"],
    "FAF0E6": [[250, 240, 230], [30, 67, 94], "linen"],
    "FAFAD2": [[250, 250, 210], [60, 80, 90], "lightGoldenrodYellow"],
    "FDF5E6": [[253, 245, 230], [39, 85, 95], "oldLace"],
    "FF0000": [[255, 0, 0], [0, 100, 50], "red"],
    "FF00FF": [[255, 0, 255], [300, 100, 50], "magenta"],
    "FF1493": [[255, 20, 147], [328, 100, 54], "deepPink"],
    "FF4500": [[255, 69, 0], [16, 100, 50], "orangeRed"],
    "FF6347": [[255, 99, 71], [9, 100, 64], "tomato"],
    "FF69B4": [[255, 105, 180], [330, 100, 71], "hotPink"],
    "FF7F50": [[255, 127, 80], [16, 100, 66], "coral"],
    "FF8C00": [[255, 140, 0], [33, 100, 50], "darkOrange"],
    "FFA07A": [[255, 160, 122], [17, 100, 74], "lightSalmon"],
    "FFA500": [[255, 165, 0], [39, 100, 50], "orange"],
    "FFB6C1": [[255, 182, 193], [351, 100, 86], "lightPink"],
    "FFC0CB": [[255, 192, 203], [350, 100, 88], "pink"],
    "FFD700": [[255, 215, 0], [51, 100, 50], "gold"],
    "FFDAB9": [[255, 218, 185], [28, 100, 86], "peachPuff"],
    "FFDEAD": [[255, 222, 173], [36, 100, 84], "navajoWhite"],
    "FFE4B5": [[255, 228, 181], [38, 100, 85], "moccasin"],
    "FFE4C4": [[255, 228, 196], [33, 100, 88], "bisque"],
    "FFE4E1": [[255, 228, 225], [6, 100, 94], "mistyRose"],
    "FFEBCD": [[255, 235, 205], [36, 100, 90], "blanchedAlmond"],
    "FFEFD5": [[255, 239, 213], [37, 100, 92], "papayaWhip"],
    "FFF0F5": [[255, 240, 245], [340, 100, 97], "lavenderBlush"],
    "FFF5EE": [[255, 245, 238], [25, 100, 97], "seaShell"],
    "FFF8DC": [[255, 248, 220], [48, 100, 93], "cornsilk"],
    "FFFACD": [[255, 250, 205], [54, 100, 90], "lemonChiffon"],
    "FFFAF0": [[255, 250, 240], [40, 100, 97], "floralWhite"],
    "FFFAFA": [[255, 250, 250], [0, 100, 99], "snow"],
    "FFFF00": [[255, 255, 0], [60, 100, 50], "yellow"],
    "FFFFE0": [[255, 255, 224], [60, 100, 94], "lightYellow"],
    "FFFFF0": [[255, 255, 240], [60, 100, 97], "ivory"],
    "FFFFFF": [[255, 255, 255], [0, 100, 100], "white"]
};

// Simple Values
WebInspector.Color.Nicknames = {
    "aliceblue": "F0F8FF",
    "antiquewhite": "FAEBD7",
    "aqua": "00FFFF",
    "aquamarine": "7FFFD4",
    "azure": "F0FFFF",
    "beige": "F5F5DC",
    "bisque": "FFE4C4",
    "black": "000000",
    "blanchedalmond": "FFEBCD",
    "blue": "0000FF",
    "blueviolet": "8A2BE2",
    "brown": "A52A2A",
    "burlywood": "DEB887",
    "cadetblue": "5F9EA0",
    "chartreuse": "7FFF00",
    "chocolate": "D2691E",
    "coral": "FF7F50",
    "cornflowerblue": "6495ED",
    "cornsilk": "FFF8DC",
    "crimson": "DC143C",
    "cyan": "00FFFF",
    "darkblue": "00008B",
    "darkcyan": "008B8B",
    "darkgoldenrod": "B8860B",
    "darkgray": "A9A9A9",
    "darkgreen": "006400",
    "darkkhaki": "BDB76B",
    "darkmagenta": "8B008B",
    "darkolivegreen": "556B2F",
    "darkorange": "FF8C00",
    "darkorchid": "9932CC",
    "darkred": "8B0000",
    "darksalmon": "E9967A",
    "darkseagreen": "8FBC8F",
    "darkslateblue": "483D8B",
    "darkslategray": "2F4F4F",
    "darkturquoise": "00CED1",
    "darkviolet": "9400D3",
    "deeppink": "FF1493",
    "deepskyblue": "00BFFF",
    "dimgray": "696969",
    "dodgerblue": "1E90FF",
    "firebrick": "B22222",
    "floralwhite": "FFFAF0",
    "forestgreen": "228B22",
    "fuchsia": "FF00FF",
    "gainsboro": "DCDCDC",
    "ghostwhite": "F8F8FF",
    "gold": "FFD700",
    "goldenrod": "DAA520",
    "gray": "808080",
    "green": "008000",
    "greenyellow": "ADFF2F",
    "honeydew": "F0FFF0",
    "hotpink": "FF69B4",
    "indianred": "CD5C5C",
    "indigo": "4B0082",
    "ivory": "FFFFF0",
    "khaki": "F0E68C",
    "lavender": "E6E6FA",
    "lavenderblush": "FFF0F5",
    "lawngreen": "7CFC00",
    "lemonchiffon": "FFFACD",
    "lightblue": "ADD8E6",
    "lightcoral": "F08080",
    "lightcyan": "E0FFFF",
    "lightgoldenrodyellow": "FAFAD2",
    "lightgreen": "90EE90",
    "lightgrey": "D3D3D3",
    "lightpink": "FFB6C1",
    "lightsalmon": "FFA07A",
    "lightseagreen": "20B2AA",
    "lightskyblue": "87CEFA",
    "lightslategray": "778899",
    "lightsteelblue": "B0C4DE",
    "lightyellow": "FFFFE0",
    "lime": "00FF00",
    "limegreen": "32CD32",
    "linen": "FAF0E6",
    "magenta": "FF00FF",
    "maroon": "800000",
    "mediumaquamarine": "66CDAA",
    "mediumblue": "0000CD",
    "mediumorchid": "BA55D3",
    "mediumpurple": "9370D8",
    "mediumseagreen": "3CB371",
    "mediumslateblue": "7B68EE",
    "mediumspringgreen": "00FA9A",
    "mediumturquoise": "48D1CC",
    "mediumvioletred": "C71585",
    "midnightblue": "191970",
    "mintcream": "F5FFFA",
    "mistyrose": "FFE4E1",
    "moccasin": "FFE4B5",
    "navajowhite": "FFDEAD",
    "navy": "000080",
    "oldlace": "FDF5E6",
    "olive": "808000",
    "olivedrab": "6B8E23",
    "orange": "FFA500",
    "orangered": "FF4500",
    "orchid": "DA70D6",
    "palegoldenrod": "EEE8AA",
    "palegreen": "98FB98",
    "paleturquoise": "AFEEEE",
    "palevioletred": "D87093",
    "papayawhip": "FFEFD5",
    "peachpuff": "FFDAB9",
    "peru": "CD853F",
    "pink": "FFC0CB",
    "plum": "DDA0DD",
    "powderblue": "B0E0E6",
    "purple": "800080",
    "red": "FF0000",
    "rosybrown": "BC8F8F",
    "royalblue": "4169E1",
    "saddlebrown": "8B4513",
    "salmon": "FA8072",
    "sandybrown": "F4A460",
    "seagreen": "2E8B57",
    "seashell": "FFF5EE",
    "sienna": "A0522D",
    "silver": "C0C0C0",
    "skyblue": "87CEEB",
    "slateblue": "6A5ACD",
    "slategray": "708090",
    "snow": "FFFAFA",
    "springgreen": "00FF7F",
    "steelblue": "4682B4",
    "tan": "D2B48C",
    "teal": "008080",
    "thistle": "D8BFD8",
    "tomato": "FF6347",
    "turquoise": "40E0D0",
    "violet": "EE82EE",
    "wheat": "F5DEB3",
    "white": "FFFFFF",
    "whitesmoke": "F5F5F5",
    "yellow": "FFFF00",
    "yellowgreen": "9ACD32"
};

// Advanced Values [rgba, hsla, nickname]
WebInspector.Color.AdvancedNickNames = {
    "transparent": [[0, 0, 0, 0], [0, 0, 0, 0], "transparent"],
    "rgba(0,0,0,0)": [[0, 0, 0, 0], [0, 0, 0, 0], "transparent"],
    "hsla(0,0,0,0)": [[0, 0, 0, 0], [0, 0, 0, 0], "transparent"],
};
