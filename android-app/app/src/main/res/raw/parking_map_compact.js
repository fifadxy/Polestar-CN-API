(function () {
  if (document.getElementById('psa-map-compact')) return;
  const style = document.createElement('style');
  style.id = 'psa-map-compact';
  // The native PsAssistant dialog supplies the controls. Remove embedded Amap
  // navigation / share chrome, NOT the map, vehicle pin, logo or attribution.
  style.textContent = `
    html,body {overflow:hidden!important;overscroll-behavior:none!important;}
    .common_bottom, .call_native_button_wrapper, #wx_open_app_float_button,
    .amap-callnative, .amap-embed-nav,
    [class*="route-footer"], [class*="nav-toolbar"], [class*="navigation-button"]
      {display:none!important;}
    .psa-phone-distance {
      position:absolute!important;top:6px!important;right:8px!important;left:auto!important;
      z-index:4000!important;max-width:calc(100% - 16px)!important;
      padding:4px 7px!important;border-radius:6px!important;
      background:rgba(25,29,34,.91)!important;color:#fff!important;
      font-size:12px!important;line-height:16px!important;
      height:auto!important;white-space:nowrap!important;
      overflow:hidden!important;text-overflow:ellipsis!important;
      pointer-events:none!important;
      box-shadow:0 1px 4px rgba(0,0,0,.16)!important;
    }
  `;
  (document.head || document.documentElement).appendChild(style);
  window.dispatchEvent(new Event('resize'));
})();
