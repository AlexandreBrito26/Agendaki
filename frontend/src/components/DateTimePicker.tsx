import { useEffect, useMemo, useRef, useState } from 'react'
import { api } from '../api'
import { addDays, dayNumber, monthShort, todayIn, weekdayShort } from '../lib/format'

interface Props {
  slug: string
  timezone: string
  serviceId: number
  professionalId: number
  date: string | null
  time: string | null
  onChange: (date: string, time: string | null) => void
  daysAhead?: number
}

const PERIODS = [
  { label: 'Manhã', test: (h: number) => h < 12 },
  { label: 'Tarde', test: (h: number) => h >= 12 && h < 18 },
  { label: 'Noite', test: (h: number) => h >= 18 },
]

/** Faixa de dias + grade de horários livres (por período). Busca a disponibilidade a cada dia escolhido. */
export default function DateTimePicker({
  slug, timezone, serviceId, professionalId, date, time, onChange, daysAhead = 30,
}: Props) {
  const today = useMemo(() => todayIn(timezone), [timezone])
  const days = useMemo(() => Array.from({ length: daysAhead }, (_, i) => addDays(today, i)), [today, daysAhead])
  const [loaded, setLoaded] = useState<{ date: string; slots: string[]; error: string | null } | null>(null)
  const [searching, setSearching] = useState(false)
  const [searchMsg, setSearchMsg] = useState<string | null>(null)
  const stripRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!date) return
    let cancelled = false
    api.availability(slug, serviceId, professionalId, date)
      .then(r => { if (!cancelled) setLoaded({ date, slots: r.slots, error: null }) })
      .catch(e => { if (!cancelled) setLoaded({ date, slots: [], error: e.message }) })
    return () => { cancelled = true }
  }, [slug, serviceId, professionalId, date])

  // mantém o dia escolhido visível na faixa
  useEffect(() => {
    stripRef.current?.querySelector('.day--on')?.scrollIntoView?.({ inline: 'center', block: 'nearest', behavior: 'smooth' })
  }, [date])

  const current = loaded && loaded.date === date ? loaded : null // null = carregando este dia
  const slots = current?.slots ?? []

  const scroll = (dir: 1 | -1) => stripRef.current?.scrollBy({ left: dir * 260, behavior: 'smooth' })
  const pick = (d: string) => { if (d !== date) { setSearchMsg(null); onChange(d, null) } }

  /** Procura o próximo dia (até 14 à frente) que tenha algum horário livre. */
  const findNext = async () => {
    if (!date) return
    setSearching(true); setSearchMsg(null)
    try {
      for (let i = 1; i <= 14; i++) {
        const d = addDays(date, i)
        if (!days.includes(d)) break
        const r = await api.availability(slug, serviceId, professionalId, d)
        if (r.slots.length > 0) { onChange(d, null); return }
      }
      setSearchMsg('Não encontramos horários livres nos próximos dias. Fale direto com o negócio.')
    } catch {
      setSearchMsg('Não foi possível buscar agora. Tente de novo.')
    } finally {
      setSearching(false)
    }
  }

  return (
    <div className="dtp">
      <div className="dtp__strip-wrap">
        <button type="button" className="dtp__arrow" onClick={() => scroll(-1)} aria-label="Dias anteriores">‹</button>
        <div className="dtp__strip" ref={stripRef} role="listbox" aria-label="Dia">
          {days.map(d => (
            <button
              key={d}
              type="button"
              role="option"
              aria-selected={d === date}
              className={`day${d === date ? ' day--on' : ''}`}
              onClick={() => pick(d)}
            >
              <span className="day__wk">{d === today ? 'hoje' : weekdayShort(d)}</span>
              <span className="day__n">{dayNumber(d)}</span>
              <span className="day__m">{monthShort(d)}</span>
            </button>
          ))}
        </div>
        <button type="button" className="dtp__arrow" onClick={() => scroll(1)} aria-label="Próximos dias">›</button>
      </div>

      {!date && <p className="hint">Escolha um dia para ver os horários livres.</p>}

      {date && (
        <div className="dtp__slots" aria-live="polite" aria-busy={!current}>
          {!current && (
            <div className="slots" aria-label="Buscando horários">
              {Array.from({ length: 8 }, (_, i) => <span key={i} className="skel skel--slot" />)}
            </div>
          )}
          {current?.error && <p className="alert alert--error">{current.error}</p>}
          {current && !current.error && slots.length === 0 && (
            <div className="empty-day">
              <p className="muted">Sem horários livres neste dia.</p>
              <button type="button" className="btn btn--small" onClick={findNext} disabled={searching}>
                {searching ? 'Procurando…' : 'Ver o próximo dia com horário'}
              </button>
              {searchMsg && <p className="muted">{searchMsg}</p>}
            </div>
          )}
          {PERIODS.map(p => {
            const group = slots.filter(s => p.test(Number(s.slice(0, 2))))
            return group.length === 0 ? null : (
              <div key={p.label} className="period">
                <h3 className="period__label">{p.label}</h3>
                <div className="slots">
                  {group.map(s => (
                    <button key={s} type="button" className={`slot${s === time ? ' slot--on' : ''}`} onClick={() => onChange(date, s)}>
                      {s}
                    </button>
                  ))}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
