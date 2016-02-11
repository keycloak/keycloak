"use strict";
/**
 * @module BrowserSync.options
 */
module.exports = {

    /**
     * Browsersync includes a user-interface that is accessed via a separate port.
     * The UI allows to controls all devices, push sync updates and much more.
     * @property ui
     * @type Object
     * @param {Number} [port=3001]
     * @param {Number} [weinre.port=8080]
     * @since 2.0.0
     * @default false
     */
    ui: {
        port: 3001,
        weinre: {
            port: 8080
        }
    },

    /**
     * Browsersync can watch your files as you work. Changes you make will either
     * be injected into the page (CSS & images) or will cause all browsers to do
     * a full-page refresh. See [anymatch](https://github.com/es128/anymatch) for more information on glob patterns.
     * @property files
     * @type Array|String
     * @default false
     */
    files: false,

    /**
     * File watching options that get passed along to [Chokidar](https://github.com/paulmillr/chokidar).
     * Check their docs for available options
     * @property watchOptions
     * @type Object
     * @default undefined
     * @since 2.6.0
     */
    watchOptions: {
        /*
         persistent: true,

         ignored: '*.txt',
         ignoreInitial: false,
         followSymlinks: true,
         cwd: '.',

         usePolling: true,
         alwaysStat: false,
         depth: undefined,
         interval: 100,

         ignorePermissionErrors: false,
         atomic: true
         */
    },

    /**
     * Use the built-in static server for basic HTML/JS/CSS websites.
     * @property server
     * @type Object|Boolean
     * @default false
     */
    server: false,

    /**
     * Proxy an EXISTING vhost. Browsersync will wrap your vhost with a proxy URL to view your site.
     * @property proxy
     * @type String|Object|Boolean
     * @param {String} [target]
     * @param {Boolean} [ws] - Enable websocket proxying
     * @param {Function|Array} [middleware]
     * @param {Function} [reqHeaders]
     * @param {Array} [proxyRes]
     * @default false
     */
    proxy: false,

    /**
     * @property port
     * @type Number
     * @default 3000
     */
    port: 3000,

    /**
     * @property middleware
     * @type Function|Array
     * @default false
     */
    middleware: false,

    /**
     * Add additional directories from which static
     * files should be served. Should only be used in `proxy` or `snippet`
     * mode.
     * @property serveStatic
     * @type Array
     * @default []
     * @since 2.8.0
     */
    serveStatic: [],

    /**
     * Enable https for localhost development. **Note** - this is not needed for proxy
     * option as it will be inferred from your target url.
     * @property https
     * @type Boolean
     * @default undefined
     * @since 1.3.0
     */

    /**
     * Clicks, Scrolls & Form inputs on any device will be mirrored to all others.
     * @property ghostMode
     * @param {Boolean} [clicks=true]
     * @param {Boolean} [scroll=true]
     * @param {Boolean} [forms=true]
     * @param {Boolean} [forms.submit=true]
     * @param {Boolean} [forms.inputs=true]
     * @param {Boolean} [forms.toggles=true]
     * @type Object
     */
    ghostMode: {
        clicks: true,
        scroll: true,
        forms: {
            submit: true,
            inputs: true,
            toggles: true
        }
    },

    /**
     * Can be either "info", "debug", "warn", or "silent"
     * @property logLevel
     * @type String
     * @default info
     */
    logLevel: "info",

    /**
     * Change the console logging prefix. Useful if you're creating your
     * own project based on Browsersync
     * @property logPrefix
     * @type String
     * @default BS
     * @since 1.5.1
     */
    logPrefix: "BS",

    /**
     * @property logConnections
     * @type Boolean
     * @default false
     */
    logConnections: false,

    /**
     * @property logFileChanges
     * @type Boolean
     * @default true
     */
    logFileChanges: true,

    /**
     * Log the snippet to the console when you're in snippet mode (no proxy/server)
     * @property logSnippet
     * @type: Boolean
     * @default true
     * @since 1.5.2
     */
    logSnippet: true,

    /**
     * You can control how the snippet is injected
     * onto each page via a custom regex + function.
     * You can also provide patterns for certain urls
     * that should be ignored from the snippet injection.
     * @property snippetOptions
     * @since 2.0.0
     * @param {Boolean} [async] - should the script tags have the async attribute?
     * @param {Array} [blacklist]
     * @param {Array} [whitelist]
     * @param {RegExp} [rule.match=/&lt;body&#91;^&gt;&#93;*&gt;/i]
     * @param {Function} [rule.fn=Function]
     * @type Object
     */
    snippetOptions: {
        async: true,
        whitelist: [],
        blacklist: [],
        rule: {
            match: /<body[^>]*>/i,
            fn: function (snippet, match) {
                return match + snippet;
            }
        }
    },

    /**
     * Add additional HTML rewriting rules.
     * @property rewriteRules
     * @since 2.4.0
     * @type Array|Boolean
     * @default false
     */
    rewriteRules: false,

    /**
     * @property tunnel
     * @type String|Boolean
     * @default null
     */

    /**
     * Some features of Browsersync (such as `xip` & `tunnel`) require an internet connection, but if you're
     * working offline, you can reduce start-up time by setting this option to `false`
     * @property online
     * @type Boolean
     * @default undefined
     */

    /**
     * Decide which URL to open automatically when Browsersync starts. Defaults to "local" if none set.
     * Can be true, `local`, `external`, `ui`, `ui-external`, `tunnel` or `false`
     * @property open
     * @type Boolean|String
     * @default true
     */
    open: "local",

    /**
     * @property browser
     * @type String|Array
     * @default default
     */
    browser: "default",

    /**
     * Requires an internet connection - useful for services such as [Typekit](https://typekit.com/)
     * as it allows you to configure domains such as `*.xip.io` in your kit settings
     * @property xip
     * @type Boolean
     * @default false
     */
    xip: false,

    hostnameSuffix: false,

    /**
     * Reload each browser when Browsersync is restarted.
     * @property reloadOnRestart
     * @type Boolean
     * @default false
     */
    reloadOnRestart: false,

    /**
     * The small pop-over notifications in the browser are not always needed/wanted.
     * @property notify
     * @type Boolean
     * @default true
     */
    notify: true,

    /**
     * @property scrollProportionally
     * @type Boolean
     * @default true
     */
    scrollProportionally: true,

    /**
     * @property scrollThrottle
     * @type Number
     * @default 0
     */
    scrollThrottle: 0,

    /**
     * Decide which technique should be used to restore
     * scroll position following a reload.
     * Can be `window.name` or `cookie`
     * @property scrollRestoreTechnique
     * @type String
     * @default 'window.name'
     */
    scrollRestoreTechnique: "window.name",

    /**
     * Sync the scroll position of any element
     * on the page. Add any amount of CSS selectors
     * @property scrollElements
     * @type Array
     * @default []
     * @since 2.9.0
     */
    scrollElements: [],

    /**
     * Sync the scroll position of any element
     * on the page - where any scrolled element
     * will cause all others to match scroll position.
     * This is helpful when a breakpoint alters which element
     * is actually scrolling
     * @property scrollElementMapping
     * @type Array
     * @default []
     * @since 2.9.0
     */
    scrollElementMapping: [],

    /**
     * Time, in milliseconds, to wait before
     * instructing the browser to reload/inject following a
     * file change event
     * @property reloadDelay
     * @type Number
     * @default 0
     */
    reloadDelay: 0,

    /**
     * Restrict the frequency in which browser:reload events
     * can be emitted to connected clients
     * @property reloadDebounce
     * @type Number
     * @default 0
     * @since 2.6.0
     */
    reloadDebounce: 0,

    /**
     * User provided plugins
     * @property plugins
     * @type Array
     * @default []
     * @since 2.6.0
     */
    plugins: [],

    /**
     * @property injectChanges
     * @type Boolean
     * @default true
     */
    injectChanges: true,

    /**
     * @property startPath
     * @type String|Null
     * @default null
     */
    startPath: null,

    /**
     * Whether to minify client script, or not.
     * @property minify
     * @type Boolean
     * @default true
     */
    minify: true,

    /**
     * @property host
     * @type String
     * @default null
     */
    host: null,

    /**
     * @property codeSync
     * @type Boolean
     * @default true
     */
    codeSync: true,

    /**
     * @property timestamps
     * @type Boolean
     * @default true
     */
    timestamps: true,

    clientEvents: [
        "scroll",
        "scroll:element",
        "input:text",
        "input:toggles",
        "form:submit",
        "form:reset",
        "click"
    ],

    /**
     * Alter the script path for complete control over where the Browsersync
     * Javascript is served from. Whatever you return from this function
     * will be used as the script path.
     * @property scriptPath
     * @default undefined
     * @since 1.5.0
     * @type Function
     */

    /**
     * Configure the Socket.IO path and namespace & domain to avoid collisions.
     * @property socket
     * @param {String} [path="/browser-sync/socket.io"]
     * @param {String} [clientPath="/browser-sync"]
     * @param {String|Function} [namespace="/browser-sync"]
     * @param {String|Function} [domain=undefined]
     * @param {String|Function} [port=undefined]
     * @param {Object} [clients.heartbeatTimeout=5000]
     * @since 1.6.2
     * @type Object
     */
    socket: {
        socketIoOptions: {
            log: false
        },
        socketIoClientConfig: {
            reconnectionAttempts: 50
        },
        path: "/browser-sync/socket.io",
        clientPath: "/browser-sync",
        namespace: "/browser-sync",
        clients: {
            heartbeatTimeout: 5000
        }
    },

    tagNames: {
        "less": "link",
        "scss": "link",
        "css":  "link",
        "jpg":  "img",
        "jpeg": "img",
        "png":  "img",
        "svg":  "img",
        "gif":  "img",
        "js":   "script"
    },

    injectFileTypes: ["css", "png", "jpg", "jpeg", "svg", "gif", "webp"],
    excludedFileTypes: [
        "js",
        "css",
        "pdf",
        "map",
        "svg",
        "ico",
        "woff",
        "json",
        "eot",
        "ttf",
        "png",
        "jpg",
        "jpeg",
        "webp",
        "gif",
        "mp4",
        "mp3",
        "3gp",
        "ogg",
        "ogv",
        "webm",
        "m4a",
        "flv",
        "wmv",
        "avi",
        "swf",
        "scss"
    ]
};
