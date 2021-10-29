/*!
 * Bootstrap-select v1.12.2 (http://silviomoreto.github.io/bootstrap-select)
 *
 * Copyright 2013-2017 bootstrap-select
 * Licensed under MIT (https://github.com/silviomoreto/bootstrap-select/blob/master/LICENSE)
 */
!function(a,b){"function"==typeof define&&define.amd?define(["jquery"],function(a){return b(a)}):"object"==typeof module&&module.exports?module.exports=b(require("jquery")):b(a.jQuery)}(this,function(a){!function(a){a.fn.selectpicker.defaults={noneSelectedText:"Odaberite stavku",noneResultsText:"Nema rezultata pretrage {0}",countSelectedText:function(a,b){return 1==a?"{0} stavka selektirana":"{0} stavke selektirane"},maxOptionsText:function(a,b){return[1==a?"Limit je postignut ({n} stvar maximalno)":"Limit je postignut ({n} stavke maksimalno)",1==b?"Grupni limit je postignut ({n} stvar maksimalno)":"Grupni limit je postignut ({n} stavke maksimalno)"]},selectAllText:"Selektiraj sve",deselectAllText:"Deselektiraj sve",multipleSeparator:", "}}(a)});