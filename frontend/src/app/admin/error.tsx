"use client";

import { useEffect } from "react";
import { WakingUp } from "@/components/waking-up";

export default function AdminError({ error, retry }: { error: unknown; retry: () => void }) {
  useEffect(() => console.error("[admin]", error), [error]);
  return (
    <main className="flex flex-1 items-center justify-center">
      <WakingUp retry={retry} />
    </main>
  );
}
