$(document).ready(function() {
  function formPostData(url, fields) {
    var form = $('<form style="display: none;" method="post" action="' + url + '"></form>');
    $.each(fields, function(name, value) {
      var input = $('<input type="hidden" name="' + name + '">');
      input.attr('value', value);
      form.append(input);
    });

    $(document).find('body').append(form);

    form[0].submit(function(e) {
      e.preventDefault();
    });

    form.remove();
  }
  
  function plnkrOpener() {
    var ctrl = {};
  
    ctrl.example = {
      path: ctrl.examplePath,
      manifest: undefined,
      files: undefined,
      name: 'bootstrap-select example'
    };
  
    ctrl.open = function() {
      var postData = {
        'tags[0]': 'jquery',
        'tags[1]': 'bootstrap-select',
        'private': true
      };
  
      ctrl.example.files = [
        {
          name: 'index.html',
          url: 'test.html',
          content: ''
        },
        {
          name: 'bootstrap-select.js',
          url: 'https://raw.githubusercontent.com/silviomoreto/bootstrap-select/master/dist/js/bootstrap-select.js',
          content: ''
        },
        {
          name: 'bootstrap-select.css',
          url: 'https://raw.githubusercontent.com/silviomoreto/bootstrap-select/master/dist/css/bootstrap-select.css',
          content: ''
        }
      ]

      function getData(file) {
        return $.ajax({
          method: 'GET',
          url: file.url
        })
        .then(function(data) {
          file.content = data;
          postData['files[' + file.name + ']'] = file.content;
        });
      }

      var files = [];

      $.each(ctrl.example.files, function(i, file) {
        files.push(getData(file));
      });

      function sendData() {
        postData.description = ctrl.example.name;

        formPostData('https://plnkr.co/edit/?p=preview', postData);
      };

      $.when.apply(this, files).done(function() {
        sendData();
      });
    };
    
    return ctrl.open()
  }

  plnkrOpener();
});