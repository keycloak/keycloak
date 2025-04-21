/**
 * @summary     pfResize for DataTables
 * @description A collection of API methods providing resize functionality in DataTables. This ensures DataTables
 * meets the Patternfly design pattern for IE browsers. Inline actions are typically located in the last column of
 * DataTables and expected to be styled with table-view-pf-actions. Inline action buttons must also be wrapped with the
 * table-view-pf-btn class.
 *
 * Example:
 *
 * <!-- NOTE: Some configuration may be omitted for clarity -->
 * ...
 * <table class="table table-striped table-bordered table-hover" id="table1">
 *   <thead>
 *     <tr>
 *       <th><input type="checkbox" name="selectAll"></th>
 *       <th>Rendering Engine</th>
 *       <th>Browser</th>
 *       <th colspan="2">Actions</th>
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
 *       { data: null,
 *         className: "table-view-pf-actions",
 *         render: function (data, type, full, meta) {
 *           // Inline action button renderer
 *           return '<div class="table-view-pf-btn"><button class="btn btn-default" type="button">Actions</button></div>';
 *         }
 *       }, {
 *         data: null,
 *         className: "table-view-pf-actions",
 *         render: function (data, type, full, meta) {
 *           // Inline action kebab renderer
 *           return '<div class="dropdown dropdown-kebab-pf">' +
 *             '<button class="btn btn-default dropdown-toggle" type="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">' +
 *             '<span class="fa fa-ellipsis-v"></span></button>' +
 *             '<ul class="dropdown-menu dropdown-menu-right" aria-labelledby="dropdownKebabRight">' +
 *             '<li><a href="#">Action</a></li>' +
 *             '<li><a href="#">Another action</a></li>' +
 *             '<li><a href="#">Something else here</a></li>' +
 *             '<li role="separator" class="divider"></li>' +
 *             '<li><a href="#">Separated link</a></li></ul></div>';
 *         }
 *       }
 *     ],
 *     data: [
 *       { engine: "Gecko", browser: "Firefox" }
 *       { engine: "Trident", browser: "Mozilla" }
 *     ],
 *     dom: "t",
 *     pfConfig: {
 *       ...
 *     }
 *   });
 *   dt.table().pfResize.resize(); // Optional API to force resize
 * });
 * </script>
 *
 * Note: This functionality requires the following Javascript library files to be loaded:
 *
 * https://cdn.datatables.net/select/1.2.0/js/dataTables.select.min.js
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
  var BUTTON_ACTIONS_SELECTOR = "td.table-view-pf-actions .table-view-pf-btn";
  var KEBAB_ACTIONS_SELECTOR = "td.table-view-pf-actions .dropdown";

  DataTable.pfResize = {};

  /**
   * Initialize
   *
   * @param {DataTable.Api} dt DataTable
   * @private
   */
  DataTable.pfResize.init = function (dt) {
    var ctx = dt.settings()[0];

    ctx._pfResize = {};

    if (isIE()) {
      // Resize buttons to fill table cells.
      $(window).on("resize", function () {
        resize(dt);
      });

      // Initialize
      resize(dt);
    }
  };

  // Local functions

  /**
   * Get inline actions
   *
   * @param {DataTable.Api} dt DataTable
   * @private
   */
  function getActions (dt) {
    return $(BUTTON_ACTIONS_SELECTOR + ", " + KEBAB_ACTIONS_SELECTOR, dt.table().container());
  }

  /**
   * Detect IE
   *
   * @return {boolean} True if IE is detected
   * @private
   */
  function isIE () {
    return /(MSIE|Trident\/|Edge\/)/i.test(window.navigator.userAgent);
  }

  /**
   * Reset table cell height prior to resizing inline actions
   *
   * @param {DataTable.Api} dt DataTable
   * @param {Object} actions Inline actions to reset height
   * @private
   */
  function resetTableCells (dt, actions) {
    if (actions === undefined || actions.length === 0) {
      return;
    }
    $(actions).each(function (index, el) {
      $(el).css({height: "auto"});
    });
  }

  /**
   * Resize inline actions
   *
   * @param {DataTable.Api} dt DataTable
   * @private
   */
  function resize (dt) {
    var actions = $(BUTTON_ACTIONS_SELECTOR + ", " + KEBAB_ACTIONS_SELECTOR, dt.table().container());
    resetTableCells(dt, actions);
    resizeInlineActions(dt, actions);
  }

  /**
   * Resize inline actions to fill table cells
   *
   * @param {DataTable.Api} dt DataTable
   * @param {Object} actions Inline actions to resize
   * @private
   */
  function resizeInlineActions (dt, actions) {
    if (actions === undefined || actions.length === 0) {
      return;
    }
    $(actions).each(function (index, el) {
      var parent = $(el).parent("td");
      if (parent === undefined || parent.length === 0) {
        return;
      }
      $(el).css({height: parent[0].clientHeight});
    });
  }

  // DataTables API

  /**
   * Resize inline actions
   *
   * Example: dt.table().pfResize.resize();
   */
  DataTable.Api.register("pfResize.resize()", function () {
    return this.iterator("table", function (ctx) {
      resize(new DataTable.Api(ctx));
    });
  });

  // DataTables creation
  $(document).on("init.dt", function (e, ctx, json) {
    if (e.namespace !== "dt") {
      return;
    }
    DataTable.pfResize.init(new DataTable.Api(ctx));
  });
  return DataTable.pfResize;
}));
