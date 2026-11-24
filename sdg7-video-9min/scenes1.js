// ======================================================================
//  Szenen Teil 1 – Energie im Alltag & Was ist SDG 7?
//  Signatur: (lt, D, c, L) – lt = Zeit in der Szene, D = Dauer, c[i] = Start von Satz i, L[i] = Länge von Satz i
// ======================================================================
const S = {};
const at=(c,L,i,fr)=>c[i]+L[i]*fr;

S.phone=(lt,D)=>{
  const charge=lerp(0.18,1,eio(P(lt,0.4,D-0.5)));
  const bc=charge<0.4?C.red:(charge<0.75?C.mustard:C.green);
  let s=stripes()+`<rect x="0" y="520" width="1280" height="200" fill="${C.wood}" stroke="${O}" stroke-width="3"/><rect x="0" y="545" width="1280" height="10" fill="${C.woodD}"/>`;
  s+=`<rect x="960" y="330" width="110" height="110" rx="26" fill="${C.white}" stroke="${O}" stroke-width="3"/><circle cx="1015" cy="385" r="34" fill="#E9E2D2" stroke="${O}" stroke-width="2"/><rect x="992" y="368" width="46" height="34" rx="10" fill="#DDD5C2" stroke="${O}" stroke-width="3"/>`;
  s+=`<path d="M1015 402 C1015 520 760 560 680 520" fill="none" stroke="${O}" stroke-width="10" stroke-linecap="round"/><path d="M1015 402 C1015 520 760 560 680 520" fill="none" stroke="#F4EFE3" stroke-width="5" stroke-linecap="round"/>`;
  s+=`<path d="M560 520 l30 -30 h140 l30 30z" fill="${C.navy}" stroke="${O}" stroke-width="3"/><rect x="570" y="200" width="180" height="310" rx="30" fill="${C.navy}" stroke="${O}" stroke-width="3"/><rect x="584" y="216" width="152" height="278" rx="20" fill="#3E5368"/><rect x="630" y="226" width="60" height="8" rx="4" fill="${C.navy}"/>`;
  s+=`<rect x="610" y="300" width="96" height="150" rx="18" fill="none" stroke="#fff" stroke-width="6"/><rect x="640" y="288" width="36" height="12" rx="4" fill="#fff"/>`;
  const fh=134*charge;
  s+=`<rect x="618" y="${f(442-fh)}" width="80" height="${f(fh)}" rx="11" fill="${bc}"/>`;
  s+=`<path d="M664 330 l-22 46 h18 l-8 40 l28 -52 h-18 l10 -34z" fill="#fff" stroke="${O}" stroke-width="2" stroke-linejoin="round" opacity="${f((0.6+0.4*Math.sin(NOW*6))*100)/100}"/>`;
  s+=txt(658,480,Math.round(charge*100)+' %',26,'#fff',900);
  s+=P2({x:300,y:600,s:0.95,look:1.6,blink:blinkAt(1),armR:[30,-60],mouth:charge>0.9?'big':'smile',bob:Math.sin(NOW*3)*2});
  return s;
};

S.train=(lt,D)=>{
  let s=`<rect width="1280" height="720" fill="#E9E1CC"/><circle cx="1000" cy="170" r="70" fill="${C.peach}" opacity=".9"/>`;
  for(let i=0;i<3;i++) s+=cloud(((200+i*420)-lt*20)%1400,120+i*30);
  s+=`<path d="M0 420 Q200 330 420 400 T860 380 T1280 400 V720 H0z" fill="${C.sage}" stroke="${O}" stroke-width="3"/><path d="M0 480 Q260 430 520 470 T1280 460 V720 H0z" fill="${C.sageD}" stroke="${O}" stroke-width="3"/>`;
  s+=`<line x1="0" y1="300" x2="1280" y2="300" stroke="${O}" stroke-width="3"/>`;
  for(let i=0;i<4;i++){const px=((i*380)-lt*60)%1520+80; s+=`<rect x="${f(px)}" y="290" width="12" height="290" fill="${C.navy}" stroke="${O}" stroke-width="2"/><rect x="${f(px-30)}" y="290" width="50" height="10" fill="${C.navy}"/>`;}
  s+=`<rect x="0" y="575" width="1280" height="16" fill="${C.navy}"/><rect x="0" y="591" width="1280" height="130" fill="#B9A98C" stroke="${O}" stroke-width="3"/>`;
  for(let x=-40;x<1320;x+=46) s+=`<rect x="${f(x-((lt*600)%46))}" y="591" width="20" height="10" fill="#8F7F66"/>`;
  const tx=lerp(1350,-1750,eio(P(lt,0.15,D-0.15)));
  let tr='';
  for(let c=0;c<3;c++){
    const x=tx+c*330, nose=c===0;
    tr+= nose ? `<path d="M${f(x+60)} 420 h260 v150 h-310 q-20 0 -20 -24 q0 -70 70 -126z" fill="${C.teal}" stroke="${O}" stroke-width="3"/>`
              : `<rect x="${f(x)}" y="420" width="320" height="150" rx="26" fill="${C.teal}" stroke="${O}" stroke-width="3"/>`;
    tr+=`<rect x="${f(x+(nose?60:20))}" y="440" width="${nose?240:280}" height="54" rx="16" fill="${C.white}" stroke="${O}" stroke-width="3"/>`;
    for(let wv=0; wv<(nose?3:4); wv++){const wx=x+(nose?110:40)+wv*66; tr+=`<line x1="${f(wx+40)}" y1="442" x2="${f(wx+40)}" y2="492" stroke="${O}" stroke-width="3"/><circle cx="${f(wx+12)}" cy="470" r="12" fill="${['#6B4038','#2F3E4C','#D9A93A'][(wv+c)%3]}"/>`;}
    tr+=`<rect x="${f(x)}" y="520" width="320" height="12" fill="${C.tealD}"/>`;
    for(const wx of [50,110,210,270]) tr+=`<circle cx="${f(x+wx)}" cy="572" r="16" fill="${O}"/><circle cx="${f(x+wx)}" cy="572" r="6" fill="${C.grey}"/>`;
  }
  tr+=`<path d="M${f(tx+150)} 420 l40 -60 l-40 -60 M${f(tx+120)} 300 h80" fill="none" stroke="${O}" stroke-width="5" stroke-linejoin="round"/>`;
  if(Math.sin(NOW*40)>0.3) tr+=`<path d="M${f(tx+160)} 296 l10 -14 l4 10 l10 -12" stroke="${C.yellow}" stroke-width="4" fill="none"/>`;
  for(let i=0;i<5;i++) tr+=`<line x1="${f(tx+1000+i*20)}" y1="${440+i*25}" x2="${f(tx+1100+i*40)}" y2="${440+i*25}" stroke="${O}" stroke-width="3" stroke-linecap="round" opacity=".35"/>`;
  return s+tr;
};

S.city=(lt,D)=>{
  let s=`<rect width="1280" height="720" fill="${C.night}"/>`+stars();
  s+=`<circle cx="1100" cy="110" r="44" fill="#F3E7C4"/><circle cx="1122" cy="96" r="40" fill="${C.night}"/>`;
  s+=city(id=>clamp((lt-0.3-rnd(id)*(D-1.3))/0.25));
  for(let i=0;i<6;i++){const x=100+i*220,on=lt>0.6+i*0.35;
    s+=`<rect x="${x}" y="560" width="8" height="70" fill="${C.navy}"/><circle cx="${x+4}" cy="556" r="12" fill="${on?C.lamp:'#4A5163'}" stroke="${O}" stroke-width="2"/>`;
    if(on) s+=`<circle cx="${x+4}" cy="556" r="40" fill="${C.lamp}" opacity=".18"/>`;}
  return s;
};

S.blackout=(lt,D,c,L)=>{
  let s=`<rect width="1280" height="720" fill="${C.night}"/>`;
  if(lt<1.2){const flick=lt<0.7?(lt>0.3&&Math.sin(lt*70)>0?0.15:1):0; s+=city(()=>flick);}
  s+=`<rect width="1280" height="720" fill="#0E1016" opacity="${f((P(lt,0.7,0.8)*0.85+P(lt,1.0,1.6)*0.15)*100)/100}"/>`;
  const ca=P(lt,1.4,2.3);
  if(ca>0){
    let k=`<defs><radialGradient id="glow"><stop offset="0" stop-color="#F9C66A" stop-opacity=".55"/><stop offset="1" stop-color="#F9C66A" stop-opacity="0"/></radialGradient></defs>`;
    k+=`<circle cx="640" cy="370" r="${f(220*(1+0.06*Math.sin(NOW*23)))}" fill="url(#glow)"/><ellipse cx="640" cy="560" rx="90" ry="16" fill="#1c1f29"/>`;
    k+=`<rect x="600" y="410" width="80" height="150" rx="14" fill="#F1E6CF" stroke="${O}" stroke-width="3"/><path d="M610 420 q10 30 0 50" stroke="#E2D4B5" stroke-width="8" fill="none" stroke-linecap="round"/><line x1="640" y1="410" x2="640" y2="392" stroke="${O}" stroke-width="4"/>`;
    k+=flame(640,395);
    s+=G(k,'',ca);
  }
  // Was alles fehlt
  const ic=[
    [330,300,`${bulb(0,0,0.9,0)}`],
    [640,170,wifi(0,0,1.3,'#CFC8B6')],
    [950,300,`<rect x="-70" y="-46" width="140" height="92" rx="14" fill="#CFC8B6" stroke="${O}" stroke-width="3"/><rect x="-56" y="-32" width="80" height="64" rx="8" fill="#5a6070"/><circle cx="46" cy="-14" r="8" fill="${O}"/><circle cx="46" cy="14" r="8" fill="${O}"/>`]];
  ic.forEach(([x,y,g],i)=>{const k=pop(lt,at(c,L,1,[0.02,0.25,0.5][i])); s+=popAt(x,y,k,g)+cross(x+60,y-50,pop(lt,at(c,L,1,[0.02,0.25,0.5][i])+0.3),22);});
  return s;
};

S.world=(lt,D,c,L)=>{
  let s=stripes();
  const cnt=Math.round(700*eout(P(lt,c[0],c[0]+2.2)));
  s+=G(txt(310,190,'≈ '+cnt+' Mio.',72,O,900)+txt(310,236,'Menschen ohne Strom',28,O,800),'',P(lt,c[0]-0.2,c[0]+0.3));
  // Menschen-Raster: jeder 12. ohne Strom
  for(let r=0;r<4;r++) for(let q=0;q<12;q++){
    const i=r*12+q, k=pop(lt,c[0]+0.2+i*0.03,0.4), off=[5,18,31,44].includes(i);
    const hi=off?P(lt,c[0]+2.4,c[0]+3):0;
    s+=popAt(90+q*38,310+r*58,k,personIcon(0,0,0.85,off?(hi>0.5?'#5a6070':C.teal):C.teal));
  }
  s+=G(pill(310,560,'jeder 12. Mensch',C.white,O,24),'',P(lt,c[0]+2.6,c[0]+3.1));
  s+=globeAfrica(900,330,215,'gaf',P(lt,c[1],c[1]+0.9));
  s+=G(pill(900,74,'Afrika südlich der Sahara',C.mustard,O,26),'',P(lt,c[1]+0.4,c[1]+0.9));
  // Stromnetz weit weg
  if(lt>at(c,L,1,0.55)){const k=P(lt,at(c,L,1,0.55),at(c,L,1,0.75));
    s+=G(`<path d="M1050 470 q60 40 150 20" stroke="${O}" stroke-width="3" stroke-dasharray="8 8" fill="none"/>`+txt(1150,540,'Stromnetz?',24,O,900),'',k);}
  return s;
};

function card(x,y,w,h,k,inner,col='#F8F3E8',stroke=O){
  if(k<=0) return '';
  return `<g transform="translate(${f(x+w/2)},${f(y+h/2)}) scale(${k.toFixed(4)}) translate(${-w/2},${-h/2})"><rect x="0" y="0" width="${w}" height="${h}" rx="30" fill="${col}" stroke="${stroke}" stroke-width="3"/>${inner}</g>`;
}
S.without=(lt,D,c,L)=>{
  let s=stripes();
  s+=title('Ohne Strom …',lt,90,46,0.1);
  const t=[at(c,L,0,0.02),at(c,L,0,0.36),at(c,L,0,0.68)];
  // Lernen
  let a=`<rect x="20" y="20" width="300" height="260" rx="20" fill="#2F3442"/>`+flame(110,170,0.6)+`<rect x="98" y="172" width="24" height="50" rx="6" fill="#F1E6CF" stroke="${O}" stroke-width="2"/>`;
  a+=`<g transform="translate(220,280)">${P1({s:0.55,armL:[20,-110],armR:[20,-110],mouth:'flat',brow:1,lookY:1,blink:blinkAt(2),front:''})}</g><rect x="182" y="170" width="76" height="50" rx="6" fill="${C.white}" stroke="${O}" stroke-width="2.5"/><line x1="220" y1="170" x2="220" y2="220" stroke="${O}" stroke-width="2"/>`;
  a+=txt(170,345,'Lernen',30,O,900);
  s+=card(70,140,340,380,pop(lt,t[0]),a)+cross(390,160,pop(lt,t[0]+0.5));
  // Medikamente
  let b=`<rect x="100" y="40" width="140" height="240" rx="18" fill="${C.white}" stroke="${O}" stroke-width="3"/><line x1="100" y1="120" x2="240" y2="120" stroke="${O}" stroke-width="3"/><rect x="220" y="60" width="8" height="40" rx="4" fill="${O}"/>`;
  for(let i=0;i<4;i++) b+=`<rect x="${118+i*28}" y="150" width="18" height="44" rx="5" fill="${['#9FC3C0','#E8B795','#9FC3C0','#E8B795'][i]}" stroke="${O}" stroke-width="2"/>`;
  b+=`<path d="M150 70 l12 -8 l12 8 v18 l-12 8 l-12 -8z" fill="none" stroke="#9FC3C0" stroke-width="3"/>`+txt(170,345,'Medizin kühlen',30,O,900);
  s+=card(470,140,340,380,pop(lt,t[1]),b)+cross(790,160,pop(lt,t[1]+0.5));
  // Betriebe
  let d=`<rect x="60" y="110" width="220" height="170" fill="${C.peach}" stroke="${O}" stroke-width="3"/><path d="M40 110 h260 l-20 -50 h-220z" fill="${C.red}" stroke="${O}" stroke-width="3" stroke-linejoin="round"/>`;
  for(let i=0;i<5;i++) d+=`<path d="M${40+i*52} 110 q26 30 52 0" fill="${i%2?C.white:C.red}" stroke="${O}" stroke-width="2.5"/>`;
  d+=`<rect x="90" y="150" width="90" height="70" rx="8" fill="#5a6070" stroke="${O}" stroke-width="2.5"/><rect x="200" y="160" width="60" height="120" rx="6" fill="${C.teal}" stroke="${O}" stroke-width="2.5"/>`;
  d+=`<g transform="translate(135,186) rotate(-6)"><rect x="-60" y="-16" width="120" height="32" rx="6" fill="${C.white}" stroke="${O}" stroke-width="2"/>${txt(0,7,'GESCHLOSSEN',14,C.red,900)}</g>`+txt(170,345,'Kleine Betriebe',30,O,900);
  s+=card(870,140,340,380,pop(lt,t[2]),d)+cross(1190,160,pop(lt,t[2]+0.5));
  return s;
};

// Küche in einer Hütte – rauchig (clean=0) oder sauberer Kocher (clean=1)
function kitchen(lt,smoke,clean,happy){
  let s=`<rect width="1280" height="720" fill="#D7BE97"/>`;
  for(let x=0;x<1280;x+=90) s+=`<line x1="${x}" y1="0" x2="${x}" y2="560" stroke="#C9AE86" stroke-width="3"/>`;
  s+=`<rect x="0" y="560" width="1280" height="160" fill="#A88660" stroke="${O}" stroke-width="3"/>`;
  s+=`<rect x="140" y="140" width="170" height="140" rx="14" fill="#BFD7D4" stroke="${O}" stroke-width="3"/><line x1="225" y1="140" x2="225" y2="280" stroke="${O}" stroke-width="3"/>`;
  s+=`<rect x="1000" y="250" width="200" height="16" rx="6" fill="${C.woodD}" stroke="${O}" stroke-width="3"/>`;
  for(let i=0;i<3;i++) s+=`<ellipse cx="${1040+i*60}" cy="236" rx="22" ry="16" fill="${['#C8553D','#E8B795','#8FAE8B'][i]}" stroke="${O}" stroke-width="2.5"/>`;
  // offenes Feuer
  if(clean<1){
    let fire=`<ellipse cx="700" cy="580" rx="110" ry="18" fill="#7a6045"/>`;
    for(const [x,r] of [[620,26],[700,30],[780,26]]) fire+=`<ellipse cx="${x}" cy="566" rx="${r}" ry="18" fill="${C.grey}" stroke="${O}" stroke-width="3"/>`;
    fire+=`<path d="M640 576 l120 -20 M650 556 l110 22" stroke="#6B4038" stroke-width="12" stroke-linecap="round"/>`+flame(700,560,1.1);
    fire+=`<path d="M630 470 h140 q0 70 -70 70 q-70 0 -70 -70z" fill="#2E2E36" stroke="${O}" stroke-width="3"/><rect x="620" y="460" width="160" height="14" rx="7" fill="#2E2E36" stroke="${O}" stroke-width="3"/>`;
    s+=G(fire,'',1-clean);
  }
  if(clean>0){
    let st=`<rect x="610" y="480" width="180" height="90" rx="16" fill="${C.teal}" stroke="${O}" stroke-width="3"/><rect x="640" y="500" width="120" height="16" rx="8" fill="${C.navy}"/>`;
    st+=`<g transform="translate(700,480)">${[-30,-10,10,30].map(x=>`<path d="M${x} 0 q-6 -14 0 -26 q6 12 0 26z" fill="#5AA9E6" stroke="${O}" stroke-width="1.5"/>`).join('')}</g>`;
    st+=`<path d="M630 400 h140 q0 70 -70 70 q-70 0 -70 -70z" fill="#C0C4CC" stroke="${O}" stroke-width="3"/><rect x="620" y="390" width="160" height="14" rx="7" fill="#C0C4CC" stroke="${O}" stroke-width="3"/>`;
    st+=smokePuffs(700,390,4,20,0.6,'#ffffff',3,60);
    s+=G(st,'',clean);
  }
  s+=P4({x:470,y:640,s:0.95,style:'scarf',hair:C.red,dress:'#B85C4A',armR:[60,-50+Math.sin(NOW*3)*10],armL:[14,-8],look:1.4,mouth:happy?'big':'flat',brow:happy?0:1,blink:blinkAt(3)});
  const cough=smoke>0.4&&!happy&&Math.sin(NOW*5)>0;
  s+=P1({x:960,y:640,s:0.62,shirt:C.mustard,skin:C.skin3,hair:'#2a2420',mouth:happy?'big':(cough?'o':'sad'),brow:happy?0:1,look:-1.4,blink:blinkAt(4),
    armL:cough?[40,-120]:[12,-8]});
  if(cough) s+=txt(860,370,'hust!',26,O,900,'middle','opacity=".7"');
  if(smoke>0) s+=smokePuffs(700,470,12,90,smoke,'#6E7280',1,300)+`<rect width="1280" height="560" fill="#5D6170" opacity="${f(smoke*35)/100}"/>`;
  return s;
}
S.cooking=(lt,D,c,L)=>{
  const sm=0.35+0.55*P(lt,c[1],c[1]+2);
  let s=kitchen(lt,sm,0,false);
  s+=G(pill(230,80,'≈ 2 Milliarden Menschen',C.white,O,26),scaleAt(230,80,pop(lt,at(c,L,0,0.45))),1);
  const ic=[['Holz','#6B4038'],['Kohle','#3A3A42'],['Dung','#8A6A45']];
  ic.forEach(([n,col],i)=>{const k=pop(lt,at(c,L,0,0.78+i*0.07)); s+=popAt(860+i*130,80,k,`<circle r="42" fill="${col}" stroke="${O}" stroke-width="3"/>`+txt(0,8,n,20,'#fff',900));});
  s+=G(pill(1000,180,'Rauch macht krank',C.red,'#fff',26),'',P(lt,at(c,L,1,0.3),at(c,L,1,0.5)));
  return s;
};

S.home=(lt,D,c,L)=>{
  let s=stripes()+ground(590,C.sage);
  // Haus im Schnitt
  s+=`<path d="M130 210 L570 50 L1010 210z" fill="${C.red}" stroke="${O}" stroke-width="3" stroke-linejoin="round"/>`;
  s+=`<rect x="150" y="210" width="840" height="380" fill="#F4EDDF" stroke="${O}" stroke-width="3"/><line x1="150" y1="400" x2="990" y2="400" stroke="${O}" stroke-width="6"/><line x1="570" y1="210" x2="570" y2="400" stroke="${O}" stroke-width="3"/><line x1="570" y1="400" x2="570" y2="590" stroke="${O}" stroke-width="3"/>`;
  const t=[0.18,0.32,0.46,0.58,0.82].map(fr=>at(c,L,0,fr));
  const on=i=>P(lt,t[i],t[i]+0.4);
  // Kühlschrank
  s+=`<rect x="200" y="420" width="100" height="170" rx="14" fill="${C.white}" stroke="${O}" stroke-width="3"/><line x1="200" y1="480" x2="300" y2="480" stroke="${O}" stroke-width="3"/><rect x="282" y="440" width="6" height="26" rx="3" fill="${O}"/><rect x="282" y="500" width="6" height="40" rx="3" fill="${O}"/>`;
  // Heizung
  s+=`<rect x="680" y="500" width="200" height="80" rx="12" fill="${C.white}" stroke="${O}" stroke-width="3"/>`;
  for(let i=1;i<5;i++) s+=`<line x1="${680+i*40}" y1="508" x2="${680+i*40}" y2="572" stroke="${O}" stroke-width="3"/>`;
  if(on(1)>0) for(let i=0;i<3;i++){const a=(NOW*0.7+i/3)%1; s+=`<path d="M${720+i*60} ${f(490-a*60)} q10 -12 0 -24 q-10 -12 0 -24" stroke="${C.red}" stroke-width="4" fill="none" stroke-linecap="round" opacity="${f((1-a)*on(1)*70)/100}"/>`;}
  // Lampe
  s+=`<line x1="360" y1="210" x2="360" y2="260" stroke="${O}" stroke-width="3"/><path d="M320 290 Q320 260 360 260 Q400 260 400 290z" fill="${C.teal}" stroke="${O}" stroke-width="3"/><circle cx="360" cy="294" r="11" fill="${on(2)>0.5?'#FBE7A1':'#CFC7B5'}" stroke="${O}" stroke-width="2"/>`;
  if(on(2)>0) s+=`<path d="M330 296 L250 400 H470 L390 296z" fill="${C.lamp}" opacity="${f(on(2)*25)/100}"/>`;
  // Handy auf dem Tisch
  s+=`<rect x="700" y="340" width="200" height="14" rx="6" fill="${C.woodD}" stroke="${O}" stroke-width="3"/><rect x="715" y="354" width="12" height="46" fill="${C.woodD}"/><rect x="873" y="354" width="12" height="46" fill="${C.woodD}"/>`;
  s+=phone(800,312,0.32,on(3)>0.5?'#9FC3C0':'#3E5368');
  // Figur
  s+=P2({x:460,y:590,s:0.62,look:Math.sin(lt*0.8)*1.5,blink:blinkAt(2),mouth:'smile'});
  // Fabrik
  s+=G(factory(1150,590,0.5,on(4)>0.5,0,on(4)),'',P(lt,t[4]-0.3,t[4]+0.2));
  const lab=[[250,410,'Kühlschrank'],[780,490,'Heizung'],[360,200,'Licht'],[800,240,'Handy'],[1150,420,'Fabrik']];
  lab.forEach(([x,y,n],i)=>{s+=G(pill(x,y-40,n,C.yellow,O,20),scaleAt(x,y-40,pop(lt,t[i])),1);});
  for(let i=0;i<4;i++) if(on(i)>0) s+=bolt([300,880,400,850][i],[430,500,290,310][i],0.35*pop(lt,t[i]));
  return s;
};

S.mobility=(lt,D,c,L)=>{
  let s=`<rect width="1280" height="720" fill="${C.sky}"/>`+cloud(300+lt*8,120)+cloud(900+lt*5,90,0.8);
  s+=`<rect x="0" y="360" width="1280" height="90" fill="${C.water}" stroke="${O}" stroke-width="3"/>`;
  for(let i=0;i<8;i++) s+=`<path d="M${f(((i*170+NOW*20)%1360)-40)} 400 q15 -8 30 0" stroke="#fff" stroke-width="3" fill="none" opacity=".6"/>`;
  s+=`<rect x="0" y="450" width="1280" height="70" fill="${C.sage}" stroke="${O}" stroke-width="3"/><rect x="0" y="520" width="1280" height="200" fill="#7C808A" stroke="${O}" stroke-width="3"/>`;
  for(let x=0;x<1300;x+=120) s+=`<rect x="${f(x-((lt*120)%120))}" y="584" width="60" height="8" rx="4" fill="${C.white}"/>`;
  const t=[0.08,0.2,0.32,0.45].map(fr=>at(c,L,0,fr));
  if(lt>t[3]-0.3) s+=G(ship(lerp(1150,850,P(lt,t[3],D)),380,0.7),'',P(lt,t[3]-0.3,t[3]+0.2));
  if(lt>t[2]-0.3) s+=G(plane(lerp(-200,1500,P(lt,t[2]-0.3,t[2]+5)),200-30*P(lt,t[2],t[2]+5),0.9),'',1);
  if(lt>t[1]-0.5) s+=bus(lerp(1500,-300,P(lt,t[1]-0.5,t[1]+5)),560,0.85);
  if(lt>t[0]-0.5) s+=car(lerp(-200,1500,P(lt,t[0]-0.5,t[0]+4)),640,C.red,0.9);
  // Zapfsäule
  const k=pop(lt,at(c,L,0,0.62));
  s+=popAt(1150,250,k,`<rect x="-40" y="-70" width="80" height="140" rx="12" fill="${C.red}" stroke="${O}" stroke-width="3"/><rect x="-26" y="-56" width="52" height="34" rx="6" fill="${C.white}" stroke="${O}" stroke-width="2"/><path d="M40 -30 q30 0 30 30 v50" fill="none" stroke="${O}" stroke-width="6"/>`);
  s+=G(pill(640,80,'Benzin · Diesel · Kerosin',C.white,O,28),'',P(lt,at(c,L,0,0.65),at(c,L,0,0.75)));
  return s;
};

S.tile=(lt,D)=>{
  let s=stripes();
  s+=G(sdgTile(lt,0.3),`translate(640,300) scale(${pop(lt,0.3).toFixed(4)}) translate(-150,-150)`);
  s+=G(txt(640,530,'Bezahlbare und saubere Energie',38,O,900),'',P(lt,2.2,2.8));
  const a=P(lt,2.5,3.3);
  s+=G(P1({x:200,y:700,s:0.8,armR:[150,wave()-20],mouth:'big',look:1,blink:blinkAt(2)})+
       P2({x:1080,y:700,s:0.8,shirt:C.mustard,armL:[150,-wave(1)-20],mouth:'big',look:-1,blink:blinkAt(3)}),`translate(0,${f((1-eout(a))*300)})`,a);
  return s;
};

S.ring=(lt,D,c,L)=>{
  let s=stripes();
  s+=G(sdgTile(lt,-5),`translate(170,170) scale(0.55) translate(-150,-150)`);
  const gx=740, gy=350, R1=150, R2=205;
  let g=globe(gx,gy,130,'gr');
  const hi=P(lt,c[1],c[1]+0.8);
  for(let i=0;i<17;i++){
    const ap=pop(lt,0.4+i*0.16,0.4); if(ap<=0) continue;
    const a0=rad(-90+i*360/17+1.2), a1=rad(-90+(i+1)*360/17-1.2);
    const ext=(i===6?hi*26:0), rr1=R1+ext, rr2=R2*ap+ext;
    const pth=`M${f(gx+Math.cos(a0)*rr1)} ${f(gy+Math.sin(a0)*rr1)} L${f(gx+Math.cos(a0)*rr2)} ${f(gy+Math.sin(a0)*rr2)} A${f(rr2)} ${f(rr2)} 0 0 1 ${f(gx+Math.cos(a1)*rr2)} ${f(gy+Math.sin(a1)*rr2)} L${f(gx+Math.cos(a1)*rr1)} ${f(gy+Math.sin(a1)*rr1)} A${rr1} ${rr1} 0 0 0 ${f(gx+Math.cos(a0)*rr1)} ${f(gy+Math.sin(a0)*rr1)}Z`;
    const am=(a0+a1)/2;
    g+=`<path d="${pth}" fill="${SDG[i]}" stroke="${O}" stroke-width="2.5" opacity="${i===6?1:f((1-hi*0.45)*100)/100}"/>`;
    if(ap>0.8) g+=txt(gx+Math.cos(am)*(R1+28+ext),gy+Math.sin(am)*(R1+28+ext)+8,i+1,22,'#fff',900);
  }
  if(hi>0){const am=rad(-90+6.5*360/17), lx=gx+Math.cos(am)*250, ly=gy+Math.sin(am)*250;
    g+=G(`<line x1="${f(gx+Math.cos(am)*236)}" y1="${f(gy+Math.sin(am)*236)}" x2="${f(lx+70)}" y2="${f(ly-24)}" stroke="${O}" stroke-width="3"/><rect x="${f(lx+70)}" y="${f(ly-50)}" width="150" height="52" rx="26" fill="${C.yellow}" stroke="${O}" stroke-width="3"/>`+txt(lx+145,ly-14,'Ziel 7',28,O,900),'',hi);}
  s+=g;
  s+=G(txt(1110,90,'17 Ziele',40,O,900)+txt(1110,124,'Vereinte Nationen',22,O,700)+txt(1110,152,'seit 2015',22,O,700),'',P(lt,0.8,1.4));
  s+=G(pill(1110,210,'193 Staaten',C.teal,'#fff',24),scaleAt(1110,210,pop(lt,at(c,L,0,0.7))),1);
  s+=G(pill(1110,280,'bis 2030',C.mustard,O,24),scaleAt(1110,280,pop(lt,c[1]+0.2)),1);
  return s;
};

S.links=(lt,D,c,L)=>{
  let s=stripes();
  const t=[at(c,L,0,0.42),at(c,L,0,0.6),at(c,L,0,0.82)];
  const pts=[[250,330],[640,120],[1030,330]];
  pts.forEach(([x,y],i)=>{const pr=eio(P(lt,t[i]-0.3,t[i]+0.3)); if(pr>0) s+=`<line x1="640" y1="360" x2="${f(lerp(640,x,pr))}" y2="${f(lerp(360,y,pr))}" stroke="${O}" stroke-width="5" stroke-dasharray="12 10" stroke-linecap="round"/>`;});
  s+=G(sdgTile(lt,-5,180),`translate(640,370) scale(${pop(lt,0.2).toFixed(3)}) translate(-90,-90)`);
  const book=`<g transform="translate(30,68) scale(0.8)"><path d="M-30 0 q15 -8 30 0 q15 -8 30 0 v-30 q-15 -8 -30 0 q-15 -8 -30 0z" fill="none" stroke="#fff" stroke-width="4"/></g>`;
  const heart=`<path transform="translate(10,-6)" d="M20 70 c-20 -14 -30 -26 -18 -38 c8 -8 18 -2 18 4 c0 -6 10 -12 18 -4 c12 12 2 24 -18 38z" fill="none" stroke="#fff" stroke-width="4"/>`;
  const fam=personIcon(4,70,0.5,'#fff')+personIcon(24,70,0.5,'#fff')+personIcon(42,73,0.38,'#fff');
  s+=miniTile(250,330,'4','HOCHWERTIGE','BILDUNG',SDG[3],pop(lt,t[0]),book);
  s+=miniTile(640,120,'3','GESUNDHEIT UND','WOHLERGEHEN',SDG[2],pop(lt,t[1]),heart);
  s+=miniTile(1030,330,'1','KEINE','ARMUT',SDG[0],pop(lt,t[2]),fam);
  s+=G(txt(640,560,'Alles hängt zusammen',36,O,900),'',P(lt,t[2]+0.6,t[2]+1.1));
  return s;
};

S.promise=(lt,D,c,L)=>{
  let s=stripes();
  s+=card(140,110,1000,430,pop(lt,0.2),'',C.white);
  s+=G(txt(640,190,'SDG 7 verspricht:',44,O,900),'',P(lt,0.5,0.9));
  s+=G(pill(640,250,'bis 2030 · für jeden Menschen',C.yellow,O,26),scaleAt(640,250,pop(lt,at(c,L,0,0.25))),1);
  const items=[['bezahlbar',euro(0,0,0.9)],['verlässlich',plug(0,0,0.9)+check(30,-30,1,16)],['nachhaltig',leaf(0,34,1)],['modern',bolt(0,0,1)]];
  items.forEach(([n,ic],i)=>{const k=pop(lt,at(c,L,0,0.55+i*0.1)); const x=280+i*240;
    s+=popAt(x,380,k,`<circle r="62" fill="#F3EDE1" stroke="${O}" stroke-width="3"/>${ic}`)+G(txt(x,486,n,28,O,900),'',k>0.5?1:0);});
  return s;
};

S.meaning=(lt,D,c,L)=>{
  let s=stripes();
  const rows=[['Bezahlbar',C.yellow],['Verlässlich',C.teal],['Nachhaltig',C.green]];
  rows.forEach(([n,col],i)=>{
    const k=pop(lt,c[i]), y=130+i*160, act=lt>=c[i]&&(i===2||lt<c[i+1]);
    if(k<=0) return;
    let r=`<rect x="-500" y="-62" width="1000" height="124" rx="30" fill="${act?C.white:'#F5F0E5'}" stroke="${act?col:O}" stroke-width="${act?6:3}"/>`;
    r+=`<circle cx="-420" cy="0" r="44" fill="${col}" stroke="${O}" stroke-width="3"/>`+txt(-420,12,i+1,34,'#fff',900)+txt(-350,14,n,40,O,900,'start');
    if(i===0){ // Waage Strom vs. Essen
      const tilt=Math.sin(NOW*1.5)*4*(1-P(lt,c[0]+2,c[0]+3));
      r+=`<g transform="translate(250,10)"><path d="M0 40 L-16 54 H16z" fill="${C.grey}" stroke="${O}" stroke-width="3"/><line x1="0" y1="-10" x2="0" y2="40" stroke="${O}" stroke-width="4"/><g transform="rotate(${f(tilt)})"><line x1="-130" y1="-10" x2="130" y2="-10" stroke="${O}" stroke-width="5" stroke-linecap="round"/>`+
        `<g transform="translate(-130,-10)"><path d="M-40 0 h80 q-10 24 -40 24 q-30 0 -40 -24z" fill="${C.white}" stroke="${O}" stroke-width="3"/>${bulb(0,-40,0.4,1)}</g>`+
        `<g transform="translate(130,-10)"><path d="M-40 0 h80 q-10 24 -40 24 q-30 0 -40 -24z" fill="${C.white}" stroke="${O}" stroke-width="3"/><ellipse cx="0" cy="-14" rx="30" ry="12" fill="${C.peach}" stroke="${O}" stroke-width="2.5"/><circle cx="-8" cy="-22" r="8" fill="${C.green}" stroke="${O}" stroke-width="2"/></g></g></g>`;
      r+=check(440,0,pop(lt,c[0]+2.2),24);
    }
    if(i===1){ // Uhr + Licht immer an
      const ang=NOW*120;
      r+=`<g transform="translate(180,0)"><circle r="44" fill="${C.white}" stroke="${O}" stroke-width="3"/><line x1="0" y1="0" x2="0" y2="-30" stroke="${O}" stroke-width="4" stroke-linecap="round" transform="rotate(${f(ang)})"/><line x1="0" y1="0" x2="20" y2="0" stroke="${O}" stroke-width="4" stroke-linecap="round" transform="rotate(${f(ang/12)})"/></g>`+txt(260,12,'24/7',30,O,900,'start')+bulb(420,-10,0.55,1);
    }
    if(i===2){ r+=globe(260,0,48,'gm',0)+leaf(330,30,0.9)+check(440,0,pop(lt,c[2]+1.5),24); }
    s+=popAt(640,y,k,r);
  });
  return s;
};

S.targets=(lt,D,c,L)=>{
  let s=stripes();
  s+=G(txt(640,82,'Die 5 Unterziele von SDG 7',42,O,900),scaleAt(640,70,pop(lt,c[0])),1);
  const rows=[['7.1','Zugang für alle',plug(0,0,0.55),at(c,L,1,0.0)],['7.2','Mehr erneuerbare Energie',turbine(0,40,0.42),at(c,L,1,0.3)],
    ['7.3','Effizienz doppelt so schnell steigern',bulb(0,-6,0.45,1,true),at(c,L,1,0.62)],['7.a','Internationale Zusammenarbeit',globe(0,0,28,'gt',0),at(c,L,2,0.05)],
    ['7.b','Bessere Infrastruktur für ärmere Länder',gear(0,0,20,NOW*60),at(c,L,2,0.55)]];
  rows.forEach(([b,n,ic,t0],i)=>{
    const k=pop(lt,t0); if(k<=0) return;
    const y=150+i*88;
    s+=G(`<rect x="210" y="${y-36}" width="860" height="72" rx="36" fill="${C.white}" stroke="${O}" stroke-width="3"/><rect x="210" y="${y-36}" width="120" height="72" rx="36" fill="${C.yellow}" stroke="${O}" stroke-width="3"/>`+
      txt(270,y+11,b,30,O,900)+`<g transform="translate(380,${y})">${ic}</g>`+txt(430,y+10,n,28,O,800,'start'),`translate(${f((1-eout(P(lt,t0,t0+0.5)))*-60)},0)`,P(lt,t0,t0+0.3));
  });
  return s;
};

S.progress=(lt,D,c,L)=>{
  let s=stripes();
  s+=G(txt(640,80,'Zugang zu Strom weltweit',40,O,900),'',P(lt,0.2,0.6));
  const X=y=>200+(y-2010)/20*820, Y=v=>520-(v-70)/30*360;
  s+=`<line x1="190" y1="520" x2="1040" y2="520" stroke="${O}" stroke-width="4" stroke-linecap="round"/><line x1="200" y1="530" x2="200" y2="150" stroke="${O}" stroke-width="4" stroke-linecap="round"/>`;
  for(const yr of [2010,2015,2020,2025,2030]) s+=txt(X(yr),556,yr,22,O,800);
  for(const v of [70,80,90,100]) s+=txt(186,Y(v)+8,v+' %',20,O,800,'end')+`<line x1="200" y1="${Y(v)}" x2="1030" y2="${Y(v)}" stroke="#D8D0BE" stroke-width="2"/>`;
  // Ziel-Linie
  s+=G(`<line x1="${X(2010)}" y1="${Y(100)}" x2="${X(2030)}" y2="${Y(100)}" stroke="${C.green}" stroke-width="5" stroke-dasharray="14 10"/>`+pill(X(2030)-10,Y(100)-34,'Ziel 2030: 100 %',C.green,'#fff',20),'',P(lt,0.5,1));
  const data=[[2010,84],[2012,85],[2014,86.5],[2016,88],[2018,89.5],[2020,90.5],[2022,91]];
  const pr=eio(P(lt,c[0]+0.3,c[0]+3.5));
  const pts=[]; for(let i=0;i<data.length;i++){const u=i/(data.length-1); if(u>pr) break; pts.push([X(data[i][0]),Y(data[i][1])]);}
  if(pts.length>1) s+=`<polyline points="${pts.map(p=>p.join(',')).join(' ')}" fill="none" stroke="${C.teal}" stroke-width="10" stroke-linecap="round" stroke-linejoin="round"/>`;
  if(pr>0.95){
    s+=G(personIcon(X(2016),Y(88)-40,0.9,C.yellow)+personIcon(X(2019),Y(90)-40,0.9,C.yellow)+bulb(X(2013)-10,Y(86)-70,0.4,1),'',P(lt,c[0]+3.5,c[0]+4));
  }
  const pr2=eio(P(lt,c[1],c[1]+2));
  if(pr2>0){const x2=lerp(X(2022),X(2030),pr2), y2=lerp(Y(91),Y(92),pr2);
    s+=`<line x1="${X(2022)}" y1="${Y(91)}" x2="${f(x2)}" y2="${f(y2)}" stroke="${C.red}" stroke-width="8" stroke-dasharray="12 10" stroke-linecap="round"/>`;}
  if(pr2>=1) s+=G(`<path d="M${X(2030)+30} ${Y(100)} h14 v${Y(92)-Y(100)} h-14" fill="none" stroke="${C.red}" stroke-width="5"/>`+pill(X(2030)+40,Y(96),'Lücke!',C.red,'#fff',22),'',P(lt,c[1]+2,c[1]+2.5));
  return s;
};

S.hand1=(lt,D,c,L)=>{
  let s=stripes();
  s+=P1({x:640,y:640,s:1,armL:[100,-60],armR:[100,-60],mouth:'flat',brow:1,lookY:-1,blink:blinkAt(1)});
  for(let i=0;i<3;i++){const k=pop(lt,0.3+i*0.4),bob=Math.sin(NOW*3+i)*6; s+=popAt([460,820,640][i],[260+bob,250+bob,150+bob][i],k,txt(0,0,'?',70,[C.yellow,C.teal,C.red][i],900,'middle',`stroke="${O}" stroke-width="2"`));}
  s+=G(pill(1060,120,'Weiter mit Teil 2 →',C.teal,'#fff',26),`translate(${f((1-eout(P(lt,at(c,L,0,0.55),at(c,L,0,0.85))))*300)},0)`,P(lt,at(c,L,0,0.55),at(c,L,0,0.85)));
  return s;
};
