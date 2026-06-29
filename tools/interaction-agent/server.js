// Generic local user-interaction server.
// Reads prompt.json (the question + options), serves a clean page, captures Submit,
// writes response.json, then exits. Reusable for any agent<->human choice.
const http = require('http');
const fs = require('fs');
const path = require('path');

const ROOT = process.cwd();
const PORT = parseInt(process.env.PORT || '8099', 10);
const spec = JSON.parse(fs.readFileSync(path.join(ROOT, 'prompt.json'), 'utf8'));
const ASSETS = path.resolve(ROOT, spec.assetsDir || '.');
const type = spec.type || 'single';

const esc = s => String(s ?? '').replace(/[&<>"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
const opt = o => `<div class="card" data-id="${esc(o.id)}" onclick="pick('${esc(o.id)}')">
  ${o.image ? `<img src="/assets/${encodeURIComponent(o.image)}">` : ''}
  <h3>${esc(o.label || o.id)}</h3><div class="desc">${esc(o.desc || '')}</div></div>`;
const body = type === 'text'
  ? `<textarea id="t" rows="6" placeholder="Type your answer..."></textarea>`
  : `<div class="grid">${(spec.options || []).map(opt).join('')}</div>`;

const page = `<!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>${esc(spec.title || 'Choose')}</title><style>
*{box-sizing:border-box;margin:0;padding:0}body{font-family:Segoe UI,Arial;background:#05080f;color:#e9eefb;padding:36px}
h1{font-size:28px;font-weight:800}h1 span{background:linear-gradient(90deg,#37c2ff,#ff7a1a);-webkit-background-clip:text;color:transparent}
.sub{color:#8aa0c8;margin:8px 0 18px}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:20px;max-width:1180px}
.card{background:#0d1526;border:2px solid #1b2a4a;border-radius:18px;padding:16px;text-align:center;cursor:pointer;transition:.2s}
.card:hover{border-color:#2f7bff;transform:translateY(-4px)}.card.sel{border-color:#ff7a1a;box-shadow:0 0 0 3px #ff7a1a55}
.card img{width:190px;height:190px;border-radius:34px;object-fit:contain}h3{margin:12px 0 4px}.desc{color:#8aa0c8;font-size:13px;min-height:30px}
textarea{width:100%;max-width:760px;background:#0d1526;border:2px solid #1b2a4a;border-radius:12px;color:#e9eefb;padding:14px;font-size:15px}
#bar{position:sticky;top:0;background:#05080fcc;backdrop-filter:blur(6px);padding:12px;margin:0 0 16px;display:flex;gap:14px;align-items:center;z-index:5}
b{color:#ffb020}button{background:#ff7a1a;border:0;color:#fff;font-weight:700;padding:10px 24px;border-radius:10px;cursor:pointer}button:disabled{opacity:.4}</style></head>
<body><h1><span>${esc(spec.title||'Choose')}</span></h1><p class="sub">${esc(spec.subtitle||'')}</p>
<div id="bar">Selected: <b id="cur">none</b><button id="go" ${type==='text'?'':'disabled'} onclick="go()">${esc(spec.submitLabel||'Submit')}</button></div>
${body}
<script>const M=${type==='multi'};let one=null;const set=new Set();
function pick(id){if(M){set.has(id)?set.delete(id):set.add(id);document.querySelectorAll('.card').forEach(c=>c.classList.toggle('sel',set.has(c.dataset.id)));document.getElementById('cur').textContent=[...set].join(', ')||'none';document.getElementById('go').disabled=!set.size;}
else{one=id;document.querySelectorAll('.card').forEach(c=>c.classList.toggle('sel',c.dataset.id===id));document.getElementById('cur').textContent=id;document.getElementById('go').disabled=false;}}
function go(){const t='${type}';const p=t==='text'?{text:document.getElementById('t').value}:t==='multi'?{choices:[...set]}:{choice:one};
fetch('/submit',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(p)}).then(()=>{document.body.innerHTML='<h1>Thanks — sent to the agent. You can close this tab.</h1>';});}</script></body></html>`;

http.createServer((req,res)=>{
  if(req.url==='/'){res.writeHead(200,{'Content-Type':'text/html'});return res.end(page);}
  if(req.url.startsWith('/assets/')){const f=path.join(ASSETS, decodeURIComponent(req.url.slice(8)));
    if(f.startsWith(ASSETS)&&fs.existsSync(f)){const ext=path.extname(f).slice(1);res.writeHead(200,{'Content-Type':ext==='svg'?'image/svg+xml':'image/'+ext});return res.end(fs.readFileSync(f));}res.writeHead(404);return res.end();}
  if(req.url==='/submit'&&req.method==='POST'){let b='';req.on('data',c=>b+=c);req.on('end',()=>{fs.writeFileSync(path.join(ROOT,'response.json'),b);res.writeHead(200);res.end('ok');console.log('RESPONSE:'+b);setTimeout(()=>process.exit(0),400);});return;}
  res.writeHead(404);res.end();
}).listen(PORT,()=>console.log('Interaction page at http://localhost:'+PORT));
