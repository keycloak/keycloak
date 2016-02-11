openurl – Node.js module for opening URLs
=========================================

openurl is a Node.js module for opening a URL via the operating system. This will usually trigger actions such as:

- http URLs: open the default browser
- mailto URLs: open the default email client
- file URLs: open a window showing the directory (on OS X)

Example interaction on the Node.js REPL:

    > require("openurl").open("http://rauschma.de")
    > require("openurl").open("mailto:john@example.com")
    
You can generate emails as follows:

    require("openurl").mailto(["john@example.com", "jane@example.com"],
        { subject: "Hello!", body: "This is\nan automatically sent email!\n" });
    
Install via npm:

    npm install openurl

I’m not yet terribly familiar with implementing npm packages, so any feedback is welcome
(especially experience reports on Windows and Linux, which I can’t test on).

Related reading:

- [Write your shell scripts in JavaScript, via Node.js](http://www.2ality.com/2011/12/nodejs-shell-scripting.html)