import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { Target } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { api } from '@/api/client';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { useAuthStore } from '@/store/auth';

const schema = z.object({
  email: z.string().email(),
  password: z.string().min(6),
});

type FormValues = z.infer<typeof schema>;

export function LoginPage() {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const { register, handleSubmit, formState } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: 'employee@demo.com', password: '123456' },
  });

  async function onSubmit(values: FormValues) {
    try {
      const { data } = await api.post('/auth/login', values);
      login(data);
      toast.success('Welcome to ATOMQUEST');
      navigate('/');
    } catch {
      toast.error('Invalid credentials');
    }
  }

  return (
    <main className="grid min-h-screen place-items-center px-4">
      <Card className="w-full max-w-md">
        <div className="mb-7 flex items-center gap-3">
          <div className="grid h-12 w-12 place-items-center rounded-lg bg-primary text-white"><Target /></div>
          <div>
            <h1 className="text-2xl font-bold">ATOMQUEST</h1>
            <p className="text-sm text-slate-500">Enterprise goal tracking portal</p>
          </div>
        </div>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="text-sm font-medium">Email</label>
            <Input {...register('email')} />
          </div>
          <div>
            <label className="text-sm font-medium">Password</label>
            <Input type="password" {...register('password')} />
          </div>
          <Button className="w-full" disabled={formState.isSubmitting}>Login</Button>
        </form>
        <div className="mt-5 grid gap-2 text-xs text-slate-500">
          <button className="text-left" onClick={() => loginAs('employee@demo.com')}>Employee: employee@demo.com</button>
          <button className="text-left" onClick={() => loginAs('manager@demo.com')}>Manager: manager@demo.com</button>
          <button className="text-left" onClick={() => loginAs('admin@demo.com')}>Admin: admin@demo.com</button>
        </div>
      </Card>
    </main>
  );

  async function loginAs(email: string) {
    const { data } = await api.post('/auth/login', { email, password: '123456' });
    login(data);
    navigate('/');
  }
}
