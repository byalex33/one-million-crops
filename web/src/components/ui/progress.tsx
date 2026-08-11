import { cn } from "@/lib/utils"

function Progress({ value = 0, className, indicatorClassName }: { value?: number; className?: string; indicatorClassName?: string }) {
  return (
    <div data-slot="progress" className={cn("relative h-1.5 w-full overflow-hidden rounded-full bg-white/[0.07]", className)}>
      <div className={cn("h-full rounded-full bg-lime-400 transition-[width] duration-700 ease-out", indicatorClassName)} style={{ width: `${Math.max(0, Math.min(100, value))}%` }} />
    </div>
  )
}

export { Progress }
