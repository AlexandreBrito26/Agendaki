import { Route, Routes } from 'react-router-dom'
import ThemeSwitcher from './components/ThemeSwitcher'
import Home from './pages/Home'
import BookingPage from './pages/BookingPage'
import ManagePage from './pages/ManagePage'
import AuthPage from './pages/AuthPage'
import PanelLayout from './pages/panel/PanelLayout'
import AgendaTab from './pages/panel/AgendaTab'
import ServicesTab from './pages/panel/ServicesTab'
import ProfessionalsTab from './pages/panel/ProfessionalsTab'

export default function App() {
  return (
    <>
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/entrar" element={<AuthPage mode="login" />} />
      <Route path="/cadastro" element={<AuthPage mode="register" />} />
      <Route path="/painel" element={<PanelLayout />}>
        <Route index element={<AgendaTab />} />
        <Route path="servicos" element={<ServicesTab />} />
        <Route path="profissionais" element={<ProfessionalsTab />} />
      </Route>
      <Route path="/:slug" element={<BookingPage />} />
      <Route path="/:slug/agendamento/:token" element={<ManagePage />} />
    </Routes>
    <ThemeSwitcher />
    </>
  )
}
