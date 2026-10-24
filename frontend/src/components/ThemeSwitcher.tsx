import { useEffect, useRef, useState } from 'react'
import { THEMES, applyTheme, loadTheme, type ThemeMode } from '../theme'

const GROUPS: { mode: ThemeMode; label: string }[] = [
  { mode: 'dia', label: 'Dia' },
  { mode: 'noite', label: 'Noite' },
]

export default function ThemeSwitcher() {
  const [current, setCurrent] = useState(() => loadTheme())
  const [open, setOpen] = useState(false)
  const root = useRef<HTMLDivElement>(null)

  useEffect(() => { applyTheme(current) }, [current])

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') setOpen(false) }
    const onClick = (e: MouseEvent) => { if (!root.current?.contains(e.target as Node)) setOpen(false) }
    document.addEventListener('keydown', onKey)
    document.addEventListener('mousedown', onClick)
    return () => { document.removeEventListener('keydown', onKey); document.removeEventListener('mousedown', onClick) }
  }, [open])

  const pick = (id: string) => {
    const t = THEMES.find(x => x.id === id)!
    applyTheme(t, true)
    setCurrent(t)
  }

  return (
    <div className="theme" ref={root}>
      {open && (
        <div className="theme__panel" role="group" aria-label="Escolher tema">
          {GROUPS.map(g => (
            <fieldset key={g.mode} className="theme__group">
              <legend>{g.label}</legend>
              <div className="theme__opts">
                {THEMES.filter(t => t.mode === g.mode).map(t => (
                  <button key={t.id} type="button" className="theme__opt" aria-pressed={t.id === current.id} onClick={() => pick(t.id)}>
                    <span className="theme__sw" style={{ background: `linear-gradient(135deg, ${t.bg} 50%, ${t.accent} 50%)` }} />
                    {t.label}
                  </button>
                ))}
              </div>
            </fieldset>
          ))}
        </div>
      )}
      <button type="button" className="theme__toggle" aria-expanded={open} onClick={() => setOpen(o => !o)}>
        {current.mode === 'noite' ? '🌙' : '☀️'} Tema
      </button>
    </div>
  )
}
