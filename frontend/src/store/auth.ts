import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { Role, User } from '@/types/domain';

interface AuthState {
  token?: string;
  user?: User;
  login: (payload: { token: string; id: number; name: string; email: string; role: Role; department: string }) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      login: (payload) =>
        set({
          token: payload.token,
          user: {
            id: payload.id,
            name: payload.name,
            email: payload.email,
            role: payload.role,
            department: payload.department,
          },
        }),
      logout: () => set({ token: undefined, user: undefined }),
    }),
    { name: 'atomquest-auth' },
  ),
);
