(function(){
 if(window.PsaBackdrop)return;
 var lastGeometry='',lastFocus='',lastMarker=null,shiftX=0,shiftY=0,activeRoot=null;
 function limited(value,low,high,fallback){value=Number(value);return isFinite(value)?Math.max(low,Math.min(high,value)):fallback;}
 function element(tag,id){var n=document.getElementById(id);if(!n){n=document.createElement(tag);n.id=id;(tag==='style'?document.head:document.body).appendChild(n);}return n;}
 window.PsaBackdrop={style:function(dark,x,y,blur,spread,zoom,pinSize,pinColor){
  x=limited(x,0,1,.5);y=limited(y,0,1,.42);blur=limited(blur,0,80,12);spread=limited(spread,0,300,60);zoom=limited(zoom,3,20,16);
  pinSize=limited(pinSize,25,300,100);pinColor=/^#[0-9a-f]{6}$/i.test(pinColor||'')?pinColor.toUpperCase():'#FF6B00';
  var geometry=[zoom,window.innerWidth,window.innerHeight].join(':'),changed=lastGeometry!==geometry;lastGeometry=geometry;
  var focus=[x,y,geometry].join(':'),focusChanged=window.PsaBackdrop.focusToken!==focus;window.PsaBackdrop.focusToken=focus;
  var s=element('style','psa-backdrop-style');
  s.textContent='html,body{margin:0!important;padding:0!important;border:0!important;overflow:hidden!important;width:100%!important;height:100%!important;pointer-events:none!important;background:'+(dark?'#151a20':'#f4f4f2')+'!important}'
   +'body> :not(#psa-map-stage):not(#psa-backdrop-glass):not(.amap-logo):not(.amap-copyright){display:none!important}'
   +'#psa-map-stage{position:fixed!important;inset:0!important;overflow:hidden!important;border:0!important;border-radius:0!important;contain:paint!important;isolation:isolate!important}'
   +'#psa-map-stage>.amap-container{position:absolute!important;inset:auto!important;left:-50vw!important;top:-50vh!important;width:200vw!important;height:200vh!important;min-width:0!important;min-height:0!important;margin:0!important;border:0!important;border-radius:0!important;box-shadow:none!important;transform-origin:center!important;transform:translate(0px,0px) scale(1.12)}'
   +'.common_bottom,.common_top,.amap-toolbar,.amap-maptypecontrol,.amap-maptype-list,.amap-traffic,.amap-controlbar,.amap-layer-switch,.amap-satellite,.call_native_button_wrapper,.amap-locate,.amap-geolocation,.amap-scalecontrol,.amap-info,.amap-info-contentContainer,.amap-marker-label,.amap-search,.amap-zoomcontrol,.amap-touch-toolbar,.amap-ui-control-container,.amap-controls,header,nav,button,#psa-parking-distance{display:none!important}'
   +'#psa-map-stage>.amap-container>:not(.amap-maps):not(.amap-layers):not(.amap-overlays):not(.amap-marker):not(canvas){display:none!important}'
   +'.amap-logo,.amap-copyright{display:block!important;position:fixed!important;bottom:max(3px,env(safe-area-inset-bottom))!important;top:auto!important;right:auto!important;z-index:1002!important;transform:none!important;max-width:calc(100vw - 88px)!important;height:auto!important;font-size:9px!important;line-height:12px!important;color:'+(dark?'#c6c9cc':'#505458')+'!important;background:'+(dark?'rgba(21,26,32,.8)':'rgba(244,244,242,.8)')+'!important}'
   +'.amap-logo{left:4px!important}.amap-copyright{left:80px!important}'
   +'.amap-layer{filter:'+(dark?'grayscale(1) invert(.88) brightness(.62)':'grayscale(1) brightness(1.08)')+'!important}';
  var stage=element('div','psa-map-stage'),overlay=element('div','psa-backdrop-glass');
  overlay.style.cssText='position:fixed;inset:0;pointer-events:none;z-index:1000;background:'+(dark?'rgba(15,19,23,.20)':'rgba(250,250,250,.18)')+';backdrop-filter:blur('+blur+'px);-webkit-backdrop-filter:blur('+blur+'px);mask-image:radial-gradient(ellipse at '+x*100+'% '+y*100+'%,transparent 0%,black '+spread+'%);-webkit-mask-image:radial-gradient(ellipse at '+x*100+'% '+y*100+'%,transparent 0%,black '+spread+'%)';
  var width=24*pinSize/100,height=32*pinSize/100,pinSignature=pinSize+':'+pinColor;
  var svg='<span aria-hidden="true" style="position:absolute;left:50%;bottom:0;transform:translateX(-50%);width:'+width+'px;height:'+height+'px;pointer-events:none"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 32" width="'+width+'" height="'+height+'"><path fill="'+pinColor+'" d="M12 1C5.9 1 1 5.9 1 12c0 8 11 19 11 19s11-11 11-19C23 5.9 18.1 1 12 1Z"/><circle cx="12" cy="12" r="4" fill="white"/></svg></span>';
  function align(marker,force){
   if(!marker||typeof marker.getBoundingClientRect!=='function'||(!force&&lastMarker===marker&&lastFocus===focus))return;
   var root=document.querySelector('.amap-container'),m=window.map||window.amap||window.mapObj;if(!root||!window.innerWidth||!window.innerHeight)return;
   if(m&&typeof m.panBy==='function'&&(shiftX||shiftY)){root.style.transform='scale(1.12)';shiftX=0;shiftY=0;}
   var tip=typeof marker.querySelector==='function'?marker.querySelector('span[aria-hidden]'):null;
   var r=(tip||marker).getBoundingClientRect(),dx=window.innerWidth*x-(r.left+r.width/2),dy=window.innerHeight*y-r.bottom;
   if(!isFinite(dx)||!isFinite(dy))return;lastMarker=marker;lastFocus=focus;
   if(Math.abs(dx)<1&&Math.abs(dy)<1)return;
   if(m&&typeof m.panBy==='function'){m.panBy(dx/1.12,dy/1.12);return;}
   shiftX+=dx;shiftY+=dy;root.style.transform='translate('+shiftX+'px,'+shiftY+'px) scale(1.12)';
  }
  function clean(){
   var root=document.querySelector('.amap-container');
   // Only move the live SDK container; the share-page shell and future sibling cards stay outside the clipped stage.
   if(root&&root.parentNode!==stage){stage.appendChild(root);if(root!==activeRoot){activeRoot=root;shiftX=shiftY=0;lastMarker=null;lastFocus='';}window.dispatchEvent(new Event('resize'));}
   document.querySelectorAll('.amap-logo,.amap-copyright').forEach(function(n){if(n.parentNode!==document.body)document.body.appendChild(n);});
   var first=null;document.querySelectorAll('.amap-marker,.amap-geolocation-marker').forEach(function(marker){if(!first)first=marker;if(marker.getAttribute('data-psa-pin')===pinSignature)return;marker.setAttribute('data-psa-pin',pinSignature);marker.innerHTML=svg;marker.style.filter='none';});
   document.querySelectorAll('[title],[aria-label]').forEach(function(n){var label=(n.getAttribute('title')||'')+' '+(n.getAttribute('aria-label')||'');if(/^(路况|卫星|Traffic|Satellite)(图|地图)?\s*$/i.test(label.trim()))n.style.display='none';});align(first,false);
  }
  window.PsaBackdrop.clean=clean;
  var m=window.map||window.amap||window.mapObj;if(m&&typeof m.setZoom==='function'&&window.PsaBackdrop.zoom!==zoom){if(!m.getZoom||m.getZoom()!==zoom)m.setZoom(zoom);window.PsaBackdrop.zoom=zoom;}
  if(changed)window.dispatchEvent(new Event('resize'));
  clean();
  if(focusChanged&&typeof setTimeout==='function'){[350,1000].forEach(function(delay){setTimeout(function(){if(window.PsaBackdrop.focusToken===focus)align(document.querySelector('.amap-marker,.amap-geolocation-marker'),true);},delay);});}
  if(!window.__psaMapCleaner&&typeof MutationObserver!=='undefined'){var scheduled=false;window.__psaMapCleaner=new MutationObserver(function(){if(scheduled)return;scheduled=true;setTimeout(function(){scheduled=false;window.PsaBackdrop.clean();},100);});window.__psaMapCleaner.observe(document.body,{childList:true,subtree:true});}
 }};
})();
