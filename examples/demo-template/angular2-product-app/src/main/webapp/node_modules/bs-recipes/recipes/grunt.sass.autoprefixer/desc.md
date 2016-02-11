
This example shows how you can chain potentially slow-running tasks, but still achieve CSS
Injection. The trick, as seen below, is to use the `bsReload` task that now comes 
bundled with `grunt-browser-sync` since `2.1.0`

Don't forget the `spawn: false` option for the watch task - it's a requirement
that allows Browsersync to work correctly

```js
watch: {
    options: {
        spawn: false // Important, don't remove this!
    },
    files: 'app/**/*.scss',
    tasks: ['sass', 'autoprefixer', 'bsReload:css']
},
```
