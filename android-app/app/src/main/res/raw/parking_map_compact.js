(function () {
  if (document.getElementById('psa-map-compact')) return;
  const style = document.createElement('style');
  style.id = 'psa-map-compact';
  style.textContent = `
    .call_native_button_wrapper, #wx_open_app_float_button { display:none!important; }
    .common_bottom .operation, .common_bottom .goto_home_for_search { display:none!important; }
    .common_bottom { height:48px!important; bottom:4px!important; }
    .common_bottom .o-cardview, .common_bottom .J-swipe,
    .common_bottom .poi_card, .common_bottom .item-inner {
      height:48px!important; min-height:0!important; padding-top:0!important; padding-bottom:0!important;
    }
    .common_bottom .gallery_card_outer { height:40px!important; margin:0 10px!important; padding:4px 0!important; }
    .common_bottom .name-wrapper, .common_bottom .name { font-size:12px!important; line-height:18px!important; height:18px!important; margin:0!important; }
    .common_bottom .distance { font-size:11px!important; line-height:16px!important; height:16px!important; margin:0!important; }
  `;
  (document.head || document.documentElement).appendChild(style);
  // Preserve the provider's map, marker, logo and attribution. Only compact the share-page overlays.
  window.dispatchEvent(new Event('resize'));
})();
