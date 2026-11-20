import json,wave,numpy as np,re
SR=48000
W=json.load(open('words.json'))
cues={'1':['morgens','mit dem','abends','und plötzlich','für','genau','es ist','bis'],
      '2':['das','beim','gleichzeitig','wie','stg7','erstens','zweitens'],
      '3':['drittens','warum','krankenhäuser','schulen','und fabriken','was','die heizung','oder','jede']}
starts=[0.3,5,10,15,20.6,26,32,38.3,45.2,52,60.2,67.2,75.3,80,92,106,120.2,127.5,134.5,141.5,150.2,154.6,159,164.4]
ends=starts[1:]+[169]
def load(k):
    w=wave.open(f'voice/clean{k}.wav'); return np.frombuffer(w.readframes(w.getnframes()),np.int16).astype(np.float32)/32768
track=np.zeros(170*SR,np.float32)
idx=0
for k in '123':
    a=load(k); words=W[k]; norm=[re.sub(r'[^a-zäöüß0-9]','',x[2].lower()) for x in words]
    pos=[];j=0
    for c in cues[k]:
        cc=c.split()
        while not all(norm[j+i].startswith(cc[i]) for i in range(len(cc))): j+=1
        pos.append(j); j+=1
    for n,p in enumerate(pos):
        q=pos[n+1]-1 if n+1<len(pos) else len(words)-1
        s=max(0,words[p][0]-0.10); e=words[q][1]+0.25
        if n+1<len(pos): e=min(e,words[pos[n+1]][0]-0.02)
        if p>0: s=max(s,words[p-1][1]+0.02)
        clip=a[int(s*SR):int(e*SR)].copy()
        fl=int(0.015*SR); clip[:fl]*=np.linspace(0,1,fl); clip[-fl:]*=np.linspace(1,0,fl)
        at=starts[idx]+0.15; d=len(clip)/SR
        print(f'{idx:2d} @{at:6.1f}s  {d:4.1f}s / Platz {ends[idx]-at:4.1f}s  {"".join(x[2] for x in words[p:q+1]).strip()[:60]}')
        i=int(at*SR); track[i:i+len(clip)]+=clip; idx+=1
d=(np.clip(track,-1,1)*32767).astype(np.int16)
w=wave.open('voice/voice_track.wav','wb'); w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(d.tobytes()); w.close()
