/*!
 * Bootstrap-select v1.12.4 (http://silviomoreto.github.io/bootstrap-select)
 *
 * Copyright 2013-2017 bootstrap-select
 * Licensed under MIT (https://github.com/silviomoreto/bootstrap-select/blob/master/LICENSE)
 */
!function(a,b){"function"==typeof define&&define.amd?define(["jquery"],function(a){return b(a)}):"object"==typeof module&&module.exports?module.exports=b(require("jquery")):b(a.jQuery)}(this,function(a){!function(a){a.fn.selectpicker.defaults={noneSelectedText:"Нищо избрано",noneResultsText:"Няма резултат за {0}",countSelectedText:function(a,b){return 1==a?"{0} избран елемент":"{0} избрани елемента"},maxOptionsText:function(a,b){return[1==a?"Лимита е достигнат ({n} елемент максимум)":"Лимита е достигнат ({n} елемента максимум)",1==b?"Груповия лимит е достигнат ({n} елемент максимум)":"Груповия лимит е достигнат ({n} елемента максимум)"]},selectAllText:"Избери всички",deselectAllText:"Размаркирай всички",multipleSeparator:", "}}(a)});