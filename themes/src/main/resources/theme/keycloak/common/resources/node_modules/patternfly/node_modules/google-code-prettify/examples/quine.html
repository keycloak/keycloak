<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<title>Making Quines Prettier</title>
<!-- The defer is not necessary for autoloading, but is necessary for the
     script at the bottom to work as a Quine. -->
<script src="https://google-code-prettify.googlecode.com/svn/loader/run_prettify.js?autoload=true&amp;skin=sunburst&amp;lang=css" defer="defer"></script>
<style>.operative { font-weight: bold; border:1px solid yellow }</style>
</head>

<body>
<h1>Making Quines Prettier</h1>

<p>
Below is the content of this page prettified.  The <code>&lt;pre&gt;</code>
element is prettified because it has <code>class="prettyprint"</code> and
because the sourced script loads a JavaScript library that styles source
code.
</p>

<p>
The line numbers to the left appear because the preceding comment
<code>&lt;?prettify lang=html linenums=true?&gt;</code> turns on
line-numbering and the
<a href="http://google-code-prettify.googlecode.com/svn/trunk/styles/index.html">stylesheet</a>
(see <code>skin=sunburst</code> in the <code>&lt;script src&gt;</code>)
specifies that every fifth line should be numbered.
</p>

<!-- Language hints can be put in XML application directive style comments. -->
<?prettify lang=html linenums=true?>
<pre class="prettyprint" id="quine" style="border:4px solid #88c"></pre>

<script>//<![CDATA[
(function () {
  function html(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  var quineHtml = html(
        '<!DOCTYPE html>\n<html>\n'
      + document.documentElement.innerHTML 
      + '\n<\/html>\n');

  // Highlight the operative parts:
  quineHtml = quineHtml.replace(
    /&lt;script src[\s\S]*?&gt;&lt;\/script&gt;|&lt;!--\?[\s\S]*?--&gt;|&lt;pre\b[\s\S]*?&lt;\/pre&gt;/g,
    '<span class="operative">$&</span>');

  document.getElementById("quine").innerHTML = quineHtml;
})();
//]]>
</script></body>
</html>
