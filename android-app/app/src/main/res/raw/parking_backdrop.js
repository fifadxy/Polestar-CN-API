(function(){
 if(window.PsaBackdrop)return;
 var last='';
 window.PsaBackdrop={style:function(dark,x,y,blur,spread,zoom){
  var signature=[dark,x,y,blur,spread,zoom].join(':');var unchanged=last===signature;last=signature;
  var s=document.getElementById('psa-backdrop-style');if(!s){s=document.createElement('style');s.id='psa-backdrop-style';document.head.appendChild(s);}
  s.textContent='.common_bottom,.common_top,.amap-toolbar,.amap-maptypecontrol,.amap-maptype-list,.amap-traffic,.amap-controlbar,.amap-layer-switch,.amap-satellite,.call_native_button_wrapper,header,nav,button,#psa-parking-distance{display:none!important}html,body{overflow:hidden!important;pointer-events:none!important}.amap-container{position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important}.amap-layer{filter:'+(dark?'grayscale(1) invert(.88) brightness(.62)':'grayscale(1) brightness(1.08)')+'!important}';
  var overlay=document.getElementById('psa-backdrop-glass');if(!overlay){overlay=document.createElement('div');overlay.id='psa-backdrop-glass';document.body.appendChild(overlay);}
  overlay.style.cssText='position:fixed;inset:0;pointer-events:none;z-index:1000;background:'+(dark?'rgba(15,19,23,.20)':'rgba(250,250,250,.18)')+';backdrop-filter:blur('+blur+'px);-webkit-backdrop-filter:blur('+blur+'px);mask-image:radial-gradient(ellipse at '+x*100+'% '+y*100+'%,transparent 0%,black '+spread+'%);-webkit-mask-image:radial-gradient(ellipse at '+x*100+'% '+y*100+'%,transparent 0%,black '+spread+'%)';
  var svg='<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 32" width="24" height="32"><path fill="#FF6B00" d="M12 1C5.9 1 1 5.9 1 12c0 8 11 19 11 19s11-11 11-19C23 5.9 18.1 1 12 1Z"/><circle cx="12" cy="12" r="4" fill="white"/></svg>';
  function clean(){document.querySelectorAll('.amap-marker,.amap-geolocation-marker').forEach(function(marker){if(marker.getAttribute('data-psa-pin'))return;marker.setAttribute('data-psa-pin','1');marker.innerHTML=svg;marker.style.filter='none';});document.querySelectorAll('[title],[aria-label]').forEach(function(n){var label=(n.getAttribute('title')||'')+' '+(n.getAttribute('aria-label')||'');if(/^(路况|卫星|Traffic|Satellite)(图|地图)?\s*$/i.test(label.trim()))n.style.display='none';});}
  clean();var root=document.querySelector('.amap-container');if(!window.__psaMapCleaner&&root&&typeof MutationObserver!=='undefined'){var scheduled=false;window.__psaMapCleaner=new MutationObserver(function(){if(scheduled)return;scheduled=true;setTimeout(function(){scheduled=false;clean();},250);});window.__psaMapCleaner.observe(root,{childList:true,subtree:true});}
  var m=window.map||window.amap||window.mapObj;if(!unchanged&&m&&typeof m.setZoom==='function'&&(!m.getZoom||m.getZoom()!==zoom))m.setZoom(zoom);
  if(!unchanged)window.dispatchEvent(new Event('resize'));
 }};
})();
