(function () {
  if (document.getElementById('psa-map-compact')) return;
  const style = document.createElement('style');
  style.id = 'psa-map-compact';
  style.textContent = `
    .call_native_button_wrapper, #wx_open_app_float_button { display:none!important; }
    /* The host dialog already exposes coordinates, refresh and Open map.
       Remove the large share-card overlay but retain map, marker, Amap logo and attribution. */
    .common_bottom { display:none!important; }
    .call_native_button_wrapper, #wx_open_app_float_button { display:none!important; }
  `;
  (document.head || document.documentElement).appendChild(style);
  // Preserve the provider's map, marker, logo and attribution. Only compact the share-page overlays.
  window.dispatchEvent(new Event('resize'));
})();
