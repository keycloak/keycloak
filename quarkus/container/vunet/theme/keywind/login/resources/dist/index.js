import { m as o } from './assets/module.esm-62c37d0d.js';
window.Alpine = o;
o.start();

if (location.pathname.indexOf('protocol/openid-connect/auth') != -1) {
  localStorage.removeItem('vunet.last_interaction');
}
const loadCustomerLogo = async () => {
  const logoutEl = document.getElementById('logoutImage');
  try {
    const response = await fetch('/assets/customer/customer.json');
    const jsonRes = await response.json();
    const logo = jsonRes.logo;

    if (logo) {
      logoutEl.setAttribute('src', logo);
    } else {
      throw new Error();
    }
  } catch (error) {
    logoutEl.setAttribute('src', '/vui/public/img/vunet_icon.svg');
  }
};
loadCustomerLogo();
