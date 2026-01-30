import { RegisterForm } from "@/components/auth";

export default function RegisterPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-background to-muted p-4">
      <RegisterForm />
    </div>
  );
}

export const metadata = {
  title: "Register - Splitter",
  description: "Create your Splitter account",
};
