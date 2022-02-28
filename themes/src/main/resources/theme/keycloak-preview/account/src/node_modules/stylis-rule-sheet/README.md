# stylis rule-sheet

stylis plugin to extract individual rules to use with the insertRule API

```js
var sheet = document.head.appendChild(document.createElement('style')).sheet
var length = sheet.cssRules.length

var plugin = stylisRuleSheet((value) => {
	length = sheet.insertRule(value, length) + 1
})

stylis.use(plugin)
```
