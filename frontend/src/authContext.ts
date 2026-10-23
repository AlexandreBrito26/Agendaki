import { createContext, useContext } from 'react'
import type { Session } from './ownerApi'

export interface AuthCtx { session: Session | null; signIn: (s: Session) => void; signOut: () => void }
export const AuthContext = createContext<AuthCtx>(null!)
export const useAuth = () => useContext(AuthContext)
