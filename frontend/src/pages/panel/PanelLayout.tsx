import { NavLink, Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../../authContext'

export default function PanelLayout() {
  const { session, signOut } = useAuth()
  if (!session) return <Navigate to="/entrar" replace />
  return (
    <main className="shell">
      <header className="panel-head">
        <div>
          <span className="brand">Agendaki</span>
          <h1 className="page-title">{session.businessName}</h1>
        </div>
        <div className="panel-head__right">
          <a className="btn btn--small" href={`/${session.slug}`} target="_blank" rel="noreferrer">Ver minha página ↗</a>
          <button className="btn btn--small" onClick={signOut}>Sair</button>
        </div>
      </header>
      <nav className="tabs" aria-label="Painel">
        <NavLink to="/painel" end>Agenda</NavLink>
        <NavLink to="/painel/servicos">Serviços</NavLink>
        <NavLink to="/painel/profissionais">Profissionais</NavLink>
      </nav>
      <Outlet />
    </main>
  )
}
