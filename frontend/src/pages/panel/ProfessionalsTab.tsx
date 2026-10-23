import { useEffect, useState } from 'react'
import type { ApiError } from '../../api'
import { owner, type Professional, type Service, type WorkingHours } from '../../ownerApi'

const DAYS = ['Domingo', 'Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado']
const ORDER = [1, 2, 3, 4, 5, 6, 0]

export default function ProfessionalsTab() {
  const [pros, setPros] = useState<Professional[] | null>(null)
  const [services, setServices] = useState<Service[]>([])
  const [editing, setEditing] = useState<Professional | 'new' | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = () => Promise.all([owner.professionals(), owner.services()])
    .then(([p, s]) => { setPros(p); setServices(s) }).catch(e => setError(e.message))
  useEffect(() => { load() }, [])

  const toggle = async (p: Professional) => {
    try { await owner.updateProfessional(p.id, { name: p.name, serviceIds: p.serviceIds, active: !p.active }); await load() }
    catch (e) { setError((e as Error).message) }
  }
  const nameOf = (id: number) => services.find(s => s.id === id)?.name

  if (editing) {
    return <Editor key={editing === 'new' ? 'new' : editing.id} pro={editing === 'new' ? null : editing} services={services.filter(s => s.active)}
      onDone={async () => { setEditing(null); await load() }} onCancel={() => setEditing(null)} />
  }
  return (
    <section className="stack">
      <div className="row"><h2>Profissionais</h2><button className="btn btn--primary btn--small" onClick={() => setEditing('new')}>+ Novo profissional</button></div>
      {error && <p className="alert alert--error">{error}</p>}
      {pros?.length === 0 && <div className="card empty">Nenhum profissional ainda. Cadastre um, escolha os serviços e defina os horários.</div>}
      <ul className="options">
        {pros?.map(p => (
          <li key={p.id} className={`card item${p.active ? '' : ' item--off'}`}>
            <span className="avatar" aria-hidden>{p.name.charAt(0).toUpperCase()}</span>
            <div className="item__main">
              <strong>{p.name}{p.active ? '' : ' (inativo)'}</strong>
              <small className="muted">{p.serviceIds.map(nameOf).filter(Boolean).join(', ') || 'Sem serviços'}</small>
              {p.workingHours.length === 0 && <small className="warn">Sem horários: ninguém consegue agendar com este profissional.</small>}
            </div>
            <div className="actions">
              <button className="btn btn--small" onClick={() => setEditing(p)}>Editar</button>
              <button className="btn btn--small" onClick={() => toggle(p)}>{p.active ? 'Desativar' : 'Reativar'}</button>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}

function Editor({ pro, services, onDone, onCancel }: { pro: Professional | null; services: Service[]; onDone: () => void; onCancel: () => void }) {
  const [name, setName] = useState(pro?.name ?? '')
  const [ids, setIds] = useState<Set<number>>(new Set(pro?.serviceIds ?? []))
  const [hours, setHours] = useState<WorkingHours[]>(pro?.workingHours ?? [])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const setDay = (wd: number, windows: WorkingHours[]) => setHours([...hours.filter(h => h.weekday !== wd), ...windows])
  const winsOf = (wd: number) => hours.filter(h => h.weekday === wd).sort((a, b) => a.startTime.localeCompare(b.startTime))

  const save = async (e: React.FormEvent) => {
    e.preventDefault()
    setBusy(true); setError(null)
    try {
      const body = { name: name.trim(), serviceIds: [...ids] }
      const saved = pro ? await owner.updateProfessional(pro.id, { ...body, active: pro.active }) : await owner.createProfessional(body)
      await owner.saveHours(saved.id, hours)
      onDone()
    } catch (err) { setError((err as ApiError).message) } finally { setBusy(false) }
  }

  return (
    <form className="stack" onSubmit={save} noValidate>
      <button type="button" className="linkbtn" onClick={onCancel}>← Voltar</button>
      <h2>{pro ? 'Editar profissional' : 'Novo profissional'}</h2>
      <div className="card stack">
        <label className="field"><span>Nome</span><input value={name} onChange={e => setName(e.target.value)} required /></label>
        <fieldset className="checks"><legend>Serviços que realiza</legend>
          {services.length === 0 && <p className="muted">Cadastre serviços primeiro.</p>}
          {services.map(s => (
            <label key={s.id}><input type="checkbox" checked={ids.has(s.id)}
              onChange={() => { const n = new Set(ids); if (n.has(s.id)) n.delete(s.id); else n.add(s.id); setIds(n) }} /> {s.name}</label>
          ))}
        </fieldset>
      </div>
      <div className="card stack">
        <h3>Horários de atendimento</h3>
        {ORDER.map(wd => {
          const wins = winsOf(wd)
          return (
            <div key={wd} className="hday">
              <span className="hday__name">{DAYS[wd]}</span>
              <div className="hday__wins">
                {wins.length === 0 && <span className="muted">Fechado</span>}
                {wins.map((w, i) => (
                  <div key={i} className="win">
                    <input type="time" value={w.startTime} aria-label={`${DAYS[wd]} início`} onChange={e => setDay(wd, wins.map((x, j) => j === i ? { ...x, startTime: e.target.value } : x))} />
                    <span>às</span>
                    <input type="time" value={w.endTime} aria-label={`${DAYS[wd]} fim`} onChange={e => setDay(wd, wins.map((x, j) => j === i ? { ...x, endTime: e.target.value } : x))} />
                    <button type="button" className="iconbtn" aria-label="Remover janela" onClick={() => setDay(wd, wins.filter((_, j) => j !== i))}>×</button>
                  </div>
                ))}
              </div>
              <button type="button" className="btn btn--small" onClick={() => setDay(wd, [...wins, wins.length ? { weekday: wd, startTime: '14:00', endTime: '18:00' } : { weekday: wd, startTime: '09:00', endTime: '18:00' }])}>+ Horário</button>
            </div>
          )
        })}
      </div>
      {error && <p className="alert alert--error">{error}</p>}
      <button className="btn btn--primary" disabled={busy || !name.trim() || hours.some(h => !h.startTime || !h.endTime)}>{busy ? 'Salvando…' : 'Salvar'}</button>
    </form>
  )
}
