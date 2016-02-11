/*
 * Copyright (C) 2009 Apple Inc. All rights reserved.
 * Copyright (C) 2009 Google Inc. All rights reserved.
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

WebInspector.KeyboardShortcut = function()
{
};

/**
 * Constants for encoding modifier key set as a bit mask.
 * @see #_makeKeyFromCodeAndModifiers
 */
WebInspector.KeyboardShortcut.Modifiers = {
    None: 0,   // Constant for empty modifiers set.
    Shift: 1,
    Ctrl: 2,
    Alt: 4,
    Meta: 8,   // Command key on Mac, Win key on other platforms.
    get CtrlOrMeta()
    {
        // "default" command/ctrl key for platform, Command on Mac, Ctrl on other platforms
        return WebInspector.isMac() ? this.Meta : this.Ctrl;
    }
};

WebInspector.KeyboardShortcut.Keys = {
    Backspace: { code: 8, name: "\u21a4" },
    Tab: { code: 9, name: { mac: "\u21e5", other: "<Tab>" } },
    Enter: { code: 13, name: { mac: "\u21a9", other: "<Enter>" } },
    Esc: { code: 27, name: { mac: "\u238b", other: "<Esc>" } },
    Space: { code: 32, name: "<Space>" },
    PageUp: { code: 33,  name: { mac: "\u21de", other: "<PageUp>" } },      // also NUM_NORTH_EAST
    PageDown: { code: 34, name: { mac: "\u21df", other: "<PageDown>" } },   // also NUM_SOUTH_EAST
    End: { code: 35, name: { mac: "\u2197", other: "<End>" } },             // also NUM_SOUTH_WEST
    Home: { code: 36, name: { mac: "\u2196", other: "<Home>" } },           // also NUM_NORTH_WEST
    Left: { code: 37, name: "\u2190" },           // also NUM_WEST
    Up: { code: 38, name: "\u2191" },             // also NUM_NORTH
    Right: { code: 39, name: "\u2192" },          // also NUM_EAST
    Down: { code: 40, name: "\u2193" },           // also NUM_SOUTH
    Delete: { code: 46, name: "<Del>" },
    Zero: { code: 48, name: "0" },
    F1: { code: 112, name: "F1" },
    F2: { code: 113, name: "F2" },
    F3: { code: 114, name: "F3" },
    F4: { code: 115, name: "F4" },
    F5: { code: 116, name: "F5" },
    F6: { code: 117, name: "F6" },
    F7: { code: 118, name: "F7" },
    F8: { code: 119, name: "F8" },
    F9: { code: 120, name: "F9" },
    F10: { code: 121, name: "F10" },
    F11: { code: 122, name: "F11" },
    F12: { code: 123, name: "F12" },
    Semicolon: { code: 186, name: ";" },
    Plus: { code: 187, name: "+" },
    Comma: { code: 188, name: "," },
    Minus: { code: 189, name: "-" },
    Period: { code: 190, name: "." },
    Slash: { code: 191, name: "/" },
    Apostrophe: { code: 192, name: "`" },
    SingleQuote: { code: 222, name: "\'" }
};

/**
 * Creates a number encoding keyCode in the lower 8 bits and modifiers mask in the higher 8 bits.
 * It is useful for matching pressed keys.
 * keyCode is the Code of the key, or a character "a-z" which is converted to a keyCode value.
 * optModifiers is an Optional list of modifiers passed as additional paramerters.
 */
WebInspector.KeyboardShortcut.makeKey = function(keyCode, optModifiers)
{
    if (typeof keyCode === "string")
        keyCode = keyCode.charCodeAt(0) - 32;
    var modifiers = WebInspector.KeyboardShortcut.Modifiers.None;
    for (var i = 1; i < arguments.length; i++)
        modifiers |= arguments[i];
    return WebInspector.KeyboardShortcut._makeKeyFromCodeAndModifiers(keyCode, modifiers);
};

WebInspector.KeyboardShortcut.makeKeyFromEvent = function(keyboardEvent)
{
    var modifiers = WebInspector.KeyboardShortcut.Modifiers.None;
    if (keyboardEvent.shiftKey)
        modifiers |= WebInspector.KeyboardShortcut.Modifiers.Shift;
    if (keyboardEvent.ctrlKey)
        modifiers |= WebInspector.KeyboardShortcut.Modifiers.Ctrl;
    if (keyboardEvent.altKey)
        modifiers |= WebInspector.KeyboardShortcut.Modifiers.Alt;
    if (keyboardEvent.metaKey)
        modifiers |= WebInspector.KeyboardShortcut.Modifiers.Meta;
    return WebInspector.KeyboardShortcut._makeKeyFromCodeAndModifiers(keyboardEvent.keyCode, modifiers);
};

WebInspector.KeyboardShortcut.makeDescriptor = function(key, optModifiers)
{
    var modifiers = 0;
    for (var i = 1; i < arguments.length; i++)
        modifiers |= arguments[i];

    return {
        key: WebInspector.KeyboardShortcut.makeKey(typeof key === "string" ? key : key.code, modifiers),
        name: WebInspector.KeyboardShortcut.shortcutToString(key, modifiers)
    };
}

WebInspector.KeyboardShortcut.shortcutToString = function(key, modifiers)
{
    return WebInspector.KeyboardShortcut._modifiersToString(modifiers) + WebInspector.KeyboardShortcut._keyName(key);
}

WebInspector.KeyboardShortcut._keyName = function(key)
{
    if (typeof key === "string")
        return key.toUpperCase();
    if (typeof key.name === "string")
        return key.name;
    return key.name[WebInspector.platform] || key.name.other;
}

WebInspector.KeyboardShortcut._makeKeyFromCodeAndModifiers = function(keyCode, modifiers)
{
    return (keyCode & 255) | (modifiers << 8);
};

WebInspector.KeyboardShortcut._modifiersToString = function(modifiers)
{
    var cmdKey = "\u2318";
    var optKey = "\u2325";
    var shiftKey = "\u21e7";
    var ctrlKey = "\u2303";

    var isMac = WebInspector.isMac();
    var res = "";
    if (modifiers & WebInspector.KeyboardShortcut.Modifiers.Ctrl)
        res += isMac ? ctrlKey : "<Ctrl> + ";
    if (modifiers & WebInspector.KeyboardShortcut.Modifiers.Alt)
        res += isMac ? optKey : "<Alt> + ";
    if (modifiers & WebInspector.KeyboardShortcut.Modifiers.Shift)
        res += isMac ? shiftKey : "<Shift> + ";
    if (modifiers & WebInspector.KeyboardShortcut.Modifiers.Meta)
        res += isMac ? cmdKey : "<Win> + ";

    return res;
};
