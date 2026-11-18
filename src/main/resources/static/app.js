'use strict';
// Credentials and complaint drafts live only in this tab's memory.
const $ = (selector, root = document) => root.querySelector(selector);
const main = $('#main');
let session = null, draft = {}, page = 0, filters = '', currentDetail = null;
let imageUrls = [], noticeTimer, managerVersion = 0, listVersion = 0;
const categories = {NOISE:'Barulho e perturbação', SECURITY:'Segurança', COMMON_AREA_MISUSE:'Uso das áreas comuns', GARBAGE_VIOLATION:'Descarte de lixo', RULES_VIOLATION:'Regras de convivência', OTHER:'Outro assunto'};
const statuses = {RECEIVED:'Recebida', UNDER_REVIEW:'Em análise', IN_PROGRESS:'Em andamento', RESOLVED:'Resolvida', REJECTED:'Rejeitada'};
const transitions = {RECEIVED:['UNDER_REVIEW'], UNDER_REVIEW:['IN_PROGRESS','REJECTED'], IN_PROGRESS:['RESOLVED'], RESOLVED:[], REJECTED:[]};
const esc = value => String(value ?? '').replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
const date = value => new Date(value).toLocaleString('pt-BR', {dateStyle:'short',timeStyle:'short'});
const options = (values, blank = '') => (blank ? `<option value="">${blank}</option>` : '') + Object.entries(values).map(([key,value]) => `<option value="${key}">${esc(value)}</option>`).join('');
const heading = (title, subtitle, eyebrow = 'ESPAÇO DE CONVIVÊNCIA') => `<div class="heading reveal"><div><p class="eyebrow">${eyebrow}</p><h1>${title}</h1><p class="lede">${subtitle}</p></div><span class="tag">● &nbsp; Cuidado em cada relato</span></div>`;
const field = (label, name, type = 'text', required = true, extra = '') => `<div class="field"><label for="${name}">${label}${required ? ' *' : ' (opcional)'}</label><input id="${name}" name="${name}" type="${type}" ${required ? 'required' : ''} ${extra}></div>`;
const errorBox = '<div class="error" role="alert" hidden></div>';
const formShell = (id, contents, action) => `<form id="${id}" class="panel">${errorBox}${contents}<div class="form-actions"><button class="btn" type="submit">${action} <span aria-hidden="true">→</span></button></div></form>`;
function notify(message) { clearTimeout(noticeTimer); $('#notice').textContent = message; $('#notice').hidden = false; noticeTimer = setTimeout(() => { $('#notice').hidden = true; }, 6000); }
async function api(path, {method = 'GET', body, publicAccess = false, blob = false} = {}) {
  const headers = {};
  if (session && !publicAccess) headers.Authorization = `Bearer ${session.token}`;
  if (body && !(body instanceof FormData)) headers['Content-Type'] = 'application/json';
  let response;
  try { response = await fetch(`/api/v1${path}`, {method, headers, body:body instanceof FormData ? body : body ? JSON.stringify(body) : undefined, cache:'no-store'}); }
  catch { throw new Error('Não foi possível conectar. Confira se a aplicação está em execução e tente novamente.'); }
  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    const messages = {400:'Confira os dados informados.',401:'Entre novamente para continuar.',403:'Sua conta não tem permissão para esta ação. Para relatar, seu vínculo precisa estar aprovado.',404:'Não encontramos esse registro. Confira o protocolo informado.',409:'Esta solicitação já foi processada ou conflita com um cadastro existente.',413:'Os arquivos ultrapassam o limite de envio.',415:'O formato enviado não é aceito.',429:'Muitas tentativas em pouco tempo. Aguarde um minuto para tentar novamente.'};
    const details = (data.validationErrors || []).map(item => `${item.field}: ${item.message}`).join('\n');
    throw new Error((messages[response.status] || 'Não foi possível concluir a operação. Tente novamente.') + (details ? '\n' + details : '') + (response.status === 409 && data.message ? '\n' + data.message : ''));
  }
  return response.status === 204 ? null : blob ? response.blob() : response.json();
}
function bindForm(id, handler) {
  const form = $('#' + id);
  form.addEventListener('submit', async event => {
    event.preventDefault(); const error = $('.error', form), button = $('[type=submit]', form);
    error.hidden = true;
    for (const input of form.querySelectorAll('input[type=password]')) {
      if (new TextEncoder().encode(input.value).length > 72) { error.textContent = 'A senha deve ter no máximo 72 bytes UTF-8. Use uma senha mais curta.'; error.hidden = false; return; }
    }
    button.disabled = true; form.setAttribute('aria-busy','true');
    try { await handler(Object.fromEntries(new FormData(form)), form); }
    catch (failure) { error.textContent = failure.message; error.hidden = false; error.scrollIntoView({block:'nearest',behavior:'smooth'}); }
    finally { button.disabled = false; form.removeAttribute('aria-busy'); }
  });
}
function home() {
  main.innerHTML = heading('Uma boa convivência<br>começa por aqui.', 'Um canal para ser ouvido, acompanhar soluções e cuidar do lugar que você chama de lar.', 'BEM-VINDO AO ENTRE NÓS') + `
  <section class="hero reveal"><div class="hero-copy"><p class="eyebrow">SUA VOZ FAZ A DIFERENÇA</p><h2>Pequenos relatos.<br>Grandes melhorias.</h2><p>Comunique uma ocorrência ao síndico com tranquilidade. Sua identidade não é vinculada ao relato.</p><a class="btn" href="#relatar">Registrar uma ocorrência <span aria-hidden="true">↗</span></a></div><div class="hero-art" role="img" aria-label="Ilustração de dois edifícios em uma comunidade"><span class="sun"></span><div class="building back">${'<i></i>'.repeat(6)}</div><div class="building">${'<i></i>'.repeat(9)}</div><span class="art-label">Mais diálogo, mais cuidado.</span></div></section>
  <div class="section-line"><h2>Como podemos ajudar hoje?</h2><small>Simples de usar. Fácil de acompanhar.</small></div>
  <div class="cards"><a class="card" href="#relatar"><span class="icon" aria-hidden="true">＋</span><h3>Quero relatar</h3><p>Conte o que aconteceu e ajude a melhorar a convivência.</p><span class="link">Começar meu relato &nbsp; →</span></a><a class="card" href="#acompanhar"><span class="icon" aria-hidden="true">◎</span><h3>Quero acompanhar</h3><p>Use seu protocolo para consultar o andamento da ocorrência.</p><span class="link">Consultar protocolo &nbsp; →</span></a><a class="card" href="#acesso"><span class="icon" aria-hidden="true">⌂</span><h3>Sou novo por aqui</h3><p>Solicite seu vínculo à unidade para participar do condomínio.</p><span class="link">Acessar ou me cadastrar &nbsp; →</span></a></div>
  <div class="info-strip reveal"><span class="icon" aria-hidden="true">◇</span><p><strong>Seu relato é anônimo. Seu acompanhamento é transparente.</strong><br>Após o envio, guarde o protocolo. Com ele, você acompanha cada mudança de status, sem precisar entrar na conta.</p></div>`;
}
function access(mode = 'login') {
  if (session && mode === 'login') {
    main.innerHTML = heading('Você está conectado.', `Perfil: ${session.role === 'MANAGER' ? 'síndico' : 'morador'} · Condomínio ${esc(session.condoId)}`) + `<div class="panel"><h2>O que deseja fazer?</h2><p class="lede">O acesso será encerrado ao recarregar ou fechar esta aba.</p><div class="toolbar"><a class="btn" href="${session.role === 'MANAGER' ? '#gestao' : '#relatar'}">${session.role === 'MANAGER' ? 'Abrir painel' : 'Registrar ocorrência'}</a><button id="logout" class="btn secondary">Sair da conta</button>${session.role === 'RESIDENT' ? '<button id="additional" class="btn secondary">Vincular outro morador</button>' : ''}</div><div id="additional-form"></div></div>`;
    $('#logout').onclick = () => { session = null; draft = {}; updateAccount(); route(); };
    if ($('#additional')) $('#additional').onclick = () => { $('#additional-form').innerHTML = bindingForm('additional-binding'); bindBinding('additional-binding', '/bindings/additional'); };
    return;
  }
  main.innerHTML = heading(mode === 'login' ? 'Seu espaço no condomínio.' : 'Vamos conectar você à sua unidade.', 'Entre para registrar ocorrências ou solicite seu cadastro como morador.') + `<div class="split"><div><div class="toolbar"><button id="show-login" class="btn ${mode === 'login' ? '' : 'secondary'}">Entrar</button><button id="show-register" class="btn ${mode !== 'login' ? '' : 'secondary'}">Cadastrar morador</button></div>${mode === 'login' ? formShell('login', field('CPF','cpf','text',true,'maxlength="14" autocomplete="username"') + field('Senha','password','password',true,'autocomplete="current-password"') + field('ID do condomínio','condoId','number',false,'min="1" step="1"') + '<p class="hint">Informe o condomínio apenas se sua conta tiver acesso a mais de um.</p>', 'Entrar com segurança') : bindingForm('binding')}</div><aside class="help reveal"><p class="eyebrow">COMO FUNCIONA</p><h2>Um acesso, mais tranquilidade.</h2><ol><li>Peça ao síndico o ID da sua unidade.</li><li>Cadastre seus dados e aguarde a aprovação do vínculo.</li><li>Após a aprovação, envie relatos anônimos.</li></ol><p>Você pode entrar enquanto aguarda a aprovação. O envio de ocorrências depende de um vínculo aprovado.</p></aside></div>`;
  $('#show-login').onclick = () => access(); $('#show-register').onclick = () => access('register');
  if (mode === 'login') bindForm('login', async data => { if (!data.condoId) delete data.condoId; else data.condoId = Number(data.condoId); session = await api('/auth/login', {method:'POST',body:data,publicAccess:true}); updateAccount(); notify('Acesso realizado com sucesso.'); location.hash = session.role === 'MANAGER' ? '#gestao' : '#relatar'; });
  else bindBinding('binding','/bindings');
}
function bindingForm(id) { return formShell(id, '<h2>Dados do morador</h2>' + field('Nome completo','name','text',true,'maxlength="150" autocomplete="name"') + field('CPF','cpf','text',true,'maxlength="14" autocomplete="username"') + field('Senha de acesso','password','password',true,'autocomplete="new-password"') + field('ID da unidade','unitId','number',true,'min="1" step="1"') + '<p class="hint">Use o ID fornecido pelo síndico, que é diferente do número do apartamento. Campos com * são obrigatórios.</p>', 'Solicitar vínculo'); }
function bindBinding(id, path) { bindForm(id, async (data,form) => { await api(path,{method:'POST',body:{...data,unitId:Number(data.unitId)},publicAccess:path === '/bindings'}); form.reset(); const box = document.createElement('div'); box.className='success'; box.textContent='Solicitação enviada. Aguarde a aprovação do síndico para registrar ocorrências.'; form.prepend(box); }); }
function setup() {
  main.innerHTML = heading('O primeiro passo da sua comunidade.', 'Cadastre o condomínio e o síndico responsável. Esta instalação permite um único condomínio.') + `<div class="split">${formShell('setup', '<h2>Condomínio</h2><div class="fields">' + field('Nome do condomínio','condoName','text',true,'maxlength="150"') + field('CNPJ','cnpj','text',false,'maxlength="18"') + field('Endereço','address','text',false,'maxlength="255"') + field('Telefone do condomínio','phone','tel',false,'maxlength="20"') + '</div><h2 style="margin-top:30px">Síndico responsável</h2><div class="fields">' + field('Nome completo','name','text',true,'maxlength="150" autocomplete="name"') + field('CPF','cpf','text',true,'maxlength="14" autocomplete="username"') + field('Senha de acesso','password','password',true,'autocomplete="new-password"') + '</div>', 'Cadastrar condomínio')}<aside class="help"><h2>Comece uma demonstração completa</h2><ol><li>Cadastre o condomínio e entre como síndico.</li><li>Crie uma unidade no painel e compartilhe seu ID.</li><li>Cadastre um morador e aprove seu vínculo.</li><li>Envie uma ocorrência e acompanhe o atendimento.</li></ol><p>Os dados do perfil local setup são temporários e se perdem ao reiniciar o servidor.</p></aside></div>`;
  bindForm('setup', async data => { const result = await api('/condominiums',{method:'POST',publicAccess:true,body:{condominium:{name:data.condoName,cnpj:data.cnpj || null,address:data.address || null,phone:data.phone || null},manager:{name:data.name,cpf:data.cpf,password:data.password}}}); main.innerHTML = heading('Seu condomínio está pronto.', 'Agora entre com o CPF e a senha cadastrados para preparar as unidades.') + `<div class="success">Condomínio cadastrado com sucesso. ID: <strong>${esc(result.condoId)}</strong>.</div><a class="btn" href="#acesso">Entrar como síndico →</a>`; });
}
function report(step = 1) {
  if (!session || session.role !== 'RESIDENT') { main.innerHTML = heading('Sua voz merece ser ouvida.', 'Para registrar uma ocorrência, entre como morador com vínculo aprovado.') + '<div class="panel"><h2>Vamos começar pelo seu acesso?</h2><p class="lede">A autenticação valida sua permissão de envio. Sua conta e sua unidade não ficam vinculadas à denúncia.</p><a class="btn" href="#acesso">Entrar ou me cadastrar →</a></div>'; return; }
  main.innerHTML = heading('Conte com a gente.', 'Descreva a situação com clareza. Seu relato será recebido pelo síndico, sem identificação do autor.') + `<div class="split"><div><div class="steps" aria-label="Etapas do relato">${['A ocorrência','Revisar relato','Protocolo'].map((name,index) => `<span class="step ${index + 1 === step ? 'current' : ''}" ${index + 1 === step ? 'aria-current="step"' : ''}>0${index+1} &nbsp; ${name}</span>`).join('')}</div><div id="report-content"></div></div><aside class="help"><p class="eyebrow">RELATAR COM CUIDADO</p><h2>Os detalhes fazem a diferença.</h2><p>Conte o que aconteceu, onde e quando. Evite incluir seu nome, CPF, unidade ou informações que revelem sua identidade.</p><p>Descrição, local e anexos são acessíveis à administração. O protocolo mostra apenas informações públicas do andamento.</p><p>Este canal não é destinado a emergências.</p></aside></div>`;
  const target = $('#report-content');
  if (step === 1) {
    target.innerHTML = formShell('report', '<h2>Sobre a ocorrência</h2><div class="field"><label for="category">Qual é o assunto? *</label><select id="category" name="category" required>' + options(categories,'Selecione uma categoria') + '</select></div><div class="field"><label for="description">O que aconteceu? *</label><textarea id="description" name="description" required aria-describedby="description-help"></textarea><p class="hint" id="description-help">Inclua os fatos relevantes e evite dados pessoais.</p></div>' + field('Local da ocorrência','location','text',false,'maxlength="255"') + '<div class="field"><label for="images">Imagens (opcional)</label><input id="images" type="file" accept="image/jpeg,image/png,image/webp" multiple><p class="hint">Até 3 imagens JPEG, PNG ou WebP, com até 5 MiB por arquivo e 16 milhões de pixels. Não inclua pessoas ou dados pessoais.</p></div>', 'Revisar meu relato');
    for (const key of ['category','description','location']) if (draft[key]) $('#' + key).value = draft[key];
    if (draft.images?.length) { const note = document.createElement('p'); note.className='hint'; note.textContent=`${draft.images.length} imagem(ns) já selecionada(s). Escolher novos arquivos substitui a seleção.`; $('#images').after(note); }
    bindForm('report', async data => { const files = $('#images').files.length ? [...$('#images').files] : draft.images || []; if (files.length > 3 || files.some(file => file.size > 5*1024*1024)) throw new Error('Escolha até 3 imagens, cada uma com até 5 MiB.'); draft = {...data,images:files}; report(2); });
  } else {
    target.innerHTML = formShell('review', '<h2>Está tudo certo?</h2>' + [['Assunto',categories[draft.category]],['Descrição',draft.description],['Local',draft.location || 'Não informado'],['Anexos',`${draft.images.length} imagem(ns)`]].map(([label,value]) => `<div class="review-row"><small>${label}</small><p>${esc(value)}</p></div>`).join('') + '<p class="hint" style="margin-top:18px">Depois do envio, guarde o protocolo. Não é possível recuperar o relato pela sua conta.</p><button type="button" class="btn secondary" id="edit-report" style="margin-top:20px">← Editar relato</button>', 'Enviar ocorrência');
    $('#edit-report').onclick = () => report(1);
    bindForm('review', async () => {
      const data = {category:draft.category,description:draft.description,location:draft.location || null}; let body = data;
      if (draft.images.length) { body = new FormData(); body.append('complaint',new Blob([JSON.stringify(data)],{type:'application/json'})); draft.images.forEach(file => body.append('images',file)); }
      const receipt = await api('/complaints',{method:'POST',body}); draft = {};
      document.querySelectorAll('.step').forEach((el,index) => { el.classList.toggle('current',index === 2); if(index === 2) el.setAttribute('aria-current','step'); else el.removeAttribute('aria-current'); });
      target.innerHTML = `<div class="panel"><span class="badge">Recebida com sucesso</span><h2 style="margin-top:22px">Obrigado por cuidar da convivência.</h2><p class="lede">Guarde seu protocolo em um local seguro. Ele é a sua chave para acompanhar este relato.</p><code class="protocol">${esc(receipt.protocol)}</code><div class="toolbar"><button id="copy-protocol" class="btn secondary">Copiar protocolo</button><a class="btn" href="#acompanhar">Acompanhar →</a></div></div>`;
      $('#copy-protocol').onclick = async () => { try { await navigator.clipboard.writeText(receipt.protocol); notify('Protocolo copiado.'); } catch { notify('Selecione o protocolo e copie manualmente.'); } };
    });
  }
}
function historyMarkup(result) { return `<span class="badge">${esc(statuses[result.status])}</span><h2 style="margin-top:20px">${esc(categories[result.category])}</h2><p class="hint">Recebida em ${date(result.createdAt)} · Atualizada em ${date(result.updatedAt)}</p><ul class="timeline" style="margin-top:25px"><li><strong>Recebida</strong><time>${date(result.createdAt)}</time></li>${result.statusHistory.map(item => `<li><strong>${esc(statuses[item.nextStatus])}</strong><time>${date(item.changedAt)}</time>${item.publicJustification ? `<p>${esc(item.publicJustification)}</p>` : ''}</li>`).join('')}</ul>`; }
function tracking() {
  main.innerHTML = heading('Cada etapa, mais perto da solução.', 'Consulte o andamento com o protocolo recebido. Não é necessário entrar na sua conta.') + formShell('tracking', field('Protocolo da ocorrência','protocol','text',true,'autocomplete="off" spellcheck="false" pattern="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"') + '<p class="hint">Informe o código completo, incluindo os hífens. Por privacidade, não buscamos denúncias por CPF ou unidade.</p>', 'Consultar andamento') + '<section id="tracking-result" aria-live="polite"></section>';
  bindForm('tracking', async data => { $('#tracking-result').innerHTML=''; const result = await api('/complaints/protocol/' + encodeURIComponent(data.protocol.trim()),{publicAccess:true}); $('#tracking-result').innerHTML = '<div class="panel reveal">' + historyMarkup(result) + '</div>'; });
}
function management() {
  if (!session || session.role !== 'MANAGER') { main.innerHTML = heading('Um olhar atento à comunidade.', 'Esta área é exclusiva do síndico responsável pelo condomínio.') + '<div class="panel"><h2>Acesse sua conta de síndico</h2><p class="lede">Gerencie unidades, aprove vínculos e dê andamento às ocorrências.</p><a class="btn" href="#acesso">Entrar →</a></div>'; return; }
  main.innerHTML = heading('Cuidar é acompanhar de perto.', `Gestão do condomínio ${esc(session.condoId)}. Cada solicitação é uma oportunidade de melhorar a convivência.`, 'PAINEL DO SÍNDICO') + '<div class="toolbar"><button class="btn" id="tab-complaints">Ocorrências</button><button class="btn secondary" id="tab-bindings">Vínculos pendentes</button><button class="btn secondary" id="tab-units">Unidades</button></div><section class="panel" id="manager-content"></section>';
  $('#tab-complaints').onclick = () => { page=0; managerTab('complaints'); }; $('#tab-bindings').onclick = () => managerTab('bindings'); $('#tab-units').onclick = () => managerTab('units');
  managerTab('complaints');
}
async function managerTab(tab) {
  const version = ++managerVersion;
  const target = $('#manager-content'); target.innerHTML = '<p class="empty" role="status">Carregando…</p>';
  for (const key of ['complaints','bindings','units']) { $('#tab-' + key).classList.toggle('secondary',key !== tab); $('#tab-' + key).setAttribute('aria-pressed',String(key === tab)); }
  try {
    if (tab === 'complaints') {
      target.innerHTML = '<h2>Ocorrências da comunidade</h2><form id="filters"><div class="filters"><label>Categoria<select name="category">' + options(categories,'Todas') + '</select></label><label>Status<select name="status">' + options(statuses,'Todos') + '</select></label><label>Desde<input type="date" name="from"></label><label>Até<input type="date" name="to"></label></div><button class="btn secondary" type="submit">Aplicar filtros</button></form><div id="complaint-list" class="list" style="margin-top:22px"></div>';
      const params = new URLSearchParams(filters); for (const [key,value] of params) if ($('#filters').elements[key]) $('#filters').elements[key].value = value;
      $('#filters').onsubmit = event => { event.preventDefault(); filters = new URLSearchParams([...new FormData(event.target)].filter(([,value]) => value)).toString(); page=0; loadComplaints(); }; await loadComplaints();
    } else if (tab === 'bindings') {
      const rows = await api('/bindings'); if (!target.isConnected || version !== managerVersion) return;
      target.innerHTML = '<h2>Vínculos aguardando aprovação</h2><p class="hint">Aprovações respeitam a ordem de solicitação por unidade. Confira os dados antes de decidir.</p><div class="list" style="margin-top:20px">' + (rows.length ? rows.map(row => `<article class="list-item"><div><strong>${esc(row.residentName)}</strong><p>Unidade ${esc(row.number)} ${row.block ? '· Bloco ' + esc(row.block) : ''} · ${date(row.requestedAt)}</p></div><div class="toolbar"><button class="btn" data-binding="${row.bindingId}" data-decision="approve">Aprovar</button><button class="btn danger" data-binding="${row.bindingId}" data-decision="reject">Rejeitar</button></div></article>`).join('') : '<div class="empty"><strong>Tudo em dia por aqui.</strong>Nenhum vínculo aguarda uma decisão.</div>') + '</div>';
      target.querySelectorAll('[data-binding]').forEach(button => button.onclick = async () => { button.disabled = true; try { await api(`/bindings/${button.dataset.binding}/${button.dataset.decision}`,{method:'POST'}); notify('Decisão registrada.'); await managerTab('bindings'); } catch(error) { notify(error.message); button.disabled=false; } });
    } else {
      const units = await api(`/condominiums/${session.condoId}/units`); if (!target.isConnected || version !== managerVersion) return;
      target.innerHTML = '<h2>Unidades cadastradas</h2><p class="hint">Compartilhe o ID da unidade com o morador para que ele solicite o vínculo.</p><div class="list" style="margin:20px 0">' + (units.length ? units.map(unit => `<div class="list-item"><strong>Unidade ${esc(unit.number)} ${unit.block ? '· Bloco ' + esc(unit.block) : ''}</strong><span class="badge">ID ${unit.id}</span></div>`).join('') : '<p class="empty">Nenhuma unidade cadastrada. Cadastre a primeira abaixo.</p>') + '</div>' + formShell('unit','<h3>Adicionar unidade</h3><div class="fields">' + field('Número','number','text',true,'maxlength="20"') + field('Bloco','block','text',false,'maxlength="20"') + '</div>','Cadastrar unidade');
      bindForm('unit', async data => { await api(`/condominiums/${session.condoId}/units`,{method:'POST',body:{number:data.number,block:data.block || null}}); notify('Unidade cadastrada.'); await managerTab('units'); });
    }
  } catch (error) { if(target.isConnected) target.innerHTML = `<div class="error" role="alert">${esc(error.message)}</div><button class="btn secondary" id="retry-tab">Tentar novamente</button>`; if($('#retry-tab')) $('#retry-tab').onclick = () => managerTab(tab); }
}
async function loadComplaints() {
  const version = ++listVersion;
  const list = $('#complaint-list'); list.innerHTML='<p class="empty" role="status">Buscando ocorrências…</p>';
  try {
    const result = await api(`/complaints?${filters}&page=${page}&size=10`); if (!list.isConnected || version !== listVersion) return;
    list.innerHTML = result.content.length ? result.content.map(item => `<article class="list-item"><div><span class="badge">${esc(statuses[item.status])}</span><h3 style="margin:12px 0 0">${esc(categories[item.category])}</h3><p>${date(item.createdAt)} · ${esc(item.protocol)}</p></div><button class="btn secondary" data-detail="${item.id}">Ver detalhes →</button></article>`).join('') : '<div class="empty"><strong>Nenhuma ocorrência encontrada.</strong>Novos relatos aparecerão aqui. Você também pode ajustar os filtros.</div>';
    list.innerHTML += `<div class="pager"><button class="btn secondary" id="previous" ${page === 0 ? 'disabled' : ''}>← Anterior</button><span>${result.totalElements} relato(s) · Página ${page+1} de ${Math.max(1,result.totalPages)}</span><button class="btn secondary" id="next" ${page+1 >= result.totalPages ? 'disabled' : ''}>Próxima →</button></div>`;
    $('#previous').onclick=()=>{page--;loadComplaints();}; $('#next').onclick=()=>{page++;loadComplaints();}; list.querySelectorAll('[data-detail]').forEach(button => button.onclick=()=>openDetail(button.dataset.detail));
  } catch(error) { if(list.isConnected) list.innerHTML=`<div class="error" role="alert">${esc(error.message)}</div>`; }
}
async function openDetail(id) {
  const dialog = $('#detail-dialog'), target = $('#detail-body');
  if (!dialog.open) dialog.showModal(); target.innerHTML='<p class="empty" role="status">Carregando detalhes…</p>';
  imageUrls.forEach(URL.revokeObjectURL); imageUrls=[];
  try {
    const detail = await api('/complaints/' + id); if (!dialog.open) return; currentDetail = detail;
    target.innerHTML = `<p class="eyebrow">ATENDIMENTO DA OCORRÊNCIA</p><h2 id="detail-title">${esc(categories[detail.category])}</h2><span class="badge">${esc(statuses[detail.status])}</span><p class="hint" style="margin-top:15px">${esc(detail.protocol)} · ${date(detail.createdAt)}</p><h3 style="margin-top:25px">Relato</h3><p class="prewrap">${esc(detail.fullDescription)}</p><p class="prewrap"><strong>Local:</strong> ${esc(detail.location || 'Não informado')}</p><div id="attachments" class="attachments"></div><hr><h3>Andamento</h3>${transitions[detail.status].length ? formShell('status', '<div class="field"><label for="newStatus">Próximo status</label><select id="newStatus" name="newStatus">' + transitions[detail.status].map(status=>`<option value="${status}">${statuses[status]}</option>`).join('') + '</select></div><div class="field"><label for="justification">Justificativa pública</label><textarea id="justification" name="justification"></textarea><p class="hint">Obrigatória na rejeição. Este texto será visível a quem consultar o protocolo. Use providências para informações internas.</p></div>', 'Atualizar status') : '<p class="hint">O atendimento desta ocorrência foi encerrado.</p>'}<h3>Providências internas</h3><div class="list">${detail.internalActions.map(action=>`<div class="review-row"><p class="prewrap">${esc(action.actionDescription)}</p><small>${esc(action.registeredBy)} · ${date(action.executedAt)}</small></div>`).join('') || '<p class="hint">Nenhuma providência registrada.</p>'}</div>${formShell('action', '<div class="field"><label for="actionDescription">Nova providência</label><textarea id="actionDescription" name="actionDescription" required></textarea><p class="hint">Visível apenas à administração do condomínio.</p></div>', 'Registrar providência')}`;
    if ($('#status')) { const sync = () => { $('#justification').required = $('#newStatus').value === 'REJECTED'; }; $('#newStatus').onchange=sync; sync(); bindForm('status', async data => { await api(`/complaints/${id}/status`,{method:'PATCH',body:{...data,justification:data.justification || null}}); notify('Status atualizado.'); await openDetail(id); if($('#complaint-list')) await loadComplaints(); }); }
    bindForm('action', async data => { await api(`/complaints/${id}/actions`,{method:'POST',body:data}); notify('Providência registrada.'); await openDetail(id); });
    const attachmentContainer = $('#attachments');
    for (const attachment of detail.attachments) {
      try { const blob = await api(`/complaints/${id}/images/${attachment.imageId}`,{blob:true}); if (!attachmentContainer.isConnected || !dialog.open) break; const url=URL.createObjectURL(blob); imageUrls.push(url); const image=document.createElement('img'); image.src=url; image.alt='Imagem anexada à ocorrência'; attachmentContainer.append(image); }
      catch { if(attachmentContainer.isConnected) { const message=document.createElement('p'); message.textContent='Não foi possível carregar um anexo.'; attachmentContainer.append(message); } }
    }
  } catch(error) { target.innerHTML=`<div class="error" role="alert">${esc(error.message)}</div>`; }
}
function updateAccount() { $('#account').textContent = session ? (session.role === 'MANAGER' ? 'Síndico · Minha conta' : 'Morador · Minha conta') : 'Entrar na minha conta ↗'; }
const routes = {inicio:home,acesso:access,cadastro:setup,relatar:report,acompanhar:tracking,gestao:management};
function route() {
  if ($('#detail-dialog').open) $('#detail-dialog').close();
  const name=location.hash.slice(1) || 'inicio'; const selected=routes[name] ? name : 'inicio';
  document.querySelectorAll('nav a').forEach(link => { const active=link.hash==='#'+selected; link.classList.toggle('active',active); if(active) { link.setAttribute('aria-current','page'); $('#breadcrumb').textContent=link.textContent.trim().slice(2).trim(); } else link.removeAttribute('aria-current'); });
  routes[selected](); document.title = `${$('#breadcrumb').textContent} · Entre nós`; main.focus({preventScroll:true}); window.scrollTo({top:0});
  if ('IntersectionObserver' in window) { const observer=new IntersectionObserver(entries=>entries.forEach(entry=>{if(entry.isIntersecting){entry.target.classList.add('reveal');observer.unobserve(entry.target);}}),{threshold:.1}); main.querySelectorAll('.card,.info-strip,.help').forEach(element=>observer.observe(element)); }
}
$('#account').onclick=()=>{location.hash='#acesso';};
$('.close').onclick=()=>$('#detail-dialog').close();
$('#detail-dialog').addEventListener('close',()=>{imageUrls.forEach(URL.revokeObjectURL);imageUrls=[];currentDetail=null;});
window.addEventListener('hashchange',route); route();
