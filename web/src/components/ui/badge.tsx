import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"

const badgeVariants = cva("inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[.12em]", {
  variants: {
    variant: {
      live: "border-lime-400/20 bg-lime-400/10 text-lime-300",
      outline: "border-white/10 bg-white/[0.03] text-[#a6aea2]",
      gold: "border-amber-300/20 bg-amber-300/10 text-amber-200",
    },
  },
  defaultVariants: { variant: "outline" },
})

function Badge({ className, variant, ...props }: React.ComponentProps<"span"> & VariantProps<typeof badgeVariants>) {
  return <span data-slot="badge" className={cn(badgeVariants({ variant }), className)} {...props} />
}

export { Badge, badgeVariants }
