(function(){
 if(window.PsaBackdrop)return;
 window.PsaBackdrop={style:function(dark,x,y,blur,spread,zoom){
  var s=document.getElementById('psa-backdrop-style');if(!s){s=document.createElement('style');s.id='psa-backdrop-style';document.head.appendChild(s);}
  s.textContent='.common_bottom,.common_top,.amap-toolbar,.call_native_button_wrapper,header,nav,button,#psa-parking-distance{display:none!important}html,body{overflow:hidden!important;pointer-events:none!important}.amap-container{position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important}.amap-layer{filter:'+(dark?'grayscale(1) invert(.88) brightness(.62)':'grayscale(1) brightness(1.08)')+'!important}';
  var overlay=document.getElementById('psa-backdrop-glass');if(!overlay){overlay=document.createElement('div');overlay.id='psa-backdrop-glass';document.body.appendChild(overlay);}
  overlay.style.cssText='position:fixed;inset:0;pointer-events:none;z-index:1000;background:'+(dark?'rgba(15,19,23,.20)':'rgba(250,250,250,.18)')+';backdrop-filter:blur('+blur+'px);-webkit-backdrop-filter:blur('+blur+'px);mask-image:radial-gradient(ellipse at '+x*100+'% '+y*100+'%,transparent 0%,black '+spread+'%);-webkit-mask-image:radial-gradient(ellipse at '+x*100+'% '+y*100+'%,transparent 0%,black '+spread+'%)';
  var m=window.map||window.amap||window.mapObj;if(m&&typeof m.setZoom==='function')m.setZoom(zoom);
  window.dispatchEvent(new Event('resize'));
 }};
})();
