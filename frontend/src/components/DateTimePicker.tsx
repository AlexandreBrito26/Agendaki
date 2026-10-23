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

/** Faixa de dias + grade de horários livres. Busca a disponibilidade a cada dia escolhido. */
export default function DateTimePicker({
  slug, timezone, serviceId, professionalId, date, time, onChange, daysAhead = 30,
}: Props) {
  const today = useMemo(() => todayIn(timezone), [timezone])
  const days = useMemo(() => Array.from({ length: daysAhead }, (_, i) => addDays(today, i)), [today, daysAhead])
  const [slots, setSlots] = useState<string[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const stripRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!date) return
    let cancelled = false
    api.availability(slug, serviceId, professionalId, date)
      .then(r => { if (!cancelled) { setSlots(r.slots); setError(null) } })
      .catch(e => { if (!cancelled) { setSlots([]); setError(e.message) } })
    return () => { cancelled = true }
  }, [slug, serviceId, professionalId, date])

  const scroll = (dir: 1 | -1) =>
    stripRef.current?.scrollBy({ left: dir * 260, behavior: 'smooth' })

  const pick = (d: string) => {
    if (d === date) return
    setSlots(null)
    onChange(d, null)
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

      {date && (
        <div className="dtp__slots" aria-live="polite">
          {slots === null && <p className="muted">Buscando horários…</p>}
          {error && <p className="alert alert--error">{error}</p>}
          {slots && !error && slots.length === 0 && (
            <p className="muted">Sem horários livres neste dia. Tente outro dia.</p>
          )}
          {slots && slots.length > 0 && (
            <div className="slots">
              {slots.map(s => (
                <button
                  key={s}
                  type="button"
                  className={`slot${s === time ? ' slot--on' : ''}`}
                  onClick={() => onChange(date, s)}
                >
                  {s}
                </button>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
