import { useEffect, useState } from 'react'
import type { ApiError } from '../../api'
import { formatDuration, formatPrice } from '../../lib/format'
import { owner, type Service } from '../../ownerApi'

interface Draft { id?: number; name: string; durationMin: string; price: string }
const empty: Draft = { name: '', durationMin: '30', price: '' }
const toCents = (v: string) => Math.round(parseFloat(v.replace(',', '.')) * 100)

export default function ServicesTab() {
  const [items, setItems] = useState<Service[] | null>(null)
  const [draft, setDraft] = useState<Draft | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [fields, setFields] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)

  const load = () => owner.services().then(setItems).catch(e => setError(e.message))
  useEffect(() => { load() }, [])

  const save = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!draft) return
    const cents = toCents(draft.price)
    const body = { name: draft.name.trim(), durationMin: Number(draft.durationMin), priceCents: Number.isNaN(cents) ? 0 : cents }
    setBusy(true); setError(null); setFields({})
    try {
      if (draft.id) await owner.updateService(draft.id, body); else await owner.createService(body)
      setDraft(null); await load()
    } catch (err) { const a = err as ApiError; setError(a.message); setFields(a.fields ?? {}) } finally { setBusy(false) }
  }
  const toggle = async (s: Service) => {
    try { await owner.updateService(s.id, { name: s.name, durationMin: s.durationMin, priceCents: s.priceCents, active: !s.active }); await load() }
    catch (e) { setError((e as Error).message) }
  }

  return (
    <section className="stack">
      <div className="row"><h2>Serviços</h2>{!draft && <button className="btn btn--primary btn--small" onClick={() => setDraft(empty)}>+ Novo serviço</button>}</div>
      {error && !draft && <p className="alert alert--error">{error}</p>}

      {draft && (
        <form className="card stack" onSubmit={save} noValidate>
          <h3>{draft.id ? 'Editar serviço' : 'Novo serviço'}</h3>
          <label className="field"><span>Nome</span>
            <input value={draft.name} onChange={e => setDraft({ ...draft, name: e.target.value })} required />
            {fields.name && <em className="field__err">{fields.name}</em>}</label>
          <div className="grid2">
            <label className="field"><span>Duração (min)</span>
              <input type="number" min={5} step={5} value={draft.durationMin} onChange={e => setDraft({ ...draft, durationMin: e.target.value })} />
              {fields.durationMin && <em className="field__err">{fields.durationMin}</em>}</label>
            <label className="field"><span>Preço (R$)</span>
              <input inputMode="decimal" placeholder="45,00" value={draft.price} onChange={e => setDraft({ ...draft, price: e.target.value })} />
              {fields.priceCents && <em className="field__err">{fields.priceCents}</em>}</label>
          </div>
          {error && <p className="alert alert--error">{error}</p>}
          <div className="actions">
            <button className="btn btn--primary" disabled={busy || !draft.name.trim() || !draft.price}>{busy ? 'Salvando…' : 'Salvar'}</button>
            <button type="button" className="btn" onClick={() => { setDraft(null); setError(null); setFields({}) }}>Cancelar</button>
          </div>
        </form>
      )}

      {items?.length === 0 && !draft && <div className="card empty">Cadastre seu primeiro serviço para começar a receber agendamentos.</div>}
      <ul className="options">
        {items?.map(s => (
          <li key={s.id} className={`card item${s.active ? '' : ' item--off'}`}>
            <div className="item__main"><strong>{s.name}</strong><small className="muted">{formatDuration(s.durationMin)} · {formatPrice(s.priceCents)}{s.active ? '' : ' · inativo'}</small></div>
            <div className="actions">
              <button className="btn btn--small" onClick={() => setDraft({ id: s.id, name: s.name, durationMin: String(s.durationMin), price: (s.priceCents / 100).toFixed(2).replace('.', ',') })}>Editar</button>
              <button className="btn btn--small" onClick={() => toggle(s)}>{s.active ? 'Desativar' : 'Reativar'}</button>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
