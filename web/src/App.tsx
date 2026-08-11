import { useEffect, useMemo, useState, type ReactNode } from "react"
import {
  Activity,
  Check,
  Leaf,
  Radio,
  RotateCw,
  Target,
  TrendingUp,
  Trophy,
  Users,
  WifiOff,
} from "lucide-react"
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts"
import { cn } from "@/lib/utils"

type Crop = {
  id: string
  name: string
  material: string
  amount: number
  target: number
  percent: number
  complete: boolean
  color: string
}

type Leader = { uuid: string; name: string; total: number; share: number; cropCount: number }
type ActivityItem = {
  id: number
  time: number
  player: string
  cropId: string
  cropName: string
  amount: number
  type: "pickup" | "reset"
}
type HistoryPoint = { time: number; total: number }

type Snapshot = {
  ready: boolean
  generatedAt: number
  challenge: {
    targetPerCrop: number
    cropCount: number
    completedCount: number
    overall: number
    goal: number
    percent: number
    remaining: number
    hourlyRate: number
  }
  server: {
    name: string
    minecraftVersion: string
    pluginVersion: string
    onlinePlayers: number
    maxPlayers: number
  }
  crops: Crop[]
  leaderboard: Leader[]
  activity: ActivityItem[]
  history: HistoryPoint[]
}

type ConnectionState = "loading" | "live" | "reconnecting"

const number = new Intl.NumberFormat("en", { maximumFractionDigits: 0 })
const compact = new Intl.NumberFormat("en", { notation: "compact", maximumFractionDigits: 1 })

function useDashboard() {
  const [snapshot, setSnapshot] = useState<Snapshot | null>(null)
  const [connection, setConnection] = useState<ConnectionState>("loading")

  useEffect(() => {
    let active = true
    const events = new EventSource("/api/v1/events")

    fetch("/api/v1/progress", { cache: "no-store" })
      .then((response) => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`)
        return response.json() as Promise<Snapshot>
      })
      .then((data) => {
        if (!active) return
        setSnapshot(data)
        setConnection("live")
      })
      .catch(() => active && setConnection("reconnecting"))

    events.addEventListener("snapshot", (event) => {
      if (!active) return
      setSnapshot(JSON.parse((event as MessageEvent).data) as Snapshot)
      setConnection("live")
    })
    events.onerror = () => active && setConnection("reconnecting")

    return () => {
      active = false
      events.close()
    }
  }, [])

  return { snapshot, connection }
}

function timeAgo(timestamp: number) {
  const seconds = Math.max(0, Math.floor((Date.now() - timestamp) / 1000))
  if (seconds < 10) return "just now"
  if (seconds < 60) return `${seconds}s ago`
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}m ago`
  return `${Math.floor(minutes / 60)}h ago`
}

function Panel({ className, children }: { className?: string; children: ReactNode }) {
  return <section className={cn("panel", className)}>{children}</section>
}

function Metric({
  label,
  value,
  note,
  icon: Icon,
}: {
  label: string
  value: string
  note: string
  icon: typeof Target
}) {
  return (
    <div className="metric">
      <div className="metric-label">
        <span>{label}</span>
        <Icon aria-hidden="true" />
      </div>
      <strong>{value}</strong>
      <span className="metric-note">{note}</span>
    </div>
  )
}

function CropRow({ crop }: { crop: Crop }) {
  return (
    <div className="crop-row">
      <div className="crop-identity">
        <span className="crop-dot" style={{ backgroundColor: crop.color }} />
        <div>
          <strong>{crop.name}</strong>
          <span>{number.format(crop.amount)} / {number.format(crop.target)}</span>
        </div>
      </div>
      <div className="crop-status">
        <div className="crop-track" aria-label={`${crop.name}: ${crop.percent.toFixed(1)}%`}>
          <span style={{ width: `${Math.min(100, crop.percent)}%`, backgroundColor: crop.color }} />
        </div>
        <span>{crop.complete ? <Check aria-label="Complete" /> : `${crop.percent.toFixed(1)}%`}</span>
      </div>
    </div>
  )
}

function MomentumTooltip({
  active,
  payload,
}: {
  active?: boolean
  payload?: Array<{ value?: number }>
}) {
  if (!active || !payload?.length) return null
  return (
    <div className="chart-tooltip">
      <span>Total harvested</span>
      <strong>{number.format(payload[0]?.value ?? 0)}</strong>
    </div>
  )
}

function LoadingScreen() {
  return (
    <main className="loading-screen">
      <div className="brand-mark"><Leaf /></div>
      <strong>Loading dashboard</strong>
      <span>Connecting to the Minecraft server…</span>
    </main>
  )
}

export default function App() {
  const { snapshot, connection } = useDashboard()
  const [, tick] = useState(0)

  useEffect(() => {
    const timer = window.setInterval(() => tick((value) => value + 1), 10_000)
    return () => window.clearInterval(timer)
  }, [])

  const history = useMemo(() => {
    if (!snapshot) return []
    const points = snapshot.history.length === 1
      ? [{ time: snapshot.history[0].time - 60_000, total: snapshot.history[0].total }, ...snapshot.history]
      : snapshot.history
    return points.map((point) => ({
      ...point,
      label: new Date(point.time).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    }))
  }, [snapshot])

  if (!snapshot) return <LoadingScreen />

  const { challenge, server, crops, leaderboard, activity } = snapshot
  const etaHours = challenge.hourlyRate > 0 ? challenge.remaining / challenge.hourlyRate : 0
  const eta = etaHours > 0
    ? etaHours < 24
      ? `${etaHours.toFixed(1)} hours remaining`
      : `${(etaHours / 24).toFixed(1)} days remaining`
    : "Calculating pace"
  const leadingCrop = crops[0]

  return (
    <main className="dashboard-shell">
      <div className="dashboard">
        <header className="topbar">
          <div className="brand">
            <div className="brand-mark"><Leaf aria-hidden="true" /></div>
            <div>
              <strong>One Million Crops</strong>
              <span>{server.name} · Minecraft {server.minecraftVersion}</span>
            </div>
          </div>
          <div className="server-state">
            <span><Users aria-hidden="true" /> {server.onlinePlayers}/{server.maxPlayers} online</span>
            <span className={cn("connection", connection)}>
              {connection === "live" ? <Radio aria-hidden="true" /> : <RotateCw className="spin" aria-hidden="true" />}
              {connection === "live" ? "Live" : "Reconnecting"}
            </span>
          </div>
        </header>

        <div className="page-heading">
          <div>
            <span className="eyebrow">Challenge overview</span>
            <h1>Harvest dashboard</h1>
            <p>A live view of every pickup, contributor, and crop objective.</p>
          </div>
          <span className="last-updated">Updated {timeAgo(snapshot.generatedAt)}</span>
        </div>

        <div className="metrics-grid">
          <Metric
            label="Total harvested"
            value={number.format(challenge.overall)}
            note={`${compact.format(challenge.remaining)} still to collect`}
            icon={Leaf}
          />
          <Metric
            label="Overall progress"
            value={`${challenge.percent.toFixed(2)}%`}
            note={`${number.format(challenge.goal)} combined goal`}
            icon={Target}
          />
          <Metric
            label="Harvest pace"
            value={`${compact.format(challenge.hourlyRate)}/hr`}
            note={eta}
            icon={TrendingUp}
          />
          <Metric
            label="Completed"
            value={`${challenge.completedCount}/${challenge.cropCount}`}
            note={leadingCrop ? `${leadingCrop.name} leads at ${leadingCrop.percent.toFixed(1)}%` : "No crops tracked"}
            icon={Trophy}
          />
        </div>

        <div className="overview-grid">
          <Panel className="progress-panel">
            <div className="panel-header">
              <div>
                <span className="eyebrow">Global target</span>
                <h2>Overall progress</h2>
              </div>
              <span className="status-pill"><span /> Active season</span>
            </div>
            <div className="progress-summary">
              <div>
                <strong>{challenge.percent.toFixed(2)}<small>%</small></strong>
                <span>of {number.format(challenge.goal)} crops</span>
              </div>
              <div className="progress-breakdown">
                <span><strong>{number.format(challenge.overall)}</strong> harvested</span>
                <span><strong>{number.format(challenge.remaining)}</strong> remaining</span>
              </div>
            </div>
            <div className="overall-track" aria-label={`Overall progress ${challenge.percent.toFixed(2)}%`}>
              <span style={{ width: `${Math.min(100, challenge.percent)}%` }} />
            </div>
            <div className="progress-footer">
              <span>{number.format(challenge.targetPerCrop)} required per crop</span>
              <span>{challenge.cropCount - challenge.completedCount} objectives open</span>
            </div>
          </Panel>

          <Panel className="leaderboard-panel">
            <div className="panel-header">
              <div>
                <span className="eyebrow">Community</span>
                <h2>Top contributors</h2>
              </div>
              <Trophy aria-hidden="true" />
            </div>
            <div className="leader-list">
              {leaderboard.length === 0 && <EmptyState icon={Trophy} title="No harvests yet" />}
              {leaderboard.slice(0, 5).map((player, index) => (
                <div className="leader-row" key={player.uuid}>
                  <span className={cn("rank", index < 3 && `rank-${index + 1}`)}>{index + 1}</span>
                  <div>
                    <strong>{player.name}</strong>
                    <span>{player.cropCount} crops · {player.share.toFixed(1)}% share</span>
                  </div>
                  <strong>{compact.format(player.total)}</strong>
                </div>
              ))}
            </div>
          </Panel>
        </div>

        <div className="activity-grid">
          <Panel className="momentum-panel">
            <div className="panel-header">
              <div>
                <span className="eyebrow">Server session</span>
                <h2>Harvest momentum</h2>
              </div>
              <span className="subtle-label"><Activity aria-hidden="true" /> Live series</span>
            </div>
            <div className="chart-wrap">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={history} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
                  <defs>
                    <linearGradient id="momentum-fill" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="oklch(0.6144 0.1605 267.44)" stopOpacity={0.34} />
                      <stop offset="100%" stopColor="oklch(0.6144 0.1605 267.44)" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid stroke="oklch(0.32 0 0)" vertical={false} />
                  <XAxis dataKey="label" axisLine={false} tickLine={false} tick={{ fill: "oklch(0.72 0 0)", fontSize: 11 }} minTickGap={36} />
                  <YAxis axisLine={false} tickLine={false} tick={{ fill: "oklch(0.72 0 0)", fontSize: 11 }} width={48} tickFormatter={(value) => compact.format(value)} domain={["dataMin", "dataMax"]} />
                  <Tooltip content={<MomentumTooltip />} cursor={{ stroke: "oklch(0.5144 0.1605 267.44)", strokeDasharray: "4 4" }} />
                  <Area type="monotone" dataKey="total" stroke="oklch(0.6144 0.1605 267.44)" strokeWidth={2} fill="url(#momentum-fill)" activeDot={{ r: 4, fill: "oklch(0.2103 0 267.51)", stroke: "oklch(0.7597 0.0804 267.01)", strokeWidth: 2 }} />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </Panel>

          <Panel className="feed-panel">
            <div className="panel-header">
              <div>
                <span className="eyebrow">Latest events</span>
                <h2>Live activity</h2>
              </div>
              <span className="live-dot" />
            </div>
            <div className="feed-list">
              {activity.length === 0 && <EmptyState icon={WifiOff} title="The fields are quiet" />}
              {activity.slice(0, 7).map((item) => (
                <div className="feed-row" key={item.id}>
                  <span className={cn("event-icon", item.type === "reset" && "reset")}>
                    {item.type === "reset" ? <RotateCw /> : <Leaf />}
                  </span>
                  <div>
                    <p><strong>{item.player}</strong> {item.type === "reset" ? "reset" : "harvested"} {item.cropName}</p>
                    <span>{timeAgo(item.time)}</span>
                  </div>
                  {item.type === "pickup" && <strong>+{number.format(item.amount)}</strong>}
                </div>
              ))}
            </div>
          </Panel>
        </div>

        <Panel className="crops-panel">
          <div className="panel-header">
            <div>
              <span className="eyebrow">All objectives</span>
              <h2>Crop progress</h2>
            </div>
            <span className="subtle-label">{crops.length} tracked</span>
          </div>
          <div className="crop-grid">
            {crops.map((crop) => <CropRow key={crop.id} crop={crop} />)}
          </div>
        </Panel>

        <footer>
          <span>OneMillionCrops v{server.pluginVersion}</span>
          <span>Read-only live telemetry</span>
        </footer>
      </div>
    </main>
  )
}

function EmptyState({ icon: Icon, title }: { icon: typeof Trophy; title: string }) {
  return (
    <div className="empty-state">
      <Icon aria-hidden="true" />
      <span>{title}</span>
    </div>
  )
}
