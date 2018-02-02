/*!
 * Bootstrap-select v1.12.4 (http://silviomoreto.github.io/bootstrap-select)
 *
 * Copyright 2013-2017 bootstrap-select
 * Licensed under MIT (https://github.com/silviomoreto/bootstrap-select/blob/master/LICENSE)
 */
!function(a,b){"function"==typeof define&&define.amd?define(["jquery"],function(a){return b(a)}):"object"==typeof module&&module.exports?module.exports=b(require("jquery")):b(a.jQuery)}(this,function(a){!function(a){a.fn.selectpicker.defaults={noneSelectedText:"Niekas nepasirinkta",noneResultsText:"Niekas nesutapo su {0}",countSelectedText:function(a,b){return 1==a?"{0} elementas pasirinktas":"{0} elementai(-ų) pasirinkta"},maxOptionsText:function(a,b){return[1==a?"Pasiekta riba ({n} elementas daugiausiai)":"Riba pasiekta ({n} elementai(-ų) daugiausiai)",1==b?"Grupės riba pasiekta ({n} elementas daugiausiai)":"Grupės riba pasiekta ({n} elementai(-ų) daugiausiai)"]},selectAllText:"Pasirinkti visus",deselectAllText:"Atmesti visus",multipleSeparator:", "}}(a)});