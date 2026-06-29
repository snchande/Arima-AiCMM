/* AiCMM FAA — Floating Agentic Assistance.
   Page-aware help across the site. Uses a local LLM CLI (agentic) when available,
   else a built-in offline knowledge base. Settings let you pick the CLI, model and temperature. */
(function () {
  var PAGES = {
    'home':        { title: 'Home',        tips: ['What is AiCMM?', 'Show me the 12 dimensions', 'How do I score an agent?'] },
    'framework':   { title: 'Framework',   tips: ['Explain the 3 dimension groups', 'What is the agency layer?', 'List governance rules'] },
    'architecture':{ title: 'Architecture',tips: ['How do modules fit together?', 'Where is scoring implemented?'] },
    'catalog':     { title: 'Catalog',     tips: ['Which agent scores highest on tool use?', 'Compare two cards'] },
    'cards':       { title: 'Agent Card',  tips: ['Explain this card\'s footprint', 'Does it pass governance?'] },
    'create':      { title: 'Create Card', tips: ['Create a card from a brochure', 'Score my agent and show the radar'] },
    'schema':      { title: 'Schema',      tips: ['Explain a card field', 'Show a minimal valid card'] },
    'brochure':    { title: 'Brochure',    tips: ['Summarize AiCMM', 'How is the Agency Index computed?'] },
    'user-guide':  { title: 'Guide',       tips: ['Walk me through creating my first card', 'CLI prompts to use'] },
    'release-notes':{title: 'Releases',    tips: ['What changed recently?'] }
  };
  function detectPage() {
    var a = document.querySelector('.nav-links a.active');
    if (a) { return (a.getAttribute('href') || '/').replace(/^\//, '') || 'home'; }
    return (location.pathname.replace(/^\//, '') || 'home');
  }
  var pageKey = detectPage();
  var page = PAGES[pageKey] || { title: document.title, tips: ['How can AiCMM help on this page?'] };
  var catalogue = null; // {providers, settings, active, activeAgentic}

  // ---- build widget ----
  var faa = document.createElement('button');
  faa.id = 'aicmm-faa'; faa.title = 'Ask the AiCMM assistant';
  faa.innerHTML = '<img src="/img/aicmm-icon.svg" alt="AiCMM">';

  var panel = document.createElement('div'); panel.id = 'aicmm-panel';
  panel.innerHTML =
    '<div class="faa-head"><img src="/img/aicmm-icon.svg">'+
      '<span class="t">AiCMM Assistant</span><span class="pg">· ' + page.title + '</span>'+
      '<button class="ic gear" title="Settings">&#9881;</button>'+
      '<button class="ic x" title="Close">&times;</button></div>'+
    '<div class="faa-status" id="faa-status">Checking assistant…</div>'+
    '<div class="faa-body" id="faa-body"></div>'+
    '<div class="faa-settings" id="faa-settings" hidden></div>'+
    '<div class="faa-foot"><textarea id="faa-in" rows="2" placeholder="Ask about this page or your agents…"></textarea>'+
      '<button id="faa-send">Send</button></div>';
  document.body.appendChild(faa); document.body.appendChild(panel);

  var body = panel.querySelector('#faa-body');
  var statusEl = panel.querySelector('#faa-status');
  var settingsEl = panel.querySelector('#faa-settings');

  function add(text, cls){ var d=document.createElement('div'); d.className='faa-msg '+cls; d.textContent=text; body.appendChild(d); body.scrollTop=body.scrollHeight; return d; }
  add('Hi! I understand the AiCMM platform and the "' + page.title + '" page. Ask anything or pick a suggestion.', 'bot');
  var chips=document.createElement('div'); chips.className='faa-chips';
  page.tips.forEach(function(t){ var b=document.createElement('button'); b.textContent=t; b.onclick=function(){ send(t); }; chips.appendChild(b); });
  body.appendChild(chips);

  function esc(s){ var d=document.createElement('div'); d.textContent=s==null?'':s; return d.innerHTML; }

  // ---- status + settings ----
  function renderStatus(){
    if(!catalogue){ statusEl.textContent='Checking assistant…'; return; }
    var act=null; catalogue.providers.forEach(function(p){ if(p.id===catalogue.active) act=p; });
    var label = act ? act.label : catalogue.active;
    var agentic = catalogue.activeAgentic;
    statusEl.innerHTML = '<span class="dot '+(agentic?'on':'off')+'"></span>'+
      (agentic ? 'Agentic · ' : 'Offline · ') + '<strong>'+esc(label)+'</strong>'+
      ' <button class="link" id="faa-pick">change</button>';
    var pick=statusEl.querySelector('#faa-pick'); if(pick) pick.onclick=openSettings;
  }

  function loadCatalogue(){
    return fetch('/api/assist/providers').then(function(r){return r.json();}).then(function(j){ catalogue=j; renderStatus(); }).catch(function(){ statusEl.textContent='Assistant status unavailable.'; });
  }

  function openSettings(){
    if(!catalogue){ return; }
    var s = catalogue.settings || {provider:'auto',model:'',temperature:null};
    var html = '<div class="set-h">Assistant settings</div>'+
      '<div class="set-sec"><div class="set-l">Default CLI / provider</div>';
    html += providerRow('auto','Auto — first available CLI, else offline', true, true, s.provider==='auto');
    catalogue.providers.forEach(function(p){
      html += providerRow(p.id, p.label, p.available, p.agentic, s.provider===p.id);
    });
    html += '</div>';
    html += '<div class="set-sec" id="set-model-sec"><div class="set-l">Model</div>'+
      '<select id="set-model"></select>'+
      '<input id="set-model-custom" placeholder="custom model id (optional)" value="'+esc(s.model||'')+'"></div>';
    html += '<div class="set-sec" id="set-temp-sec"><div class="set-l">Temperature <span id="set-temp-val"></span></div>'+
      '<input type="range" id="set-temp" min="0" max="1" step="0.1"></div>';
    html += '<div class="set-actions"><button id="set-save">Save</button><button id="set-cancel" class="ghost">Cancel</button></div>';
    html += '<div class="set-note">No CLI? Install one — e.g. <code>npm install -g @github/copilot</code> — then pick it here. Other CLIs are a call for contributions.</div>';
    settingsEl.innerHTML = html;
    body.hidden = true; settingsEl.hidden = false;

    settingsEl.querySelectorAll('input[name=faa-prov]').forEach(function(r){ r.onchange=refreshModelTemp; });
    settingsEl.querySelector('#set-cancel').onclick=closeSettings;
    settingsEl.querySelector('#set-save').onclick=saveSettings;
    var tr=settingsEl.querySelector('#set-temp'); tr.value = (s.temperature==null?0.7:s.temperature);
    tr.oninput=function(){ settingsEl.querySelector('#set-temp-val').textContent=tr.value; };
    refreshModelTemp();
  }
  function providerRow(id,label,available,agentic,checked){
    var badge = (id==='auto') ? '' : (available ? '<span class="b ok">available</span>' : '<span class="b no">not installed</span>');
    var tag = agentic ? '<span class="b ag">agentic</span>' : (id==='auto'?'':'<span class="b of">offline</span>');
    return '<label class="prov'+(checked?' sel':'')+'">'+
      '<input type="radio" name="faa-prov" value="'+id+'"'+(checked?' checked':'')+'>'+
      '<span class="pn">'+esc(label)+'</span>'+badge+tag+'</label>';
  }
  function selectedProvider(){
    var r=settingsEl.querySelector('input[name=faa-prov]:checked'); return r?r.value:'auto';
  }
  function providerById(id){ var f=null; (catalogue.providers||[]).forEach(function(p){ if(p.id===id) f=p; }); return f; }
  function refreshModelTemp(){
    var id=selectedProvider();
    var p = id==='auto' ? null : providerById(id);
    var modelSec=settingsEl.querySelector('#set-model-sec'), tempSec=settingsEl.querySelector('#set-temp-sec');
    var sel=settingsEl.querySelector('#set-model');
    var models = p && p.models ? p.models : [];
    modelSec.style.display = (id==='auto') ? 'none' : 'block';
    sel.innerHTML='<option value="">(provider default)</option>';
    models.forEach(function(m){ var o=document.createElement('option'); o.value=m; o.textContent=m; sel.appendChild(o); });
    var cur=(catalogue.settings&&catalogue.settings.model)||'';
    if(cur && models.indexOf(cur)>=0) sel.value=cur;
    tempSec.style.display = (p && p.supportsTemperature) ? 'block' : 'none';
    settingsEl.querySelector('#set-temp-val').textContent=settingsEl.querySelector('#set-temp').value;
  }
  function closeSettings(){ settingsEl.hidden=true; body.hidden=false; }
  function saveSettings(){
    var id=selectedProvider();
    var sel=settingsEl.querySelector('#set-model');
    var custom=settingsEl.querySelector('#set-model-custom').value.trim();
    var model = custom || sel.value || '';
    var p = id==='auto'?null:providerById(id);
    var temp = (p && p.supportsTemperature) ? parseFloat(settingsEl.querySelector('#set-temp').value) : null;
    var payload={provider:id, model:model, temperature:temp};
    fetch('/api/assist/settings',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)})
      .then(function(r){return r.json();}).then(function(){ return loadCatalogue(); })
      .then(function(){ closeSettings(); var act=providerById(catalogue.active); add('Settings saved — using '+(act?act.label:catalogue.active)+'.','tip'); });
  }

  // ---- open/close + send ----
  function toggle(open){ panel.classList.toggle('open', open); if(open){ panel.querySelector('#faa-in').focus(); if(!catalogue) loadCatalogue(); } }
  faa.onclick=function(){ toggle(!panel.classList.contains('open')); };
  panel.querySelector('.x').onclick=function(){ toggle(false); };
  panel.querySelector('.gear').onclick=function(){ if(settingsEl.hidden) openSettings(); else closeSettings(); };

  function send(text){
    var inp=panel.querySelector('#faa-in');
    var msg=(text||inp.value).trim(); if(!msg) return;
    inp.value=''; if(!settingsEl.hidden) closeSettings(); add(msg,'user');
    var sb=panel.querySelector('#faa-send'); sb.disabled=true; var w=add('Thinking…','tip');
    fetch('/api/assist',{method:'POST',headers:{'Content-Type':'application/json'},
      body:JSON.stringify({page:pageKey,url:location.pathname,question:msg})})
      .then(function(r){return r.json();}).then(function(j){
        w.remove();
        add(j.answer||j.error||'No response','bot');
        if(j.provider){ var tag=document.createElement('div'); tag.className='faa-by';
          tag.textContent=(j.agentic?'via ':'offline · ')+(j.providerLabel||j.provider)+(j.fellBack?' (fallback)':''); body.appendChild(tag); }
        if(j.agentic===false && catalogue){ catalogue.active=j.provider; catalogue.activeAgentic=false; renderStatus(); }
      })
      .catch(function(e){ w.remove(); add('Assistant unavailable: '+e,'bot'); })
      .finally(function(){ sb.disabled=false; body.scrollTop=body.scrollHeight; });
  }
  panel.querySelector('#faa-send').onclick=function(){ send(); };
  panel.querySelector('#faa-in').addEventListener('keydown',function(e){ if(e.key==='Enter'&&!e.shiftKey){e.preventDefault();send();} });

  loadCatalogue();
})();
