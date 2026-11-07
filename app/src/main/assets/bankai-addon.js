(function(){
'use strict';
const $=id=>document.getElementById(id), CAT='bankai_categories_v1', WL='bankai_watchlist_v1';
let customCats=[],watchlist=[],nativeVideos={},activeView='home';
const builtinGenres=genres.slice();
try{customCats=JSON.parse(localStorage.getItem(CAT)||'[]')||[]}catch(e){}
try{watchlist=JSON.parse(localStorage.getItem(WL)||'[]')||[]}catch(e){}
const native=!!(window.NOVA&&typeof window.NOVA.episodeVideoMap==='function');
function saveCats(){localStorage.setItem(CAT,JSON.stringify([...new Set(customCats.filter(Boolean))]));}
function saveWL(){localStorage.setItem(WL,JSON.stringify([...new Set(watchlist)]));}
function isWL(name){return watchlist.some(x=>String(x).toLowerCase()===String(name).toLowerCase());}
function track(name){return watchingList.find(x=>x.name.toLowerCase()===String(name).toLowerCase());}
function cover(item){return (item&&item.poster)||((typeof posterCache!=='undefined'&&posterCache[item.name])||'')||posterFallback(item.name);}
function statusFor(item){
 if(isWL(item.name))return '★ Watchlist';
 const t=track(item.name);
 if(t){const p=watchingProgress(t);if(p.total&&p.done===p.total)return '✓ Watched';if(p.done>0)return '◐ Watching · '+p.done+'/'+p.total;return '○ Unwatched';}
 const s=statuses[item.name];return s==='watched'?'✓ Watched':s==='partial'?'◐ Watching':'○ Unwatched';
}
function ensureCats(){const next=[...builtinGenres,...customCats.filter(c=>!builtinGenres.includes(c))];if(!next.includes('Other'))next.push('Other');genres.splice(0,genres.length,...next);}
function manageCats(){
 const current=customCats.join(', ');
 const raw=prompt('তোমার Category-গুলোর নাম comma দিয়ে লেখো। Add/Rename/Delete সব করা যাবে:',current);
 if(raw===null)return;
 customCats=[...new Set(raw.split(',').map(x=>x.trim()).filter(Boolean))];
 ensureCats();saveCats();render();renderAll();
 toast('Category list saved.');
}
function toggleWL(name){
 if(isWL(name))watchlist=watchlist.filter(x=>String(x).toLowerCase()!==String(name).toLowerCase());
 else watchlist.unshift(name);
 saveWL();render();renderAll();if(activeView==='watchlist')renderWatchlistPage();
 toast(isWL(name)?'Watchlist-এ যোগ হয়েছে':'Watchlist থেকে সরানো হয়েছে');
}
function img(name,src,cls){return '<img class="'+(cls||'')+'" loading="lazy" decoding="async" src="'+esc(src||posterFallback(name))+'" alt="'+esc(name)+' poster">';}
function mini(item,label){
 const p=watchingProgress(item),n=nextEpisode(item),meta=n?'S'+n.s+' · E'+n.e:'Completed';
 return '<div class="bk-item-row">'+img(item.name,cover(item),'bk-thumb')+'<div class="bk-item-copy"><strong>'+esc(item.name)+'</strong><small>'+esc(label||meta)+' · '+p.done+'/'+p.total+' EP</small><div class="bk-progress-small"><i style="width:'+p.pct+'%"></i></div><div class="bk-mini-actions"><button onclick="openWatchingDetail(\''+item.id+'\')">Manage Episodes</button></div></div></div>';
}
function recently(){return watchingList.filter(x=>watchingProgress(x).done>0).sort((a,b)=>(b.updated||b.created||0)-(a.updated||a.created||0)).slice(0,3);}
function nextUp(){return watchingList.filter(x=>nextEpisode(x)).sort((a,b)=>(b.updated||b.created||0)-(a.updated||a.created||0)).slice(0,3);}
function renderWeekGrid(){
 const box=$('bkWeeklyGrid');if(!box)return;
 const days=['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'],list=watchingList.filter(x=>x.inWeekly);
 box.innerHTML=days.map(d=>{const items=list.filter(x=>x.day===d),a=items[0];return '<div class="bk-week-day"><b>'+d.slice(0,3)+'</b><small>'+items.length+' Anime</small>'+(a?img(a.name,cover(a)):'<small>—</small>')+(items.length>1?'<small>+'+(items.length-1)+' more</small>':'')+'</div>';}).join('');
}
function savedPlayback(){
 if(!native)return null;
 const candidates=Object.entries(nativeVideos).map(([key,video])=>{
  const match=key.match(/^(.+)__s(\\d+)__e(\\d+)$/);
  if(!match||!video||!(video.positionMs>1500))return null;
  const item=watchingList.find(x=>x.id===match[1]);
  if(!item||(video.durationMs>0&&video.positionMs>=video.durationMs-5000))return null;
  return {item,season:Number(match[2]),episode:Number(match[3]),video};
 }).filter(Boolean).sort((a,b)=>(b.video.lastPlayed||b.video.added||0)-(a.video.lastPlayed||a.video.added||0));
 return candidates[0]||null;
}
function timeLabel(ms){const sec=Math.floor(Math.max(0,ms)/1000);return String(Math.floor(sec/3600)).padStart(2,'0')+':'+String(Math.floor(sec%3600/60)).padStart(2,'0')+':'+String(sec%60).padStart(2,'0');}
function renderDashboard(){
 const root=$('bkDashboard');if(!root)return;
 const saved=savedPlayback(),item=saved?saved.item:nextUp()[0],p=item?watchingProgress(item):{done:0,total:0,pct:0},n=saved?{s:saved.season,e:saved.episode}:(item?nextEpisode(item):null);
 const percent=saved&&saved.video.durationMs>0?Math.min(100,Math.round(saved.video.positionMs/saved.video.durationMs*100)):p.pct;
 $('bkFeatureTitle').textContent=item?item.name:'Your Anime Universe';
 $('bkFeatureMeta').textContent=item?'Season '+n.s+' • Episode '+n.e+(saved?' · Resume':' / '+p.total):'Your personal anime library';
 $('bkFeatureDesc').textContent=saved?'Continue from '+timeLabel(saved.video.positionMs):(item?'A journey of destiny, power and forgotten memories...':'Add your favourite anime and build your own episode library.');
 $('bkFeatureProgress').style.width=percent+'%';
 $('bkFeatureProgressLabel').textContent=saved?percent+'% · Resume episode':(item?p.pct+'% · '+p.done+'/'+p.total+' episodes watched':'Ready to start tracking');
 $('bkFeatureArt').src='hero.webp';
 $('bkContinue').textContent=saved?'▶ Continue Episode':(item?'▶ Continue':'＋ Add Anime');
 $('bkContinue').onclick=()=>saved&&native?NOVA.playEpisodeVideo(item.id,saved.season,saved.episode):(item?openWatchingDetail(item.id):openWatchingAdd());
 $('bkFeatureManage').onclick=()=>item?openWatchingDetail(item.id):showBankaiView('anime');
 const rec=recently(),up=nextUp();
 $('bkRecently').innerHTML=rec.length?rec.map(x=>mini(x,'Recently Watched')).join(''):'<div class="bk-empty">এখনো কোনো Episode Watched করা হয়নি।</div>';
 $('bkNewEpisodes').innerHTML=up.length?up.map(x=>mini(x,'Next Episode')).join(''):'<div class="bk-empty">এখনো কোনো Upcoming Episode নেই।</div>';
 const completed=watchingList.filter(x=>{const q=watchingProgress(x);return q.total&&q.done===q.total}).length;
 const active=watchingList.filter(x=>{const q=watchingProgress(x);return q.done>0&&q.done<q.total}).length;
 $('bkStatWatching').textContent=active;$('bkStatCompleted').textContent=completed;
 $('bkStatEpisodes').textContent=watchingList.reduce((v,x)=>v+watchingProgress(x).done,0);
 $('bkStatWatchlist').textContent=watchlist.length;
 $('bkWeeklyCount').textContent=watchingList.filter(x=>x.inWeekly).length+' This Week';
 renderWeekGrid();
 const rows=watchlist.map(name=>anime.find(a=>a.name.toLowerCase()===String(name).toLowerCase())||{name,genre:'Anime'});
 $('bkWatchlistStrip').innerHTML=rows.length?rows.slice(0,5).map(a=>'<div class="bk-watchlist-poster" data-name="'+esc(a.name)+'">'+img(a.name,cover(a))+'<b>'+esc(a.name)+'</b></div>').join(''):'<div class="bk-empty">পরে দেখার Anime এখানে রাখো।</div>';
}
function renderWeeklyPage(){
 const box=$('bkWeeklyCards');if(!box)return;
 const list=watchingList.filter(x=>x.inWeekly);
 box.innerHTML=list.length?list.map(x=>{const p=watchingProgress(x);return '<article class="bk-week-card">'+img(x.name,cover(x))+'<div style="flex:1;min-width:0"><strong>'+esc(x.name)+'</strong><small>'+esc(x.day||'TBA')+' · '+x.seasons+' Season · '+p.total+' Episodes</small><small>'+p.done+'/'+p.total+' watched</small><div class="bk-action-row"><button onclick="openWatchingDetail(\''+x.id+'\')">Manage</button><button onclick="bankaiRemoveWeekly(\''+x.id+'\')">Remove from Weekly</button></div></div></article>';}).join(''):'<div class="bk-empty">Weekly list খালি। Anime Details থেকে Add to Weekly করো।</div>';
}
function renderWatchlistPage(){
 const box=$('bkWatchlistGrid');if(!box)return;
 const rows=watchlist.map(name=>anime.find(a=>a.name.toLowerCase()===String(name).toLowerCase())||{name,genre:'Anime'});
 box.innerHTML=rows.length?rows.map(a=>'<div class="bk-watch-card" data-name="'+esc(a.name)+'">'+img(a.name,cover(a))+'<strong>'+esc(a.name)+'</strong><small>Watchlist</small><div class="bk-action-row"><button onclick="bankaiStartWatching(\''+esc(a.name).replace(/'/g,"\\'")+'\')">Start Watching</button><button onclick="bankaiToggleWatchlist(\''+esc(a.name).replace(/'/g,"\\'")+'\')">Remove</button></div></div>').join(''):'<div class="bk-empty">Watchlist empty. Anime poster থেকে Add to Watchlist করো।</div>';
}
function renderStatsPage(){
 const box=$('bkStatsMetrics');if(!box)return;
 const watched=watchingList.reduce((v,x)=>v+watchingProgress(x).done,0),total=watchingList.reduce((v,x)=>v+watchingProgress(x).total,0);
 const rows=[['Total Anime',anime.length],['Watching',watchingList.filter(x=>{const p=watchingProgress(x);return p.done>0&&p.done<p.total}).length],['Completed',watchingList.filter(x=>{const p=watchingProgress(x);return p.total&&p.done===p.total}).length],['Episodes Watched',watched],['Total Episodes',total],['Watchlist',watchlist.length],['Weekly Anime',watchingList.filter(x=>x.inWeekly).length]];
 box.innerHTML=rows.map(([a,b])=>'<div class="stat glass" style="--c:#d946ef"><div><b>'+b+'</b><span>'+a+'</span></div></div>').join('');
}
function renderAll(){renderDashboard();renderWeeklyPage();renderWatchlistPage();renderStatsPage();}
function showBankaiView(view){
 activeView=view;
 const pages={home:$('bkDashboard'),anime:$('library'),episodes:$('watching'),weekly:$('bkWeeklyPage'),stats:$('bkStatsPage'),watchlist:$('bkWatchlistPage'),settings:$('bkSettingsPage')};
 Object.entries(pages).forEach(([k,node])=>{if(node)node.hidden=k!==view});
 const reels=$('reels');if(reels)reels.hidden=true;
 document.querySelectorAll('[data-bk-view]').forEach(n=>n.classList.toggle('active',n.dataset.bkView===view));
 if(view==='anime')render();if(view==='episodes')renderWatching();if(view==='weekly')renderWeeklyPage();if(view==='watchlist')renderWatchlistPage();if(view==='stats')renderStatsPage();if(view==='home')renderDashboard();
 window.scrollTo({top:0,behavior:'smooth'});
}
function createShell(){
 const wrap=document.querySelector('.wrap'),nav=$('nav');if(!wrap||!nav||$('bankaiSidebar'))return;
 const side=document.createElement('aside');side.id='bankaiSidebar';
 side.innerHTML='<div class="bk-logo"><svg viewBox="0 0 100 100"><path d="M18 47c-6-8 5-19 18-16 2-17 27-21 35-7 13-1 21 9 15 19 12 7 8 22-6 24-5 12-22 13-30 5-13 9-30 2-29-9-14-2-14-13-3-16Z" fill="#ed174d" stroke="#ffc0db" stroke-width="2"/><path d="M30 49c6-13 18-12 23-2 8-13 22-8 18 4-3 8-17 10-22 18-7-10-25-9-19-20Z" fill="#26001e" stroke="#ff5b9c" stroke-width="2"/><circle cx="50" cy="51" r="11" fill="#090014" stroke="#ff286b" stroke-width="3"/><circle cx="50" cy="51" r="4" fill="#f7bfd9"/></svg><strong>BANKAI<br>Anime Hub</strong></div>'+
 '<button class="bk-side-link active" data-bk-view="home" onclick="showBankaiView(\'home\')"><span>⌂</span>Home</button>'+
 '<button class="bk-side-link" data-bk-view="anime" onclick="showBankaiView(\'anime\')"><span>☆</span>My Anime</button>'+
 '<button class="bk-side-link" data-bk-view="episodes" onclick="showBankaiView(\'episodes\')"><span>▶</span>Episodes</button>'+
 '<button class="bk-side-link" data-bk-view="weekly" onclick="showBankaiView(\'weekly\')"><span>▦</span>Weekly</button>'+
 '<button class="bk-side-link" data-bk-view="stats" onclick="showBankaiView(\'stats\')"><span>▥</span>Stats</button>'+
 '<button class="bk-side-link" data-bk-view="watchlist" onclick="showBankaiView(\'watchlist\')"><span>☆</span>Watchlist</button>'+
 '<div class="bk-side-spacer"></div><div class="bk-dark-toggle"><span>☾ Dark Mode</span><i></i></div>';
 wrap.insertBefore(side,wrap.firstChild);
 nav.innerHTML='<nav class="links" aria-label="Main navigation">'+
 '<a data-bk-view="home" class="active" href="#home" onclick="showBankaiView(\'home\');return false">⌂ Home</a>'+
 '<a data-bk-view="anime" href="#anime" onclick="showBankaiView(\'anime\');return false">☆ My Anime</a>'+
 '<a data-bk-view="episodes" href="#episodes" onclick="showBankaiView(\'episodes\');return false">▶ Episodes</a>'+
 '<a data-bk-view="weekly" href="#weekly" onclick="showBankaiView(\'weekly\');return false">▦ Weekly</a>'+
 '<a data-bk-view="stats" href="#stats" onclick="showBankaiView(\'stats\');return false">▥ Stats</a></nav>'+
 '<div class="nav-actions"><input id="bankaiSearch" type="search" placeholder="⌕ Search anime..." aria-label="Search anime"><button class="store-btn" onclick="toast(\'No new notifications\')" title="Notifications">♧</button><button class="btn primary" onclick="openAdd()">＋ Anime</button></div>';
 const dashboard=document.createElement('section');dashboard.id='bkDashboard';dashboard.className='bk-dashboard';
 dashboard.innerHTML='<div class="bk-feature-row"><section class="bk-feature"><div class="bk-feature-art"><img id="bkFeatureArt" src="hero.webp" alt=""></div><div class="bk-feature-copy"><span class="bk-feature-pill">▶ &nbsp; Continue Watching</span><h1 id="bkFeatureTitle">Your Anime Universe</h1><p id="bkFeatureMeta" class="bk-feature-meta">Your personal anime library</p><p id="bkFeatureDesc" class="bk-feature-desc">A journey of destiny, power and forgotten memories...</p><div class="bk-progress"><i id="bkFeatureProgress" style="width:0%"></i></div><div id="bkFeatureProgressLabel" class="bk-progress-label">Ready to start tracking</div><div class="bk-feature-actions"><button class="btn primary" id="bkContinue">▶ Continue</button><button class="btn" id="bkFeatureManage">Manage Episodes</button></div></div></section>'+
 '<aside class="bk-stats-rail"><h3>♧ &nbsp; Your Anime Stats</h3>'+
 '<div class="bk-stat-line"><span class="bk-stat-icon">▶</span><div><span class="bk-stat-label">Watching</span><div class="bk-stat-mini"><i style="width:65%"></i></div></div><div><b id="bkStatWatching" class="bk-stat-value">0</b><div class="bk-stat-sub">Active</div></div></div>'+
 '<div class="bk-stat-line"><span class="bk-stat-icon">✓</span><div><span class="bk-stat-label">Completed</span><div class="bk-stat-mini"><i style="width:45%"></i></div></div><div><b id="bkStatCompleted" class="bk-stat-value">0</b><div class="bk-stat-sub">Anime</div></div></div>'+
 '<div class="bk-stat-line"><span class="bk-stat-icon">▦</span><div><span class="bk-stat-label">Episodes Watched</span><div class="bk-stat-mini"><i style="width:70%"></i></div></div><div><b id="bkStatEpisodes" class="bk-stat-value">0</b><div class="bk-stat-sub">Total</div></div></div>'+
 '<div class="bk-stat-line"><span class="bk-stat-icon">☆</span><div><span class="bk-stat-label">Watchlist</span><div class="bk-stat-mini"><i style="width:32%"></i></div></div><div><b id="bkStatWatchlist" class="bk-stat-value">0</b><div class="bk-stat-sub">Saved</div></div></div></aside></div>'+
 '<div class="bk-panel-grid"><section class="bk-panel"><div class="bk-panel-head"><h3>◷ Recently Watched</h3><button onclick="showBankaiView(\'episodes\')">See all →</button></div><div id="bkRecently"></div></section>'+
 '<section class="bk-panel"><div class="bk-panel-head"><h3>ϟ New Episodes</h3><button onclick="showBankaiView(\'episodes\')">See all →</button></div><div id="bkNewEpisodes"></div></section>'+
 '<section class="bk-panel bk-weekly-panel"><div class="bk-panel-head"><h3>▦ Weekly Releases</h3><button onclick="showBankaiView(\'weekly\')"><span id="bkWeeklyCount">0 This Week</span> →</button></div><div class="bk-week-grid" id="bkWeeklyGrid"></div></section></div>'+
 '<div class="bk-lower-grid"><section class="bk-panel"><div class="bk-panel-head"><h3>☆ My Watchlist</h3><button onclick="showBankaiView(\'watchlist\')">View All →</button></div><div class="bk-watchlist-strip" id="bkWatchlistStrip"></div></section><div class="bk-quote">“Anime is not just entertainment,<br> it’s a journey of endless imagination.”　✦</div></div>';
 nav.insertAdjacentElement('afterend',dashboard);
 const weekly=document.createElement('section');weekly.id='bkWeeklyPage';weekly.className='bk-page';weekly.hidden=true;weekly.innerHTML='<h2 class="bk-page-title">Weekly Releases</h2><p class="bk-page-sub">Remove an Anime from Weekly without deleting its seasons, episodes or videos.</p><div class="bk-week-cards" id="bkWeeklyCards"></div>';dashboard.insertAdjacentElement('afterend',weekly);
 const stats=document.createElement('section');stats.id='bkStatsPage';stats.className='bk-page';stats.hidden=true;stats.innerHTML='<h2 class="bk-page-title">Anime Statistics</h2><p class="bk-page-sub">Your watch progress and library overview.</p><div class="stats" id="bkStatsMetrics" style="margin:0;padding:0;grid-template-columns:repeat(auto-fit,minmax(130px,1fr))"></div>';weekly.insertAdjacentElement('afterend',stats);
 const wl=document.createElement('section');wl.id='bkWatchlistPage';wl.className='bk-page';wl.hidden=true;wl.innerHTML='<h2 class="bk-page-title">My Watchlist</h2><p class="bk-page-sub">Anime saved to watch later.</p><div class="bk-watch-grid" id="bkWatchlistGrid"></div>';stats.insertAdjacentElement('afterend',wl);
 const settings=document.createElement('section');settings.id='bkSettingsPage';settings.className='bk-page';settings.hidden=true;
 settings.innerHTML='<h2 class="bk-page-title">Settings & Backup</h2><p class="bk-page-sub">Manage your categories, library backup and private episode videos.</p>'+
 '<div class="bk-panel" style="margin-bottom:12px"><div class="bk-panel-head"><h3>♙ Data & Backup</h3></div>'+
 '<p class="bk-page-sub">Export saves your Anime list, categories, statuses, Weekly list, Watchlist and episode tracking data. App-private video files need a separate copy before uninstalling.</p>'+
 '<div class="bk-action-row"><button class="btn primary" onclick="exportData()">↓ Export JSON Backup</button><button class="btn" onclick="document.getElementById(\'importFile\').click()">↑ Restore JSON Backup</button></div></div>'+
 '<div class="bk-panel" style="margin-bottom:12px"><div class="bk-panel-head"><h3>▣ Library & Storage</h3></div>'+
 '<p class="bk-page-sub">Imported videos stay in BANKAI private storage, separate from the original Gallery files.</p>'+
 '<div class="bk-action-row"><button class="btn" onclick="openBankaiLibrary()">Open Video Library</button><button class="btn" onclick="manageCats()">Manage Categories</button></div>'+
 '<p id="bkStorageInfo" class="bk-page-sub" style="margin-top:12px">Storage information available on Android.</p></div>'+
 '<div class="bk-panel"><div class="bk-panel-head"><h3>🌙 Appearance</h3></div><p class="bk-page-sub">BANKAI currently uses the dark purple anime theme from your reference image.</p><button class="btn" onclick="showBankaiView(\'home\')">Back to Home</button></div>';
 wl.insertAdjacentElement('afterend',settings);
 const oldHero=$('top');if(oldHero)oldHero.hidden=true;const oldStats=document.querySelector('.stats.home-only');if(oldStats)oldStats.hidden=true;
 const search=$('bankaiSearch');search.addEventListener('input',()=>{const q=$('search');if(q){q.value=search.value;render();}if(activeView!=='anime')showBankaiView('anime');});
 const genre=$('genreTabs');if(genre&&!$('bkCategoryManage')){const b=document.createElement('button');b.id='bkCategoryManage';b.className='tab';b.textContent='＋ Manage Categories';b.onclick=manageCats;genre.appendChild(b);}
 const form=$('watchAddBg')?.querySelector('.watch-form-grid');if(form&&!$('bankaiWeeklyCheckbox'))form.insertAdjacentHTML('beforeend','<label class="field full"><span><input id="bankaiWeeklyCheckbox" type="checkbox"> Add this Anime to Weekly</span><small class="smallnote">You can change this later from Anime Details.</small></label>');
 const genreInput=$('newGenre');if(genreInput&&!$('bankaiGenreList')){const dl=document.createElement('datalist');dl.id='bankaiGenreList';genreInput.setAttribute('list','bankaiGenreList');genreInput.insertAdjacentElement('afterend',dl);}
}
function decorateCard(){
 if(typeof window.card!=='function'||window.card.__bankai)return;
 const old=window.card,wrapped=function(a){
  let html=old(a);html=html.replace(/(<div class="meta">[\s\S]*?<\/div>)/,'$1<div class="bk-custom-state">'+esc(statusFor(a))+'</div>');
  html=html.replace('</div></article>','<div class="bk-card-extra"><button type="button" data-bankai-watchlist>'+(isWL(a.name)?'★ In Watchlist':'＋ Watchlist')+'</button></div></div></article>');
  return html;
 };wrapped.__bankai=true;window.card=wrapped;
}
function decorateDetail(id){
 const box=$('watchDetail'),item=watchingList.find(x=>x.id===id);if(!box||!item)return;
 if(!box.querySelector('.bk-week-toggle')){
  const row=document.createElement('div');row.className='bk-week-toggle';row.innerHTML='<strong>Weekly Schedule</strong><span></span><button type="button"></button>';
  (box.querySelector('.detail-head')||box.querySelector('.edit-row')||box).insertAdjacentElement('afterend',row);
 }
 const row=box.querySelector('.bk-week-toggle');row.querySelector('span').textContent=item.inWeekly?'✓ '+(item.day||'TBA')+' · Added to Weekly':'Not in Weekly';
 const toggle=row.querySelector('button');toggle.textContent=item.inWeekly?'Remove from Weekly':'＋ Add to Weekly';
 toggle.onclick=()=>{item.inWeekly=!item.inWeekly;if(item.inWeekly&&(!item.day||item.day==='TBA'))item.day='Friday';saveWatching();openWatchingDetail(id);renderAll();};
 if(!native)return;
 box.querySelectorAll('.episode-season').forEach((seasonNode,si)=>seasonNode.querySelectorAll('.episode-tile').forEach((tile,ei)=>{
  const sn=si+1,ep=ei+1,key=videoKey(item.id,sn,ep),rec=nativeVideos[key]||(typeof videoRecords!=='undefined'?videoRecords[key]:null),actions=tile.querySelector('.episode-file-actions');if(!actions)return;
  const tools=document.createElement('div');tools.className='bk-video-tools';
  if(rec){
   const b=(label,fn,cls)=>{const n=document.createElement('button');n.textContent=label;if(cls)n.className=cls;n.onclick=fn;return n;};
   tools.append(b('▶ Play',()=>window.NOVA.playEpisodeVideo(item.id,sn,ep)),b('↗ VLC / MX',()=>window.NOVA.openEpisodeExternalPlayer(item.id,sn,ep),'bk-vlc'),b('↻ Replace',()=>window.NOVA.pickEpisodeVideo(item.id,sn,ep)),b('Delete',()=>{if(confirm('এই Episode-এর video delete করবে?'))window.NOVA.deleteEpisodeVideo(item.id,sn,ep);},'danger'));
  }else{const n=document.createElement('button');n.className='video-add';n.textContent='＋ Upload Video';n.onclick=()=>window.NOVA.pickEpisodeVideo(item.id,sn,ep);tools.append(n);}
  actions.replaceWith(tools);
  const info=tile.querySelector('.video-saved,.video-empty');if(info)info.textContent=rec?'VIDEO SAVED IN APP':'No video added';
 }));
}
async function syncNative(){if(!native)return;try{nativeVideos=JSON.parse(NOVA.episodeVideoMap()||'{}')||{};Object.assign(videoRecords,nativeVideos);}catch(e){console.warn(e);}}
function renderAll(){renderDashboard();renderWeeklyPage();renderWatchlistPage();renderStatsPage();}
function wire(){
 const oldRender=window.render;if(oldRender&&!oldRender.__bankai){const f=function(){oldRender();renderAll();};f.__bankai=true;window.render=f;}
 const oldDetail=window.openWatchingDetail;if(oldDetail&&!oldDetail.__bankai){const f=function(id){oldDetail(id);decorateDetail(id);};f.__bankai=true;window.openWatchingDetail=f;}
 const oldToggle=window.toggleWatchEpisode;if(oldToggle&&!oldToggle.__bankai){const f=function(id,s,e){oldToggle(id,s,e);const x=watchingList.find(v=>v.id===id);if(x){x.updated=Date.now();saveWatching();}renderAll();};f.__bankai=true;window.toggleWatchEpisode=f;}
 const oldSave=window.saveWatchingAnime;if(oldSave&&!oldSave.__bankai){const f=async function(){await oldSave();const x=watchingList[0];if(x){x.inWeekly=!!$('bankaiWeeklyCheckbox')?.checked;x.updated=Date.now();saveWatching();}renderAll();};f.__bankai=true;window.saveWatchingAnime=f;}
 const oldAdd=window.addAnime;if(oldAdd&&!oldAdd.__bankai){const f=function(){const g=$('newGenre')?.value.trim();if(g&&!genres.includes(g)){customCats.push(g);ensureCats();saveCats();}oldAdd();renderAll();};f.__bankai=true;window.addAnime=f;}
 if(native){
  window.chooseEpisodeVideo=(id,s,e)=>NOVA.pickEpisodeVideo(id,s,e);
  window.removeEpisodeVideo=(id,s,e)=>{if(confirm('এই Episode-এর video delete করবে?'))NOVA.deleteEpisodeVideo(id,s,e);};
  window.playEpisode=(id,s,e)=>NOVA.playEpisodeVideo(id,s,e);
 }
}
window.showBankaiView=showBankaiView;
window.openBankaiLibrary=()=>{if(native&&window.NOVA&&typeof NOVA.openLibrary==='function')NOVA.openLibrary();else showBankaiView('settings');};
window.bankaiRefreshNativeVideos=async()=>{await syncNative();renderAll();};
window.bankaiToggleWatchlist=toggleWL;
window.bankaiRemoveWeekly=id=>{const x=watchingList.find(v=>v.id===id);if(!x)return;x.inWeekly=false;saveWatching();renderAll();};
window.bankaiStartWatching=name=>{watchlist=watchlist.filter(v=>String(v).toLowerCase()!==String(name).toLowerCase());saveWL();showBankaiView('episodes');openLibraryAnime(name);renderAll();};
window.bankaiNativeVideoChanged=async(id)=>{await syncNative();openWatchingDetail(id);renderAll();toast('Video app-এর storage-এ save হয়েছে।');};
window.bankaiNativeVideoDeleted=async(id)=>{await syncNative();openWatchingDetail(id);renderAll();toast('Episode video delete হয়েছে।');};
document.addEventListener('click',e=>{
 const b=e.target.closest('[data-bankai-watchlist]');if(b){e.preventDefault();e.stopPropagation();const name=b.closest('.card')?.dataset.name;if(name)toggleWL(name);}
 const p=e.target.closest('.bk-watchlist-poster,.bk-watch-card');if(p&&!e.target.closest('button')){const name=p.dataset.name,t=track(name);if(t)openWatchingDetail(t.id);else openLibraryAnime(name);}
});
function init(){
 ensureCats();
 watchingList.forEach(x=>{if(typeof x.inWeekly==='undefined')x.inWeekly=!!(x.day&&x.day!=='TBA');});
 saveWatching();
 createShell();decorateCard();wire();
 if(typeof window.render==='function')window.render();
 if(typeof window.renderWatching==='function')window.renderWatching();
 if(native)syncNative().then(()=>{renderAll();const info=$('bkStorageInfo');if(info)info.textContent='App video storage: '+(NOVA.storageBytes()/1048576).toFixed(1)+' MB';});
 renderAll();showBankaiView('home');
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(init,0));else setTimeout(init,0);
})();