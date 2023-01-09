var logoutRedirectUri = document.currentScript.getAttribute( 'logoutRedirectUri' );

function readystatechange(event) {
  if (document.readyState=='complete') {
    window.location.replace(logoutRedirectUri);
  }
}
document.addEventListener('readystatechange', readystatechange);