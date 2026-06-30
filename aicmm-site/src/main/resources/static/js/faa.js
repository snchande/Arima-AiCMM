/* AiCMM FAA — Floating Agentic Assistance.
   Page-aware help across the site. The assistant can also act on the page you are looking at:
     • aicmm-fill   — populate form fields (any page that has them)
     • aicmm-edit   — reword / replace visible text live
     • aicmm-reload — refresh after a persistent source edit (Develop mode)
   Modes: Assist (default) and Develop & Extend (revealed in power-user mode).
   Auto-tucks on outside click unless pinned. The Assistant Engine panel picks the CLI, model
   and generation tuning (temperature / top-p / max tokens) the chosen CLI supports. */
(function () {
  var DIMS = ['autonomy','reasoning','memory','learning','toolUse','collaboration','embodiment',
              'explainability','safety','interoperability','costEfficiency','domainAlignment'];
  var PAGES = {
    'home':        { title: 'Home',        tips: ['What is AiCMM?', 'Show me the 12 dimensions', 'How do I score an agent?'] },
    'framework':   { title: 'Framework',   tips: ['Explain the 3 dimension groups', 'What is the agency layer?', 'List governance rules'] },
    'architecture':{ title: 'Architecture',tips: ['How do modules fit together?', 'Where is scoring implemented?'] },
    'catalog':     { title: 'Catalog',     tips: ['Which agent scores highest on tool use?', 'Compare two cards'] },
    'cards':       { title: 'Agent Card',  tips: ['Explain this card\'s footprint', 'Does it pass governance?'] },
    'create':      { title: 'Create Card', tips: ['Fill this form with a sample card', 'Score my agent and show the radar'] },
    'schema':      { title: 'Schema',      tips: ['Explain a card field', 'Show a minimal valid card'] },
    'brochure':    { title: 'Brochure',    tips: ['Summarize AiCMM', 'How is the Agency Index computed?'] },
    'user-guide':  { title: 'Guide',       tips: ['Walk me through creating my first card', 'CLI prompts to use'] },
    'release-notes':{title: 'Releases',    tips: ['What changed recently?'] }
  };
  var CONTRIB = [
    { label: '🛠 Make a code change', prompt: 'I want to make a code change to AiCMM. If anything is unclear, ask me what to change, then implement it, build with Maven, and restart the site.' },
    { label: '📝 Improve documentation', prompt: 'Help me improve AiCMM documentation. Ask what to improve or where, then make the edit and show me the diff.' },
    { label: '⭐ Rate an agent / create a card', prompt: 'Help me rate an agent and create an AiCMM Agent Card. Ask me for the agent details or a doc/URL, then write examples/<name>-agent-card.json and validate it.' },
    { label: '🔀 Open a PR with my changes', prompt: 'Review my current local changes (git status and diff) and summarize them. Then run scripts/run-foundational-tests.ps1, and only if it passes create a feature branch, commit with the Copilot co-author trailer, push, and open a PR with gh — paste the test summary into the PR and confirm with me before pushing.' }
  ];
  function detectPage() {
    var a = document.querySelector('.nav-links a.active');
    if (a) { return (a.getAttribute('href') || '/').replace(/^\//, '') || 'home'; }
    return (location.pathname.replace(/^\//, '') || 'home');
  }
  var pageKey = detectPage();
  var page = PAGES[pageKey] || { title: document.title, tips: ['How can AiCMM help on this page?'] };
  var catalogue = null;
  var pinned = localStorage.getItem('faa-pin') === '1';
  var mode = localStorage.getItem('faa-mode') || 'assist';
  var powerUser = false;
  var history = [];

  // ---- build widget ----
  var faa = document.createElement('button');
  faa.id = 'aicmm-faa'; faa.title = 'Ask the AiCMM assistant';
  faa.innerHTML = '<img src="/img/aicmm-icon.svg" alt="AiCMM">';

  var panel = document.createElement('div'); panel.id = 'aicmm-panel';
  panel.innerHTML =
    '<div class="faa-head"><img src="/img/aicmm-icon.svg">'+
      '<span class="t">AiCMM Assistant</span><span class="pg">· ' + page.title + '</span>'+
      '<button class="ic pin" title="Pin (keep open)">&#128204;</button>'+
      '<button class="ic gear" title="Assistant Engine">&#9881;</button>'+
      '<button class="ic x" title="Close">&times;</button></div>'+
    '<div class="faa-modes" id="faa-modes"><button data-m="assist" class="mbtn">Assist</button>'+
      '<button data-m="develop" class="mbtn">Develop &amp; Extend</button></div>'+
    '<div class="faa-status" id="faa-status">Checking assistant…</div>'+
    '<div class="faa-body" id="faa-body"></div>'+
    '<div class="faa-settings" id="faa-settings" hidden></div>'+
    '<div class="faa-foot"><textarea id="faa-in" rows="2" placeholder="Ask about this page, or tell me to fill / reword it…"></textarea>'+
      '<button id="faa-send">Send</button></div>';
  document.body.appendChild(faa); document.body.appendChild(panel);

  var body = panel.querySelector('#faa-body');
  var statusEl = panel.querySelector('#faa-status');
  var settingsEl = panel.querySelector('#faa-settings');
  var modesEl = panel.querySelector('#faa-modes');
  var pinBtn = panel.querySelector('.pin');

  function esc(s){ var d=document.createElement('div'); d.textContent=s==null?'':s; return d.innerHTML; }
  function add(text, cls){ var d=document.createElement('div'); d.className='faa-msg '+cls; d.textContent=text; body.appendChild(d); body.scrollTop=body.scrollHeight; return d; }

  // ================= PAGE ACTION ENGINE =================
  // Skip elements that belong to the FAA widget itself.
  function inWidget(el){ return panel.contains(el) || faa.contains(el); }
  function labelFor(el){
    if (el.id){ var l=document.querySelector('label[for="'+el.id+'"]'); if(l && l.textContent.trim()) return l.textContent.trim().slice(0,60); }
    var wrap = el.closest('label'); if (wrap && wrap.textContent.trim()) return wrap.textContent.trim().slice(0,60);
    return el.getAttribute('placeholder') || el.name || el.id || '';
  }
  // Build a compact schema of fillable fields on the current page.
  function fieldSchema(){
    var fields = [];
    document.querySelectorAll('input, select, textarea').forEach(function(el){
      if (inWidget(el)) return;
      var t = (el.type||'').toLowerCase();
      if (t==='hidden' || t==='submit' || t==='button' || t==='file' || t==='password') return;
      if (el.disabled || el.offsetParent === null) return;        // skip hidden/disabled
      if (el.id && /^score-/.test(el.id)) return;                 // scores handled separately
      var key = el.name || el.id; if (!key) return;
      var f = { key: key, kind: el.tagName.toLowerCase()+(t?(':'+t):''), label: labelFor(el) };
      if (el.tagName.toLowerCase()==='select'){ f.options = Array.prototype.map.call(el.options, function(o){return o.value;}).filter(Boolean).slice(0,20); }
      fields.push(f);
    });
    var scores = DIMS.filter(function(d){ return document.getElementById('score-'+d); });
    return { fields: fields.slice(0, 50), scores: scores };
  }
  function hasFillable(){ var s=fieldSchema(); return s.fields.length>0 || s.scores.length>0; }
  function schemaForServer(){
    if (!hasFillable()) return '';
    var s = fieldSchema();
    var lines = [];
    if (s.fields.length){
      lines.push('Form inputs (key — type — label' + ' / options):');
      s.fields.forEach(function(f){ lines.push('  • '+f.key+' — '+f.kind+(f.label?(' — '+f.label):'')+(f.options&&f.options.length?(' — options: '+f.options.join(', ')):'')); });
    }
    if (s.scores.length){ lines.push('Score sliders (0-5) — use the "scores" object: '+s.scores.join(', ')); }
    return lines.join('\n');
  }

  // ----- animated filling: type into each field like a human, field by field -----
  var TYPE_MS = 32;        // per-character typing delay
  var FIELD_GAP_MS = 260;  // pause between fields
  var SCORE_STEP_MS = 130; // per-step delay when ramping a 0-5 slider
  function wait(ms){ return new Promise(function(r){ setTimeout(r, ms); }); }
  function findField(key){
    return document.querySelector('[name="'+(window.CSS&&CSS.escape?CSS.escape(key):key)+'"]') || document.getElementById(key);
  }
  function focusField(el){
    try{ el.scrollIntoView({behavior:'smooth', block:'center'}); }catch(e){}
    try{ el.focus({preventScroll:true}); }catch(e){ try{ el.focus(); }catch(_){} }
    el.classList.add('aicmm-typing');
  }
  function blurField(el){ el.classList.remove('aicmm-typing'); }
  function typeText(el, value){
    value = String(value);
    el.value = '';
    el.dispatchEvent(new Event('input', {bubbles:true}));
    var i = 0;
    return new Promise(function(resolve){
      (function step(){
        if (i >= value.length){
          el.dispatchEvent(new Event('change', {bubbles:true}));
          return resolve();
        }
        el.value = value.slice(0, ++i);
        el.dispatchEvent(new Event('input', {bubbles:true}));
        try{ el.setSelectionRange(el.value.length, el.value.length); }catch(e){}
        setTimeout(step, TYPE_MS);
      })();
    });
  }
  function fillFieldAnimated(key, value){
    var el = findField(key);
    if (!el || inWidget(el)) return Promise.resolve(false);
    var tag = el.tagName.toLowerCase();
    focusField(el);
    return wait(140).then(function(){
      if (tag === 'select'){
        // Match case-insensitively against option value OR visible text. Also try a
        // loose contains match so e.g. "Digital Assistant" maps to the "digital" option.
        var want = String(value == null ? '' : value).trim().toLowerCase();
        var matched = false;
        Array.prototype.forEach.call(el.options, function(o){
          if (matched || !o.value) return;
          var ov = o.value.trim().toLowerCase(), ot = o.textContent.trim().toLowerCase();
          if (ov === want || ot === want){ el.value = o.value; matched = true; }
        });
        if (!matched){
          Array.prototype.forEach.call(el.options, function(o){
            if (matched || !o.value || !want) return;
            var ov = o.value.trim().toLowerCase(), ot = o.textContent.trim().toLowerCase();
            if (want.indexOf(ov) >= 0 || ov.indexOf(want) >= 0 || want.indexOf(ot) >= 0){ el.value = o.value; matched = true; }
          });
        }
        // If still unmatched, KEEP the current/default option — never set an unknown
        // value, which would blank the dropdown (the reported "blank box" bug).
        if (!matched && el.selectedIndex < 0){
          var firstReal = Array.prototype.filter.call(el.options, function(o){ return o.value; })[0];
          if (firstReal) el.value = firstReal.value;
        }
        el.dispatchEvent(new Event('input', {bubbles:true}));
        el.dispatchEvent(new Event('change', {bubbles:true}));
        return wait(120);
      }
      if (el.type === 'checkbox'){
        el.checked = !!value && value!=='false';
        el.dispatchEvent(new Event('change', {bubbles:true}));
        return wait(120);
      }
      return typeText(el, value);
    }).then(function(){ return wait(FIELD_GAP_MS); }).then(function(){ blurField(el); return true; });
  }
  function animateScore(dim, val){
    var el = document.getElementById('score-'+dim);
    if (!el) return Promise.resolve(false);
    var target = Math.max(0, Math.min(5, parseInt(val, 10) || 0));
    focusField(el);
    return wait(140).then(function(){
      return new Promise(function(resolve){
        var cur = parseInt(el.value, 10) || 0;
        var dir = target > cur ? 1 : -1;
        (function ramp(){
          if (cur === target) return resolve();
          cur += dir; el.value = cur;
          if (typeof window.updateScoreDisplay === 'function'){ try{ window.updateScoreDisplay(el); }catch(e){} }
          else { var d=document.getElementById('display-'+dim); if(d) d.textContent=cur; }
          el.dispatchEvent(new Event('input', {bubbles:true}));
          setTimeout(ramp, SCORE_STEP_MS);
        })();
      });
    }).then(function(){ return wait(FIELD_GAP_MS/2); }).then(function(){ blurField(el); return true; });
  }
  // Play the whole fill as a sequence so the user watches it "type" field by field.
  function animateFill(obj){
    var chain = Promise.resolve();
    if (obj && obj.fields){
      Object.keys(obj.fields).forEach(function(k){ chain = chain.then(function(){ return fillFieldAnimated(k, obj.fields[k]); }); });
    }
    if (obj && obj.scores){
      Object.keys(obj.scores).forEach(function(k){ chain = chain.then(function(){ return animateScore(k, obj.scores[k]); }); });
      chain = chain.then(function(){ if (typeof window.previewRadar==='function'){ try{ window.previewRadar(); }catch(e){} } });
    }
    return chain;
  }
  function countFillable(obj){
    var n = 0;
    if (obj && obj.fields){ Object.keys(obj.fields).forEach(function(k){ if(findField(k)) n++; }); }
    if (obj && obj.scores){ Object.keys(obj.scores).forEach(function(k){ if(document.getElementById('score-'+k)) n++; }); }
    return n;
  }
  function applyFill(obj){
    var n = countFillable(obj);
    animateFill(obj);   // fire-and-forget: the typing plays out after we report the count
    return n;
  }
  // Live reword: replace text within individual text nodes (skips the FAA widget + script/style).
  function rewordText(find, replace){
    if (!find) return 0;
    var count=0, walker=document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
      acceptNode: function(node){
        if (!node.nodeValue || node.nodeValue.indexOf(find)<0) return NodeFilter.FILTER_REJECT;
        var p=node.parentNode;
        while(p){ if(p===panel||p===faa) return NodeFilter.FILTER_REJECT; if(p.nodeName==='SCRIPT'||p.nodeName==='STYLE'||p.nodeName==='TEXTAREA') return NodeFilter.FILTER_REJECT; p=p.parentNode; }
        return NodeFilter.FILTER_ACCEPT;
      }
    });
    var nodes=[], cur; while((cur=walker.nextNode())) nodes.push(cur);
    nodes.forEach(function(node){ node.nodeValue = node.nodeValue.split(find).join(replace); count++; });
    return count;
  }
  function applyEdit(arr){
    var n=0; (arr||[]).forEach(function(e){ if(e && e.find!=null){ n += rewordText(String(e.find), String(e.replace==null?'':e.replace)); } }); return n;
  }
  // Parse + apply any aicmm-fill / aicmm-edit / aicmm-reload blocks. Returns {text, summary, reload}.
  function applyActions(answer){
    var summary=[], reload=false;
    var cleaned = answer.replace(/```aicmm-(fill|edit|reload)\s*([\s\S]*?)```/g, function(_, type, content){
      content = content.trim();
      try {
        if (type==='fill'){ var n=applyFill(JSON.parse(content)); if(n) summary.push('filled '+n+' field'+(n===1?'':'s')); }
        else if (type==='edit'){ var m=applyEdit(JSON.parse(content)); if(m) summary.push('reworded '+m+' text spot'+(m===1?'':'s')); }
        else if (type==='reload'){ reload=true; }
      } catch(err){ /* leave a hint if a block was malformed */ summary.push('could not apply an '+type+' block'); }
      return '';
    }).trim();
    return { text: cleaned, summary: summary, reload: reload };
  }

  // ---- chips per mode ----
  var chips;
  function renderChips(){
    if(chips) chips.remove();
    chips=document.createElement('div'); chips.className='faa-chips';
    var items;
    if (mode==='develop'){ items = CONTRIB.map(function(c){ return {label:c.label, text:c.prompt}; }); }
    else {
      items = page.tips.map(function(t){ return {label:t, text:t}; });
      if (hasFillable() && pageKey!=='create'){ items.unshift({ label:'✨ Fill this form for me', text:'Fill in the visible form on this page with sensible sample values using the page-fill protocol.' }); }
    }
    items.forEach(function(it){ var b=document.createElement('button'); b.textContent=it.label; b.onclick=function(){ send(it.text); }; chips.appendChild(b); });
    body.appendChild(chips);
  }

  add('Hi! I understand AiCMM and the "' + page.title + '" page. Ask anything — I can also fill forms or reword text right here on the page.', 'bot');
  renderChips();

  // ---- mode (power-user gated) ----
  function applyPowerUser(){
    modesEl.style.display = powerUser ? 'flex' : 'none';
    if (!powerUser && mode!=='assist'){ mode='assist'; localStorage.setItem('faa-mode','assist'); }
    applyMode();
  }
  function applyMode(){
    panel.querySelectorAll('.mbtn').forEach(function(b){ b.classList.toggle('on', b.getAttribute('data-m')===mode); });
    panel.classList.toggle('develop', mode==='develop');
    renderChips();
    renderStatus();
  }
  panel.querySelectorAll('.mbtn').forEach(function(b){
    b.onclick=function(){
      mode=b.getAttribute('data-m'); localStorage.setItem('faa-mode', mode); applyMode();
      if(mode==='develop'){
        var ag = catalogue && catalogue.activeAgentic;
        add(ag ? 'Develop & Extend mode: I can edit code/docs, build, restart, and open PRs. Tell me what to change, or pick an action.'
               : 'Develop & Extend needs an agentic CLI. Install one and pick it in the Assistant Engine (⚙) to let me edit code and open PRs.', 'tip');
      } else { add('Assist mode: page-aware help. I can also fill forms and reword text on this page.', 'tip'); }
    };
  });

  // ---- pin + auto-tuck (CSS rotates + greys the pin) ----
  function applyPin(){ pinBtn.classList.toggle('on', pinned); pinBtn.title = pinned ? 'Pinned — click to unpin (auto-hide)' : 'Pin (keep open)'; }
  pinBtn.onclick=function(){ pinned=!pinned; localStorage.setItem('faa-pin', pinned?'1':'0'); applyPin(); };
  document.addEventListener('mousedown', function(e){
    if(!panel.classList.contains('open') || pinned) return;
    if(panel.contains(e.target) || faa.contains(e.target)) return;
    toggle(false); // silently tuck away
  });

  // ---- status ----
  function renderStatus(){
    if(!catalogue){ statusEl.textContent='Checking assistant…'; return; }
    var act=null; catalogue.providers.forEach(function(p){ if(p.id===catalogue.active) act=p; });
    var label = act ? act.label : catalogue.active;
    var agentic = catalogue.activeAgentic;
    var devNote = mode==='develop' ? (agentic?' · <em>can edit code &amp; open PRs</em>':' · <em>needs a CLI</em>') : '';
    statusEl.innerHTML = '<span class="dot '+(agentic?'on':'off')+'"></span>'+
      (agentic ? 'Agentic · ' : 'Offline · ') + '<strong>'+esc(label)+'</strong>'+devNote+
      ' <button class="link" id="faa-pick">engine</button>';
    var pick=statusEl.querySelector('#faa-pick'); if(pick) pick.onclick=openSettings;
  }
  function loadCatalogue(){
    return fetch('/api/assist/providers').then(function(r){return r.json();}).then(function(j){
      catalogue=j; powerUser = !!(j.settings && j.settings.powerUser); applyPowerUser(); renderStatus();
    }).catch(function(){ statusEl.textContent='Assistant status unavailable.'; });
  }

  // ================= ASSISTANT ENGINE (settings) =================
  function providerById(id){ var f=null; (catalogue.providers||[]).forEach(function(p){ if(p.id===id) f=p; }); return f; }
  function selectedProvider(){ var r=settingsEl.querySelector('input[name=faa-prov]:checked'); return r?r.value:'auto'; }

  function openSettings(){
    if(!catalogue){ return; }
    var s = catalogue.settings || {provider:'auto',model:'',temperature:null,topP:null,maxTokens:null,powerUser:false};
    var html =
      '<div class="set-h">Assistant Engine</div>'+
      '<div class="set-sub">Choose which LLM CLI powers FAA, its model, and the generation tuning it supports.</div>'+
      '<div class="set-sec"><div class="set-l">Engine (LLM CLI / provider)</div>'+
        providerRow('auto','Auto — first available CLI, else offline', true, true, s.provider==='auto');
    (catalogue.providers||[]).forEach(function(p){ html += providerRow(p.id, p.label, p.available, p.agentic, s.provider===p.id); });
    html += '</div>';
    html += '<div class="set-sec" id="set-model-sec"><div class="set-l">Model</div>'+
      '<select id="set-model"></select>'+
      '<input id="set-model-custom" placeholder="custom / BYOK model id (optional)" value="'+esc(s.model||'')+'"></div>';
    html += '<div class="set-sec" id="set-gen-sec"><div class="set-l">Generation tuning</div>'+
      tuneRow('temp','Temperature','range','0','1','0.1', s.temperature)+
      tuneRow('topp','Top-p (nucleus)','range','0','1','0.05', s.topP)+
      tuneRow('maxt','Max output tokens','number','1','200000','1', s.maxTokens)+
      '<div class="set-hint" id="set-gen-hint"></div></div>';
    html += '<div class="set-sec"><label class="set-toggle"><input type="checkbox" id="set-power"'+(s.powerUser?' checked':'')+'>'+
      '<span>Power-user mode — reveal <strong>Develop &amp; Extend</strong> and developer tools</span></label></div>';
    html += '<div class="set-actions"><button id="set-save">Save</button><button id="set-cancel" class="ghost">Cancel</button></div>';
    html += '<div class="set-note">No CLI? Install one — e.g. <code>npm install -g @github/copilot</code> — then pick it here. Other CLIs are a call for contributions.</div>';
    settingsEl.innerHTML = html;
    body.hidden = true; settingsEl.hidden = false;

    // wire provider selection (radio change + whole-row click for clarity)
    settingsEl.querySelectorAll('label.prov').forEach(function(lbl){
      lbl.addEventListener('click', function(){
        var r=lbl.querySelector('input[type=radio]'); if(r){ r.checked=true; }
        markSelectedProvider(); refreshTuning();
      });
    });
    settingsEl.querySelector('#set-cancel').onclick=closeSettings;
    settingsEl.querySelector('#set-save').onclick=saveSettings;
    markSelectedProvider();
    refreshTuning();
  }
  function providerRow(id,label,available,agentic,checked){
    var badge = (id==='auto') ? '' : (available ? '<span class="b ok">available</span>' : '<span class="b no">not installed</span>');
    var tag = agentic ? '<span class="b ag">agentic</span>' : (id==='auto'?'':'<span class="b of">offline</span>');
    return '<label class="prov'+(checked?' sel':'')+'">'+
      '<input type="radio" name="faa-prov" value="'+id+'"'+(checked?' checked':'')+'>'+
      '<span class="pn">'+esc(label)+'</span>'+badge+tag+'</label>';
  }
  function tuneRow(id,label,type,min,max,step,val){
    var common='id="set-'+id+'" min="'+min+'" max="'+max+'" step="'+step+'"';
    var input = type==='range'
      ? '<input type="range" '+common+'><span class="tval" id="set-'+id+'-val"></span>'
      : '<input type="number" '+common+' placeholder="provider default">';
    return '<div class="tune-row" id="set-'+id+'-row"><span class="tlbl">'+label+'</span>'+input+'</div>';
  }
  function markSelectedProvider(){
    var id=selectedProvider();
    settingsEl.querySelectorAll('label.prov').forEach(function(lbl){
      var r=lbl.querySelector('input[type=radio]');
      lbl.classList.toggle('sel', !!r && r.value===id);
    });
  }
  function refreshTuning(){
    var id=selectedProvider();
    var p = id==='auto' ? null : providerById(id);
    var s = catalogue.settings || {};
    // Model section
    var modelSec=settingsEl.querySelector('#set-model-sec'); var sel=settingsEl.querySelector('#set-model');
    var models = p && p.models ? p.models : [];
    modelSec.style.display = (id==='auto') ? 'none' : 'block';
    sel.innerHTML='<option value="">(provider default)</option>';
    models.forEach(function(m){ var o=document.createElement('option'); o.value=m; o.textContent=m; sel.appendChild(o); });
    if(s.model && models.indexOf(s.model)>=0) sel.value=s.model;
    // Generation tuning — enable only what the selected CLI supports
    var genSec=settingsEl.querySelector('#set-gen-sec');
    genSec.style.display = (id==='auto') ? 'none' : 'block';
    setTune('temp', p&&p.supportsTemperature, s.temperature, 0.7);
    setTune('topp', p&&p.supportsTopP, s.topP, 1.0);
    setTuneNumber('maxt', p&&p.supportsMaxTokens, s.maxTokens);
    var supported = p && (p.supportsTemperature||p.supportsTopP||p.supportsMaxTokens);
    var hint=settingsEl.querySelector('#set-gen-hint');
    hint.textContent = (id!=='auto' && !supported)
      ? (p?p.label:'This CLI')+' is tuned via model selection — it does not expose temperature/top-p/max-tokens.'
      : '';
  }
  function setTune(id, on, val, dflt){
    var row=settingsEl.querySelector('#set-'+id+'-row'); var inp=settingsEl.querySelector('#set-'+id);
    var out=settingsEl.querySelector('#set-'+id+'-val');
    row.classList.toggle('off', !on); inp.disabled=!on;
    inp.value = (val==null?dflt:val);
    if(out) out.textContent = on ? inp.value : 'n/a';
    inp.oninput=function(){ if(out) out.textContent=inp.value; };
  }
  function setTuneNumber(id, on, val){
    var row=settingsEl.querySelector('#set-'+id+'-row'); var inp=settingsEl.querySelector('#set-'+id);
    row.classList.toggle('off', !on); inp.disabled=!on;
    inp.value = (val==null?'':val);
  }
  function closeSettings(){ settingsEl.hidden=true; body.hidden=false; }
  function saveSettings(){
    var id=selectedProvider();
    var p = id==='auto'?null:providerById(id);
    var custom=settingsEl.querySelector('#set-model-custom').value.trim();
    var sel=settingsEl.querySelector('#set-model');
    var model = custom || sel.value || '';
    var temp = (p&&p.supportsTemperature) ? parseFloat(settingsEl.querySelector('#set-temp').value) : null;
    var topP = (p&&p.supportsTopP) ? parseFloat(settingsEl.querySelector('#set-topp').value) : null;
    var maxRaw = settingsEl.querySelector('#set-maxt').value;
    var maxTokens = (p&&p.supportsMaxTokens && maxRaw!=='') ? parseInt(maxRaw,10) : null;
    var power = settingsEl.querySelector('#set-power').checked;
    var saveBtn=settingsEl.querySelector('#set-save'); saveBtn.disabled=true; saveBtn.textContent='Saving…';
    fetch('/api/assist/settings',{method:'POST',headers:{'Content-Type':'application/json'},
      body:JSON.stringify({provider:id, model:model, temperature:temp, topP:topP, maxTokens:maxTokens, powerUser:power})})
      .then(function(r){ if(!r.ok) throw new Error('HTTP '+r.status); return r.json(); })
      .then(function(){ return loadCatalogue(); })
      .then(function(){ closeSettings(); var act=providerById(catalogue.active);
        add('Engine saved — using '+(act?act.label:catalogue.active)+(power?' · power-user mode on':'')+'.','tip'); })
      .catch(function(e){ saveBtn.disabled=false; saveBtn.textContent='Save'; add('Could not save engine settings: '+e.message,'tip'); });
  }

  // ================= open/close + send =================
  function toggle(open){ panel.classList.toggle('open', open); if(open){ panel.querySelector('#faa-in').focus(); if(!catalogue) loadCatalogue(); } }
  faa.onclick=function(){ toggle(!panel.classList.contains('open')); };
  panel.querySelector('.x').onclick=function(){ toggle(false); };
  panel.querySelector('.gear').onclick=function(){ if(settingsEl.hidden) openSettings(); else closeSettings(); };

  // ---- keyboard shortcut: Alt+A toggles FAA open/closed; Esc closes when open ----
  function isOpen(){ return panel.classList.contains('open'); }
  document.addEventListener('keydown', function(e){
    if (e.altKey && !e.ctrlKey && !e.metaKey && (e.key==='a' || e.key==='A')){
      e.preventDefault(); toggle(!isOpen()); return;
    }
    if (e.key==='Escape' && isOpen() && !pinned){
      // don't steal Esc while typing a multi-line message — only close from the toggle's perspective
      if (settingsEl.hidden) { toggle(false); } else { closeSettings(); }
    }
  });
  faa.title = 'Ask the AiCMM assistant (Alt+A)';

  function historyString(){
    return history.slice(-8).map(function(h){ return (h.role==='user'?'User: ':'Assistant: ')+h.text; }).join('\n');
  }
  function send(text){
    var inp=panel.querySelector('#faa-in');
    var msg=(text||inp.value).trim(); if(!msg) return;
    inp.value=''; if(!settingsEl.hidden) closeSettings(); add(msg,'user');
    var hist=historyString(); history.push({role:'user', text:msg});
    var sb=panel.querySelector('#faa-send'); sb.disabled=true;
    var w=add(mode==='develop'?'Working…':'Thinking…','tip');
    fetch('/api/assist',{method:'POST',headers:{'Content-Type':'application/json'},
      body:JSON.stringify({page:pageKey,url:location.pathname,question:msg,mode:mode,history:hist,formFields:schemaForServer()})})
      .then(function(r){return r.json();}).then(function(j){
        w.remove();
        var raw=j.answer||j.error||'No response';
        var res=applyActions(raw);
        var shown = res.text || (res.summary.length ? '' : raw);
        if (shown) add(shown,'bot');
        if (res.summary.length){ add('✓ Updated the page — '+res.summary.join(', ')+'.','tip'); }
        history.push({role:'assistant', text:(res.text||raw).slice(0,2000)});
        if(j.provider){ var tag=document.createElement('div'); tag.className='faa-by';
          tag.textContent=(j.mode==='develop'?'develop · ':'')+(j.agentic?'via ':'offline · ')+(j.providerLabel||j.provider)+(j.fellBack?' (fallback)':''); body.appendChild(tag); }
        if(j.agentic===false && catalogue){ catalogue.active=j.provider; catalogue.activeAgentic=false; renderStatus(); }
        if(res.reload){ add('Reloading to show the saved change…','tip'); setTimeout(function(){ location.reload(); }, 1200); }
      })
      .catch(function(e){ w.remove(); add('Assistant unavailable: '+e,'bot'); })
      .finally(function(){ sb.disabled=false; body.scrollTop=body.scrollHeight; });
  }
  panel.querySelector('#faa-send').onclick=function(){ send(); };
  panel.querySelector('#faa-in').addEventListener('keydown',function(e){ if(e.key==='Enter'&&!e.shiftKey){e.preventDefault();send();} });

  applyPin(); applyMode();
  loadCatalogue();
})();
