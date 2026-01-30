import { LoginForm } from "@/components/auth";

export default function LoginPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-background to-muted p-4">
      <LoginForm />
    </div>
  );
}

export const metadata = {
  title: "Login - Splitter",
  description: "Sign in to your Splitter account",
};
