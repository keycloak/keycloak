/**
 * @summary     pfSelect for DataTables
 * @description A collection of API methods providing individual row selection and select all functionality in
 * DataTables using traditional HTML checkboxes. This ensures DataTables meets the Patternfly design pattern while
 * maintaining accessibility.
 *
 * The following selection styles are supported for user interaction with DataTables:
 *
 * api - Selection can only be performed via the API
 * multi - Multiple items can be selected
 * multi+shift - a hybrid between the os style and multi
 * os - Operating system style selection with complex behaviors such as ctrl/cmd, shift and an unmodified click
 * single - Only a single item can be selected
 *
 * For details see: https://datatables.net/reference/option/select.style
 *
 * Note that when a selection is made, the selection results text is also updated in the toolbar. The toolbar layouts is
 * expected to contain the classes as shown in the example below. Selection checkboxes are also expected to be located
 * in the first column.
 *
 * Example:
 *
 * <!-- NOTE: Some configuration may be omitted for clarity -->
 * <div class="row toolbar-pf table-view-pf-toolbar" id="toolbar1">
 *   <div class="col-sm-12">
 *     ...
 *     <div class="row toolbar-pf-results">
 *       <div class="col-sm-9">
 *         ...
 *       </div>
 *       <div class="col-sm-3 table-view-pf-select-results">
 *         <strong>0</strong> of <strong>0</strong> selected
 *       </div>
 *     </div>
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
 *       { data: null,
 *         className: "table-view-pf-select",
 *         render: function (data, type, full, meta) {
 *           return '<input type="checkbox" name="select">';
 *         },
 *         sortable: false
 *       },
 *       { data: "engine" },
 *       { data: "browser" }
 *     ],
 *     data: [
 *       { engine: "Gecko", browser: "Firefox" }
 *       { engine: "Trident", browser: "Mozilla" }
 *     ],
 *     dom: "t",
 *     order: [[ 1, "asc" ]],
 *     pfConfig: {
 *       ...
 *       toolbarSelector: "#toolbar1",
 *       selectAllSelector: 'th:first-child input[type="checkbox"]'
 *     }
 *     select: {
 *       selector: 'td:first-child input[type="checkbox"]',
 *       style: "multi"
 *     }
 *   });
 *   dt.table().pfSelect.selectAllRows(true); // Optional API to select all rows
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
  var RESULTS_SELECTOR = ".table-view-pf-select-results"; // Toolbar selection results
  var SELECT_ALL_SELECTOR = 'th:first-child input[type="checkbox"]'; // Default checkbox for selecting all rows
  var SELECT_SELECTOR = 'td:first-child input[type="checkbox"]'; // Default checkboxes for row selection

  DataTable.pfSelect = {};

  /**
   * Initialize
   *
   * @param {DataTable.Api} dt DataTable
   * @private
   */
  DataTable.pfSelect.init = function (dt) {
    var ctx = dt.settings()[0];
    var opts = (ctx.oInit.pfConfig) ? ctx.oInit.pfConfig : {};
    var select = (ctx.oInit.select) ? ctx.oInit.select : {};
    var style = dt.select.style();

    ctx._pfSelect = {};
    ctx._pfSelect.selectAllSelector = (opts.selectAllSelector !== undefined)
      ? opts.selectAllSelector : SELECT_ALL_SELECTOR; // Select all checkbox
    ctx._pfSelect.selector = (select.selector !== undefined)
      ? select.selector : SELECT_SELECTOR; // Select checkbox
    ctx._pfSelect.results = $(RESULTS_SELECTOR, opts.toolbarSelector); // Toolbar selection results

    if (style === "api") {
      // Select all checkbox
      $(dt.table().container()).on("click", ctx._pfSelect.selectAllSelector, function (evt) {
        evt.preventDefault();
      });

      // Select checkboxes
      $(dt.table().container()).on("click", ctx._pfSelect.selector, function (evt) {
        evt.preventDefault();
      });

      dt.table().on("select.dt", function () {
        syncSelectCheckboxes(dt);
      });
    } else {
      // Select all checkbox
      $(dt.table().container()).on("click", ctx._pfSelect.selectAllSelector, function (evt) {
        selectAllRows(dt, evt.target.checked);
      });

      // Select checkboxes
      $(dt.table().container()).on("click", ctx._pfSelect.selector, function (evt) {
        if (style !== "multi" || style !== "multi+shift") {
          syncSelectCheckboxes(dt); // No need to sync checkbox selections when "multi" is used
        } else {
          syncSelectAllCheckbox(dt); // Still need to sync select all checkbox
        }
      });
    }

    // Sync checkbox selections when paging and filtering is applied
    dt.table().on("draw.dt", function () {
      syncSelectCheckboxes(dt);
    });

    // Initialize selected rows text
    updateSelectedRowsText(dt);
  };

  // Local functions

  /**
   * Select all rows on current page
   *
   * @param {DataTable.Api} dt DataTable
   * @param {boolean} select True to select all rows on current page, defaults to false
   * @private
   */
  function selectAllRows (dt, select) {
    var ctx = dt.settings()[0];

    // Retrieve all rows taking into account currently applied filter
    var filteredRows = dt.rows({"page": "current", "search": "applied"});

    // Check if style is single
    if (dt.select.style() === "single") {
      throw new Error("Cannot select all rows with selection style 'single'");
    }

    // Select rows
    if (select) {
      filteredRows.select();
    } else {
      filteredRows.deselect();
    }
    $(ctx._pfSelect.selector, dt.table().container()).prop("checked", select); // De/select checkboxes in view
    syncSelectAllCheckbox(dt);
  }

  /**
   * Sync select all checkbox with row selections on current page
   *
   * @param {DataTable.Api} dt DataTable
   * @private
   */
  function syncSelectAllCheckbox (dt) {
    var ctx = dt.settings()[0];

    // Retrieve all rows taking into account currently applied filter
    var filteredRows = dt.rows({"page": "current", "search": "applied"}).flatten().length;
    var selectedFilteredRows = dt.rows({"page": "current", "search": "applied", "selected": true}).flatten().length;

    // De/select the select all checkbox
    var selectAll = $(ctx._pfSelect.selectAllSelector, dt.table().container())[0];
    if (selectAll) {
      selectAll.checked = (filteredRows !== 0 && filteredRows === selectedFilteredRows);
    }
    updateSelectedRowsText(dt);
  }

  /**
   * Sync select checkboxes with row selections on current page
   *
   * @param {DataTable.Api} dt DataTable
   * @private
   */
  function syncSelectCheckboxes (dt) {
    var ctx = dt.settings()[0];

    $(ctx._pfSelect.selector, dt.table().container()).prop("checked", false); // Deselect all checkboxes
    dt.rows({"page": "current", "search": "applied", "selected": true}).every(function (index) {
      $(ctx._pfSelect.selector, dt.table().row(index).node()).prop("checked", true); // Select checkbox for selected row
    });
    syncSelectAllCheckbox(dt);
  }

  /**
   * Update selection results text
   *
   * @param {DataTable.Api} dt DataTable
   * @private
   */
  function updateSelectedRowsText (dt) {
    var ctx = dt.settings()[0];
    var selectedRows = dt.rows({"selected": true}).flatten().length;
    var totalRows = dt.rows().flatten().length;
    if (ctx._pfSelect.results !== undefined && ctx._pfSelect.results.length !== 0) {
      $(ctx._pfSelect.results).html("<strong>" + selectedRows + "</strong> of <strong>" +
        totalRows + "</strong> selected");
    }
  }

  // DataTables API

  /**
   * Select all rows on current page
   *
   * Example: dt.table().pfSelect.selectAllRows(true);
   *
   * @param {boolean} select True to select all rows on current page, defaults to false
   */
  DataTable.Api.register("pfSelect.selectAllRows()", function (select) {
    return this.iterator("table", function (ctx) {
      selectAllRows(new DataTable.Api(ctx), select);
    });
  });

  // DataTables creation
  $(document).on("preInit.dt.dtSelect", function (e, ctx) {
    if (e.namespace !== "dt") {
      return;
    }
    DataTable.pfSelect.init(new DataTable.Api(ctx));
  });
  return DataTable.pfSelect;
}));
