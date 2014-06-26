<!doctype html>
<html>
  <head>
    <title>PerfTest</title>
    <script>
      function updateElementWithToken(tokenStr, elementId) {
        if (tokenStr && tokenStr != "") {
          var tokenParsed = JSON.stringify(JSON.parse(decodeURIComponent(escape(window.atob( tokenStr.split('.')[1] )))), null, " ");
          document.getElementById(elementId).innerHTML = tokenParsed;
        }
      }
    </script>
  </head>
  <body>

    <p><a href="${requestURI}?action=code">Login and get code</a> | <a href="${requestURI}?action=exchangeCode">Exchange code</a> | <a
        href="${requestURI}?action=refresh">Refresh token</a> | <a href="${requestURI}?action=logout">Logout</a>
    </p>

    <p>
      <#if code??>
        <b>Code Available</b><br>
        Code=${code} <br>
        <hr />
      </#if>

      <#if accessToken??>
        <b>Access Token Available</b><br>
        AccessToken=${accessToken} <br>
        Username=${accessTokenParsed.preferredUsername} <br>
        SessionState=${accessTokenParsed.sessionState}  <br>
        Expiration=${accessTokenExpiration} <br>
        <hr />
      </#if>

      <#if refreshToken??>
        <b>Refresh token available</b><br>
        RefreshToken=${refreshToken} <br>
        Expiration=${refreshTokenExpiration} <br>
        <hr />
      </#if>

      <#if actionDone??>
        RequestAction=${actionDone}
        <hr />
      </#if>

    </p>
    <br><br>
  </body>
</html>