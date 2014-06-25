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
        Code: ${code}
        <hr />
      </#if>

      <#if accessToken??>
        <b>accessToken: </b> ${accessToken}
        <br />
        <pre style="background-color: #ddd; border: 1px solid #ccc; padding: 10px;" id="accessTokenParsed"></pre>
        <hr />

        <script>
          updateElementWithToken("${accessToken}", "accessTokenParsed");
        </script>
      </#if>

      <#if refreshToken??>
        <b>refreshToken:  </b> ${refreshToken}
        <br />
        <pre style="background-color: #ddd; border: 1px solid #ccc; padding: 10px;" id="refreshTokenParsed"></pre>
        <hr />

        <script>
          updateElementWithToken("${refreshToken}", "refreshTokenParsed");
        </script>
      </#if>

    </p>
    <br><br>
  </body>
</html>