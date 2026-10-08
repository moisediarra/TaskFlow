import { Bar, BarChart, CartesianGrid, LabelList, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { TooltipContentProps } from 'recharts'
import { Tooltip as UiTooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip'

/*
 * Chart tokens (validated with the dataviz palette validator on the white card surface):
 * - single series: teal-600, passes band, chroma and 3:1 contrast;
 * - pipeline: ordinal teal ramp light→dark (Backlog→Done), monotone with visible steps.
 * TaskFlow is light-only (dark mode is a future feature, claude.md §42).
 */
const SERIES = '#0d9488'
const PIPELINE = ['#14b8a6', '#0d9488', '#0f766e', '#134e4a']
const INK = '#1e293b'
const MUTED = '#64748b'
const GRID = '#e2e8f0'

export interface PipelineSegment {
  label: string
  value: number
}

/** Part-to-whole of the four columns as one bar, with a legend that carries every value. */
export function PipelineBar({ segments }: { segments: PipelineSegment[] }) {
  const total = segments.reduce((sum, segment) => sum + segment.value, 0)
  const visible = segments.map((segment, index) => ({ ...segment, color: PIPELINE[index] })).filter((segment) => segment.value > 0)
  return (
    <figure>
      {total === 0 ? (
        <div className="h-6 rounded-md bg-muted" aria-hidden />
      ) : (
        <div className="flex h-6 gap-[2px]" aria-hidden>
          {visible.map((segment) => (
            <UiTooltip key={segment.label}>
              <TooltipTrigger asChild>
                <div
                  className="h-full min-w-1 transition-opacity hover:opacity-85 first:rounded-l-[4px] last:rounded-r-[4px]"
                  style={{ flexGrow: segment.value, backgroundColor: segment.color }}
                />
              </TooltipTrigger>
              <TooltipContent>
                <span className="font-semibold">{segment.value}</span> {segment.label.toLowerCase()} ·{' '}
                {Math.round((segment.value / total) * 100)}%
              </TooltipContent>
            </UiTooltip>
          ))}
        </div>
      )}
      <figcaption>
        <ul className="mt-3 grid grid-cols-2 gap-x-4 gap-y-1.5 text-sm sm:grid-cols-4">
          {segments.map((segment, index) => (
            <li key={segment.label} className="flex items-center gap-2">
              <span className="size-2.5 shrink-0 rounded-[3px]" style={{ backgroundColor: PIPELINE[index] }} aria-hidden />
              <span className="text-muted-foreground">{segment.label}</span>
              <span className="ml-auto font-semibold tabular-nums sm:ml-0">{segment.value}</span>
              <span className="text-xs text-muted-foreground tabular-nums">
                {total > 0 ? `${Math.round((segment.value / total) * 100)}%` : ''}
              </span>
            </li>
          ))}
        </ul>
      </figcaption>
    </figure>
  )
}

export interface BarDatum {
  name: string
  value: number
}

interface HorizontalBarsProps {
  data: BarDatum[]
  valueLabel: string
  /** Optional reference value, e.g. the workload warning threshold. */
  threshold?: { value: number; label: string }
  categoryWidth?: number
}

/** One series of horizontal bars: value at the tip, hairline grid, value-first tooltip, table twin. */
export function HorizontalBars({ data, valueLabel, threshold, categoryWidth = 112 }: HorizontalBarsProps) {
  const rowHeight = 34
  const height = data.length * rowHeight + 36
  const max = Math.max(threshold?.value ?? 0, ...data.map((datum) => datum.value), 1)
  return (
    <figure>
      <div style={{ height }} aria-hidden>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} layout="vertical" margin={{ top: 4, right: 36, bottom: 4, left: 0 }} barCategoryGap={8}>
            <CartesianGrid horizontal={false} stroke={GRID} strokeWidth={1} />
            <XAxis
              type="number"
              domain={[0, Math.ceil(max * 1.1)]}
              allowDecimals={false}
              tick={{ fill: MUTED, fontSize: 12 }}
              axisLine={{ stroke: GRID }}
              tickLine={false}
            />
            <YAxis
              type="category"
              dataKey="name"
              width={categoryWidth}
              tick={{ fill: INK, fontSize: 12 }}
              axisLine={false}
              tickLine={false}
              interval={0}
            />
            <Tooltip
              cursor={{ fill: 'rgba(15, 23, 42, 0.04)' }}
              content={({ active, payload, label }) => (
                <ValueTooltip active={active} value={payload?.[0]?.value} label={label} unit={valueLabel} />
              )}
            />
            {threshold ? (
              <ReferenceLine
                x={threshold.value}
                stroke="#b45309"
                strokeWidth={1}
                label={{ value: threshold.label, position: 'insideTopRight', fill: '#92400e', fontSize: 11 }}
              />
            ) : null}
            <Bar dataKey="value" fill={SERIES} barSize={18} radius={[0, 4, 4, 0]} isAnimationActive={false}>
              <LabelList dataKey="value" position="right" fill={INK} fontSize={12} />
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
      <table className="sr-only">
        <caption>{valueLabel}</caption>
        <tbody>
          {data.map((datum) => (
            <tr key={datum.name}>
              <th scope="row">{datum.name}</th>
              <td>{datum.value}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </figure>
  )
}

function ValueTooltip({ active, payload, label, unit }: TooltipContentProps<number, string> & { unit: string }) {
  if (!active || !payload?.length) return null
  return (
    <div className="rounded-lg border bg-popover px-3 py-2 text-xs shadow-md">
      <p className="flex items-center gap-2">
        <span className="h-0.5 w-3 rounded-full" style={{ backgroundColor: SERIES }} aria-hidden />
        <span className="text-sm font-semibold text-foreground">{payload[0].value}</span>
        <span className="text-muted-foreground">{unit}</span>
      </p>
      <p className="mt-0.5 text-muted-foreground">{label}</p>
    </div>
  )
}
