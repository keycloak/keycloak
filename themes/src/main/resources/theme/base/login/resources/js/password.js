'use strict';

function togglePasswords(passwords) {
  passwords.forEach(function (id) {
    let password = document.getElementById(id);
    const type = password.getAttribute('type') === 'password' ? 'text' : 'password';
    password.setAttribute('type', type);
    password.classList.toggle('password-visible');
  });
}
