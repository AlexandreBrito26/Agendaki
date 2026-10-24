export type ThemeMode = 'dia' | 'noite'
export interface Theme { id: string; label: string; mode: ThemeMode; bg: string; accent: string }

export const THEMES: Theme[] = [
  { id: 'floresta', label: 'Floresta', mode: 'dia', bg: '#faf6ee', accent: '#14532d' },
  { id: 'oceano', label: 'Oceano', mode: 'dia', bg: '#f1f7fb', accent: '#0b5d8a' },
  { id: 'vinho', label: 'Vinho', mode: 'dia', bg: '#fbf3f3', accent: '#8c1d40' },
  { id: 'ambar', label: 'Âmbar', mode: 'dia', bg: '#fff8ee', accent: '#b45309' },
  { id: 'noite', label: 'Noite', mode: 'noite', bg: '#0f1a14', accent: '#5fd38d' },
  { id: 'meia-noite', label: 'Meia-noite', mode: 'noite', bg: '#0b1220', accent: '#60a5fa' },
  { id: 'grafite', label: 'Grafite', mode: 'noite', bg: '#121212', accent: '#fbbf24' },
  { id: 'ameixa', label: 'Ameixa', mode: 'noite', bg: '#150f1f', accent: '#c084fc' },
]

const KEY = 'agendaki-theme'
const find = (id: string | null) => THEMES.find(t => t.id === id)

/** Tema salvo; sem escolha salva, segue o modo claro/escuro do sistema. */
export function loadTheme(): Theme {
  try {
    const saved = find(localStorage.getItem(KEY))
    if (saved) return saved
  } catch { /* storage indisponível */ }
  const dark = typeof matchMedia === 'function' && matchMedia('(prefers-color-scheme: dark)').matches
  return THEMES[dark ? 4 : 0]
}

export function applyTheme(theme: Theme, persist = false) {
  document.documentElement.dataset.theme = theme.id
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', theme.bg)
  if (persist) {
    try { localStorage.setItem(KEY, theme.id) } catch { /* ignora */ }
  }
}
