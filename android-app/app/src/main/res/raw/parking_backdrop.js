(function(){
 if(window.PsaBackdrop)return;
 var last='',lastFocus='',lastMarker=null,shiftX=0,shiftY=0;
 function limited(value,low,high,fallback){value=Number(value);return isFinite(value)?Math.max(low,Math.min(high,value)):fallback;}
 window.PsaBackdrop={style:function(dark,x,y,blur,spread,zoom,pinSize,pinColor){
  x=limited(x,0,1,.5);y=limited(y,0,1,.42);pinSize=limited(pinSize,25,300,100);pinColor=/^#[0-9a-f]{6}$/i.test(pinColor||'')?pinColor.toUpperCase():'#FF6B00';
  var signature=[dark,blur,spread,zoom].join(':'),focus=[x,y,zoom].join(':'),focusChanged=window.PsaBackdrop.focusToken!==[x,y,zoom].join(':');window.PsaBackdrop.focusToken=focus;var unchanged=last===signature;last=signature;
  var s=document.getElementById('psa-backdrop-style');if(!s){s=document.createElement('style');s.id='psa-backdrop-style';document.head.appendChild(s);}
  s.textContent='.common_bottom,.common_top,.amap-toolbar,.amap-maptypecontrol,.amap-maptype-list,.amap-traffic,.amap-controlbar,.amap-layer-switch,.amap-satellite,.call_native_button_wrapper,.amap-locate,.amap-geolocation,.amap-scalecontrol,.amap-info,.amap-info-contentContainer,.amap-marker-label,.amap-search,.amap-zoomcontrol,.amap-touch-toolbar,.amap-ui-control-container,header,nav,button,#psa-parking-distance{display:none!important}html,body{margin:0!important;padding:0!important;border:0!important;overflow:hidden!important;pointer-events:none!important}body{visibility:hidden!important}.amap-container,#psa-backdrop-glass,.amap-logo,.amap-copyright{visibility:visible!important}.amap-container{position:fixed!important;inset:auto!important;left:-50vw!important;top:-50vh!important;width:200vw!important;height:200vh!important;border:0!important;border-radius:0!important;box-shadow:none!important;transform-origin:center!important;transform:translate(0px,0px) scale(1.12)}.amap-logo,.amap-copyright{position:fixed!important;bottom:2px!important;z-index:1002!important;transform:none!important}.amap-logo{left:4px!important}.amap-copyright{left:80px!important}.amap-layer{filter:'+(dark?'grayscale(1) invert(.88) brightness(.62)':'grayscale(1) brightness(1.08)')+'!important}';
  var overlay=document.getElementById('psa-backdrop-glass');if(!overlay){overlay=document.createElement('div');overlay.id='psa-backdrop-glass';document.body.appendChild(overlay);}
  overlay.style.cssText='position:fixed;inset:0;pointer-events:none;z-index:1000;background:'+(dark?'rgba(15,19,23,.20)':'rgba(250,250,250,.18)')+';backdrop-filter:blur('+blur+'px);-webkit-backdrop-filter:blur('+blur+'px);mask-image:radial-gradient(ellipse at '+x*100+'% '+y*100+'%,transparent 0%,black '+spread+'%);-webkit-mask-image:radial-gradient(ellipse at '+x*100+'% '+y*100+'%,transparent 0%,black '+spread+'%)';
  var width=24*pinSize/100,height=32*pinSize/100,pinSignature=pinSize+':'+pinColor;
  var svg='<span aria-hidden="true" style="position:absolute;left:50%;bottom:0;transform:translateX(-50%);width:'+width+'px;height:'+height+'px;pointer-events:none"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 32" width="'+width+'" height="'+height+'"><path fill="'+pinColor+'" d="M12 1C5.9 1 1 5.9 1 12c0 8 11 19 11 19s11-11 11-19C23 5.9 18.1 1 12 1Z"/><circle cx="12" cy="12" r="4" fill="white"/></svg></span>';
  function align(marker,force){
   if(!marker||typeof marker.getBoundingClientRect!=='function'||(!force&&lastMarker===marker&&lastFocus===focus))return;
   var root=document.querySelector('.amap-container'),m=window.map||window.amap||window.mapObj;if(!root||!window.innerWidth||!window.innerHeight)return;
   if(m&&typeof m.panBy==='function'&&(shiftX||shiftY)){root.style.transform='scale(1.12)';shiftX=0;shiftY=0;}
   var r=marker.getBoundingClientRect(),dx=window.innerWidth*x-(r.left+r.width/2),dy=window.innerHeight*y-r.bottom;
   if(!isFinite(dx)||!isFinite(dy))return;
   lastMarker=marker;lastFocus=focus;
   if(Math.abs(dx)<1&&Math.abs(dy)<1)return;
   if(m&&typeof m.panBy==='function'){m.panBy(dx/1.12,dy/1.12);return;}
   // Move the entire map together, never displace a pin independently of its roads.
   shiftX+=dx;shiftY+=dy;root.style.transform='translate('+shiftX+'px,'+shiftY+'px) scale(1.12)';

  }
  function clean(){document.querySelectorAll('.amap-logo,.amap-copyright').forEach(function(n){if(n.parentNode!==document.body)document.body.appendChild(n);});var first=null;document.querySelectorAll('.amap-marker,.amap-geolocation-marker').forEach(function(marker){if(!first)first=marker;if(marker.getAttribute('data-psa-pin')===pinSignature)return;marker.setAttribute('data-psa-pin',pinSignature);marker.innerHTML=svg;marker.style.filter='none';});document.querySelectorAll('[title],[aria-label]').forEach(function(n){var label=(n.getAttribute('title')||'')+' '+(n.getAttribute('aria-label')||'');if(/^(路况|卫星|Traffic|Satellite)(图|地图)?\s*$/i.test(label.trim()))n.style.display='none';});align(first,false);}
  window.PsaBackdrop.clean=clean;
  var m=window.map||window.amap||window.mapObj;if(m&&typeof m.setZoom==='function'&&window.PsaBackdrop.zoom!==zoom){if(!m.getZoom||m.getZoom()!==zoom)m.setZoom(zoom);window.PsaBackdrop.zoom=zoom;}
  if(!unchanged)window.dispatchEvent(new Event('resize'));
  clean();if(focusChanged&&typeof setTimeout==='function'){[350,1000].forEach(function(delay){setTimeout(function(){if(window.PsaBackdrop.focusToken===focus)align(document.querySelector('.amap-marker,.amap-geolocation-marker'),true);},delay);});}var root=document.querySelector('.amap-container');if(!window.__psaMapCleaner&&root&&typeof MutationObserver!=='undefined'){var scheduled=false;window.__psaMapCleaner=new MutationObserver(function(){if(scheduled)return;scheduled=true;setTimeout(function(){scheduled=false;window.PsaBackdrop.clean();},250);});window.__psaMapCleaner.observe(root,{childList:true,subtree:true});}
 }};
})();
