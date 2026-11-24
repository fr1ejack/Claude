// ======================================================================
//  Szenen Teil 3 – Warum es uns alle betrifft & was wir tun können
// ======================================================================
S.why=(lt,D)=>{
  let s=stripes()+ground(600,C.sage);
  s+=G(txt(640,150,'Warum betrifft das uns alle?',48,O,900),scaleAt(640,135,pop(lt,0.3)),1);
  s+=sun(640,lerp(-120,350,eout(P(lt,0.6,1.8))),pop(lt,0.6,0.7)*(1+0.03*Math.sin(NOW*3)));
  const ppl=[[200,P1],[380,P2],[900,P3],[1080,P4]];
  ppl.forEach(([x,fn],i)=>{const k=P(lt,1.4+i*0.25,1.9+i*0.25); s+=G(fn({x,y:690,s:0.6,look:x<640?1.5:-1.5,lookY:-1,mouth:'big',blink:blinkAt(i)}),`translate(0,${f((1-eout(k))*200)})`,k);});
  return s;
};

S.hospital=(lt,D,c,L)=>{
  let s=`<rect width="1280" height="720" fill="#DCE8E4"/>`;
  for(let x=0;x<1280;x+=80) s+=`<line x1="${x}" y1="0" x2="${x}" y2="560" stroke="#CFDDD8" stroke-width="3"/>`;
  s+=`<rect x="0" y="560" width="1280" height="160" fill="#B9C7C2" stroke="${O}" stroke-width="3"/>`;
  s+=`<g transform="translate(640,60)"><rect x="-26" y="-8" width="52" height="16" rx="5" fill="${C.red}"/><rect x="-8" y="-26" width="16" height="52" rx="5" fill="${C.red}"/></g>`;
  const t=[at(c,L,1,0.25),at(c,L,1,0.5),at(c,L,1,0.78)];
  // Beatmung / Monitor
  let mon=`<rect x="-80" y="-170" width="160" height="110" rx="14" fill="${O}"/><rect x="-70" y="-160" width="140" height="90" rx="8" fill="#2F4A4A"/>`;
  let hp=''; for(let i=0;i<=28;i++){const x=-66+i*4.7, ph=((i/28)+NOW*0.6)%1; const y=-115+(ph>0.45&&ph<0.5?-26:ph>0.5&&ph<0.55?18:0); hp+=(i?'L':'M')+f(x)+' '+f(y);}
  mon+=`<path d="${hp}" stroke="#7FE0A0" stroke-width="3" fill="none"/><rect x="-10" y="-60" width="20" height="120" fill="${C.grey}" stroke="${O}" stroke-width="2"/><rect x="-50" y="56" width="100" height="16" rx="8" fill="${C.grey}" stroke="${O}" stroke-width="2"/>`;
  s+=`<g transform="translate(330,490)">${mon}</g>`;
  // Brutkasten
  s+=`<g transform="translate(640,500)"><rect x="-120" y="-70" width="240" height="110" rx="40" fill="#EAF6F6" stroke="${O}" stroke-width="3" opacity=".95"/><ellipse cx="0" cy="10" rx="50" ry="20" fill="${C.white}" stroke="${O}" stroke-width="2"/><circle cx="-34" cy="0" r="18" fill="${C.skin}" stroke="${O}" stroke-width="2"/><rect x="-100" y="40" width="200" height="20" rx="8" fill="${C.grey}" stroke="${O}" stroke-width="2"/><rect x="-10" y="60" width="20" height="40" fill="${C.grey}"/></g>`;
  // Impfstoff-Kühlschrank
  let fr=`<rect x="-80" y="-220" width="160" height="260" rx="16" fill="${C.white}" stroke="${O}" stroke-width="3"/><rect x="-66" y="-206" width="132" height="200" rx="8" fill="#DFF1F6"/>`;
  for(let r=0;r<2;r++)for(let q=0;q<4;q++) fr+=`<rect x="${-56+q*30}" y="${-190+r*80}" width="18" height="50" rx="5" fill="${['#9FC3C0','#E8B795'][(r+q)%2]}" stroke="${O}" stroke-width="2"/>`;
  fr+=`<g transform="translate(46,-24)" stroke="#5AA9E6" stroke-width="4" stroke-linecap="round"><line x1="-12" y1="0" x2="12" y2="0"/><line x1="0" y1="-12" x2="0" y2="12"/><line x1="-8" y1="-8" x2="8" y2="8"/><line x1="8" y1="-8" x2="-8" y2="8"/></g>`;
  s+=`<g transform="translate(960,560)">${fr}</g>`;
  s+=P2({x:150,y:650,s:0.8,shirt:'#F4F1EA',armR:[60,-60],look:1.5,blink:blinkAt(7),front:`<rect x="-14" y="-160" width="28" height="22" rx="4" fill="${C.teal}"/>`,extra:`<path d="M-30 -196 q0 30 30 40 q30 -10 30 -40" stroke="${O}" stroke-width="3" fill="none"/>`});
  s+=G(pill(330,250,'Beatmung',C.white,O,22),scaleAt(330,250,pop(lt,t[0])),1);
  s+=G(pill(640,360,'Brutkasten',C.white,O,22),scaleAt(640,360,pop(lt,t[1])),1);
  s+=G(pill(960,260,'Impfstoffe',C.white,O,22),scaleAt(960,260,pop(lt,t[2])),1);
  s+=G(pill(640,150,'Strom rettet Leben',C.red,'#fff',28),scaleAt(640,150,pop(lt,at(c,L,0,0.5))),lt<c[2]?1:1-P(lt,c[2],c[2]+0.4));
  // ~1 Milliarde
  const k=pop(lt,c[2]+0.2);
  if(k>0){
    s+=`<rect width="1280" height="720" fill="${C.cream}" opacity="${f(P(lt,c[2],c[2]+0.4)*88)/100}"/>`;
    const fl=Math.sin(NOW*13)>0.2?1:0.15;
    s+=popAt(640,320,k,`<rect x="-360" y="-170" width="720" height="340" rx="40" fill="${C.white}" stroke="${O}" stroke-width="4"/>`+house(-200,110,200,140,'#F4F1EA',0,C.red)+bulb(-200,-80,0.6,fl)+txt(110,-30,'≈ 1 Milliarde',50,O,900)+txt(110,20,'Menschen in Gesundheits-',26,O,800)+txt(110,54,'zentren ohne verlässlichen Strom',26,O,800));
  }
  return s;
};

S.school=(lt,D,c,L)=>{
  let s='';
  if(lt<c[1]-0.2){
    s+=stripes('#EDE3CF','#E4D9C2',0,560)+`<rect x="0" y="560" width="1280" height="160" fill="${C.wood}" stroke="${O}" stroke-width="3"/>`;
    s+=`<rect x="240" y="90" width="520" height="260" rx="14" fill="#3E5A4E" stroke="${O}" stroke-width="6"/>`+txt(500,180,'Energie = Zukunft',34,'#F4EFE3',800)+`<path d="M330 260 q40 -40 80 0 t80 0 t80 0" stroke="${C.yellow}" stroke-width="4" fill="none"/>`;
    for(let i=0;i<2;i++){const x=400+i*480; s+=`<line x1="${x}" y1="0" x2="${x}" y2="60" stroke="${O}" stroke-width="3"/><path d="M${x-40} 90 Q${x-40} 60 ${x} 60 Q${x+40} 60 ${x+40} 90z" fill="${C.teal}" stroke="${O}" stroke-width="3"/><circle cx="${x}" cy="94" r="10" fill="#FBE7A1" stroke="${O}" stroke-width="2"/>`;}
    s+=P3({x:880,y:620,s:0.85,armL:[120,-30],look:-1.5,blink:blinkAt(3)});
    const kids=[[180,P1],[420,P2],[660,P4]];
    kids.forEach(([x,fn],i)=>{s+=fn({x,y:660,s:0.6,look:0.5,lookY:1,blink:blinkAt(i+4)})+`<rect x="${x-90}" y="560" width="180" height="14" rx="6" fill="${C.woodD}" stroke="${O}" stroke-width="3"/>`+
      `<g transform="translate(${x+30},560) scale(0.6)"><path d="M-46 0 L-38 -60 H38 L46 0z" fill="${C.navy}" stroke="${O}" stroke-width="3" stroke-linejoin="round"/><rect x="-32" y="-54" width="64" height="44" rx="4" fill="${lt>at(c,L,0,0.45)?'#9FC3C0':'#3E5368'}"/></g>`;});
    const lb=[['Licht',560,60],['Computer',690,470],['Lernen',1080,260]];
    lb.forEach(([n,x,y],i)=>{s+=G(pill(x,y,n,C.yellow,O,22),scaleAt(x,y,pop(lt,at(c,L,0,0.3+i*0.2))),1);});
  } else {
    const k=P(lt,c[1]-0.2,c[1]+0.3);
    s+=`<rect width="1280" height="720" fill="#1F2433"/>`;
    // links: dunkel
    s+=`<rect x="40" y="70" width="580" height="480" rx="30" fill="#2B3040" stroke="${O}" stroke-width="3"/>`;
    s+=`<rect x="140" y="420" width="380" height="16" rx="6" fill="#6b5a48" stroke="${O}" stroke-width="3"/>`+P1({x:330,y:560,s:0.75,skin:C.skin3,hair:'#2a2420',shirt:C.mustard,armL:[20,-110],armR:[20,-110],mouth:'sad',brow:1,lookY:1,blink:blinkAt(2)});
    s+=`<rect x="276" y="398" width="110" height="26" rx="6" fill="#bdb6a6" stroke="${O}" stroke-width="2.5"/>`+txt(330,110,'kein Licht',30,'#E7DFCC',900)+txt(330,140,'… und kein Internet',22,'#9a9a9a',800);
    // rechts: Licht
    const r=P(lt,at(c,L,1,0.3),at(c,L,1,0.5));
    s+=G(`<rect x="660" y="70" width="580" height="480" rx="30" fill="${C.cream}" stroke="${O}" stroke-width="3"/><path d="M820 130 L700 420 H1060 L940 130z" fill="${C.lamp}" opacity=".35"/>`+
      `<line x1="880" y1="70" x2="880" y2="110" stroke="${O}" stroke-width="3"/><path d="M840 140 Q840 110 880 110 Q920 110 920 140z" fill="${C.teal}" stroke="${O}" stroke-width="3"/><circle cx="880" cy="144" r="10" fill="#FBE7A1" stroke="${O}" stroke-width="2"/>`+
      `<rect x="760" y="420" width="380" height="16" rx="6" fill="${C.woodD}" stroke="${O}" stroke-width="3"/>`+P4({x:950,y:560,s:0.75,armL:[20,-110],armR:[20,-110],mouth:'big',lookY:1,blink:blinkAt(5)})+
      `<rect x="896" y="398" width="110" height="26" rx="6" fill="#fff" stroke="${O}" stroke-width="2.5"/>`+wifi(1130,170,1,C.teal)+globe(1130,250,34,'gs2',0),'',r);
    s=G(s,'',k);
  }
  return s;
};

S.work=(lt,D,c,L)=>{
  let s=stripes()+ground(600,C.sage);
  const on=P(lt,0.4,0.8);
  s+=factory(380,600,1,on>0.5,0,on);
  s+=gear(300,520,26,NOW*90)+gear(345,500,18,-NOW*130,C.mustard);
  s+=P3({x:600,y:690,s:0.6,shirt:C.teal,armL:[150,-wave()-20],mouth:'big',look:-1,blink:blinkAt(9),extra:`<path d="M-70 -300 Q-70 -350 0 -350 Q70 -350 70 -300 Z" fill="${C.yellow}" stroke="${O}" stroke-width="3"/><rect x="-80" y="-306" width="160" height="14" rx="7" fill="${C.yellow}" stroke="${O}" stroke-width="3"/>`});
  s+=G(pill(380,180,'Arbeitsplätze',C.white,O,26),scaleAt(380,180,pop(lt,at(c,L,0,0.5))),1);
  // kleiner Betrieb
  const k=pop(lt,c[1]);
  if(k>0){
    let b=`<rect x="-150" y="-180" width="300" height="180" fill="${C.peach}" stroke="${O}" stroke-width="3"/><path d="M-170 -180 h340 l-20 -50 h-300z" fill="${C.teal}" stroke="${O}" stroke-width="3" stroke-linejoin="round"/>`;
    for(let i=0;i<6;i++) b+=`<path d="M${-170+i*56.6} -180 q28 30 56.6 0" fill="${i%2?C.white:C.teal}" stroke="${O}" stroke-width="2.5"/>`;
    b+=`<rect x="-120" y="-120" width="110" height="80" rx="8" fill="${C.lamp}" stroke="${O}" stroke-width="2.5"/><rect x="20" y="-130" width="100" height="130" rx="8" fill="${C.white}" stroke="${O}" stroke-width="2.5"/>`;
    for(let i=0;i<3;i++) b+=`<rect x="${32+i*30}" y="-110" width="18" height="34" rx="5" fill="${['#C8553D','#6FA86A','#D9A93A'][i]}" stroke="${O}" stroke-width="2"/>`;
    b+=`<g transform="translate(0,-218) rotate(-3)"><rect x="-70" y="-18" width="140" height="36" rx="8" fill="${C.white}" stroke="${O}" stroke-width="2"/>${txt(0,8,'KIOSK',20,O,900)}</g>`;
    s+=popAt(960,600,k,b);
    s+=P4({x:830,y:690,s:0.55,skin:C.skin3,mouth:'big',armR:[60,-60],look:1,blink:blinkAt(4)});
  }
  // Treppe aus der Armut
  const st=eio(P(lt,at(c,L,1,0.45),at(c,L,1,0.85)));
  if(st>0){
    let g=''; for(let i=0;i<4;i++){if(i/4>st) break; g+=`<rect x="${-120+i*60}" y="${-30-i*40}" width="60" height="${30+i*40}" fill="${C.yellow}" stroke="${O}" stroke-width="3"/>`;}
    g+=arrowPath(-120,-30,120,-200,C.green,7,st);
    s+=`<g transform="translate(1120,330)">${g}</g>`+G(pill(1120,380,'raus aus der Armut',C.green,'#fff',20),'',P(st,0.8,1));
  }
  return s;
};

S.climate=(lt,D,c,L)=>{
  const clean=P(lt,at(c,L,1,0.62),at(c,L,1,0.85));
  let s=`<rect width="1280" height="720" fill="${C.sky}"/>`;
  s+=sun(640,110,0.5*(0.4+clean*0.6),true);
  s+=`<path d="M0 520 Q320 470 600 520 V720 H0z" fill="#9C9686" stroke="${O}" stroke-width="3"/><path d="M680 520 Q960 480 1280 510 V720 H680z" fill="${lerp(0,1,P(lt,c[1],c[1]+1))>0.5?'#D9B98F':C.sage}" stroke="${O}" stroke-width="3"/>`;
  s+=`<line x1="640" y1="200" x2="640" y2="720" stroke="${O}" stroke-width="4" stroke-dasharray="16 12"/>`+G(txt(640,190,'Grenze',22,O,800),'',P(lt,0.4,0.8));
  s+=factory(260,520,0.6,1,1-clean,clean)+car(470,560,C.red,0.6);
  for(let i=0;i<9;i++){const a=((NOW*0.12+i/9)%1); const x=lerp(300,1150,a), y=250+Math.sin(i*1.9)*50;
    s+=`<g opacity="${f(0.85*(1-clean)*100)/100}">${cloud(x,y,0.55,'#7C808A')}</g>`;}
  s+=G(txt(400,300,'CO₂',40,'#4F5361',900),'',(1-clean)*P(lt,0.3,0.8));
  s+=hut(900,540,0.8,0)+hut(1080,560,0.7,0);
  const k=P(lt,c[1],c[1]+1);
  if(k>0){
    s+=popAt(860,330,pop(lt,at(c,L,1,0.15)),`<circle r="40" fill="${C.red}" stroke="${O}" stroke-width="3"/>`+txt(0,10,'Hitze',18,'#fff',900));
    s+=popAt(1150,330,pop(lt,at(c,L,1,0.3)),cloud(0,0,0.7,'#7C808A')+`<path d="M-30 20 l-6 24 M0 22 l-6 24 M30 20 l-6 24" stroke="#5AA9E6" stroke-width="5" stroke-linecap="round"/>`);
    s+=G(pill(960,250,'leiden am meisten',C.red,'#fff',22),'',P(lt,at(c,L,1,0.35),at(c,L,1,0.45))*(1-clean));
  }
  s+=G(pill(640,410,'Saubere Energie = unsere Zukunft',C.green,'#fff',28),scaleAt(640,410,pop(lt,at(c,L,1,0.75))),1);
  return s;
};

S.wende=(lt,D,c,L)=>{
  let s=`<rect width="1280" height="720" fill="${C.sky}"/>`+cloud(1000+lt*5,100,0.8)+ground(560,C.sage);
  const off=P(lt,at(c,L,0,0.35),at(c,L,0,0.45));
  let cp=coalPlant(-40);
  if(off<1) cp+=smokePuffs(280,230,8,40,1-off,'#6E7280',4,160);
  s+=G(cp,`translate(0,${f(P(lt,at(c,L,0,0.5),at(c,L,0,0.7))*0)})`,1-0.5*P(lt,at(c,L,0,0.5),at(c,L,0,0.7)));
  s+=popAt(300,250,pop(lt,at(c,L,0,0.42)),`<rect x="-60" y="-30" width="120" height="60" rx="14" fill="${C.red}" stroke="${O}" stroke-width="3"/>`+txt(0,12,'AUS',32,'#fff',900));
  const tw=[[560,0.8,0.62],[700,0.65,0.68],[820,0.75,0.74]];
  tw.forEach(([x,sc,fr])=>{const k=eout(P(lt,at(c,L,0,fr),at(c,L,0,fr)+0.8)); if(k>0) s+=`<g transform="translate(${x},560) scale(1,${k.toFixed(3)}) translate(${-x},-560)">${turbine(x,560,sc)}</g>`;});
  s+=popAt(980,560,pop(lt,at(c,L,0,0.85)),solarPanel(0,0,0.65)+solarPanel(130,0,0.65));
  // Wärmepumpe
  const hk=pop(lt,c[1]);
  if(hk>0){
    s+=`<rect width="1280" height="720" fill="${C.cream}" opacity="${f(P(lt,c[1]-0.2,c[1]+0.3)*90)/100}"/>`;
    let h=house(0,0,300,220,C.peach,1,C.red,1);
    h+=`<g transform="translate(220,0)"><rect x="-60" y="-100" width="120" height="100" rx="14" fill="${C.white}" stroke="${O}" stroke-width="3"/><circle cx="0" cy="-50" r="34" fill="#E9E2D2" stroke="${O}" stroke-width="3"/>`+
      [0,1,2].map(b=>`<path d="M0 -50 q14 -10 26 -4 q-6 14 -26 4z" fill="${C.grey}" stroke="${O}" stroke-width="1.5" transform="rotate(${f(NOW*300+b*120)} 0 -50)"/>`).join('')+`</g>`;
    h+=`<g transform="translate(-260,0)"><rect x="-50" y="-90" width="100" height="90" rx="20" fill="#9a8a6a" stroke="${O}" stroke-width="3"/>${txt(0,-36,'ÖL',22,'#fff',900)}</g>`+cross(-220,-110,pop(lt,at(c,L,1,0.55)),22);
    s+=popAt(560,480,hk*(c[2]&&lt>c[2]?1-P(lt,c[2],c[2]+0.4):1),h);
    s+=G(pill(780,240,'Wärmepumpe',C.green,'#fff',24),'',P(lt,at(c,L,1,0.25),at(c,L,1,0.4))*(lt>c[2]?1-P(lt,c[2],c[2]+0.4):1));
  }
  // Neue Jobs
  if(lt>c[2]-0.1){
    const hard=`<path d="M-70 -300 Q-70 -350 0 -350 Q70 -350 70 -300 Z" fill="${C.yellow}" stroke="${O}" stroke-width="3"/><rect x="-80" y="-306" width="160" height="14" rx="7" fill="${C.yellow}" stroke="${O}" stroke-width="3"/>`;
    const J=[[330,P4,'Elektrikerin',0.35],[640,P3,'Ingenieur',0.55],[950,P1,'Handwerker',0.75]];
    J.forEach(([x,fn,n,fr],i)=>{const k=P(lt,at(c,L,2,fr),at(c,L,2,fr)+0.4);
      s+=G(fn({x,y:640,s:0.8,mouth:'big',armR:[150,wave(i)-20],look:i===1?0:(i?-1:1),blink:blinkAt(i),extra:hard})+pill(x,280,n,C.white,O,22),`translate(0,${f((1-eout(k))*120)})`,k);});
    s+=G(pill(640,100,'Neue Jobs',C.yellow,O,30),scaleAt(640,100,pop(lt,at(c,L,2,0.15))),1);
  }
  return s;
};

S.room=(lt,D,c,L)=>{
  const lightOff=P(lt,at(c,L,0,0.62),at(c,L,0,0.66));
  const stops=[[0,330],[c[0]+L[0]*0.95,330],[c[1]-0.1,520],[c[2]-0.4,520],[c[2]+0.6,940]];
  let px=stops[0][1], walking=false;
  for(let i=0;i<stops.length-1;i++){const [a,xa]=stops[i],[b,xb]=stops[i+1]; if(lt>=a&&lt<b){px=lerp(xa,xb,eio(P(lt,a,b))); walking=xa!==xb;} if(lt>=b) px=xb;}
  let s=stripes('#EDE3CF','#E4D9C2',0,560)+`<rect x="0" y="560" width="1280" height="160" fill="${C.wood}" stroke="${O}" stroke-width="3"/>`;
  for(let x=0;x<1300;x+=160) s+=`<line x1="${x}" y1="560" x2="${x-60}" y2="720" stroke="${C.woodD}" stroke-width="3"/>`;
  s+=`<rect x="40" y="130" width="160" height="210" rx="18" fill="#CFE0DD" stroke="${O}" stroke-width="3"/><line x1="120" y1="130" x2="120" y2="340" stroke="${O}" stroke-width="3"/><line x1="40" y1="235" x2="200" y2="235" stroke="${O}" stroke-width="3"/>`;
  s+=`<line x1="480" y1="0" x2="480" y2="110" stroke="${O}" stroke-width="3"/><path d="M430 150 Q430 110 480 110 Q530 110 530 150z" fill="${C.teal}" stroke="${O}" stroke-width="3"/><circle cx="480" cy="156" r="14" fill="${lightOff?'#CFC7B5':'#FBE7A1'}" stroke="${O}" stroke-width="2"/>`;
  if(lightOff<1) s+=`<path d="M440 160 L300 560 H660 L520 160z" fill="${C.lamp}" opacity="${f(0.22*(1-lightOff)*100)/100}"/>`;
  s+=`<rect x="160" y="400" width="50" height="70" rx="12" fill="${C.white}" stroke="${O}" stroke-width="3"/><rect x="174" y="${lightOff?436:414}" width="22" height="20" rx="5" fill="${C.grey}" stroke="${O}" stroke-width="2"/>`;
  // Heizung
  const heat=1-eio(P(lt,at(c,L,1,0.25),at(c,L,1,0.45)))*0.5;
  s+=`<rect x="700" y="420" width="220" height="120" rx="16" fill="${C.white}" stroke="${O}" stroke-width="3"/>`;
  for(let i=1;i<5;i++) s+=`<line x1="${700+i*44}" y1="430" x2="${700+i*44}" y2="530" stroke="${O}" stroke-width="3"/>`;
  for(let i=0;i<3;i++){const a=(NOW*0.7+i/3)%1; s+=`<path d="M${740+i*70} ${f(410-a*80)} q12 -14 0 -28 q-12 -14 0 -28" stroke="${C.red}" stroke-width="5" fill="none" stroke-linecap="round" opacity="${f((1-a)*0.7*heat*heat*100)/100}"/>`;}
  const dial=lerp(0,-40,eio(P(lt,at(c,L,1,0.2),at(c,L,1,0.4))));
  s+=`<g transform="translate(665,470)"><circle r="30" fill="${C.white}" stroke="${O}" stroke-width="3"/><g transform="rotate(${f(dial)})"><line x1="0" y1="0" x2="0" y2="-22" stroke="${C.red}" stroke-width="5" stroke-linecap="round"/></g>${txt(0,52,lt<at(c,L,1,0.35)?'21°':'20°',20,O,900)}</g>`;
  // TV + Steckdosenleiste
  const sbOff=lt>at(c,L,2,0.45);
  s+=`<rect x="1000" y="470" width="240" height="90" rx="10" fill="${C.woodD}" stroke="${O}" stroke-width="3"/>`+tv(1120,470,1,0,!sbOff)+powerStrip(1120,600,!sbOff);
  s+=`<path d="M1120 500 q0 60 -40 82" stroke="${O}" stroke-width="5" fill="none"/>`;
  // Figur
  let armL=[12,-8], armR=[12,-8], look=0, mouth='smile';
  if(lt<c[0]+L[0]*0.95){ const a=P(lt,at(c,L,0,0.45),at(c,L,0,0.6)), b=P(lt,at(c,L,0,0.75),at(c,L,0,0.9)); armL=[lerp(12,110,eio(a)-eio(b)),-10]; look=-1.5; }
  else if(lt>c[1]-0.1&&lt<c[2]-0.4){ const a=P(lt,at(c,L,1,0.05),at(c,L,1,0.2)), b=P(lt,at(c,L,1,0.5),at(c,L,1,0.6)); armR=[lerp(12,80,eio(a)-eio(b)),-10]; look=1.5; }
  else if(lt>c[2]+0.6){ const a=P(lt,at(c,L,2,0.3),at(c,L,2,0.42)); armR=[lerp(12,120,eio(a)),lerp(-8,-30,a)]; look=1.5; mouth=sbOff?'big':'smile';
    if(a>0.9) armR=[lerp(120,155,P(lt,at(c,L,2,0.42),at(c,L,2,0.5))),-30]; }
  s+=P1({x:px,y:640,s:0.95,armL,armR,look,mouth,walk:walking?NOW*12:null,blink:blinkAt(10),bob:walking?-Math.abs(Math.sin(NOW*12))*6:0});
  s+=`<rect width="1280" height="720" fill="#2B3040" opacity="${f(lightOff*12)/100}"/>`;
  s+=check(240,380,pop(lt,at(c,L,0,0.7)),26);
  s+=G(pill(810,330,'1 Grad ≈ 6 % weniger',C.green,'#fff',22),scaleAt(810,330,pop(lt,at(c,L,1,0.55))),1);
  s+=check(1230,420,pop(lt,at(c,L,2,0.5)),26);
  s+=G(pill(1080,330,'Stand-by aus!',C.white,O,22),'',P(lt,at(c,L,2,0.55),at(c,L,2,0.65)));
  return s;
};

S.move=(lt,D,c,L)=>{
  let s=`<rect width="1280" height="720" fill="${C.sky}"/>`+cloud(250+lt*10,110)+cloud(900+lt*7,80,0.8);
  s+=`<path d="M0 420 Q320 360 640 410 T1280 400 V720 H0z" fill="#B8C4B4" stroke="${O}" stroke-width="3"/>`;
  // Zug hinten
  const zt=at(c,L,0,0.55);
  if(lt>zt-1){const x=lerp(1400,-900,P(lt,zt-1,zt+5)); s+=`<g transform="translate(${f(x)},420) scale(0.45)">${[0,1,2].map(i=>`<rect x="${i*330}" y="-150" width="320" height="150" rx="26" fill="${C.teal}" stroke="${O}" stroke-width="5"/><rect x="${i*330+20}" y="-130" width="280" height="54" rx="16" fill="${C.white}" stroke="${O}" stroke-width="4"/>`).join('')}</g>`;}
  s+=`<rect x="0" y="420" width="1280" height="6" fill="${C.navy}"/>`;
  s+=`<rect x="0" y="470" width="1280" height="250" fill="#7C808A" stroke="${O}" stroke-width="3"/><rect x="0" y="470" width="1280" height="40" fill="#C9C2B0" stroke="${O}" stroke-width="3"/>`;
  s+=`<rect x="0" y="600" width="1280" height="26" fill="${C.green}" opacity=".85"/>`+txt(1180,620,'Radweg',18,'#fff',900);
  // Bus
  const bt=at(c,L,0,0.4);
  if(lt>bt-1) s+=bus(lerp(1500,-300,P(lt,bt-1,bt+5)),580,0.75);
  // Radfahrer
  const bx=lerp(-150,1450,P(lt,at(c,L,0,0.02),at(c,L,0,0.02)+7));
  s+=bike(bx,680,0.8,NOW*400)+person({x:bx-4,y:690,s:0.55,shirt:C.peach,hair:C.brown,style:'bun',armR:[60,-30],armL:[60,-30],mouth:'big',look:1.5,blink:blinkAt(3),bob:Math.sin(NOW*10)*2});
  // Fußgänger
  const wx=lerp(1300,700,P(lt,at(c,L,0,0.2),D));
  s+=P3({x:wx,y:500,s:0.45,walk:NOW*9,look:-1.5,blink:blinkAt(5),flip:-1});
  s+=G(pill(640,90,'Fahrrad · zu Fuß · Bus & Bahn',C.white,O,28),scaleAt(640,90,pop(lt,at(c,L,0,0.1))),1);
  s+=G(pill(640,170,'weniger Energie & CO₂',C.green,'#fff',24),scaleAt(640,170,pop(lt,at(c,L,0,0.75))),1);
  return s;
};

S.green=(lt,D,c,L)=>{
  let s=stripes();
  if(lt<c[1]-0.2){
    const tog=eio(P(lt,c[0]+0.8,c[0]+1.4));
    s+=`<g transform="translate(640,360)"><path d="M-330 160 L-300 -230 H300 L330 160z" fill="${C.navy}" stroke="${O}" stroke-width="4" stroke-linejoin="round"/><rect x="-280" y="-210" width="560" height="350" rx="12" fill="#EAF1EE"/>`+
      `<rect x="-280" y="-210" width="560" height="54" rx="12" fill="${C.teal}"/>`+txt(-250,-174,'Mein Stromanbieter',26,'#fff',900,'start')+
      txt(-230,-80,'Ökostrom',44,O,900,'start')+txt(-230,-40,'100 % aus Wind, Sonne & Wasser',22,O,800,'start')+
      `<rect x="100" y="-120" width="140" height="70" rx="35" fill="${tog>0.5?C.green:'#BDB6A8'}" stroke="${O}" stroke-width="3"/><circle cx="${f(135+70*tog)}" cy="-85" r="28" fill="#fff" stroke="${O}" stroke-width="3"/>`+
      popAt(0,60,pop(lt,c[0]+1.5),leaf(0,40,1.2)+check(60,-10,1,24))+
      `<path d="M-380 160 H380 L400 200 H-400z" fill="#B9BFC8" stroke="${O}" stroke-width="4" stroke-linejoin="round"/></g>`;
  } else {
    const k=P(lt,c[1]-0.2,c[1]+0.3);
    let f2=`<rect x="0" y="540" width="1280" height="180" fill="${C.wood}" stroke="${O}" stroke-width="3"/>`;
    f2+=P2({x:330,y:640,s:0.9,look:1,mouth:'smile',blink:blinkAt(2),armR:[40,-40]})+P3({x:950,y:640,s:0.9,look:-1,mouth:'big',blink:blinkAt(3),armL:[40,-40]})+P1({x:640,y:600,s:0.65,look:Math.sin(NOW)*1.5,mouth:'big',blink:blinkAt(4),armR:[140,wave()-30]});
    f2+=`<rect x="200" y="470" width="880" height="24" rx="10" fill="${C.woodD}" stroke="${O}" stroke-width="3"/>`;
    f2+=popAt(640,190,pop(lt,c[1]+0.4),`<path d="M-160 -70 h320 q30 0 30 30 v70 q0 30 -30 30 h-120 l-40 40 l0 -40 h-160 q-30 0 -30 -30 v-70 q0 -30 30 -30z" fill="${C.white}" stroke="${O}" stroke-width="4"/>`+leaf(-60,24,0.8)+plug(40,-2,0.7)+txt(110,10,'?',40,C.tealD,900));
    s+=G(f2,'',k);
  }
  return s;
};

S.schoolact=(lt,D,c,L)=>{
  let s=`<rect width="1280" height="720" fill="${C.sky}"/>`+cloud(200+lt*5,90)+ground(580,C.sage);
  // Schulgebäude
  s+=`<rect x="300" y="250" width="680" height="330" fill="#E8D7B8" stroke="${O}" stroke-width="3"/><path d="M280 250 L640 120 L1000 250z" fill="${C.red}" stroke="${O}" stroke-width="3" stroke-linejoin="round"/>`;
  s+=`<circle cx="640" cy="200" r="34" fill="${C.white}" stroke="${O}" stroke-width="3"/><line x1="640" y1="200" x2="640" y2="178" stroke="${O}" stroke-width="4"/><line x1="640" y1="200" x2="${f(640+16*Math.cos(NOW))}" y2="${f(200+16*Math.sin(NOW))}" stroke="${O}" stroke-width="3"/>`;
  s+=`<rect x="540" y="262" width="200" height="40" rx="10" fill="${C.teal}" stroke="${O}" stroke-width="3"/>`+txt(640,291,'SCHULE',26,'#fff',900);
  const win=P(lt,at(c,L,0,0.4),at(c,L,0,0.5));
  for(let i=0;i<5;i++){const x=340+i*130; if(i===2) continue; s+=`<rect x="${x}" y="340" width="80" height="90" rx="8" fill="#BFD7D4" stroke="${O}" stroke-width="3"/><line x1="${x+40}" y1="340" x2="${x+40}" y2="430" stroke="${O}" stroke-width="2.5"/>`;}
  // Fenster weit auf (kurz & kräftig)
  s+=`<rect x="600" y="340" width="80" height="90" rx="8" fill="#5a6070" stroke="${O}" stroke-width="3"/><path d="M600 340 l${f(-50*win)} 10 v70 l${f(50*win)} 10z" fill="#BFD7D4" stroke="${O}" stroke-width="3"/><path d="M680 340 l${f(50*win)} 10 v70 l${f(-50*win)} 10z" fill="#BFD7D4" stroke="${O}" stroke-width="3"/>`;
  s+=`<rect x="600" y="470" width="80" height="110" rx="8" fill="${C.teal}" stroke="${O}" stroke-width="3"/>`;
  if(win>0) s+=G(pill(640,320-90,'kurz & kräftig lüften',C.white,O,20),'',win)+popAt(760,370,pop(lt,at(c,L,0,0.5)),`<circle r="26" fill="${C.white}" stroke="${O}" stroke-width="3"/>`+txt(0,8,'5′',20,O,900));
  // Solaranlage aufs Dach
  const sp=at(c,L,0,0.82);
  for(let i=0;i<4;i++) s+=popAt(400+i*70-(i>1?0:0),228-i*26+(i>1?0:0),pop(lt,sp+i*0.15),'')+G(`<path d="M${720+i*60} ${238-i*22} l50 0 l-12 -18 l-50 0z" fill="#4F6D8A" stroke="${O}" stroke-width="2.5" stroke-linejoin="round"/>`,'',pop(lt,sp+i*0.15));
  // Kinder mit Plakat
  const kp=P(lt,at(c,L,0,0.12),at(c,L,0,0.25));
  s+=G(P2({x:170,y:690,s:0.6,armR:[150,-60],armL:[150,-60],mouth:'big',look:1,blink:blinkAt(1)})+P3({x:1110,y:690,s:0.6,armL:[150,-wave()-20],mouth:'big',look:-1,blink:blinkAt(2)})+
    `<g transform="translate(170,360)"><rect x="-90" y="-50" width="180" height="90" rx="10" fill="${C.white}" stroke="${O}" stroke-width="3"/>${txt(0,-10,'Energie',22,O,900)}${txt(0,20,'sparen!',22,C.green,900)}</g>`,`translate(0,${f((1-eout(kp))*200)})`,kp);
  // Viele Menschen
  const many=P(lt,c[1],c[1]+1.5);
  if(many>0){for(let i=0;i<22;i++){const k=pop(lt,c[1]+i*0.06,0.4); s+=popAt(60+i*55,650+((i%2)*16),k,personIcon(0,0,1,[C.teal,C.mustard,C.peach,C.purple,C.green][i%5]));}
    s+=G(pill(640,110,'Kleine Schritte · viele Menschen',C.yellow,O,28),scaleAt(640,110,pop(lt,c[1]+0.3)),1);}
  return s;
};

S.summary=(lt,D,c,L)=>{
  let s=stripes();
  s+=G(txt(640,90,'Energie für alle',52,O,900),scaleAt(640,72,pop(lt,0.2)),1);
  ['bezahlbar','verlässlich','sauber'].forEach((w,i)=>{s+=G(pill(380+i*260,160,w,[C.yellow,C.teal,C.green][i],i?'#fff':O,26),scaleAt(380+i*260,160,pop(lt,at(c,L,0,0.6+i*0.12))),1);});
  const it=[['Zugang für alle',plug(0,0,1)+check(40,-40,1,20)],['Erneuerbare Energie',turbine(-30,60,0.5)+`<g transform="translate(36,30)">${solarPanel(0,0,0.4)}</g>`],['Effizienz',bulb(0,-10,0.75,1,true)]];
  it.forEach(([n,ic],i)=>{const k=pop(lt,at(c,L,1,0.25+i*0.22)); const x=330+i*310;
    s+=popAt(x,330,k,`<circle r="90" fill="${C.white}" stroke="${O}" stroke-width="4"/>${ic}`)+G(txt(x,460,n,26,O,900),'',k>0.5?1:0);});
  if(lt>c[2]-0.2){
    const fns=[P1,P2,P3,P4,P1,P2,P3,P4,P2];
    fns.forEach((fn,i)=>{const k=P(lt,c[2]+i*0.12,c[2]+i*0.12+0.4); s+=G(fn({x:80+i*140,y:720+120,s:0.45,mouth:'big',armR:[150,wave(i)-20],blink:blinkAt(i)}),`translate(0,${f(-120*eout(k))})`,k);});
  }
  return s;
};

S.outro=(lt,D)=>{
  let s=stripes();
  s+=G(sdgTile(lt,-0.8),`translate(640,280) scale(${(pop(lt,0.2)*0.8).toFixed(4)}) translate(-150,-150)`);
  s+=G(txt(640,500,'Jede Kilowattstunde zählt!',50,O,900),scaleAt(640,485,pop(lt,1.2)));
  s+=G(txt(640,550,'SDG 7 – Energie für alle',30,C.tealD,800),'',P(lt,2,2.6));
  s+=G(P2({x:200,y:720,s:0.7,armR:[150,wave()-20],mouth:'big',look:1,blink:blinkAt(11)})+P1({x:1080,y:720,s:0.7,armL:[150,-wave(1)-20],mouth:'big',look:-1,blink:blinkAt(12)}),'',P(lt,1.5,2.2));
  return s;
};
