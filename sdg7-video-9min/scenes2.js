// ======================================================================
//  Szenen Teil 2 – Das Problem & die Lösungen
// ======================================================================
function coalPlant(x=0){
  let s=`<g transform="translate(${x},0)"><path d="M60 560 Q90 450 70 360 H210 Q190 450 220 560z" fill="#B8B0A0" stroke="${O}" stroke-width="3"/>`;
  s+=`<rect x="230" y="420" width="220" height="140" rx="14" fill="#9E9686" stroke="${O}" stroke-width="3"/>`;
  for(let i=0;i<4;i++) s+=`<rect x="${250+i*50}" y="450" width="30" height="40" rx="8" fill="${C.lamp}" stroke="${O}" stroke-width="2"/>`;
  s+=`<rect x="300" y="240" width="40" height="180" fill="${C.red}" stroke="${O}" stroke-width="3"/><rect x="300" y="280" width="40" height="22" fill="${C.white}"/><rect x="300" y="340" width="40" height="22" fill="${C.white}"/>`;
  s+=`<rect x="380" y="280" width="36" height="140" fill="${C.red}" stroke="${O}" stroke-width="3"/><rect x="380" y="320" width="36" height="20" fill="${C.white}"/></g>`;
  return s;
}
S.coal=(lt,D,c,L)=>{
  let s=`<rect width="1280" height="720" fill="${C.cream}"/>`+ground(560);
  s+=coalPlant();
  const ex=960, ey=320;
  s+=globe(ex,ey,120,'gc',0.3*P(lt,0,D));
  for(let i=0;i<40;i++){
    const age=lt-0.3-i*0.3; if(age<0) continue;
    const src=i%2?[320,240]:[398,280];
    const fly=clamp(age/3.2), ang=rad(-200+(i*47)%360+age*20), orR=165+rnd(i)*35;
    const tgx=ex+Math.cos(ang)*orR, tgy=ey+Math.sin(ang)*orR;
    const mx=lerp(src[0],ex-100,0.5), my=Math.min(src[1],tgy)-140, u=eio(fly);
    const x=(1-u)*(1-u)*src[0]+2*(1-u)*u*mx+u*u*tgx, y=(1-u)*(1-u)*src[1]+2*(1-u)*u*my+u*u*tgy;
    s+=`<circle cx="${f(x)}" cy="${f(y)}" r="${f(lerp(12,20+rnd(i+3)*8,clamp(age/2)))}" fill="${['#5D6170','#6E7280','#4F5361'][i%3]}" opacity="${f(0.85*clamp(age*3)*100)/100}" stroke="${O}" stroke-width="2"/>`;
  }
  s+=G(pill(330,180,'Kohle · Öl · Gas',C.white,O,26),scaleAt(330,180,pop(lt,at(c,L,0,0.6))),1);
  const k=pop(lt,c[1]);
  s+=popAt(150,140,k,donut(0,0,56,0.8*P(lt,c[1],c[1]+1),'#4F5361','#E4DCC9',30)+txt(0,10,'≈ 80 %',22,O,900));
  s+=G(txt(150,240,'fossil',24,O,900),'',P(lt,c[1]+0.5,c[1]+1));
  return s;
};

S.greenhouse=(lt,D,c,L)=>{
  const warm=P(lt,c[1],c[1]+3);
  let s=`<rect width="1280" height="720" fill="${C.cream}"/><rect width="1280" height="720" fill="#C8553D" opacity="${f(warm*12)/100}"/>`;
  const gx=700, gy=330;
  // Sonnenstrahlen kommen rein, Wärme bleibt gefangen
  for(let i=0;i<3;i++){const a=(NOW*0.5+i/3)%1; s+=arrowPath(gx-260+i*40, -20, gx-110+i*40, gy-120, C.mustard, 5, clamp(a*2));}
  s+=globe(gx,gy,140,'gg',0.2+warm*0.6);
  const haze=P(lt,0,1.5);
  for(let i=0;i<26;i++){const a=rad(i*360/26+NOW*6); const r=200+Math.sin(i*1.7)*12;
    s+=`<circle cx="${f(gx+Math.cos(a)*r)}" cy="${f(gy+Math.sin(a)*r)}" r="${f(26+rnd(i)*10)}" fill="#6E7280" opacity="${f(haze*0.75*100)/100}" stroke="${O}" stroke-width="2"/>`;}
  for(let i=0;i<4;i++){const a=rad(i*90+NOW*30); s+=`<path d="M${f(gx+Math.cos(a)*150)} ${f(gy+Math.sin(a)*150)} q10 -10 20 0 t20 0" transform="rotate(${f(i*90+NOW*30+90)} ${f(gx+Math.cos(a)*165)} ${f(gy+Math.sin(a)*165)})" stroke="${C.red}" stroke-width="5" fill="none" stroke-linecap="round" opacity="${f(haze*100)/100}"/>`;}
  s+=G(pill(gx,40,'CO₂ = Decke um die Erde',C.white,O,26),'',P(lt,at(c,L,0,0.35),at(c,L,0,0.5)));
  s+=G(pill(1040,60,'Treibhauseffekt',C.white,O,26),'',P(lt,at(c,L,0,0.75),at(c,L,0,0.9)));
  s+=G(thermo(1120,300,warm),'',P(lt,c[1]-0.3,c[1]));
  const ic=[
    ['Hitze',`<circle r="40" fill="${C.red}" stroke="${O}" stroke-width="3"/>`+[0,1,2,3,4,5,6,7].map(i=>{const a=rad(i*45);return `<line x1="${f(Math.cos(a)*48)}" y1="${f(Math.sin(a)*48)}" x2="${f(Math.cos(a)*62)}" y2="${f(Math.sin(a)*62)}" stroke="${C.red}" stroke-width="7" stroke-linecap="round"/>`}).join('')],
    ['Dürre',`<rect x="-60" y="-30" width="120" height="60" rx="12" fill="#D9B98F" stroke="${O}" stroke-width="3"/><path d="M-40 -30 l14 24 l-10 14 l18 22 M10 -30 l-6 20 l18 12 l-6 28 M40 -30 l-10 18 l14 16" stroke="${O}" stroke-width="3" fill="none"/>`],
    ['Unwetter',cloud(0,-10,0.9,'#7C808A')+bolt(0,40,0.6)]];
  ic.forEach(([n,g],i)=>{const k=pop(lt,at(c,L,1,0.35+i*0.17)); const y=150+i*150; s+=popAt(180,y,k,g)+G(txt(180,y+90,n,24,O,900),'',k>0.5?1:0);});
  return s;
};

S.share=(lt,D,c,L)=>{
  let s=stripes();
  s+=G(txt(640,82,'Woher kommen die Treibhausgase?',40,O,900),'',P(lt,0.1,0.5));
  const pr=eio(P(lt,c[0]+0.3,c[0]+2));
  s+=donut(380,350,150,0.75*pr,C.red,'#D8D0BE',70)+txt(380,345,'≈ 3/4',56,O,900)+txt(380,385,'Energie',26,O,800);
  const it=[['Strom & Wärme',bolt(0,0,0.7)],['Verkehr',car(0,20,C.teal,0.45)],['Industrie',gear(0,0,24,NOW*50)],['Gebäude',house(0,40,70,50,C.peach,0)]];
  it.forEach(([n,ic],i)=>{const k=pop(lt,c[0]+1.4+i*0.5); const y=180+i*110;
    s+=G(`<rect x="700" y="${y-42}" width="420" height="84" rx="42" fill="${C.white}" stroke="${O}" stroke-width="3"/><g transform="translate(760,${y})">${ic}</g>`+txt(830,y+10,n,30,O,900,'start'),`translate(${f((1-eout(k))*80)},0)`,k);});
  return s;
};

S.price=(lt,D,c,L)=>{
  let s=stripes();
  const X0=760, X1=1200;
  const g=eio(P(lt,0.5,c[1]));
  const spike=P(lt,c[2],c[2]+0.8);
  let pts=[];for(let i=0;i<=20;i++){const u=i/20; if(u>g) break; pts.push([X0+u*(X1-X0)*0.85, 520-Math.pow(u,1.8)*220-Math.sin(u*14)*12]);}
  if(spike>0&&pts.length){const L0=pts[pts.length-1]; pts.push([L0[0]+20*spike,L0[1]-150*spike]); pts.push([L0[0]+50*spike,L0[1]-110*spike]);}
  s+=`<line x1="${X0-10}" y1="540" x2="${X1}" y2="540" stroke="${O}" stroke-width="4" stroke-linecap="round"/><line x1="${X0}" y1="550" x2="${X0}" y2="160" stroke="${O}" stroke-width="4" stroke-linecap="round"/>`;
  if(pts.length>1){
    s+=`<polyline points="${pts.map(q=>f(q[0])+','+f(q[1])).join(' ')}" fill="none" stroke="${C.red}" stroke-width="10" stroke-linecap="round" stroke-linejoin="round"/>`;
    const A=pts[pts.length-1], B=pts[pts.length-2]; s+=`<path d="M0 -16 L26 0 L0 16z" transform="translate(${f(A[0])},${f(A[1])}) rotate(${f(Math.atan2(A[1]-B[1],A[0]-B[0])*180/Math.PI)})" fill="${C.red}" stroke="${O}" stroke-width="2"/>`;
  }
  const tp=pop(lt,1.0,0.7), sw=Math.sin(lt*4)*8*Math.exp(-Math.max(0,lt-1.5)*0.7);
  s+=`<g transform="translate(960,${f(lerp(-250,230,tp))}) rotate(${f(sw-12)})"><line x1="0" y1="-300" x2="0" y2="-40" stroke="${O}" stroke-width="3"/><path d="M-60 -40 L0 -76 L60 -40 V90 Q60 108 42 108 H-42 Q-60 108 -60 90Z" fill="${C.yellow}" stroke="${O}" stroke-width="3" stroke-linejoin="round"/><circle cx="0" cy="-46" r="9" fill="${C.cream}" stroke="${O}" stroke-width="3"/>${txt(0,44,'€',72,O,900)}</g>`;
  const paper=`<g transform="translate(0,-140)"><rect x="-40" y="-60" width="80" height="100" rx="8" fill="#fff" stroke="${O}" stroke-width="3"/><line x1="-26" y1="-38" x2="26" y2="-38" stroke="${O}" stroke-width="3"/><line x1="-26" y1="-22" x2="14" y2="-22" stroke="${O}" stroke-width="3"/>${txt(0,24,'€€€',24,C.red,900)}</g>`;
  s+=person({x:250,y:640,s:1.05,shirt:C.mustard,armL:[20,-110],armR:[20,-110],extra:paper,mouth:lt>2?'o':'flat',brow:lt>2?1:0,lookY:1,blink:blinkAt(4)});
  // begrenzt
  const kb=pop(lt,at(c,L,1,0.1));
  s+=popAt(500,250,kb,barrel(0,0,0.8)+`<g transform="translate(90,10)"><circle r="34" fill="${C.white}" stroke="${O}" stroke-width="3"/>${flame(0,22,0.55,'#5AA9E6','#BFE3FF')}</g>`);
  s+=popAt(640,250,pop(lt,at(c,L,1,0.25)),hourglass(0,0,0.6,P(lt,at(c,L,1,0.25),D)));
  s+=G(pill(560,140,'begrenzt',C.white,O,24),'',P(lt,at(c,L,1,0.3),at(c,L,1,0.45)));
  s+=popAt(1100,150,pop(lt,c[2]+0.3),`<path d="M0 -50 L48 36 H-48z" fill="${C.yellow}" stroke="${O}" stroke-width="4" stroke-linejoin="round"/>`+txt(0,26,'!',50,O,900));
  s+=G(pill(1100,230,'Krise',C.red,'#fff',24),'',P(lt,c[2]+0.6,c[2]+1));
  return s;
};

S.question=(lt,D)=>{
  let s=stripes();
  s+=G(txt(640,150,'Wie schaffen wir die',52,O,900)+txt(640,215,'globale Energiewende?',60,C.tealD,900),scaleAt(640,180,pop(lt,0.3,0.6)),P(lt,0.2,0.5));
  s+=globe(640,450,110,'gq');
  s+=P1({x:300,y:700,s:0.9,armR:[170,-40],mouth:'flat',brow:1,look:1,lookY:-1,blink:blinkAt(5)});
  s+=P4({x:980,y:700,s:0.9,armL:[30,-120],armR:[30,-120],look:-1,lookY:-1,blink:blinkAt(6)});
  for(let i=0;i<3;i++){const k=pop(lt,1+i*0.5,0.5),bob=Math.sin(NOW*3+i)*6; s+=popAt([420,860,520][i],[330+bob,320+bob,290+bob][i],k,txt(0,0,'?',60,[C.yellow,C.teal,C.red][i],900,'middle',`stroke="${O}" stroke-width="2"`));}
  return s;
};

// Drei Kernziele als Karten. stage = welche Karte gerade neu ist
function goalCard(x,n,ttl,k,active,inner){
  if(k<=0) return '';
  const lift=active?-10:0;
  let s=`<rect x="0" y="0" width="350" height="430" rx="30" fill="#F8F3E8" stroke="${active?C.mustard:O}" stroke-width="${active?6:3}"/><circle cx="44" cy="44" r="28" fill="${C.yellow}" stroke="${O}" stroke-width="3"/>`+txt(44,55,n,30,O,900)+inner+txt(175,400,ttl,28,O,900);
  return `<g transform="translate(${x+175},${150+215+lift}) scale(${k.toFixed(4)}) translate(-175,-215)">${s}</g>`;
}
function card1(t){ // t = Zeit seit Kartenstart (groß = fertig)
  let c1=`<rect x="105" y="80" width="140" height="140" rx="34" fill="${C.white}" stroke="${O}" stroke-width="3"/><circle cx="175" cy="150" r="46" fill="#E9E2D2" stroke="${O}" stroke-width="2"/><circle cx="158" cy="150" r="8" fill="${O}"/><circle cx="192" cy="150" r="8" fill="${O}"/>`;
  const py=lerp(330,182,eio(P(t,0.6,1.4)));
  c1+=`<path d="M175 ${f(py+40)} V340" stroke="${O}" stroke-width="12" stroke-linecap="round"/><rect x="145" y="${f(py)}" width="60" height="50" rx="14" fill="${C.navy}" stroke="${O}" stroke-width="3"/>`;
  c1+=check(250,90,pop(t,1.6),30);
  for(let i=0;i<4;i++){const hx=40+i*72, lit=t>2.4+i*0.5;
    c1+=`<path d="M${hx} 330 v-34 l28 -22 l28 22 v34z" fill="${C.peach}" stroke="${O}" stroke-width="3" stroke-linejoin="round"/><rect x="${hx+18}" y="302" width="20" height="18" rx="4" fill="${lit?C.lamp:'#7B8090'}" stroke="${O}" stroke-width="2"/>`;
    if(lit) c1+=`<circle cx="${hx+28}" cy="311" r="22" fill="${C.lamp}" opacity=".3"/>`;}
  return c1;
}
function card2(t){
  let c2=popAt(100,330,pop(t,0.4),turbine(0,0,1));
  c2+=popAt(240,300,pop(t,1.4),solarPanel(0,0,1));
  c2+=popAt(280,110,pop(t,2.2),`<circle r="26" fill="${C.yellow}" stroke="${O}" stroke-width="3"/>`+[0,1,2,3,4,5,6,7].map(i=>{const a=rad(i*45+NOW*30);return `<line x1="${f(Math.cos(a)*34)}" y1="${f(Math.sin(a)*34)}" x2="${f(Math.cos(a)*46)}" y2="${f(Math.sin(a)*46)}" stroke="${C.mustard}" stroke-width="5" stroke-linecap="round"/>`}).join(''));
  return c2;
}
function card3(t){
  const bk=P(t,0.8,1.4);
  let c3=bulb(105,130,1,bk)+bulb(245,130,1,bk,true);
  const bar=eio(P(t,2.6,4.2));
  c3+=`<line x1="40" y1="350" x2="310" y2="350" stroke="${O}" stroke-width="3"/><rect x="80" y="${f(350-110*bar)}" width="50" height="${f(110*bar)}" rx="8" fill="${C.red}" stroke="${O}" stroke-width="3"/><rect x="220" y="${f(350-15*bar)}" width="50" height="${f(15*bar)}" rx="6" fill="${C.green}" stroke="${O}" stroke-width="3"/>`;
  c3+=G(txt(105,228,'60 W',22,O,900)+txt(245,228,'8 W',22,O,900),'',P(t,4,4.4));
  c3+=G(`<rect x="95" y="20" width="160" height="40" rx="20" fill="${C.green}" stroke="${O}" stroke-width="3"/>`+txt(175,48,'gleich hell!',22,'#fff',900),'',P(t,5,5.4));
  return c3;
}
function goalsScene(lt,stage,t0){
  let s=stripes();
  s+=G(txt(640,96,'Die 3 Kernziele von SDG 7',48,O,900),scaleAt(640,80,stage===1?pop(lt,0.3):1),1);
  const big=99;
  s+=goalCard(60,1,'Zugang für alle',stage===1?pop(lt,t0,0.6):1,stage===1,card1(stage===1?lt-t0:big));
  if(stage>=2) s+=goalCard(465,2,'Erneuerbare Energie',stage===2?pop(lt,t0,0.6):1,stage===2,card2(stage===2?lt-t0:big));
  if(stage>=3) s+=goalCard(870,3,'Energieeffizienz',pop(lt,t0,0.6),true,card3(lt-t0));
  return s;
}
S.goals1=(lt,D,c)=>goalsScene(lt,1,c[1]);
S.goals2=(lt,D,c)=>goalsScene(lt,2,c[0]);
S.goals3=(lt,D,c)=>goalsScene(lt,3,c[0]);

S.offgrid=(lt,D,c,L)=>{
  const dusk=P(lt,c[3],c[3]+1.5);
  let s=`<rect width="1280" height="720" fill="${C.sky}"/><rect width="1280" height="720" fill="#2B3350" opacity="${f(dusk*60)/100}"/>`;
  s+=sun(1120,lerp(110,380,dusk),0.6,false);
  s+=`<path d="M0 470 L180 330 L330 450 L520 300 L720 460 L900 350 L1100 470 L1280 380 V720 H0z" fill="#B8C4B4" stroke="${O}" stroke-width="3" opacity=".8"/>`;
  s+=ground(540,C.sage)+`<rect width="1280" height="720" fill="#2B3350" opacity="${f(dusk*30)/100}"/>`;
  // ferne Stromleitung
  const far=P(lt,c[0]+0.2,c[0]+1)*(1-P(lt,c[1],c[1]+0.6));
  s+=G(`<g opacity=".8">${[0,1,2].map(i=>`<path d="M${60+i*90} 480 l12 -90 l12 90 M${54+i*90} 410 h36" stroke="${O}" stroke-width="4" fill="none"/>`).join('')}<path d="M72 400 q45 14 90 0 t90 0" stroke="${O}" stroke-width="2" fill="none"/></g>`+pill(190,330,'Stromnetz: weit weg',C.white,O,22)+cross(330,330,pop(lt,c[0]+1.2),20),'',far);
  // Hütten
  const H=[[360,560],[560,580],[900,580],[1110,560]];
  const lit=P(lt,c[3]+0.6,c[3]+1.2);
  H.forEach(([x,y],i)=>{s+=hut(x,y,0.9,lit,pop(lt,at(c,L,1,0.08+i*0.07)));});
  // Mini-Grid
  const mg=pop(lt,at(c,L,1,0.55));
  if(mg>0){
    let g=solarPanel(-50,0,0.6)+solarPanel(50,0,0.6)+`<rect x="-30" y="0" width="60" height="44" rx="8" fill="${C.green}" stroke="${O}" stroke-width="3"/>`+bolt(0,22,0.3,'#fff');
    s+=popAt(730,470,mg,g);
    const wp=eio(P(lt,at(c,L,1,0.62),at(c,L,1,0.9)));
    H.forEach(([x,y])=>{const xx=lerp(730,x,wp); s+=`<path d="M730 440 Q${f((730+xx)/2)} ${f(400)} ${f(xx)} ${f(lerp(440,y-110,wp))}" stroke="${O}" stroke-width="3" fill="none"/>`;});
    s+=G(pill(730,330,'Mini-Grid',C.yellow,O,24),'',P(lt,at(c,L,1,0.7),at(c,L,1,0.85)));
  }
  // Kleines Solarpanel – Blase
  const bb=pop(lt,c[2],0.6)*(1-P(lt,c[3]-0.3,c[3]));
  if(bb>0){
    let g=`<rect x="-280" y="-100" width="560" height="200" rx="40" fill="${C.white}" stroke="${O}" stroke-width="4"/>`+solarPanel(-190,40,0.7);
    g+=arrowPath(-110,0,-60,0,O,5,1);
    g+=popAt(-10,-10,pop(lt,at(c,L,2,0.45)),bulb(0,0,0.55,1,true));
    g+=popAt(90,0,pop(lt,at(c,L,2,0.62)),phone(0,0,0.5,'#9FC3C0'));
    g+=popAt(200,0,pop(lt,at(c,L,2,0.8)),`<rect x="-50" y="-34" width="100" height="68" rx="14" fill="${C.red}" stroke="${O}" stroke-width="3"/><circle cx="-18" cy="4" r="18" fill="${C.navy}"/><rect x="10" y="-14" width="28" height="8" rx="4" fill="${C.white}"/><line x1="30" y1="-34" x2="50" y2="-60" stroke="${O}" stroke-width="3"/>`);
    s+=popAt(640,180,bb,g);
  }
  // Abends: lernen + Laden offen
  if(lit>0){
    s+=G(P1({x:470,y:640,s:0.5,skin:C.skin3,hair:'#2a2420',shirt:C.mustard,armL:[20,-110],armR:[20,-110],mouth:'big',lookY:1,blink:blinkAt(2),
      extra:`<g transform="translate(0,-130)"><rect x="-36" y="-26" width="72" height="44" rx="5" fill="#fff" stroke="${O}" stroke-width="2.5"/><line x1="0" y1="-26" x2="0" y2="18" stroke="${O}" stroke-width="2"/></g>`}),'',lit);
    s+=G(`<g transform="translate(1110,430) rotate(-4)"><rect x="-56" y="-18" width="112" height="36" rx="8" fill="${C.green}" stroke="${O}" stroke-width="2.5"/>${txt(0,8,'OFFEN',20,'#fff',900)}</g>`,'',P(lt,at(c,L,3,0.55),at(c,L,3,0.7)));
  }
  return s;
};

S.stove=(lt,D,c,L)=>{
  const cl=eio(P(lt,at(c,L,0,0.3),at(c,L,0,0.5)));
  let s=kitchen(lt,0.7*(1-cl),cl,cl>0.6);
  const ic=[['Gas',`${flame(0,20,0.6,'#5AA9E6','#BFE3FF')}`],['Strom',bolt(0,0,0.55)],['Solar',`<circle r="22" fill="${C.yellow}" stroke="${O}" stroke-width="3"/>`]];
  ic.forEach(([n,g],i)=>{const k=pop(lt,at(c,L,0,0.7+i*0.08)); s+=popAt(870+i*130,90,k,`<circle r="48" fill="${C.white}" stroke="${O}" stroke-width="3"/>${g}`)+G(txt(870+i*130,170,n,22,O,900),'',k>0.5?1:0);});
  s+=check(760,380,pop(lt,at(c,L,0,0.55)),30);
  return s;
};

S.renew=(lt,D,c,L)=>{
  let s=`<rect width="1280" height="720" fill="${C.sky}"/>`+cloud(200+lt*6,100)+cloud(800+lt*4,70,0.8);
  s+=`<path d="M0 470 Q300 380 600 450 T1280 430 V720 H0z" fill="#B8C4B4" stroke="${O}" stroke-width="3"/>`+ground(540,C.sage);
  const t=[at(c,L,0,0.38),at(c,L,0,0.62),at(c,L,0,0.88)];
  // Wasserkraft (Staudamm)
  s+=popAt(160,540,pop(lt,t[0]),`<rect x="-120" y="-90" width="90" height="90" fill="${C.water}" stroke="${O}" stroke-width="3"/><path d="M-30 0 L-30 -120 L20 -120 L60 0z" fill="#B8B0A0" stroke="${O}" stroke-width="3" stroke-linejoin="round"/>`+
    [0,1,2].map(i=>{const a=(NOW*1.2+i/3)%1;return `<path d="M60 ${f(-30+a*30)} q30 ${f(a*10)} 60 ${f(a*30)}" stroke="#5AA9E6" stroke-width="6" fill="none" opacity="${f((1-a)*100)/100}"/>`}).join('')+txt(0,-140,'Wasser',22,O,900));
  // Erdwärme
  s+=popAt(420,540,pop(lt,t[1]),`<rect x="-70" y="-80" width="140" height="80" rx="10" fill="${C.white}" stroke="${O}" stroke-width="3"/><rect x="-50" y="-140" width="30" height="60" fill="#B8B0A0" stroke="${O}" stroke-width="3"/>`+smokePuffs(-35,-150,4,14,0.9,'#ffffff',2,60)+
    `<path d="M-40 20 q20 30 0 60 M0 20 q20 30 0 60 M40 20 q20 30 0 60" stroke="${C.red}" stroke-width="5" fill="none" opacity=".7"/>`+txt(0,-200,'Erdwärme',22,O,900));
  // Biomasse
  s+=popAt(660,540,pop(lt,t[2]),[0,1,2,3,4].map(i=>`<path d="M${-80+i*20} 0 q-6 -40 4 -70 M${-80+i*20} -30 q-14 -6 -18 -20 M${-76+i*20} -46 q12 -6 14 -20" stroke="${C.mustard}" stroke-width="5" fill="none" stroke-linecap="round"/>`).join('')+
    `<rect x="30" y="-70" width="70" height="70" rx="10" fill="${C.green}" stroke="${O}" stroke-width="3"/><path d="M40 -70 q25 -40 50 0" fill="${C.sageD}" stroke="${O}" stroke-width="3"/>`+txt(10,-130,'Biomasse',22,O,900));
  // Wind + Sonne von Anfang an
  s+=turbine(880,540,0.85)+turbine(980,520,0.7,140);
  s+=solarPanel(1090,540,0.7)+solarPanel(1210,540,0.7);
  s+=sun(1180,120,0.55);
  s+=G(pill(280,90,'geht nicht aus ∞',C.white,O,24),scaleAt(280,90,pop(lt,at(c,L,1,0.2))),1);
  s+=G(pill(640,90,'kaum CO₂',C.green,'#fff',24)+leaf(760,105,0.5),scaleAt(640,90,pop(lt,at(c,L,1,0.6))),1);
  const k=pop(lt,at(c,L,2,0.55));
  s+=popAt(1150,330,k,`<path d="M-70 -30 L-40 -60 H70 V20 H-40z" fill="${C.yellow}" stroke="${O}" stroke-width="3" stroke-linejoin="round"/><circle cx="-38" cy="-20" r="8" fill="${C.cream}" stroke="${O}" stroke-width="2"/>`+txt(15,-10,'günstig!',24,O,900)+arrowPath(90,-50,90,10,C.green,6,1));
  return s;
};

S.germany=(lt,D,c,L)=>{
  let s=stripes();
  s+=G(`<rect x="440" y="52" width="400" height="56" rx="28" fill="${C.white}" stroke="${O}" stroke-width="3"/><rect x="470" y="66" width="44" height="9" fill="#222"/><rect x="470" y="75" width="44" height="9" fill="#DD0000"/><rect x="470" y="84" width="44" height="9" fill="#FFCE00"/>`+txt(660,92,'Strom in Deutschland',28,O,900),'',P(lt,0.1,0.5));
  const pr=eio(P(lt,c[0]+0.5,c[0]+2.5));
  s+=donut(420,360,150,0.55*pr,C.green,'#B9B2A2',70)+txt(420,355,'> 50 %',48,O,900)+txt(420,392,'erneuerbar',24,O,800);
  s+=G(pill(860,250,'mehr als die Hälfte',C.green,'#fff',28),scaleAt(860,250,pop(lt,at(c,L,0,0.5))),1);
  s+=popAt(780,520,pop(lt,c[0]+1),turbine(0,0,0.7));
  s+=popAt(920,520,pop(lt,c[0]+1.3),solarPanel(0,0,0.6));
  s+=popAt(1060,520,pop(lt,c[0]+1.6),`<path d="M-50 0 h100 v-50 l-30 0 l-10 -30 h-20 l-10 30 h-30z" fill="${C.water}" stroke="${O}" stroke-width="3" stroke-linejoin="round"/>`+txt(0,30,'Wasser',18,O,900));
  s+=popAt(1170,520,pop(lt,c[0]+1.9),leaf(0,0,0.9));
  return s;
};

S.storage=(lt,D,c,L)=>{
  const night=eio(P(lt,at(c,L,0,0.3),at(c,L,0,0.6)));
  let s=`<rect width="1280" height="720" fill="${C.sky}"/><rect width="1280" height="720" fill="${C.night}" opacity="${f(night*92)/100}"/>`;
  if(night>0.4) s+=G(stars(30),'',P(night,0.4,1));
  s+=G(sun(lerp(300,180,night),lerp(130,520,night),0.6,false),'',1-P(night,0.45,0.8));
  s+=G(`<circle cx="1000" cy="110" r="40" fill="#F3E7C4"/><circle cx="1020" cy="96" r="36" fill="${C.night}"/>`,'',P(night,0.6,1));
  s+=ground(560,C.sage)+`<rect x="0" y="560" width="1280" height="160" fill="${C.night}" opacity="${f(night*50)/100}"/>`;
  s+=turbine(150,560,0.8,30+120*Math.abs(Math.sin(NOW*0.4)))+solarPanel(330,560,0.8);
  const fill=night<0.5?lerp(0.2,1,P(lt,0.3,at(c,L,0,0.3))):lerp(1,0.35,P(night,0.5,1)*P(lt,at(c,L,0,0.6),D));
  s+=battery(620,470,fill,1.2)+txt(620,600-40,'Speicher',22,O,900,'middle',`fill="${night>0.5?'#fff':O}"`);
  const lit=P(night,0.5,0.9);
  s+=house(880,560,140,110,C.peach,lit)+house(1060,560,130,100,C.teal,lit,C.mustard)+house(1200,560,110,90,C.peach,lit);
  // Energiefluss
  const flowA=(1-night), flowB=night;
  for(let k=0;k<6;k++){const u=((NOW*0.5+k/6)%1);
    if(flowA>0.05){ s+=`<circle cx="${f(lerp(380,560,u))}" cy="${f(470+Math.sin(u*3.14)*-30)}" r="7" fill="${C.yellow}" stroke="${O}" stroke-width="1.5" opacity="${f(flowA*100)/100}"/>`; }
    if(flowB>0.05){ s+=`<circle cx="${f(lerp(680,860,u))}" cy="${f(470+Math.sin(u*3.14)*-30)}" r="7" fill="${C.yellow}" stroke="${O}" stroke-width="1.5" opacity="${f(flowB*100)/100}"/>`; }
  }
  // Stromnetz
  const g=P(lt,c[1],c[1]+1.2);
  if(g>0){
    let n='';
    for(let i=0;i<6;i++){const x=120+i*210; n+=`<path d="M${x-20} 700 l20 -110 l20 110 M${x-30} 610 h60" stroke="${night>0.5?'#C9C2B0':O}" stroke-width="4" fill="none"/>`;}
    n+=`<path d="M120 600 ${[1,2,3,4,5].map(i=>`Q${120+i*210-105} 625 ${120+i*210} 600`).join(' ')}" stroke="${night>0.5?'#C9C2B0':O}" stroke-width="2.5" fill="none"/>`;
    for(let k=0;k<8;k++){const u=((NOW*0.25+k/8)%1); n+=`<circle cx="${f(120+u*1050)}" cy="${f(605+Math.abs(Math.sin(u*Math.PI*5))*14)}" r="6" fill="${C.yellow}"/>`;}
    s+=G(n,'',g)+G(pill(640,660,'Stromnetz',C.white,O,22),'',g);
  }
  return s;
};

S.efficiency=(lt,D,c,L)=>{
  let s=stripes();
  const ph=lt<c[1]-0.2?0:(lt<c[2]-0.2?1:2);
  if(ph===0){
    const k=P(lt,0,0.4);
    s+=G(txt(640,90,'Gleiches Ergebnis – weniger Energie',40,O,900),'',k);
    const fr=(x,lbl,col,bar,w)=>`<g transform="translate(${x},0)"><rect x="-70" y="200" width="140" height="300" rx="18" fill="${C.white}" stroke="${O}" stroke-width="3"/><line x1="-70" y1="300" x2="70" y2="300" stroke="${O}" stroke-width="3"/><rect x="52" y="230" width="8" height="40" rx="4" fill="${O}"/><rect x="52" y="320" width="8" height="60" rx="4" fill="${O}"/>`+
      `<rect x="-60" y="400" width="70" height="60" rx="8" fill="${col}" stroke="${O}" stroke-width="3"/>${txt(-25,442,lbl,34,'#fff',900)}`+
      `<rect x="110" y="${f(500-bar)}" width="40" height="${f(bar)}" rx="8" fill="${col}" stroke="${O}" stroke-width="3"/>${txt(130,530,w,20,O,900)}</g>`;
    const b=eio(P(lt,c[0]+1,c[0]+2.5));
    s+=G(fr(360,'G',C.red,220*b,'alt'),'',P(lt,0.3,0.7))+G(fr(820,'A',C.green,70*b,'neu'),'',P(lt,0.6,1));
    s+=G(txt(640,360,'=',80,O,900),'',P(lt,1,1.4));
  } else if(ph===1){
    const k=P(lt,c[1]-0.2,c[1]+0.3);
    s+=G(txt(640,90,'Dämmung hält die Wärme drin',40,O,900),'',k);
    const hs=(x,ins)=>{let g=house(x,520,260,200,C.peach,1,C.red)+(ins?`<rect x="${x-142}" y="312" width="12" height="208" fill="${C.mustard}" stroke="${O}" stroke-width="2"/><rect x="${x+130}" y="312" width="12" height="208" fill="${C.mustard}" stroke="${O}" stroke-width="2"/>`:'');
      const n=ins?2:7; for(let i=0;i<n;i++){const a=(NOW*0.6+i/n)%1, ang=rad(-160+i*(140/(n-1||1))); g+=`<path d="M${f(x+Math.cos(ang)*(140+a*90))} ${f(400+Math.sin(ang)*(140+a*90))} q8 -10 0 -20 q-8 -10 0 -20" stroke="${C.red}" stroke-width="6" fill="none" stroke-linecap="round" opacity="${f((1-a)*90)/100}"/>`;}
      return g;};
    s+=G(hs(380,false)+pill(380,590,'ohne Dämmung',C.white,O,22),'',k)+G(hs(900,true)+pill(900,590,'gedämmt',C.green,'#fff',22),'',P(lt,c[1]+0.4,c[1]+0.9));
  } else {
    const k=pop(lt,c[2]);
    s+=popAt(640,300,k,`<circle r="150" fill="${C.white}" stroke="${O}" stroke-width="4"/>${bolt(0,0,2.2)}<circle r="150" fill="none" stroke="${C.green}" stroke-width="18"/><line x1="-106" y1="106" x2="106" y2="-106" stroke="${C.green}" stroke-width="18" stroke-linecap="round"/>`);
    s+=G(txt(640,520,'Die beste Energie ist die,',38,O,900)+txt(640,566,'die wir gar nicht verbrauchen.',38,C.tealD,900),'',P(lt,c[2]+0.4,c[2]+0.9));
  }
  return s;
};

S.hand2=(lt,D,c,L)=>{
  let s=stripes();
  s+=P2({x:640,y:640,s:1,armR:[90,-30+Math.sin(NOW*2)*6],mouth:'smile',look:0,blink:blinkAt(2)});
  s+=popAt(860,330,pop(lt,0.4),`<circle r="70" fill="${C.white}" stroke="${O}" stroke-width="4"/>`+txt(0,24,'du?',50,C.tealD,900));
  for(let i=0;i<2;i++){const k=pop(lt,0.8+i*0.4),bob=Math.sin(NOW*3+i)*6; s+=popAt([440,500][i],[250+bob,170+bob][i],k,txt(0,0,'?',70,[C.yellow,C.red][i],900,'middle',`stroke="${O}" stroke-width="2"`));}
  return s;
};
