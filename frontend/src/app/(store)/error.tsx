"use client";

import { useEffect } from "react";
import { WakingUp } from "@/components/waking-up";

export default function StoreError({ error, retry }: { error: unknown; retry: () => void }) {
  useEffect(() => console.error("[store]", error), [error]);
  return <WakingUp retry={retry} />;
}
