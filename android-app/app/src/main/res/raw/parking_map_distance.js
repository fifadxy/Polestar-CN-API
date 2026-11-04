(function (root) {
  'use strict';
  function inChina(lat, lon) { return lon >= 72.004 && lon <= 137.8347 && lat >= 0.8293 && lat <= 55.8271; }
  function phoneToMap(lat, lon) {
    if (!inChina(lat, lon)) return [lat, lon];
    const x = lon - 105, y = lat - 35, pi = Math.PI;
    let dy = -100 + 2*x + 3*y + .2*y*y + .1*x*y + .2*Math.sqrt(Math.abs(x));
    dy += (20*Math.sin(6*x*pi) + 20*Math.sin(2*x*pi))*2/3;
    dy += (20*Math.sin(y*pi) + 40*Math.sin(y*pi/3))*2/3;
    dy += (160*Math.sin(y*pi/12) + 320*Math.sin(y*pi/30))*2/3;
    let dx = 300 + x + 2*y + .1*x*x + .1*x*y + .1*Math.sqrt(Math.abs(x));
    dx += (20*Math.sin(6*x*pi) + 20*Math.sin(2*x*pi))*2/3;
    dx += (20*Math.sin(x*pi) + 40*Math.sin(x*pi/3))*2/3;
    dx += (150*Math.sin(x*pi/12) + 300*Math.sin(x*pi/30))*2/3;
    const radians = lat*pi/180, magic = 1 - .00669342162296594323*Math.pow(Math.sin(radians),2);
    dy = dy*180 / ((6378245*(1-.00669342162296594323))/(magic*Math.sqrt(magic))*pi);
    dx = dx*180 / (6378245/Math.sqrt(magic)*Math.cos(radians)*pi);
    return [lat+dy, lon+dx];
  }
  function metres(lat1, lon1, lat2, lon2) {
    const rad = Math.PI/180, dlat = (lat2-lat1)*rad, dlon = (lon2-lon1)*rad;
    const a = Math.pow(Math.sin(dlat/2),2)+Math.cos(lat1*rad)*Math.cos(lat2*rad)*Math.pow(Math.sin(dlon/2),2);
    return 6371008.8*2*Math.atan2(Math.sqrt(Math.max(0,Math.min(1,a))),Math.sqrt(Math.max(0,1-a)));
  }
  function measure(phone, parked) {
    if (!phone || !parked || !Number.isFinite(phone.latitude) || !Number.isFinite(phone.longitude)
        || Math.abs(phone.latitude)>90 || Math.abs(phone.longitude)>180) return null;
    // Browser geolocation is WGS84; convert only the PHONE point. The reported car point is untouched.
    const map = phoneToMap(phone.latitude,phone.longitude);
    return metres(map[0],map[1],parked.latitude,parked.longitude);
  }
  function format(distance, accuracy, english) {
    if (!Number.isFinite(distance)) return english?'Phone location unavailable':'手机定位暂不可用';
    const amount = distance < 1000 ? Math.round(distance)+' m' : (distance/1000).toFixed(1)+' km';
    const precision = Number.isFinite(accuracy)&&accuracy>0 ? ' · ±'+Math.ceil(accuracy)+' m' : '';
    return (english?'Car ~':'距车约 ')+amount+precision;
  }
  function show(parkLat,parkLon,english,enabled) {
    function label(message,attempt) {
      const box=root.document.querySelector('.common_bottom .gallery_card_outer');
      if (!box) { if(attempt<10)root.setTimeout(function(){label(message,attempt+1);},250);return; }
      const old=box.querySelector('.distance');if(old)old.style.display='none';
      let text=box.querySelector('.psa-phone-distance');
      if(!text){text=root.document.createElement('div');text.className='psa-phone-distance';box.appendChild(text);}
      text.style.cssText='font-size:11px;line-height:16px;height:16px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;color:#626560';
      text.textContent=message;
    }
    if(!enabled){label(english?'Phone location off':'手机定位未开启',0);return;}
    label(english?'Locating phone…':'正在定位手机…',0);
    if(!root.navigator || !root.navigator.geolocation){label(format(null,null,english),0);return;}
    root.navigator.geolocation.getCurrentPosition(function(position){
      label(format(measure(position.coords,{latitude:parkLat,longitude:parkLon}),position.coords.accuracy,english),0);
    },function(){label(format(null,null,english),0);},{enableHighAccuracy:false,maximumAge:30000,timeout:8000});
  }
  const api={phoneToMap:phoneToMap,metres:metres,measure:measure,format:format,show:show};
  if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.PsaParkingDistance=api;
})(typeof window!=='undefined'?window:global);
