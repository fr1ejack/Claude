// ======================================================================
//  Grundbausteine: Farben, Hilfsfunktionen, Figur, Erde, Icons
// ======================================================================
const O = '#2d3142';
const C = {cream:'#EFE8D8', stripe:'#E7DFCC', teal:'#5E9C98', tealD:'#4A807C', navy:'#2F3E4C', skin:'#F3E3C6',
  skin2:'#D9B48F', skin3:'#9C6B4E', brown:'#6B4038', sage:'#8FAE8B', sageD:'#6F8F6B', peach:'#E8B795', yellow:'#FCC30B',
  mustard:'#D9A93A', night:'#262C3B', grey:'#8A8F99', red:'#D2695C', white:'#FBF6EC', wood:'#C9A27E', woodD:'#A9825F',
  blue:'#6F93A8', green:'#6FA86A', sky:'#DDE6E0', lamp:'#F6D77A', water:'#7FA9C0', purple:'#8E7AA8'};
const SDG = ['#E5243B','#DDA63A','#4C9F38','#C5192D','#FF3A21','#26BDE2','#FCC30B','#A21942','#FD6925','#DD1367','#FD9D24','#BF8B2E','#3F7E44','#0A97D9','#56C02B','#00689D','#19486A'];
let NOW = 0; // globale Zeit für "Leerlauf"-Bewegungen (Blinzeln, Drehen, Flackern)

const clamp=(v,a=0,b=1)=>Math.min(b,Math.max(a,v));
const lerp=(a,b,k)=>a+(b-a)*k;
const P=(t,a,b)=>clamp((t-a)/(b-a));
const eio=x=>x<.5?2*x*x:1-Math.pow(-2*x+2,2)/2;
const eout=x=>1-Math.pow(1-x,3);
const back=x=>{const c1=1.70158,c3=c1+1;return 1+c3*Math.pow(x-1,3)+c1*Math.pow(x-1,2)};
const pop=(t,a,d=.55)=>back(P(t,a,a+d));
const rnd=i=>{const x=Math.sin(i*127.1+311.7)*43758.5453;return x-Math.floor(x)};
const rad=a=>a*Math.PI/180;
const f=n=>Math.round(n*10)/10;
const blinkAt=(seed=0)=>((NOW+seed*1.7)%3.7)<0.12;

function tube(pts,col,w=22){
  const s=pts.map(q=>f(q[0])+','+f(q[1])).join(' ');
  return `<polyline points="${s}" fill="none" stroke="${O}" stroke-width="${w+6}" stroke-linecap="round" stroke-linejoin="round"/>`+
         `<polyline points="${s}" fill="none" stroke="${col}" stroke-width="${w}" stroke-linecap="round" stroke-linejoin="round"/>`;
}
function stripes(c1=C.cream,c2=C.stripe,y0=0,y1=720){
  let s=`<rect x="0" y="${y0}" width="1280" height="${y1-y0}" fill="${c1}"/>`;
  for(let x=20;x<1280;x+=72) s+=`<rect x="${x}" y="${y0}" width="34" height="${y1-y0}" fill="${c2}"/>`;
  return s;
}
function txt(x,y,s,size=30,col=O,w=800,anchor='middle',extra=''){
  return `<text x="${f(x)}" y="${f(y)}" font-family="Nunito" font-weight="${w}" font-size="${size}" fill="${col}" text-anchor="${anchor}" ${extra}>${s}</text>`;
}
function G(inner,tf='',op=1){ if(op<=0) return ''; return `<g transform="${tf}" opacity="${f(clamp(op)*100)/100}">${inner}</g>`}
function scaleAt(x,y,s){return `translate(${f(x)},${f(y)}) scale(${(+s).toFixed(4)}) translate(${-x},${-y})`}
function popAt(x,y,k,inner){ if(k<=0) return ''; return `<g transform="translate(${f(x)},${f(y)}) scale(${k.toFixed(4)})">${inner}</g>`}
function pill(x,y,s,col=C.white,tc=O,size=26,padX=26){
  const w=s.length*size*0.56+padX*2, h=size*1.9;
  return `<rect x="${f(x-w/2)}" y="${f(y-h/2)}" width="${f(w)}" height="${f(h)}" rx="${f(h/2)}" fill="${col}" stroke="${O}" stroke-width="3"/>`+txt(x,y+size*0.36,s,size,tc,900);
}
function title(s,lt,y=88,size=46,t0=0.2){ return G(txt(640,y,s,size,O,900),scaleAt(640,y-size*0.35,pop(lt,t0)),1); }
function check(x,y,k,r=26){ return popAt(x,y,k,`<circle r="${r}" fill="${C.green}" stroke="${O}" stroke-width="3"/><path d="M${-r*.42} ${r*.04} l${r*.3} ${r*.36} l${r*.6} ${-r*.7}" stroke="#fff" stroke-width="${r*.24}" fill="none" stroke-linecap="round" stroke-linejoin="round"/>`); }
function cross(x,y,k,r=26){ return popAt(x,y,k,`<circle r="${r}" fill="${C.red}" stroke="${O}" stroke-width="3"/><path d="M${-r*.38} ${-r*.38} L${r*.38} ${r*.38} M${r*.38} ${-r*.38} L${-r*.38} ${r*.38}" stroke="#fff" stroke-width="${r*.24}" stroke-linecap="round"/>`); }
function arrowPath(x1,y1,x2,y2,col=O,w=5,prog=1){
  const x=lerp(x1,x2,prog), y=lerp(y1,y2,prog), a=Math.atan2(y2-y1,x2-x1)*180/Math.PI;
  if(prog<=0) return '';
  return `<line x1="${f(x1)}" y1="${f(y1)}" x2="${f(x)}" y2="${f(y)}" stroke="${col}" stroke-width="${w}" stroke-linecap="round"/>`+
    `<path d="M0 ${-w*2.2} L${w*3.6} 0 L0 ${w*2.2}z" transform="translate(${f(x)},${f(y)}) rotate(${f(a)})" fill="${col}"/>`;
}
function ground(y=600,col=C.sage){ return `<rect x="-10" y="${y}" width="1300" height="${730-y}" fill="${col}" stroke="${O}" stroke-width="3"/>`; }
function cloud(x,y,s=1,col='#F6F1E4'){ return `<g transform="translate(${f(x)},${f(y)}) scale(${s})"><ellipse cx="0" cy="0" rx="70" ry="24" fill="${col}"/><ellipse cx="34" cy="-16" rx="40" ry="26" fill="${col}"/><ellipse cx="-28" cy="-10" rx="30" ry="20" fill="${col}"/></g>`; }

// ---------- Figur (runde Variante des Referenz-Stils) ----------
function person(o){
  const d=Object.assign({x:0,y:0,s:1,shirt:C.teal,pants:C.navy,hair:C.navy,skin:C.skin,style:'short',
    armL:[12,-8],armR:[12,-8],blink:0,mouth:'smile',look:0,lookY:0,brow:0,walk:null,extra:'',front:'',bob:0,tie:null,dress:null,flip:1},o);
  const r=rad;
  function arm(side,[a1,a2]){
    const sx=side*48, sy=-186, L=54;
    const ex=sx+side*Math.sin(r(a1))*L, ey=sy+Math.cos(r(a1))*L;
    const hx=ex+side*Math.sin(r(a1+a2))*L, hy=ey+Math.cos(r(a1+a2))*L;
    return tube([[sx,sy],[ex,ey],[hx,hy]],d.shirt,22)+`<circle cx="${f(hx)}" cy="${f(hy)}" r="12" fill="${d.skin}" stroke="${O}" stroke-width="3"/>`;
  }
  let legA=0; if(d.walk!==null) legA=Math.sin(d.walk)*22;
  function leg(side,a){
    const hx=side*22, hy=-84, L=74;
    const fx=hx+Math.sin(r(a))*L, fy=hy+Math.cos(r(a))*L;
    return tube([[hx,hy],[fx,fy]],d.pants,26)+`<ellipse cx="${f(fx+side*4)}" cy="${f(fy+4)}" rx="21" ry="10" fill="${O}"/>`;
  }
  let s='';
  s+=leg(-1,legA)+leg(1,-legA);
  if(d.style==='bun') s+=`<circle cx="-60" cy="-236" r="22" fill="${d.hair}" stroke="${O}" stroke-width="3"/>`;
  if(d.style==='long') s+=`<rect x="-70" y="-300" width="140" height="120" rx="34" fill="${d.hair}" stroke="${O}" stroke-width="3"/>`;
  if(d.style==='scarf') s+=`<rect x="-74" y="-324" width="148" height="140" rx="50" fill="${d.hair}" stroke="${O}" stroke-width="3"/>`;
  if(d.dress) s+=`<path d="M-50 -150 L-66 -70 H66 L50 -150z" fill="${d.dress}" stroke="${O}" stroke-width="3" stroke-linejoin="round"/>`;
  s+=`<rect x="-52" y="-204" width="104" height="130" rx="36" fill="${d.shirt}" stroke="${O}" stroke-width="3"/>`;
  if(d.dress) s+=`<path d="M-50 -120 L-66 -66 H66 L50 -120z" fill="${d.dress}" stroke="${O}" stroke-width="3" stroke-linejoin="round"/>`;
  s+=`<path d="M-16 -203 L0 -186 L16 -203" fill="none" stroke="${O}" stroke-width="3" stroke-linejoin="round"/>`;
  if(d.tie) s+=`<path d="M0 -186 l-6 8 l6 26 l6 -26 z" fill="${d.tie}" stroke="${O}" stroke-width="2"/>`;
  s+=d.front;
  s+=arm(-1,d.armL)+arm(1,d.armR);
  s+=`<rect x="-12" y="-214" width="24" height="16" fill="${d.skin}" stroke="${O}" stroke-width="3"/>`;
  s+=`<circle cx="-64" cy="-254" r="12" fill="${d.skin}" stroke="${O}" stroke-width="3"/><circle cx="64" cy="-254" r="12" fill="${d.skin}" stroke="${O}" stroke-width="3"/>`;
  s+=`<rect x="-64" y="-320" width="128" height="114" rx="48" fill="${d.skin}"/>`;
  if(d.style==='short') s+=`<path d="M-64 -258 L-64 -274 Q-64 -320 -16 -320 L16 -320 Q64 -320 64 -274 L64 -262 Q56 -290 22 -290 Q2 -282 -18 -294 Q-50 -292 -64 -258 Z" fill="${d.hair}"/>`;
  else if(d.style==='bald') s+='';
  else if(d.style==='scarf') s+=`<path d="M-64 -246 L-64 -274 Q-64 -320 -16 -320 L16 -320 Q64 -320 64 -274 L64 -246 Q60 -292 0 -292 Q-60 -292 -64 -246 Z" fill="${d.hair}"/>`;
  else s+=`<path d="M-64 -250 L-64 -274 Q-64 -320 -16 -320 L16 -320 Q64 -320 64 -274 L64 -250 Q58 -282 34 -290 Q6 -298 -8 -286 Q-30 -296 -50 -284 Q-62 -270 -64 -250 Z" fill="${d.hair}"/>`;
  s+=`<rect x="-64" y="-320" width="128" height="114" rx="48" fill="none" stroke="${O}" stroke-width="3"/>`;
  if(d.style==='bun') s+=`<circle cx="34" cy="-292" r="6" fill="${C.white}" stroke="${O}" stroke-width="1.5"/><circle cx="44" cy="-287" r="5" fill="${C.peach}" stroke="${O}" stroke-width="1.5"/>`;
  const ey=-250+d.lookY*3, bl=d.blink?1.2:11;
  for(const sd of [-1,1]){
    const cx=sd*25;
    s+=`<ellipse cx="${cx}" cy="${ey}" rx="13" ry="${bl}" fill="${C.white}" stroke="${O}" stroke-width="2.2"/>`;
    if(!d.blink) s+=`<circle cx="${f(cx+d.look*4)}" cy="${f(ey+d.lookY*2)}" r="5.6" fill="${O}"/>`;
    const inner=-272-d.brow*5, outer=-272+d.brow*1;
    s+=`<path d="M${sd*14} ${inner} L${sd*36} ${outer}" stroke="${O}" stroke-width="3.2" stroke-linecap="round"/>`;
  }
  s+=`<path d="M3 -244 l-6 9 l6 2" fill="none" stroke="#9b8b7a" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>`;
  const m={smile:'M-14 -222 Q0 -211 14 -222',big:'M-16 -224 Q0 -204 16 -224 Z',flat:'M-12 -219 L12 -219',sad:'M-13 -215 Q0 -225 13 -215',o:''}[d.mouth];
  if(d.mouth==='o') s+=`<ellipse cx="0" cy="-219" rx="6" ry="8" fill="${O}"/>`;
  else if(d.mouth==='big') s+=`<path d="${m}" fill="${O}" stroke="${O}" stroke-width="2.6" stroke-linejoin="round"/>`;
  else s+=`<path d="${m}" fill="none" stroke="${O}" stroke-width="3" stroke-linecap="round"/>`;
  s+=d.extra;
  return `<g transform="translate(${f(d.x)},${f(d.y+d.bob)}) scale(${d.s*d.flip},${d.s})">${s}</g>`;
}
// Bequeme Varianten
const P1=o=>person(Object.assign({shirt:C.teal,hair:C.navy},o));
const P2=o=>person(Object.assign({shirt:C.peach,hair:C.brown,style:'bun'},o));
const P3=o=>person(Object.assign({shirt:C.mustard,hair:'#2a2420',skin:C.skin3,style:'short'},o));
const P4=o=>person(Object.assign({shirt:C.purple,hair:'#3b2a22',skin:C.skin2,style:'long'},o));
const wave=(seed=0)=>Math.sin(NOW*7+seed)*25;

// ---------- Erde ----------
function globe(cx,cy,r,id,tint=0,spin=true){
  const off=spin?(NOW*18)%400:120;
  let land='';
  for(const k of [0,400]){
    const x=cx-r-off+k;
    land+=`<g transform="translate(${f(x)},${cy-r}) scale(${r/150})">`+
      `<path d="M20 70 q30 -40 70 -20 q30 10 20 50 q-10 40 -40 50 q-30 0 -40 -30 q-20 -20 -10 -50z" fill="${C.sage}"/>`+
      `<path d="M150 40 q40 -20 80 10 q20 30 -10 50 q-20 20 -10 60 q0 40 -30 50 q-30 -20 -20 -70 q-30 -20 -10 -100z" fill="${C.sage}"/>`+
      `<path d="M270 150 q30 -10 50 20 q10 30 -20 40 q-30 0 -30 -60z" fill="${C.sage}"/>`+
      `<path d="M300 40 q40 0 60 30 q0 30 -40 30 q-30 -20 -20 -60z" fill="${C.sage}"/>`+
      `<path d="M60 190 q30 0 40 30 q-10 40 -30 50 q-20 -30 -10 -80z" fill="${C.sage}"/>`+
      `</g>`;
  }
  return `<defs><clipPath id="${id}"><circle cx="${cx}" cy="${cy}" r="${r}"/></clipPath></defs>`+
    `<circle cx="${cx}" cy="${cy}" r="${r}" fill="${C.blue}"/>`+
    `<g clip-path="url(#${id})">${land}<circle cx="${cx}" cy="${cy}" r="${r}" fill="#C8553D" opacity="${f(tint*45)/100}"/>`+
    `<path d="M${cx-r} ${cy-r*0.2} a${r} ${r} 0 0 0 ${2*r} 0 v${r*1.3} h${-2*r}z" fill="#000" opacity=".08"/></g>`+
    `<circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="${O}" stroke-width="3"/>`+
    `<path d="M${cx-r*0.55} ${cy-r*0.62} q${r*0.25} ${-r*0.18} ${r*0.5} ${-r*0.16}" stroke="#fff" stroke-width="${Math.max(3,r/25)}" fill="none" stroke-linecap="round" opacity=".45"/>`;
}
// Erde mit Afrika in der Mitte (statisch)
const AFRICA='M-38 -118 q30 -14 66 -4 q22 8 34 30 q18 6 36 2 q-4 30 -24 52 q-16 22 -18 50 q-4 40 -30 70 q-18 22 -36 18 q-14 -24 -14 -56 q0 -26 -16 -44 q-30 -4 -46 -30 q-14 -24 -4 -52 q20 -26 52 -36z';
function globeAfrica(cx,cy,r,id,hi=0){
  const k=r/150;
  return `<defs><clipPath id="${id}"><circle cx="${cx}" cy="${cy}" r="${r}"/></clipPath></defs><circle cx="${cx}" cy="${cy}" r="${r}" fill="${C.blue}"/>`+
   `<g clip-path="url(#${id})"><g transform="translate(${cx},${cy+10*k}) scale(${k})">`+
   `<path d="M-120 -170 q60 -30 130 -10 q40 20 10 40 q-60 10 -100 0 q-50 -10 -40 -30z" fill="${C.sage}"/>`+
   `<path d="M80 -150 q60 -10 110 30 q20 40 -30 50 q-40 -10 -60 -40 q-30 -20 -20 -40z" fill="${C.sage}"/>`+
   `<path d="${AFRICA}" fill="${C.sage}"/>`+
   `<path d="M-38 -118 q30 -14 66 -4 q22 8 34 30 q18 6 36 2 q-4 30 -24 52 q-16 22 -18 50 q-4 40 -30 70 q-18 22 -36 18 q-14 -24 -14 -56 q0 -26 -16 -44 q-30 -4 -46 -30 q-14 -24 -4 -52 q20 -26 52 -36z" fill="${C.mustard}" opacity="${f(hi*85)/100}" transform="translate(0,0)"/>`+
   `<path d="M-130 -40 q0 0 0 0" /><path d="M-170 -20 h60 M-180 -60 h0" stroke="none"/>`+
   `<path d="M-120 -60 q20 -20 50 -40" fill="none"/>`+
   `<path d="M-120 -60 h0" />`+
   `<path d="M120 20 q30 0 40 30 q-10 30 -40 20z" fill="${C.sage}"/>`+
   `</g><path d="M${cx-r} ${cy-r*0.2} a${r} ${r} 0 0 0 ${2*r} 0 v${r*1.3} h${-2*r}z" fill="#000" opacity=".08"/></g>`+
   `<circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="${O}" stroke-width="3"/>`+
   `<path d="M${cx-r*0.55} ${cy-r*0.62} q${r*0.25} ${-r*0.18} ${r*0.5} ${-r*0.16}" stroke="#fff" stroke-width="5" fill="none" stroke-linecap="round" opacity=".45"/>`;
}

// ---------- SDG-Kacheln (eigene vereinfachte Darstellung) ----------
function sdgTile(lt,t0,size=300){
  const k=size/300;
  const draw=P(lt,t0+0.4,t0+1.6);
  const typ=P(lt,t0+0.9,t0+2.2);
  const l1='BEZAHLBARE UND', l2='SAUBERE ENERGIE';
  const n=Math.round(typ*(l1.length+l2.length));
  const s1=l1.slice(0,Math.min(n,l1.length)), s2=l2.slice(0,Math.max(0,n-l1.length));
  let rays='';
  const rot=NOW*12;
  for(let i=0;i<12;i++){
    const a=rad(i*30+rot); if(draw<=i/12) continue;
    rays+=`<line x1="${f(150+Math.cos(a)*62)}" y1="${f(200+Math.sin(a)*62)}" x2="${f(150+Math.cos(a)*80)}" y2="${f(200+Math.sin(a)*80)}" stroke="#fff" stroke-width="9" stroke-linecap="round"/>`;
  }
  const circ=2*Math.PI*44*0.84;
  return `<g transform="scale(${k})">`+
    `<rect x="0" y="0" width="300" height="300" rx="34" fill="${C.yellow}" stroke="${O}" stroke-width="3"/>`+
    txt(24,110,'7',118,'#fff',900,'start')+txt(100,52,s1,19,'#fff',900,'start')+txt(100,78,s2,19,'#fff',900,'start')+rays+
    `<circle cx="150" cy="200" r="44" fill="none" stroke="#fff" stroke-width="10" stroke-linecap="round" stroke-dasharray="${f(circ*draw)} 999" transform="rotate(-60 150 200)"/>`+
    `<line x1="150" y1="168" x2="150" y2="${f(168+34*draw)}" stroke="#fff" stroke-width="10" stroke-linecap="round"/></g>`;
}
function miniTile(x,y,n,label1,label2,col,k=1,icon=''){
  if(k<=0) return '';
  return popAt(x,y,k,`<rect x="-80" y="-80" width="160" height="160" rx="22" fill="${col}" stroke="${O}" stroke-width="3"/>`+
    txt(-64,-30,n,50,'#fff',900,'start')+txt(-64,0,label1,14,'#fff',900,'start')+txt(-64,18,label2,14,'#fff',900,'start')+icon);
}

// ---------- Icons ----------
function bulb(x,y,s=1,on=1,led=false){
  let b=`<g transform="translate(${f(x)},${f(y)}) scale(${s})">`;
  if(on>0) b+=`<circle cx="0" cy="0" r="${f(64+4*Math.sin(NOW*5))}" fill="${C.lamp}" opacity="${f(on*35)/100}"/>`;
  b+=`<path d="M-38 0 a38 38 0 1 1 76 0 q0 22 -16 36 v14 h-44 v-14 q-16 -14 -16 -36z" fill="${on>0.5?'#FBE7A1':'#E8E1D0'}" stroke="${O}" stroke-width="3"/>`;
  b+= led ? `<rect x="-16" y="-8" width="32" height="18" rx="5" fill="${C.yellow}" stroke="${O}" stroke-width="2"/>`
          : `<path d="M-12 30 v-30 l6 -8 l6 8 l6 -8 l6 8 v30" fill="none" stroke="${C.mustard}" stroke-width="3"/>`;
  b+=`<rect x="-22" y="50" width="44" height="22" rx="6" fill="${C.grey}" stroke="${O}" stroke-width="3"/></g>`;
  return b;
}
function solarPanel(x,y,s=1){
  let sp=`<g transform="translate(${f(x)},${f(y)}) scale(${s})"><path d="M-80 0 L80 0 L60 -90 L-60 -90z" fill="#4F6D8A" stroke="${O}" stroke-width="3" stroke-linejoin="round"/>`;
  for(let i=1;i<4;i++) sp+=`<line x1="${-80+i*40}" y1="0" x2="${-60+i*30}" y2="-90" stroke="#A9C3D6" stroke-width="2"/>`;
  const gl=((NOW*0.6)%1);
  sp+=`<line x1="-70" y1="-45" x2="70" y2="-45" stroke="#A9C3D6" stroke-width="2"/><rect x="-6" y="0" width="12" height="40" fill="${C.navy}"/>`;
  sp+=`<line x1="${f(-70+gl*120)}" y1="-10" x2="${f(-50+gl*120)}" y2="-80" stroke="#fff" stroke-width="6" opacity=".6" stroke-linecap="round"/></g>`;
  return sp;
}
function roofPanel(x,y,w=60){ return `<path d="M${x} ${y} l${w} 0 l-10 -22 l${-w+20} 0z" fill="#4F6D8A" stroke="${O}" stroke-width="2.5" stroke-linejoin="round"/><line x1="${x+w/2}" y1="${y}" x2="${x+w/2-5}" y2="${y-22}" stroke="#A9C3D6" stroke-width="2"/>`; }
function turbine(x,y,s=1,speed=160){
  let w=`<g transform="translate(${f(x)},${f(y)}) scale(${s})"><path d="M-8 0 L-4 -170 h8 L8 0z" fill="${C.white}" stroke="${O}" stroke-width="3"/>`;
  const ang=NOW*speed;
  for(let b=0;b<3;b++) w+=`<path d="M0 0 C10 -20 8 -80 0 -100 C-8 -80 -10 -20 0 0z" fill="${C.white}" stroke="${O}" stroke-width="3" transform="translate(0,-170) rotate(${f(ang+b*120)})"/>`;
  return w+`<circle cx="0" cy="-170" r="10" fill="${C.teal}" stroke="${O}" stroke-width="3"/></g>`;
}
function sun(x,y,s=1,face=true){
  let g=`<g transform="translate(${f(x)},${f(y)}) scale(${s})"><circle r="70" fill="${C.yellow}" stroke="${O}" stroke-width="3"/>`;
  for(let i=0;i<12;i++){const a=rad(i*30+NOW*15); g+=`<path d="M${f(Math.cos(a)*84)} ${f(Math.sin(a)*84)} L${f(Math.cos(a)*112)} ${f(Math.sin(a)*112)}" stroke="${C.mustard}" stroke-width="10" stroke-linecap="round"/>`;}
  if(face){const b=blinkAt(5); g+=`<ellipse cx="-22" cy="-8" rx="7" ry="${b?1:9}" fill="${O}"/><ellipse cx="22" cy="-8" rx="7" ry="${b?1:9}" fill="${O}"/><path d="M-22 18 Q0 36 22 18" stroke="${O}" stroke-width="4" fill="none" stroke-linecap="round"/><circle cx="-40" cy="14" r="9" fill="${C.peach}" opacity=".8"/><circle cx="40" cy="14" r="9" fill="${C.peach}" opacity=".8"/>`;}
  return g+'</g>';
}
function house(x,y,w=140,h=110,col=C.peach,lit=0,roof=C.red,panels=0){
  let s=`<g transform="translate(${f(x)},${f(y)})"><rect x="${-w/2}" y="${-h}" width="${w}" height="${h}" fill="${col}" stroke="${O}" stroke-width="3"/>`;
  s+=`<path d="M${-w/2-14} ${-h} L0 ${-h-w*0.45} L${w/2+14} ${-h}z" fill="${roof}" stroke="${O}" stroke-width="3" stroke-linejoin="round"/>`;
  if(panels>0) s+=G(`<path d="M${-w*0.05} ${-h-w*0.06} l${w*0.42} 0 l${-w*0.25} ${-w*0.2} l${-w*0.17} 0z" fill="#4F6D8A" stroke="${O}" stroke-width="2.5" stroke-linejoin="round"/>`,'',panels);
  s+=`<rect x="${-w*0.36}" y="${-h*0.72}" width="${w*0.24}" height="${h*0.28}" rx="6" fill="${lit>0?C.lamp:'#A5AEB8'}" stroke="${O}" stroke-width="2.5"/>`;
  s+=`<rect x="${w*0.1}" y="${-h*0.55}" width="${w*0.22}" height="${h*0.55}" rx="6" fill="${C.teal}" stroke="${O}" stroke-width="2.5"/>`;
  if(lit>0) s+=`<circle cx="${-w*0.24}" cy="${-h*0.58}" r="${w*0.22}" fill="${C.lamp}" opacity="${f(lit*25)/100}"/>`;
  return s+'</g>';
}
function hut(x,y,s=1,lit=0,panel=0){
  let g=`<g transform="translate(${f(x)},${f(y)}) scale(${s})"><rect x="-60" y="-80" width="120" height="80" rx="10" fill="#D9B98F" stroke="${O}" stroke-width="3"/>`;
  g+=`<path d="M-80 -74 Q0 -150 80 -74z" fill="#B58A55" stroke="${O}" stroke-width="3" stroke-linejoin="round"/>`;
  if(panel>0) g+=popAt(10,-104,panel,`<path d="M-26 8 l50 0 l-10 -20 l-32 0z" fill="#4F6D8A" stroke="${O}" stroke-width="2.5" stroke-linejoin="round"/>`);
  g+=`<rect x="-40" y="-56" width="28" height="26" rx="5" fill="${lit>0?C.lamp:'#5a4a3a'}" stroke="${O}" stroke-width="2.5"/><rect x="12" y="-50" width="30" height="50" rx="6" fill="#7a5a3a" stroke="${O}" stroke-width="2.5"/>`;
  if(lit>0) g+=`<circle cx="-26" cy="-43" r="26" fill="${C.lamp}" opacity="${f(lit*30)/100}"/>`;
  return g+'</g>';
}
function factory(x,y,s=1,lit=0,smoke=0,clean=0){
  let fb=`<g transform="translate(${f(x)},${f(y)}) scale(${s})"><path d="M-140 0 V-160 l70 -50 v50 l70 -50 v50 l70 -50 V0z" fill="${C.peach}" stroke="${O}" stroke-width="3" stroke-linejoin="round"/>`;
  fb+=`<rect x="90" y="-290" width="36" height="290" fill="#B8B0A0" stroke="${O}" stroke-width="3"/>`;
  for(let i=0;i<3;i++){const a=((NOW*0.8+i/3)%1); const op=Math.max(smoke,clean);
    if(op>0) fb+=`<circle cx="${f(108+a*30)}" cy="${f(-300-a*70)}" r="${f(10+a*16)}" fill="${clean?'#fff':'#5D6170'}" opacity="${f(0.8*(1-a)*op*100)/100}" stroke="${O}" stroke-width="1.5"/>`;}
  for(let c=0;c<3;c++) fb+=`<rect x="${-120+c*70}" y="-130" width="44" height="40" rx="8" fill="${lit>0?C.lamp:'#A5AEB8'}" stroke="${O}" stroke-width="2"/>`;
  fb+=`<rect x="-30" y="-70" width="60" height="70" rx="8" fill="${C.teal}" stroke="${O}" stroke-width="3"/>`;
  return fb+'</g>';
}
function gear(x,y,r=22,rot=0,col=C.grey){
  let g=`<circle r="${r}" fill="${col}" stroke="${O}" stroke-width="3"/><circle r="${r*0.36}" fill="${C.white}" stroke="${O}" stroke-width="2"/>`;
  for(let i=0;i<8;i++) g=`<rect x="${-r*0.27}" y="${-r*1.45}" width="${r*0.54}" height="${r*0.64}" rx="3" fill="${col}" stroke="${O}" stroke-width="2" transform="rotate(${i*45})"/>`+g;
  return `<g transform="translate(${f(x)},${f(y)}) rotate(${f(rot)})">${g}</g>`;
}
function phone(x,y,s=1,screen='#3E5368',inner=''){
  return `<g transform="translate(${f(x)},${f(y)}) scale(${s})"><rect x="-45" y="-80" width="90" height="160" rx="16" fill="${C.navy}" stroke="${O}" stroke-width="3"/><rect x="-37" y="-70" width="74" height="140" rx="10" fill="${screen}"/>${inner}</g>`;
}
function battery(x,y,fill=0.5,s=1,col=null){
  const c=col||(fill<0.3?C.red:fill<0.7?C.mustard:C.green);
  return `<g transform="translate(${f(x)},${f(y)}) scale(${s})"><rect x="-40" y="-70" width="80" height="140" rx="16" fill="${C.white}" stroke="${O}" stroke-width="4"/><rect x="-16" y="-82" width="32" height="12" rx="4" fill="${O}"/>`+
   `<rect x="-30" y="${f(60-120*fill)}" width="60" height="${f(120*fill)}" rx="8" fill="${c}"/>`+
   `<path d="M4 -34 l-20 38 h16 l-6 34 l24 -44 h-16 l8 -28z" fill="#fff" stroke="${O}" stroke-width="2" stroke-linejoin="round" opacity=".9"/></g>`;
}
function car(x,y,col=C.red,s=1){
  return `<g transform="translate(${f(x)},${f(y)}) scale(${s})"><path d="M-90 -20 q0 -24 24 -26 l20 -30 q6 -8 16 -8 h70 q12 0 18 10 l18 28 q28 4 28 26 v20 h-194z" fill="${col}" stroke="${O}" stroke-width="3" stroke-linejoin="round"/>`+
   `<path d="M-40 -48 l14 -24 h34 v24z M18 -48 v-24 h30 l14 24z" fill="${C.white}" stroke="${O}" stroke-width="2.5"/>`+
   `<circle cx="-52" cy="0" r="18" fill="${O}"/><circle cx="-52" cy="0" r="7" fill="${C.grey}"/><circle cx="62" cy="0" r="18" fill="${O}"/><circle cx="62" cy="0" r="7" fill="${C.grey}"/></g>`;
}
function bus(x,y,s=1,col=C.mustard){
  let b=`<g transform="translate(${f(x)},${f(y)}) scale(${s})"><rect x="-150" y="-110" width="300" height="100" rx="22" fill="${col}" stroke="${O}" stroke-width="3"/>`;
  for(let i=0;i<5;i++) b+=`<rect x="${-134+i*54}" y="-96" width="42" height="36" rx="8" fill="${C.white}" stroke="${O}" stroke-width="2.5"/>`;
  b+=`<circle cx="-90" cy="-8" r="20" fill="${O}"/><circle cx="-90" cy="-8" r="8" fill="${C.grey}"/><circle cx="90" cy="-8" r="20" fill="${O}"/><circle cx="90" cy="-8" r="8" fill="${C.grey}"/></g>`;
  return b;
}
function plane(x,y,s=1){
  return `<g transform="translate(${f(x)},${f(y)}) scale(${s})"><path d="M-110 0 q0 -16 20 -16 h150 q40 0 50 16 q-10 16 -50 16 h-150 q-20 0 -20 -16z" fill="${C.white}" stroke="${O}" stroke-width="3"/>`+
   `<path d="M-10 0 l-40 50 h26 l50 -50z M-10 0 l-40 -46 h26 l50 46z" fill="${C.blue}" stroke="${O}" stroke-width="3" stroke-linejoin="round"/><path d="M-100 -12 l-14 -30 h18 l20 28z" fill="${C.blue}" stroke="${O}" stroke-width="3" stroke-linejoin="round"/>`+
   `<circle cx="40" cy="-4" r="4" fill="${C.blue}"/><circle cx="20" cy="-4" r="4" fill="${C.blue}"/><circle cx="0" cy="-4" r="4" fill="${C.blue}"/></g>`;
}
function ship(x,y,s=1){
  return `<g transform="translate(${f(x)},${f(y)}) scale(${s})"><path d="M-130 -30 h260 l-30 50 h-200z" fill="${C.red}" stroke="${O}" stroke-width="3" stroke-linejoin="round"/>`+
   `<rect x="-90" y="-70" width="60" height="40" fill="${C.teal}" stroke="${O}" stroke-width="2.5"/><rect x="-26" y="-70" width="60" height="40" fill="${C.mustard}" stroke="${O}" stroke-width="2.5"/><rect x="40" y="-90" width="50" height="60" rx="6" fill="${C.white}" stroke="${O}" stroke-width="2.5"/><rect x="60" y="-116" width="16" height="26" fill="${C.navy}"/></g>`;
}
function bike(x,y,s=1,rot=0){
  const w=(cx)=>`<g transform="translate(${cx},0) rotate(${f(rot)})"><circle r="34" fill="none" stroke="${O}" stroke-width="6"/><line x1="-34" y1="0" x2="34" y2="0" stroke="${O}" stroke-width="2"/><line x1="0" y1="-34" x2="0" y2="34" stroke="${O}" stroke-width="2"/></g>`;
  return `<g transform="translate(${f(x)},${f(y)}) scale(${s})">${w(-60)}${w(60)}<path d="M-60 0 L-20 -50 L40 -50 L60 0 M-20 -50 L0 0 L40 -50 M0 0 L-60 0 M-26 -62 h24 M40 -50 l-6 -20 h20" fill="none" stroke="${C.teal}" stroke-width="8" stroke-linejoin="round" stroke-linecap="round"/></g>`;
}
function leaf(x,y,s=1,col=C.green){ return `<g transform="translate(${f(x)},${f(y)}) scale(${s})"><path d="M0 0 C-28 -12 -32 -48 0 -72 C32 -48 28 -12 0 0z" fill="${col}" stroke="${O}" stroke-width="3"/><line x1="0" y1="6" x2="0" y2="-52" stroke="${O}" stroke-width="3"/></g>`; }
function euro(x,y,s=1){ return `<g transform="translate(${f(x)},${f(y)}) scale(${s})"><circle r="40" fill="${C.yellow}" stroke="${O}" stroke-width="3"/><circle r="31" fill="none" stroke="${C.mustard}" stroke-width="3"/>${txt(0,15,'€',44,O,900)}</g>`; }
function plug(x,y,s=1){ return `<g transform="translate(${f(x)},${f(y)}) scale(${s})"><rect x="-40" y="-40" width="80" height="80" rx="22" fill="${C.white}" stroke="${O}" stroke-width="3"/><circle r="28" fill="#E9E2D2" stroke="${O}" stroke-width="2"/><circle cx="-11" r="6" fill="${O}"/><circle cx="11" r="6" fill="${O}"/></g>`; }
function bolt(x,y,s=1,col=C.yellow){ return `<g transform="translate(${f(x)},${f(y)}) scale(${s})"><path d="M8 -50 l-36 56 h24 l-10 46 l40 -62 h-24 l14 -40z" fill="${col}" stroke="${O}" stroke-width="3" stroke-linejoin="round"/></g>`; }
function wifi(x,y,s=1,col=O){ return `<g transform="translate(${f(x)},${f(y)}) scale(${s})" fill="none" stroke="${col}" stroke-width="8" stroke-linecap="round"><path d="M-44 -14 a62 62 0 0 1 88 0"/><path d="M-28 4 a40 40 0 0 1 56 0"/><circle cx="0" cy="22" r="6" fill="${col}" stroke="none"/></g>`; }
function thermo(x,y,v,s=1){ return `<g transform="translate(${f(x)},${f(y)}) scale(${s})"><rect x="-20" y="-120" width="40" height="230" rx="20" fill="${C.white}" stroke="${O}" stroke-width="3"/><circle cx="0" cy="120" r="34" fill="${C.red}" stroke="${O}" stroke-width="3"/><rect x="-10" y="${f(80-160*v)}" width="20" height="${f(50+160*v)}" rx="10" fill="${C.red}"/></g>`; }
function barrel(x,y,s=1,col='#4A4E5C'){ return `<g transform="translate(${f(x)},${f(y)}) scale(${s})"><rect x="-36" y="-50" width="72" height="100" rx="12" fill="${col}" stroke="${O}" stroke-width="3"/><line x1="-36" y1="-18" x2="36" y2="-18" stroke="${O}" stroke-width="3"/><line x1="-36" y1="18" x2="36" y2="18" stroke="${O}" stroke-width="3"/>${txt(0,8,'ÖL',20,'#fff',900)}</g>`; }
function flame(x,y,s=1,col='#F8B84E',inner='#FBE7A1'){
  const fl=1+0.08*Math.sin(NOW*23)+0.05*Math.sin(NOW*37), sw=3*Math.sin(NOW*9);
  return `<g transform="translate(${f(x)},${f(y)}) scale(${f(s*fl*100)/100},${f(s*fl*105)/100}) skewX(${f(sw)})"><path d="M0 0 C-22 -10 -16 -40 0 -70 C16 -40 22 -10 0 0z" fill="${col}"/><path d="M0 -4 C-10 -10 -8 -26 0 -44 C8 -26 10 -10 0 -4z" fill="${inner}"/></g>`;
}
function smokePuffs(x,y,n,spread,op=1,col='#6E7280',seed=0,rise=90){
  let s='';
  for(let i=0;i<n;i++){const a=((NOW*0.35+i/n+rnd(i+seed)*0.3)%1); s+=`<circle cx="${f(x+Math.sin(i*2.3+a*3)*spread*a)}" cy="${f(y-a*rise)}" r="${f(10+a*22)}" fill="${col}" opacity="${f(op*(1-a)*80)/100}"/>`;}
  return s;
}
function tv(x,y,s=1,on=0,standby=1){
  return `<g transform="translate(${f(x)},${f(y)}) scale(${s})"><rect x="-90" y="-110" width="180" height="110" rx="12" fill="${O}" stroke="${O}" stroke-width="3"/><rect x="-80" y="-100" width="160" height="90" rx="6" fill="${on?'#9FC3C0':'#3A4050'}"/>`+
   `<rect x="-10" y="0" width="20" height="20" fill="${O}"/><rect x="-50" y="18" width="100" height="10" rx="5" fill="${O}"/>`+
   (standby?`<circle cx="70" cy="-4" r="4" fill="#FF4A3A"/><circle cx="70" cy="-4" r="${f(8+3*Math.sin(NOW*4))}" fill="#FF4A3A" opacity=".25"/>`:'')+`</g>`;
}
function powerStrip(x,y,on=1){
  return `<g transform="translate(${f(x)},${f(y)})"><rect x="-80" y="-18" width="160" height="36" rx="12" fill="${C.white}" stroke="${O}" stroke-width="3"/>`+
   [-40,-5,30].map(cx=>`<circle cx="${cx}" cy="0" r="10" fill="#E9E2D2" stroke="${O}" stroke-width="2"/>`).join('')+
   `<rect x="52" y="-10" width="18" height="20" rx="4" fill="${on?C.red:'#9a9a9a'}" stroke="${O}" stroke-width="2"/></g>`;
}
function hourglass(x,y,s=1,p=0){
  return `<g transform="translate(${f(x)},${f(y)}) scale(${s})"><rect x="-40" y="-70" width="80" height="12" rx="6" fill="${C.woodD}" stroke="${O}" stroke-width="3"/><rect x="-40" y="58" width="80" height="12" rx="6" fill="${C.woodD}" stroke="${O}" stroke-width="3"/>`+
   `<path d="M-30 -58 h60 q0 40 -24 58 q24 18 24 58 h-60 q0 -40 24 -58 q-24 -18 -24 -58z" fill="#EAF1EE" stroke="${O}" stroke-width="3"/>`+
   `<path d="M${f(-22*(1-p))} ${f(-50+40*p)} h${f(44*(1-p))} q-10 ${f(30*(1-p))} -${f(22*(1-p))} ${f(40*(1-p))}z" fill="${C.mustard}"/><path d="M-24 56 q24 ${f(-40*p)} 48 0z" fill="${C.mustard}"/></g>`;
}
function donut(cx,cy,r,frac,col,bg='#D8D0BE',w=46,lbl=''){
  const a=2*Math.PI*frac, x=cx+r*Math.sin(a), y=cy-r*Math.cos(a);
  return `<circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="${bg}" stroke-width="${w}"/>`+
    (frac>0.001?`<path d="M${cx} ${cy-r} A${r} ${r} 0 ${frac>0.5?1:0} 1 ${f(x)} ${f(y)}" fill="none" stroke="${col}" stroke-width="${w}"/>`:'')+
    `<circle cx="${cx}" cy="${cy}" r="${r+w/2}" fill="none" stroke="${O}" stroke-width="3"/><circle cx="${cx}" cy="${cy}" r="${r-w/2}" fill="none" stroke="${O}" stroke-width="3"/>`+lbl;
}
function personIcon(x,y,s=1,col=C.teal){ return `<g transform="translate(${f(x)},${f(y)}) scale(${s})"><circle cx="0" cy="-26" r="13" fill="${col}" stroke="${O}" stroke-width="2.5"/><path d="M-18 14 v-12 q0 -16 18 -16 q18 0 18 16 v12z" fill="${col}" stroke="${O}" stroke-width="2.5"/></g>`; }

const BUILD=[[30,150,250],[190,120,330],[320,160,210],[490,110,380],[610,150,270],[770,120,350],[900,170,230],[1080,170,300]];
function city(litFn){
  let s='';
  BUILD.forEach(([x,w,h],bi)=>{
    const top=600-h, col=['#3A4256','#424B61','#353C4E'][bi%3];
    s+=`<rect x="${x}" y="${top}" width="${w}" height="${h+20}" rx="16" fill="${col}" stroke="${O}" stroke-width="3"/>`;
    let wi=0;
    for(let yy=top+26; yy<580; yy+=44) for(let xx=x+20; xx<x+w-30; xx+=34){
      const lit=litFn(bi*100+wi++);
      s+=`<rect x="${xx}" y="${yy}" width="20" height="26" rx="6" fill="${lit>0?C.lamp:'#2A3040'}" opacity="${lit>0?f((0.35+0.65*lit)*100)/100:1}"/>`;
    }
  });
  return s+`<rect x="0" y="600" width="1280" height="120" fill="#20242F"/>`;
}
function stars(n=60){ let s=''; for(let i=0;i<n;i++){const tw=0.4+0.6*Math.abs(Math.sin(NOW*2+i)); s+=`<circle cx="${f(rnd(i)*1280)}" cy="${f(rnd(i+99)*300)}" r="${f(1+rnd(i+7)*2)}" fill="#F6EFD9" opacity="${f(tw*100)/100}"/>`;} return s; }
