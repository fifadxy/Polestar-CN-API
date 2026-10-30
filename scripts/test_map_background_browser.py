#!/usr/bin/env python3
"""Offline Chromium layout test with synthetic tiles and provider-like late overlays."""
from pathlib import Path
from playwright.sync_api import sync_playwright
script=(Path(__file__).resolve().parents[1]/'android-app/app/src/main/res/raw/parking_backdrop.js').read_text()
with sync_playwright() as p:
 browser=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox'])
 page=browser.new_page(viewport={'width':400,'height':800})
 page.set_content('''<html><head></head><body style="margin:0"><div id="shell" style="transform:scale(.8);height:600px"><div class="amap-container"><div class="amap-maps" style="position:absolute;inset:0"><div class="amap-layer" style="position:absolute;inset:0;background:linear-gradient(90deg,#eee 49%,#aaa 50%,#eee 51%)"></div><div class="amap-marker" style="position:absolute;left:50%;top:50%"></div></div><div class="new-provider-info">Go to Amap / satellite / route</div><div class="amap-controls">Zoom</div><a class="amap-logo">AMap</a><div class="amap-copyright">Map attribution</div></div><div class="common_bottom">Parking details</div></div></body></html>''')
 page.evaluate(script)
 page.evaluate('PsaBackdrop.style(false,.5,.42,12,60,16,100,"#FF6B00")')
 page.wait_for_timeout(1200)
 def check(x,y):
  state=page.evaluate('''()=>{const root=document.querySelector('.amap-container'),pin=root.querySelector('.amap-marker span'),r=root.getBoundingClientRect(),p=pin.getBoundingClientRect();return {parent:root.parentNode.id,cover:r.left<=0&&r.top<=0&&r.right>=innerWidth&&r.bottom>=innerHeight,x:p.left+p.width/2,y:p.bottom,hidden:[...document.querySelectorAll('.new-provider-info,.amap-controls,.common_bottom,.late-info')].every(n=>n.getBoundingClientRect().width===0),attribution:document.querySelector('.amap-copyright').getBoundingClientRect().height>0,filters:getComputedStyle(document.querySelector('.amap-layer')).filter}}''')
  assert state['parent']=='psa-map-stage',state
  assert state['cover'] and state['hidden'] and state['attribution'],state
  assert abs(state['x']-400*x)<1 and abs(state['y']-800*y)<1,state
  return state
 check(.5,.42)
 page.evaluate('''()=>{const n=document.createElement('div');n.className='late-info';n.textContent='Late parking card';n.style='position:fixed;bottom:0';document.body.appendChild(n)}''')
 page.wait_for_timeout(150)
 check(.5,.42)
 for x,y in [(0,0),(1,1),(.75,.25),(.5,.42)]:
  page.evaluate('([x,y])=>PsaBackdrop.style(true,x,y,12,60,16,150,"#FF6B00")',[x,y]);page.wait_for_timeout(1100);assert 'invert' in check(x,y)['filters']
 # The stage owns the same live map. Unknown late controls cannot escape into the background.
 page.evaluate('window.originalMap=document.querySelector(".amap-container")')
 page.evaluate('PsaBackdrop.style(false,.5,.42,12,60,16,100,"#FF6B00")')
 assert page.evaluate('window.originalMap===document.querySelector(".amap-container")')
 assert 'invert' not in check(.5,.42)['filters']
 page.set_viewport_size({'width':800,'height':400})
 page.evaluate('PsaBackdrop.style(false,.5,.42,12,60,16,100,"#FF6B00")');page.wait_for_timeout(1100)
 r=page.locator('.amap-container').bounding_box();assert r['x']<=0 and r['y']<=0 and r['x']+r['width']>=800 and r['y']+r['height']>=400,r
 print('PASS: real Chromium crop, late overlays, retained attribution, focal extremes, day/night, same map and viewport resize')
 browser.close()
