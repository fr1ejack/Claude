// ======================================================================
//  Drehbuch: drei Teile à 3 Minuten. Jede Szene hat ihre Sätze.
//  Die Zeiten werden automatisch aus der Wortanzahl berechnet und pro Teil auf 180 s skaliert.
//  Nach der Aufnahme können echte Satzzeiten über window.REAL_TIMES gesetzt werden.
// ======================================================================
const PARTS = [
  {n:1, title:'Teil 1 · Energie im Alltag & Was ist SDG 7?'},
  {n:2, title:'Teil 2 · Das Problem & die Lösungen'},
  {n:3, title:'Teil 3 · Warum es uns alle betrifft & was wir tun können'},
];
const SCRIPT = [
  // ---------------- TEIL 1 ----------------
  {id:'phone',     part:1, min:3.6, cut:true, lines:['Morgens: Handy laden.']},
  {id:'train',     part:1, min:3.6, cut:true, lines:['Mit dem Zug zur Schule.']},
  {id:'city',      part:1, min:3.6, cut:true, lines:['Abends gehen überall die Lichter an.']},
  {id:'blackout',  part:1, min:9,   lines:['Und plötzlich … ist der Strom weg.', 'Kein Licht, kein Internet, kein warmes Essen aus der Mikrowelle.']},
  {id:'world',     part:1, chapter:1, lines:['Für fast 700 Millionen Menschen ist das Alltag – sie haben gar keinen Strom.', 'Die meisten leben in Afrika südlich der Sahara – oft auf dem Land, weit weg vom nächsten Stromnetz.']},
  {id:'without',   part:1, lines:['Ohne Strom können Kinder abends nicht lernen, Medikamente nicht gekühlt werden und kleine Betriebe nicht wachsen.']},
  {id:'cooking',   part:1, lines:['Und noch mehr Menschen fehlt eine saubere Art zu kochen: Rund zwei Milliarden kochen über offenem Feuer – mit Holz, Kohle oder Dung.', 'Der Rauch in den Hütten macht krank, besonders Frauen und Kinder.']},
  {id:'home',      part:1, lines:['Dabei steckt Energie in fast allem: im Kühlschrank, in der Heizung, im Licht, im Handy und in jedem Produkt aus einer Fabrik.']},
  {id:'mobility',  part:1, lines:['Auch Autos, Busse, Flugzeuge und Schiffe brauchen Energie – meistens noch Benzin, Diesel oder Kerosin.']},
  {id:'tile',      part:1, min:6, lines:['Genau darum geht es in SDG 7: Bezahlbare und saubere Energie.']},
  {id:'ring',      part:1, lines:['Es ist eines von 17 Zielen, die die Vereinten Nationen 2015 beschlossen haben. Alle 193 Mitgliedsstaaten haben zugestimmt.', 'Bis 2030 soll die Welt gerechter und nachhaltiger werden – für alle.']},
  {id:'links',     part:1, lines:['Die Ziele hängen zusammen: Ohne Energie gibt es keine gute Bildung, keine Gesundheitsversorgung und keinen Kampf gegen Armut.']},
  {id:'promise',   part:1, lines:['SDG 7 verspricht: Bis 2030 soll jeder Mensch Zugang zu bezahlbarer, verlässlicher, nachhaltiger und moderner Energie haben.']},
  {id:'meaning',   part:1, lines:['Bezahlbar heißt: Niemand soll sich zwischen Strom und Essen entscheiden müssen.', 'Verlässlich heißt: Der Strom ist da, wenn man ihn braucht.', 'Und nachhaltig heißt: Unsere Energie soll das Klima nicht zerstören.']},
  {id:'targets',   part:1, lines:['Dafür gibt es fünf Unterziele.', '7.1: Zugang für alle. 7.2: deutlich mehr erneuerbare Energie. 7.3: Energie doppelt so schnell effizienter nutzen wie bisher.', 'Dazu kommen 7.a – internationale Zusammenarbeit – und 7.b – bessere Infrastruktur für ärmere Länder.']},
  {id:'progress',  part:1, lines:['Seit 2010 hat sich einiges getan: Hunderte Millionen Menschen haben zum ersten Mal Strom bekommen.', 'Aber das Tempo reicht noch nicht, um das Ziel bis 2030 zu erreichen.']},
  {id:'hand1',     part:1, lines:['Doch warum ist das so schwierig? Darum geht es im nächsten Teil.']},
  // ---------------- TEIL 2 ----------------
  {id:'coal',      part:2, chapter:2, lines:['Das Problem: Ein großer Teil unserer Energie kommt noch aus Kohle, Öl und Gas.', 'Weltweit sind es rund vier Fünftel.']},
  {id:'greenhouse',part:2, lines:['Beim Verbrennen entsteht CO₂. Es legt sich wie eine Decke um die Erde – der Treibhauseffekt.', 'Die Erde wird wärmer, es gibt mehr Hitzewellen, Dürren und Unwetter.']},
  {id:'share',     part:2, lines:['Rund drei Viertel aller Treibhausgase stammen aus der Erzeugung und dem Verbrauch von Energie.']},
  {id:'price',     part:2, lines:['Gleichzeitig wird Energie für viele Menschen immer teurer.', 'Öl und Gas sind begrenzt und werden oft in wenigen Ländern gefördert.', 'Bei Krisen oder Kriegen steigen die Preise schnell.']},
  {id:'question',  part:2, min:5, lines:['Wie schaffen wir also die globale Energiewende?']},
  {id:'goals1',    part:2, lines:['SDG 7 hat drei Kernziele.', 'Erstens: Jeder Mensch soll Zugang zu bezahlbarem und verlässlichem Strom haben.']},
  {id:'offgrid',   part:2, lines:['Dafür muss nicht überall ein großes Stromnetz gebaut werden.', 'In abgelegenen Dörfern helfen Solaranlagen auf dem Dach oder kleine Inselnetze, sogenannte Mini-Grids.', 'Schon ein kleines Solarpanel reicht für Licht, ein Handy und ein Radio.', 'So bekommen Familien Licht zum Lernen, und kleine Läden können länger öffnen.']},
  {id:'stove',     part:2, lines:['Und statt über offenem Feuer kann mit sauberen Kochern gekocht werden – mit Gas, Strom oder Solarenergie.']},
  {id:'goals2',    part:2, lines:['Zweitens: Viel mehr Energie aus erneuerbaren Quellen – wie Wind und Sonne.']},
  {id:'renew',     part:2, lines:['Dazu gehören auch Wasserkraft, Erdwärme und Biomasse.', 'Diese Quellen gehen nicht aus und verursachen beim Betrieb kaum CO₂.', 'Solarstrom ist in vielen Ländern inzwischen sogar die günstigste Art, Strom zu erzeugen.']},
  {id:'germany',   part:2, lines:['In Deutschland kommt inzwischen mehr als die Hälfte des Stroms aus erneuerbaren Energien.']},
  {id:'storage',   part:2, lines:['Weil der Wind nicht immer weht und die Sonne nachts nicht scheint, brauchen wir Speicher wie Batterien', 'und gut ausgebaute Stromnetze, die die Energie dorthin bringen, wo sie gebraucht wird.']},
  {id:'goals3',    part:2, lines:['Drittens: Energie effizienter nutzen. Eine LED leuchtet genauso hell, braucht aber viel weniger Strom.']},
  {id:'efficiency',part:2, lines:['Effizienz bedeutet: Mit weniger Energie das Gleiche erreichen.', 'Gut gedämmte Häuser brauchen weniger Heizenergie, moderne Geräte weniger Strom.', 'Die beste Energie ist die, die wir gar nicht erst verbrauchen.']},
  {id:'hand2',     part:2, lines:['Aber was hat das alles mit uns zu tun? Darum geht es im letzten Teil.']},
  // ---------------- TEIL 3 ----------------
  {id:'why',       part:3, chapter:3, min:5, lines:['Warum betrifft das uns alle?']},
  {id:'hospital',  part:3, lines:['Krankenhäuser brauchen Strom, um Leben zu retten.', 'Ohne Strom laufen keine Beatmungsgeräte, keine Brutkästen für Babys und keine Kühlschränke für Impfstoffe.', 'Fast eine Milliarde Menschen wird in Gesundheitszentren behandelt, die keinen verlässlichen Strom haben.']},
  {id:'school',    part:3, lines:['Schulen brauchen Strom – für Licht, Computer und Lernen.', 'Wer abends kein Licht hat, kann keine Hausaufgaben machen. Und ohne Internet fehlt der Zugang zu Wissen.']},
  {id:'work',      part:3, lines:['Und Fabriken brauchen Strom für Arbeitsplätze.', 'Bezahlbare Energie hilft kleinen Betrieben zu wachsen und bringt Menschen aus der Armut.']},
  {id:'climate',   part:3, lines:['Und das Klima kennt keine Grenzen: Das CO₂, das irgendwo auf der Welt ausgestoßen wird, betrifft uns alle.', 'Länder, die am wenigsten CO₂ ausstoßen, leiden oft am stärksten unter den Folgen. Saubere Energie schützt also auch unsere Zukunft.']},
  {id:'wende',     part:3, lines:['Auch hier in Deutschland ist die Energiewende ein großes Thema: Kohlekraftwerke werden abgeschaltet, neue Windräder und Solaranlagen gebaut,', 'und viele Häuser bekommen Wärmepumpen statt Öl- oder Gasheizungen.', 'Das schafft auch neue Jobs – zum Beispiel für Elektrikerinnen, Ingenieure und Handwerker.']},
  {id:'room',      part:3, lines:['Was kannst du tun? Licht aus, wenn du den Raum verlässt.', 'Die Heizung ein bisschen runterdrehen. Schon ein Grad weniger spart rund sechs Prozent Heizenergie.', 'Geräte richtig ausschalten, statt sie im Stand-by zu lassen – zum Beispiel mit einer Steckdosenleiste mit Schalter.']},
  {id:'move',      part:3, lines:['Öfter mit dem Fahrrad, zu Fuß oder mit Bus und Bahn unterwegs sein – denn auch Verkehr verbraucht viel Energie.']},
  {id:'green',     part:3, lines:['Oder zu Ökostrom wechseln.', 'Sprich mit deiner Familie darüber – vielleicht wechselt ihr gemeinsam den Stromanbieter.']},
  {id:'schoolact', part:3, lines:['Auch in der Schule könnt ihr etwas tun: Energiesparprojekte starten, richtig lüften – kurz und kräftig statt Fenster auf Kipp – oder eine Solaranlage aufs Schuldach bringen.', 'Kleine Schritte, viele Menschen – das macht einen großen Unterschied.']},
  {id:'summary',   part:3, lines:['Fassen wir zusammen: SDG 7 will Energie für alle – bezahlbar, verlässlich und sauber.', 'Dafür brauchen wir Zugang für alle, mehr erneuerbare Energie und mehr Effizienz.', 'Die Energiewende ist eine riesige Aufgabe – aber jede und jeder kann einen Teil beitragen.']},
  {id:'outro',     part:3, min:7, lines:['Jede Kilowattstunde zählt. SDG 7 – Energie für alle.']},
];

// ---------- Zeitachse berechnen ----------
const PART_LEN = 180, WPS = 2.05, PAUSE = 0.65, LEAD = 0.45, TAILPAD = 0.5;
const words = s => s.replace(/[–…·:]/g,' ').split(/\s+/).filter(Boolean).length;
function buildTimeline(){
  const scenes = SCRIPT.map(sc => {
    let t = LEAD; const cues = [], lens = [];
    for(const l of sc.lines){ const d = words(l)/WPS; cues.push(t); lens.push(d); t += d + PAUSE; }
    const dur = Math.max(sc.min||0, t - PAUSE + TAILPAD);
    return Object.assign({}, sc, {cues, lens, dur});
  });
  // pro Teil auf 180 s skalieren
  for(const p of [1,2,3]){
    const ss = scenes.filter(s => s.part===p), sum = ss.reduce((a,s)=>a+s.dur,0), k = PART_LEN/sum;
    for(const s of ss){ s.dur*=k; s.cues=s.cues.map(c=>c*k); s.lens=s.lens.map(c=>c*k); }
  }
  let T = 0; for(const s of scenes){ s.start = T; T += s.dur; }
  return {scenes, dur: T};
}
const TL = buildTimeline();
