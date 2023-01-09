var recoveryCodesDownloadFileHeader = document.currentScript.getAttribute( 'recoveryCodesDownloadFileHeader' );
var recoveryCodesDownloadFileDescription = document.currentScript.getAttribute( 'recoveryCodesDownloadFileDescription' );
var recoveryCodesDownloadFileDate = document.currentScript.getAttribute( 'recoveryCodesDownloadFileDate' );


/* copy recovery codes  */
function copyRecoveryCodes() {
  var tmpTextarea = document.createElement("textarea");
  var codes = document.getElementById("kc-recovery-codes-list").getElementsByTagName("li");
  for (i = 0; i < codes.length; i++) {
    tmpTextarea.value = tmpTextarea.value + codes[i].innerText + "\n";
  }
  document.body.appendChild(tmpTextarea);
  tmpTextarea.select();
  document.execCommand("copy");
  document.body.removeChild(tmpTextarea);
}

var copyButton = document.getElementById("copyRecoveryCodes");
copyButton && copyButton.addEventListener("click", function () {
  copyRecoveryCodes();
});

/* download recovery codes  */
function formatCurrentDateTime() {
  var dt = new Date();
  var options = {
    month: 'long',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: 'numeric',
    timeZoneName: 'short'
  };

  return dt.toLocaleString('en-US', options);
}

function parseRecoveryCodeList() {
  var recoveryCodes = document.querySelectorAll(".kc-recovery-codes-list li");
  var recoveryCodeList = "";

  for (var i = 0; i < recoveryCodes.length; i++) {
    var recoveryCodeLiElement = recoveryCodes[i].innerText;
    recoveryCodeList += recoveryCodeLiElement + "\r\n";
  }

  return recoveryCodeList;
}

function buildDownloadContent() {
  var recoveryCodeList = parseRecoveryCodeList();
  var dt = new Date();
  var options = {
    month: 'long',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: 'numeric',
    timeZoneName: 'short'
  };

  return fileBodyContent =
    recoveryCodesDownloadFileHeader + "\n\n" +
    recoveryCodeList + "\n" +
    recoveryCodesDownloadFileDescription + "\n\n" +
    recoveryCodesDownloadFileDate + " " + formatCurrentDateTime();
}

function setUpDownloadLinkAndDownload(filename, text) {
  var el = document.createElement('a');
  el.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
  el.setAttribute('download', filename);
  el.style.display = 'none';
  document.body.appendChild(el);
  el.click();
  document.body.removeChild(el);
}

function downloadRecoveryCodes() {
  setUpDownloadLinkAndDownload('kc-download-recovery-codes.txt', buildDownloadContent());
}

var downloadButton = document.getElementById("downloadRecoveryCodes");
downloadButton && downloadButton.addEventListener("click", downloadRecoveryCodes);

/* print recovery codes */
function buildPrintContent() {
  var recoveryCodeListHTML = document.getElementById('kc-recovery-codes-list').innerHTML;
  var styles =
        `@page { size: auto;  margin-top: 0; }
                body { width: 480px; }
                div { list-style-type: none; font-family: monospace }
                p:first-of-type { margin-top: 48px }`

  return printFileContent =
    "<html><style>" + styles + "</style><body>" +
    "<title>kc-download-recovery-codes</title>" +
    "<p>" + recoveryCodesDownloadFileHeader+ "</p>" +
  "<div>" + recoveryCodeListHTML + "</div>" +
  "<p>" + recoveryCodesDownloadFileDescription + "</p>" +
  "<p>" + recoveryCodesDownloadFileDate + " " + formatCurrentDateTime() + "</p>" +
  "</body></html>";
}

function printRecoveryCodes() {
  var w = window.open();
  w.document.write(buildPrintContent());
  w.print();
  w.close();
}

var printButton = document.getElementById("printRecoveryCodes");
printButton && printButton.addEventListener("click", printRecoveryCodes);