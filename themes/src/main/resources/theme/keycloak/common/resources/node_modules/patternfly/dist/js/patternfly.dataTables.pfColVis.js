/**
 * @summary     pfColVis for DataTables
 * @description An extension providing columns visibility control functionality for DataTables. This ensures
 * DataTables meets the Patternfly design pattern with a toolbar.
 *
 * To enable a colvis, the user just need to specify a region for placing colvis menu. By default, the colvis
 * menu includes the items derived from all columns of Datatables. And of course user can also limit the
 * generation of the column's visibility checkbox through picking out column index.
 *
 * The toolbar is expected to contain the classes as shown in the example below.
 *
 * Example:
 *
 * <!-- NOTE: Some configuration may be omitted for clarity -->
 * <div class="row toolbar-pf table-view-pf-toolbar" id="toolbar1">
 *   <div class="col-sm-12">
 *     <form class="toolbar-pf-actions">
 *       <div class="form-group toolbar-pf-filter">
 *       </div>
 *       <div class="form-group toolbar-pf-sort">
 *         <div class="dropdown btn-group">
 *         </div>
 *         <button class="btn btn-link" type="button">
 *           <span class="fa fa-sort-alpha-asc"></span>
 *         </button>
 *         <div class="dropdown btn-group">
 *          <button class="btn btn-link dropdown-toggle" type="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
 *           <span class="fa fa-columns"></span>
 *          </button>
 *          <ul class="dropdown-menu table-view-pf-colvis-menu">
 *            <li><input type="checkbox" value="1" checked><label>Rendering Engine</label></li>
 *            <li><input type="checkbox" value="2" checked><label>Browser</label></li>
 *            <li><input type="checkbox" value="3" checked><label>Platform(s)</label></li>
 *            <li><input type="checkbox" value="4" checked><label>Engine Version</label></li>
 *            <li><input type="checkbox" value="5" checked><label>CSS Grade</label></li>
 *          </ul>
 *         </div>
 *       </div>
 *       ...
 *     </form>
 *   </div>
 * </div>
 * <table class="table table-striped table-bordered table-hover" id="table1">
 *   <thead>
 *     <tr>
 *       <th><input type="checkbox" name="selectAll"></th>
 *       <th>Rendering Engine</th>
 *       <th>Browser</th>
 *     </tr>
 *   </thead>
 * </table>
 * ...
 * <script>
 * // NOTE: Some properties may be omitted for clarity
 * $(document).ready(function() {
 *   var dt = $("#table1").DataTable({
 *     columns: [
 *       { data: null, ... },
 *       { data: "engine" },
 *       { data: "browser" }
 *     ],
 *     data: [
 *       { engine: "Gecko", browser: "Firefox" }
 *       { engine: "Trident", browser: "Mozilla" }
 *     ],
 *     dom: "t",
 *     pfConfig: {
 *       ...
 *       colvisMenuSelector: '.table-view-pf-colvis-menu'
 *     }
 *   });
 *
 * });
 * </script>
 */
(function (factory) {
  "use strict";
  if (typeof define === "function" && define.amd ) {
    // AMD
    define (["jquery", "datatables.net"], function ($) {
      return factory ($, window, document);
    });
  } else if (typeof exports === "object") {
    // CommonJS
    module.exports = function (root, $) {
      if (!root) {
        root = window;
      }
      if (!$ || !$.fn.dataTable) {
        $ = require("datatables.net")(root, $).$;
      }
      return factory($, root, root.document);
    };
  } else {
    // Browser
    factory(jQuery, window, document);
  }
}(function ($, window, document, undefined) {
  "use strict";
  var DataTable = $.fn.dataTable;

  DataTable.pfColVis = {};

  /**
   * Initialize
   *
   * @param {DataTable.Api} dt DataTable
   * @private
   */
  DataTable.pfColVis.init = function (dt) {
    var i;
    var ctx = dt.settings()[0];
    var opts = (ctx.oInit.pfConfig) ? ctx.oInit.pfConfig : {};

    if (opts.colvisMenuSelector === undefined) {
      return;
    }

    ctx._pfColVis = {};
    ctx._pfColVis.colvisMenu = $(opts.colvisMenuSelector, opts.toolbarSelector);

    // Attach event handler for checkbox of ColVis menu
    ctx._pfColVis.colvisMenu.on('click', 'li', { 'dt': dt }, colvisMenuHandler);
  };

  // Local functions

  /**
   * Handle actions when ColVis menu items are toggled
   *
   * @param {object} jquery eventObject - click event of ColVis menu items
   * @private
   */
  function colvisMenuHandler (event) {
    var $check = $(this).children(':checkbox');
    if (event.target.nodeName !== 'INPUT') {
      event.stopPropagation();
      $check.prop('checked', !$check.prop('checked'));
    }
    event.data.dt.column($check.val()).visible($check.prop('checked'));
  }

  // DataTables creation
  $(document).on("init.dt", function (e, ctx, json) {
    if (e.namespace !== "dt") {
      return;
    }
    DataTable.pfColVis.init(new DataTable.Api(ctx));
  });
  return DataTable.pfColVis;
}));