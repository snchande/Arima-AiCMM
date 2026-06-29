// Generates 6 AiCMM icon variants as SVG + PNG, plus a picker gallery.
const sharp = require('sharp');
const fs = require('fs');

const C = 256;
const dims = (cx, cy, R, vals) => vals.map((v,i)=>{const a=-Math.PI/2+i*Math.PI/6;const r=R*v/5;return [ (cx+r*Math.cos(a)).toFixed(1),(cy+r*Math.sin(a)).toFixed(1) ];});
const profile = [4.8,4.2,3.6,3.1,4.6,3.9,3.0,4.9,2.4,2.7,3.1,4.4];

const defs = `<defs>
 <radialGradient id="bg" cx="50%" cy="44%" r="78%"><stop offset="0%" stop-color="#1b2a4a"/><stop offset="55%" stop-color="#0d1526"/><stop offset="100%" stop-color="#05080f"/></radialGradient>
 <linearGradient id="grad" x1="0" y1="1" x2="1" y2="0"><stop offset="0%" stop-color="#2f7bff"/><stop offset="42%" stop-color="#37c2ff"/><stop offset="74%" stop-color="#ffb020"/><stop offset="100%" stop-color="#ff6a18"/></linearGradient>
 <linearGradient id="orbit" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#37c2ff"/><stop offset="100%" stop-color="#ff7a1a"/></linearGradient>
 <radialGradient id="light" cx="50%" cy="42%" r="80%"><stop offset="0%" stop-color="#ffffff"/><stop offset="100%" stop-color="#e9eefb"/></radialGradient>
 <filter id="glow" x="-40%" y="-40%" width="180%" height="180%"><feGaussianBlur stdDeviation="5" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
</defs>`;

function rings(R){let s='';for(let i=1;i<=5;i++)s+=`<circle cx="256" cy="256" r="${(R*i/5).toFixed(0)}"/>`;return `<g stroke="#3a4d72" stroke-opacity="0.4" fill="none" stroke-width="1.5">${s}</g>`;}
function spokes(R){let s='';for(let i=0;i<12;i++){const a=-Math.PI/2+i*Math.PI/6;s+=`<line x1="256" y1="256" x2="${(256+R*Math.cos(a)).toFixed(0)}" y2="${(256+R*Math.sin(a)).toFixed(0)}"/>`;}return `<g stroke="#46598099" stroke-width="1.5">${s}</g>`;}
const poly=(R)=>dims(256,256,R,profile).map(p=>p.join(',')).join(' ');
const verts=(R,c='#fff')=>dims(256,256,R,profile).map(p=>`<circle cx="${p[0]}" cy="${p[1]}" r="6" fill="${c}"/>`).join('');

// A: Orbit + radar (signature)
const A=`<svg viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg">${defs}
<rect x="8" y="8" width="496" height="496" rx="112" fill="url(#bg)"/>${rings(188)}${spokes(188)}
<polygon filter="url(#glow)" points="${poly(190)}" fill="url(#grad)" fill-opacity="0.32" stroke="url(#grad)" stroke-width="6" stroke-linejoin="round"/>${verts(190)}
<ellipse cx="256" cy="256" rx="214" ry="80" fill="none" stroke="url(#orbit)" stroke-width="8" stroke-opacity="0.85" transform="rotate(-22 256 256)"/>
<circle cx="256" cy="256" r="34" fill="#0d1526" stroke="url(#grad)" stroke-width="4"/><circle cx="256" cy="256" r="9" fill="#ffb020"/></svg>`;

// B: Flat radar, no orbit, minimal
const B=`<svg viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg">${defs}
<rect x="8" y="8" width="496" height="496" rx="112" fill="url(#bg)"/>${rings(200)}
<polygon points="${poly(202)}" fill="url(#grad)" fill-opacity="0.4" stroke="url(#grad)" stroke-width="8" stroke-linejoin="round"/>${verts(202)}
<circle cx="256" cy="256" r="14" fill="#fff"/></svg>`;

// C: Maturity ring of 12 nodes
const nodes=()=>{let s='';for(let i=0;i<12;i++){const a=-Math.PI/2+i*Math.PI/6;const x=256+160*Math.cos(a),y=256+160*Math.sin(a);s+=`<circle cx="${x.toFixed(0)}" cy="${y.toFixed(0)}" r="${10+ (i<4?8:i<7?5:6)}" fill="url(#grad)"/>`;}return s;};
const C2=`<svg viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg">${defs}
<rect x="8" y="8" width="496" height="496" rx="112" fill="url(#bg)"/>
<circle cx="256" cy="256" r="160" fill="none" stroke="url(#grad)" stroke-width="10" stroke-opacity="0.6"/>${nodes()}
<text x="256" y="290" text-anchor="middle" font-family="Segoe UI,Arial" font-size="130" font-weight="800" fill="#fff">Ai</text></svg>`;

// D: Light theme radar
const D=`<svg viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg">${defs}
<rect x="8" y="8" width="496" height="496" rx="112" fill="url(#light)"/>
<g stroke="#c7d2e8" fill="none" stroke-width="1.5"><circle cx="256" cy="256" r="40"/><circle cx="256" cy="256" r="80"/><circle cx="256" cy="256" r="120"/><circle cx="256" cy="256" r="160"/><circle cx="256" cy="256" r="200"/></g>
<polygon points="${poly(202)}" fill="url(#grad)" fill-opacity="0.45" stroke="url(#grad)" stroke-width="8" stroke-linejoin="round"/>${verts(202,'#2f7bff')}
<circle cx="256" cy="256" r="12" fill="#ff6a18"/></svg>`;

// E: Hexagon agent core + orbit
const hexv=(R)=>{let p=[];for(let i=0;i<6;i++){const a=-Math.PI/2+i*Math.PI/3;p.push([256+R*Math.cos(a),256+R*Math.sin(a)]);}return p.map(x=>x.map(n=>n.toFixed(0)).join(',')).join(' ');};
const E=`<svg viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg">${defs}
<rect x="8" y="8" width="496" height="496" rx="112" fill="url(#bg)"/>
<ellipse cx="256" cy="256" rx="206" ry="74" fill="none" stroke="url(#orbit)" stroke-width="9" transform="rotate(20 256 256)" stroke-opacity="0.8"/>
<polygon points="${hexv(150)}" fill="url(#grad)" fill-opacity="0.3" stroke="url(#grad)" stroke-width="8" stroke-linejoin="round"/>
<polygon points="${poly(120)}" fill="none" stroke="#fff" stroke-opacity="0.85" stroke-width="4"/>${verts(120)}
<circle cx="256" cy="256" r="12" fill="#ffb020"/></svg>`;

// F: Step ladder maturity + radar
const F=`<svg viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg">${defs}
<rect x="8" y="8" width="496" height="496" rx="112" fill="url(#bg)"/>${rings(180)}${spokes(180)}
<polygon filter="url(#glow)" points="${poly(182)}" fill="url(#grad)" fill-opacity="0.35" stroke="url(#grad)" stroke-width="7" stroke-linejoin="round"/>${verts(182)}
<g fill="url(#grad)" opacity="0.95"><rect x="150" y="300" width="40" height="40" rx="6"/><rect x="200" y="270" width="40" height="70" rx="6"/><rect x="250" y="235" width="40" height="105" rx="6"/><rect x="300" y="195" width="40" height="145" rx="6"/></g>
<circle cx="256" cy="256" r="10" fill="#fff"/></svg>`;

const variants={A,B,C2,D,E,F};
(async()=>{
 for(const [k,svg] of Object.entries(variants)){
   fs.writeFileSync(`variant-${k}.svg`, svg);
   await sharp(Buffer.from(svg),{density:384}).resize(512,512).png().toFile(`variant-${k}.png`);
 }
 console.log('rendered');
})();
