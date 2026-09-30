"use client";

import { useId, useState, type InputHTMLAttributes } from "react";
import { useTranslations } from "next-intl";
import { Eye, EyeOff } from "lucide-react";
import { Input } from "@/components/ui/Field";

/**
 * A password field that can be read back.
 *
 * <p>Typing a password blind on a touch keyboard is how people lock themselves
 * out, and this system locks an account after five failures — so the cost of a
 * mistyped character is not one more attempt, it is a call to an administrator.
 *
 * <p>Starts hidden, and reverts to hidden on every remount, so nothing is left
 * revealed on a shared till.
 */
export function PasswordInput({
  className = "",
  ...rest
}: Omit<InputHTMLAttributes<HTMLInputElement>, "type">) {
  const t = useTranslations("auth");
  const [shown, setShown] = useState(false);
  const describedBy = useId();

  return (
    <div className="relative">
      <Input
        {...rest}
        type={shown ? "text" : "password"}
        // Room for the button, so a long password does not run under it.
        className={`pr-10 ${className}`}
        aria-describedby={describedBy}
      />
      <button
        type="button"
        onClick={() => setShown((v) => !v)}
        aria-pressed={shown}
        aria-label={shown ? t("hidePassword") : t("showPassword")}
        // type="button" matters: inside a form, a bare button submits it, so
        // revealing the password would try to sign in with whatever is typed.
        className="absolute inset-y-0 right-0 grid w-10 place-items-center text-ink-500 hover:text-ink-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-teal-600/40"
      >
        {shown ? <EyeOff size={17} aria-hidden /> : <Eye size={17} aria-hidden />}
      </button>
      {/* Announced once when the field takes focus, rather than on each toggle;
          aria-pressed on the button already reports the state. */}
      <span id={describedBy} className="sr-only">
        {t("passwordToggleHint")}
      </span>
    </div>
  );
}
