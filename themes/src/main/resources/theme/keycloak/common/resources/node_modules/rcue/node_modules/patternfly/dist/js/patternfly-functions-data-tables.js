// Util: DataTables Settings
(function ($) {
  'use strict';
  if ($.fn.dataTableExt) {
    /* Set the defaults for DataTables initialisation */
    $.extend(true, $.fn.dataTable.defaults, {
      "bDestroy": true,
      "bAutoWidth": false,
      "iDisplayLength": 20,
      "sDom":
        "<'dataTables_header' f i r >" +
        "<'table-responsive'  t >" +
        "<'dataTables_footer' p >",
      "oLanguage": {
        "sInfo": "Showing <b>_START_</b> to <b>_END_</b> of <b>_TOTAL_</b> Items",
        "sInfoFiltered" : "(of <b>_MAX_</b>)",
        "sInfoEmpty" : "Showing <b>0</b> Results",
        "sZeroRecords":
          "<p>Suggestions</p>" +
          "<ul>" +
            "<li>Check the javascript regular expression syntax of the search term.</li>" +
            "<li>Check that the correct menu option is chosen (token ID vs. user ID).</li>" +
            "<li>Use wildcards (* to match 0 or more characters, + to match 1 or more characters, ? to match 0 or 1 character).</li>" +
            "<li>Clear the search field, then click Search to return to the 20 most recent records.</li>" +
          "</ul>",
        "sSearch": ""
      },
      "sPaginationType": "bootstrap_input",
      "oSearch": {
        "sSearch": "",
        "bRegex": true,
        "bSmart": false
      }
    });

    /* Default class modification */
    $.extend($.fn.dataTableExt.oStdClasses, {
      "sWrapper": "dataTables_wrapper"
    });

    /* API method to get paging information */
    $.fn.dataTableExt.oApi.fnPagingInfo = function (oSettings) {
      return {
        "iStart":         oSettings._iDisplayStart,
        "iEnd":           oSettings.fnDisplayEnd(),
        "iLength":        oSettings._iDisplayLength,
        "iTotal":         oSettings.fnRecordsTotal(),
        "iFilteredTotal": oSettings.fnRecordsDisplay(),
        "iPage":          oSettings._iDisplayLength === -1 ? 0 : Math.ceil(oSettings._iDisplayStart / oSettings._iDisplayLength),
        "iTotalPages":    oSettings._iDisplayLength === -1 ? 0 : Math.ceil(oSettings.fnRecordsDisplay() / oSettings._iDisplayLength)
      };
    };

    /* Combination of Bootstrap + Input Text style pagination control */
    $.extend($.fn.dataTableExt.oPagination, {
      "bootstrap_input": {
        "fnInit": function (oSettings, nPaging, fnDraw) {
          var fnClickHandler = function (e) {
              e.preventDefault();
              if (oSettings.oApi._fnPageChange(oSettings, e.data.action)) {
                fnDraw(oSettings);
              }
            },
            els,
            nInput;

          $(nPaging).append(
            '<ul class="pagination">' +
              '  <li class="first disabled"><span class="i fa fa-angle-double-left"></span></li>' +
              '  <li class="prev disabled"><span class="i fa fa-angle-left"></span></li>' +
              '</ul>' +
              '<div class="pagination-input">' +
              '  <input type="text" class="paginate_input">' +
              '  <span class="paginate_of">of <b>3</b></span>' +
              '</div>' +
              '<ul class="pagination">' +
              '  <li class="next disabled"><span class="i fa fa-angle-right"></span></li>' +
              '  <li class="last disabled"><span class="i fa fa-angle-double-right"></span></li>' +
              '</ul>'
          );

          els = $('li', nPaging);
          $(els[0]).bind('click.DT', { action: "first" }, fnClickHandler);
          $(els[1]).bind('click.DT', { action: "previous" }, fnClickHandler);
          $(els[2]).bind('click.DT', { action: "next" }, fnClickHandler);
          $(els[3]).bind('click.DT', { action: "last" }, fnClickHandler);

          nInput = $('input', nPaging);
          $(nInput).keyup(function (e) {
            var iNewStart;
            if (e.which === 38 || e.which === 39) {
              this.value += 1;
            } else if ((e.which === 37 || e.which === 40) && this.value > 1) {
              this.value -= 1;
            }

            if (this.value === "" || !this.value.match(/[0-9]/)) {
              /* Nothing entered or non-numeric character */
              return;
            }

            iNewStart = oSettings._iDisplayLength * (this.value - 1);
            if (iNewStart >= oSettings.fnRecordsDisplay()) {
              /* Display overrun */
              oSettings._iDisplayStart = (Math.ceil((oSettings.fnRecordsDisplay() - 1) /
                oSettings._iDisplayLength) - 1) * oSettings._iDisplayLength;
              fnDraw(oSettings);
              return;
            }

            oSettings._iDisplayStart = iNewStart;
            fnDraw(oSettings);
          });
        },

        "fnUpdate": function (oSettings, fnDraw) {
          var oPaging = oSettings.oInstance.fnPagingInfo(),
            an = oSettings.aanFeatures.p,
            ien = an.length,
            iPages = Math.ceil((oSettings.fnRecordsDisplay()) / oSettings._iDisplayLength),
            iCurrentPage = Math.ceil(oSettings._iDisplayStart / oSettings._iDisplayLength) + 1,
            i;

          for (i = 0; i < ien; i += 1) {
            $('.paginate_input', an[i]).val(iCurrentPage)
              .siblings('.paginate_of').find('b').html(iPages);

            // Add / remove disabled classes from the static elements
            if (oPaging.iPage === 0) {
              $('li.first', an[i]).addClass('disabled');
              $('li.prev', an[i]).addClass('disabled');
            } else {
              $('li.first', an[i]).removeClass('disabled');
              $('li.prev', an[i]).removeClass('disabled');
            }

            if (oPaging.iPage === oPaging.iTotalPages - 1 || oPaging.iTotalPages === 0) {
              $('li.next', an[i]).addClass('disabled');
              $('li.last', an[i]).addClass('disabled');
            } else {
              $('li.next', an[i]).removeClass('disabled');
              $('li.last', an[i]).removeClass('disabled');
            }
          }
        }
      }
    });
  }
}(jQuery));
