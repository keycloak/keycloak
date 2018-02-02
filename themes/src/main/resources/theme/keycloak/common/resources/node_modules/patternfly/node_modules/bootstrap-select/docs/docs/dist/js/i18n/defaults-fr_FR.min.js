/*!
 * Bootstrap-select v1.12.4 (http://silviomoreto.github.io/bootstrap-select)
 *
 * Copyright 2013-2017 bootstrap-select
 * Licensed under MIT (https://github.com/silviomoreto/bootstrap-select/blob/master/LICENSE)
 */
!function(a,b){"function"==typeof define&&define.amd?define(["jquery"],function(a){return b(a)}):"object"==typeof module&&module.exports?module.exports=b(require("jquery")):b(a.jQuery)}(this,function(a){!function(a){a.fn.selectpicker.defaults={noneSelectedText:"Aucune sélection",noneResultsText:"Aucun résultat pour {0}",countSelectedText:function(a,b){return a>1?"{0} éléments sélectionnés":"{0} élément sélectionné"},maxOptionsText:function(a,b){return[a>1?"Limite atteinte ({n} éléments max)":"Limite atteinte ({n} élément max)",b>1?"Limite du groupe atteinte ({n} éléments max)":"Limite du groupe atteinte ({n} élément max)"]},multipleSeparator:", ",selectAllText:"Tout sélectionner",deselectAllText:"Tout désélectionner"}}(a)});