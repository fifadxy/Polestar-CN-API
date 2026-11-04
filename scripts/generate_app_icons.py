#!/usr/bin/env python3
from pathlib import Path
r=Path(__file__).resolve().parents[1];res=r/'android-app/app/src/main/res';brand=r/'branding/android/icons/v3';brand.mkdir(parents=True,exist_ok=True)
outer='M20,151 Q16,151 16,146 L16,119 Q16,100 21,92 L25,82 Q26,78 34,74 L55,47 Q58,43 65,43 Q100,40 135,43 Q142,43 145,47 L166,74 Q174,78 175,82 L179,92 Q184,100 184,119 L184,146 Q184,151 180,151 L157,151 Q155,151 154,147 L153,143 Q142,137 132,135 L68,135 Q58,137 47,143 L46,147 Q45,151 43,151 Z'
mirrors='M15,67 L40,67 L35,73 Q17,75 15,70 Z M160,67 L185,67 L185,70 Q183,75 165,73 Z'
lamps='M28,83 Q34,85 36,90 L53,91 Q56,91 56,95 L53,96 L35,94 L34,109 Q34,111 31,111 L29,110 Z M172,83 Q166,85 164,90 L147,91 Q144,91 144,95 L147,96 L165,94 L166,109 Q166,111 169,111 L171,110 Z M56,95 L144,95 L143,102 Q139,105 132,108 L68,108 Q61,105 57,102 Z'
band='M36,84 L73,84 L67,88 L49,88 Q46,88 46,92 Q46,96 50,96 L150,96 Q154,96 154,92 Q154,88 151,88 L133,88 L127,84 L164,84 Q167,84 168,88 L171,99 Q172,104 168,104 L32,104 Q28,104 29,99 L32,88 Q33,84 36,84 Z'
tracks='M48,116 L62,116 L82,129 Q91,135 86,143 Q83,147 74,151 L52,161 Q43,165 46,170 L65,188 L52,188 L31,174 Q23,167 28,159 Q31,155 40,151 L62,141 Q72,137 67,133 Z M138,116 L152,116 L172,129 Q181,135 176,143 Q173,147 164,151 L142,161 Q133,165 136,170 L155,188 L142,188 L121,174 Q113,167 118,159 Q121,155 130,151 L152,141 Q162,137 157,133 Z'
styles=['outline','red','skid'];modes=['auto','day','night']
# Geometry is authored once, then shared by XML vectors and the SVG masters.
def xml_paths(style,color,mono=False):
 p=lambda d,attrs='':f'<path android:pathData="{d}" {attrs}/>'
 fill=f'android:fillColor="{color}"'
 if style=='outline':return '<group android:translateY="3">'+p(outer,'android:fillColor="#00000000" android:strokeColor="'+color+'" android:strokeWidth="8" android:strokeLineCap="round" android:strokeLineJoin="round"')+p(mirrors+' '+lamps,fill)+'</group>'
 body=p(outer+' '+band,fill+' android:fillType="evenOdd"')+p(mirrors,fill)
 if style=='red':return '<group android:translateY="3">'+('' if mono else p(band,'android:fillColor="#FF111214"'))+body+'</group>'
 return '<group android:translateY="-4"><group android:scaleX="0.8" android:scaleY="0.8" android:translateX="20" android:translateY="-14">'+body+'</group>'+p(tracks,fill)+'</group>'
def vector(style,color,mono=False):return '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="48dp" android:height="48dp" android:viewportWidth="200" android:viewportHeight="200">'+xml_paths(style,color,mono)+'</vector>\n'
for style in styles:
 for mode in modes:
  token=('ink' if style=='outline' else style if style=='red' else 'amber');key='psa_icon_'+('' if mode=='auto' else mode+'_')+token
  (res/'drawable'/f'ic_app_{style}_{mode}.xml').write_text(vector(style,'@color/'+key))
  (res/'drawable'/f'fg_app_{style}_{mode}.xml').write_text(f'<inset xmlns:android="http://schemas.android.com/apk/res/android" android:inset="22%" android:drawable="@drawable/ic_app_{style}_{mode}"/>\n')
  paper='psa_icon_'+('' if mode=='auto' else mode+'_')+'paper'
  (res/'drawable'/f'bg_app_{mode}.xml').write_text(f'<shape xmlns:android="http://schemas.android.com/apk/res/android"><solid android:color="@color/{paper}"/></shape>\n')
  for api in [26,33]:
   d=res/f'mipmap-anydpi-v{api}';d.mkdir(exist_ok=True)
   mono=f'<monochrome android:drawable="@drawable/fg_status_{style}"/>' if api==33 else ''
   (d/f'ic_launcher_{style}_{mode}.xml').write_text(f'<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"><background android:drawable="@drawable/bg_app_{mode}"/><foreground android:drawable="@drawable/fg_app_{style}_{mode}"/>{mono}</adaptive-icon>\n')
 (res/'drawable'/f'ic_status_{style}.xml').write_text(vector(style,'#FFFFFFFF',True))
 (res/'drawable'/f'fg_status_{style}.xml').write_text(f'<inset xmlns:android="http://schemas.android.com/apk/res/android" android:inset="22%" android:drawable="@drawable/ic_status_{style}"/>\n')
# Legacy IDs continue to work, but now point at the new outline mother geometry.
(res/'drawable/ic_notification.xml').write_text(vector('outline','#FFFFFFFF',True))
(res/'drawable/ic_psa_car.xml').write_text(vector('outline','@color/psa_icon_ink'))
(res/'drawable/ps_icon_foreground.xml').write_text('<inset xmlns:android="http://schemas.android.com/apk/res/android" android:inset="22%" android:drawable="@drawable/ic_psa_car"/>\n')
for api in [26,33]:
 src=res/f'mipmap-anydpi-v{api}/ic_launcher_outline_auto.xml';(src.parent/'ic_launcher.xml').write_text(src.read_text())
# Base palette plus explicit colors for forced day/night icons.
colors={'day':{'ink':'#292B2D','paper':'#F5F5F7','red':'#E60000','amber':'#F59E00'},'night':{'ink':'#E9EDF2','paper':'#111214','red':'#FF2424','amber':'#FFA000'}}
for mode,values in colors.items():
 allcolors={f'psa_icon_{k}':v for k,v in values.items()}
 for m,vs in colors.items():allcolors.update({f'psa_icon_{m}_{k}':v for k,v in vs.items()})
 (res/('values' if mode=='day' else 'values-night')/'psa_icon_colors.xml').write_text('<resources>'+''.join(f'<color name="{k}">{v}</color>' for k,v in allcolors.items())+'</resources>\n')
# Standalone masters use the identical path data, not independent regenerated shapes.
import xml.etree.ElementTree as ET
ns='{http://schemas.android.com/apk/res/android}'
def svg_node(e):
 attrs=e.attrib
 if e.tag=='group':
  x=attrs.get(ns+'translateX','0');y=attrs.get(ns+'translateY','0');sx=attrs.get(ns+'scaleX','1');sy=attrs.get(ns+'scaleY','1')
  return f'<g transform="translate({x} {y}) scale({sx} {sy})">'+''.join(svg_node(c) for c in e)+'</g>'
 if e.tag!='path':return ''
 a={'d':attrs[ns+'pathData'],'fill':attrs.get(ns+'fillColor','none')}
 if a['fill']=='#00000000':a['fill']='none'
 elif len(a['fill'])==9:a['fill']='#'+a['fill'][3:]+a['fill'][1:3]
 for k,n in [('strokeColor','stroke'),('strokeWidth','stroke-width'),('strokeLineCap','stroke-linecap'),('strokeLineJoin','stroke-linejoin'),('fillType','fill-rule')]:
  if ns+k in attrs:a[n]='evenodd' if attrs[ns+k]=='evenOdd' else attrs[ns+k]
 return '<path '+' '.join(f'{k}="{v}"' for k,v in a.items())+'/>'
for style in styles:
 for mode in ['day','night','status']:
  color='#FFFFFF' if mode=='status' else colors[mode]['ink' if style=='outline' else 'red' if style=='red' else 'amber']
  e=ET.fromstring(vector(style,color,mode=='status'));svg='<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">'+''.join(svg_node(c) for c in e)+'</svg>\n'
  (brand/f'Psa_{style}_{mode}_v3.svg').write_text(svg)
print('Generated shared XML/SVG icon geometry and adaptive layers')
