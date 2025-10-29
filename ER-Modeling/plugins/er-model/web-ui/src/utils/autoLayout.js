// Simple layered auto-layout for ER diagrams (inspired by Sugiyama)
// Usage (pseudo):
//   import { computeLayout } from '@/utils/autoLayout'
//   const positions = computeLayout({ tables, refs, getSize: id => store.getTable(id) }, { direction: 'LR' })
//   Object.entries(positions).forEach(([id, p]) => Object.assign(store.getTable(Number(id)), p))

const DEFAULT_NODE_WIDTH = 220
const DEFAULT_NODE_HEIGHT = 120

const defaultOpts = {
  direction: 'LR', // 'LR' | 'TB'
  hGap: 140,
  vGap: 100,
  baseX: 0,
  baseY: 0,
  skipMultiplier: 1.1
}

function buildGraph(tables, refs) {
  const nodes = new Map()
  tables.forEach(t => nodes.set(t.id, { id: t.id, out: new Set(), in: new Set() }))
  for (const r of refs) {
    try {
      const a = r.endpoints?.[0]?.fields?.[0]?.table?.id
      const b = r.endpoints?.[1]?.fields?.[0]?.table?.id
      if (a != null && b != null) {
        // direction: parent -> child (referenced -> referencing)
        nodes.get(a)?.out.add(b)
        nodes.get(b)?.in.add(a)
      }
    } catch (e) { /* ignore */ }
  }
  return nodes
}

function topoLayers(nodes) {
  const indeg = new Map()
  nodes.forEach(n => indeg.set(n.id, n.in.size))
  const layers = []
  const q = []
  nodes.forEach(n => { if (indeg.get(n.id) === 0) q.push(n.id) })
  const visited = new Set()
  while (q.length) {
    const layer = []
    const next = []
    for (const id of q) {
      if (!visited.has(id)) {
        visited.add(id)
        layer.push(id)
        nodes.get(id).out.forEach(v => {
          indeg.set(v, indeg.get(v) - 1)
          if (indeg.get(v) === 0) next.push(v)
        })
      }
    }
    if (layer.length) layers.push(layer)
    q.splice(0, q.length, ...next)
  }
  // remaining (cycles): place them in subsequent layers
  const remaining = [...nodes.keys()].filter(id => !visited.has(id))
  if (remaining.length) layers.push(remaining)
  return layers
}

function orderByBarycenter(layers, nodes) {
  for (let i = 1; i < layers.length; i++) {
    const prev = new Map()
    layers[i-1].forEach((id, idx) => prev.set(id, idx))
    layers[i].sort((a, b) => {
      const score = id => {
        const ins = [...nodes.get(id).in]
        if (!ins.length) return Number.MAX_SAFE_INTEGER
        const sum = ins.reduce((acc, p) => acc + (prev.get(p) ?? 0), 0)
        return sum / ins.length
      }
      return score(a) - score(b)
    })
  }
  return layers
}

function sizeCacheFactory(getSize) {
  const cache = new Map()
  return (id) => {
    if (!cache.has(id)) {
      try {
        const raw = getSize?.(Number(id)) || {}
        const width = Number.isFinite(raw?.width) ? raw.width : DEFAULT_NODE_WIDTH
        const height = Number.isFinite(raw?.height) ? raw.height : DEFAULT_NODE_HEIGHT
        cache.set(id, { width, height })
      } catch (e) {
        cache.set(id, { width: DEFAULT_NODE_WIDTH, height: DEFAULT_NODE_HEIGHT })
      }
    }
    return cache.get(id)
  }
}

function average(values) {
  if (!values?.length) return Number.NaN
  const sum = values.reduce((acc, v) => acc + v, 0)
  return sum / values.length
}

export function computeLayout(ctx, opts = {}) {
  const o = { ...defaultOpts, ...opts }
  const { tables = [], refs = [], getSize } = ctx

  if (!tables.length) {
    return {}
  }

  const nodes = buildGraph(tables, refs)
  let layers = topoLayers(nodes)
  layers = orderByBarycenter(layers, nodes)

  const getSizeCached = sizeCacheFactory(getSize)
  const layerIndex = new Map()
  layers.forEach((layer, idx) => {
    layer.forEach(id => layerIndex.set(id, idx))
  })

  const maxNodeWidth = tables.reduce((max, t) => Math.max(max, getSizeCached(t.id).width), DEFAULT_NODE_WIDTH)

  const positions = {}
  if (o.direction === 'LR') {
    // columns = layers, rows inside each column
    let xCursor = o.baseX
    layers.forEach((column) => {
      const columnNodes = column.map((id, index) => {
        const size = getSizeCached(id)
        const parents = [...(nodes.get(id)?.in ?? [])]
        const parentCenters = parents
          .map(pid => {
            const parentPos = positions[pid]
            if (!parentPos) return Number.NaN
            const parentSize = getSizeCached(pid)
            return parentPos.y + parentSize.height / 2
          })
          .filter(Number.isFinite)

        const parentAvg = average(parentCenters)

        const desiredY = Number.isFinite(parentAvg)
          ? parentAvg - size.height / 2
          : o.baseY + index * (size.height + o.vGap)

        return {
          id,
          order: index,
          size,
          desired: desiredY
        }
      })

      // stable ordering by desired position
      columnNodes.sort((a, b) => {
        if (!Number.isFinite(a.desired) && !Number.isFinite(b.desired)) return a.order - b.order
        if (!Number.isFinite(a.desired)) return 1
        if (!Number.isFinite(b.desired)) return -1
        if (a.desired === b.desired) return a.order - b.order
        return a.desired - b.desired
      })

      const columnWidth = columnNodes.reduce((max, node) => Math.max(max, node.size.width), DEFAULT_NODE_WIDTH)

      let yCursor = o.baseY
      columnNodes.forEach((node) => {
        const safeDesired = Number.isFinite(node.desired) ? node.desired : o.baseY
        const y = Math.max(safeDesired, yCursor)
        positions[node.id] = {
          x: xCursor + (columnWidth - node.size.width) / 2,
          y
        }
        yCursor = y + node.size.height + o.vGap
      })

      xCursor += columnWidth + o.hGap
    })
  } else {
    // TB: rows = layers
    let yCursor = o.baseY
    layers.forEach((row, rowIdx) => {
      const rowNodes = row.map((id, index) => {
        const size = getSizeCached(id)
        const parents = [...(nodes.get(id)?.in ?? [])]
        const parentCenters = parents
          .map(pid => {
            const parentPos = positions[pid]
            if (!parentPos) return Number.NaN
            const parentSize = getSizeCached(pid)
            return parentPos.x + parentSize.width / 2
          })
          .filter(Number.isFinite)

        const parentAvg = average(parentCenters)

        let desiredX = Number.isFinite(parentAvg)
          ? parentAvg - size.width / 2
          : o.baseX + index * (size.width + o.hGap)

        const maxSkip = parents.reduce((acc, pid) => {
          const parentLayer = layerIndex.get(pid)
          if (parentLayer == null) return acc
          return Math.max(acc, rowIdx - parentLayer - 1)
        }, 0)

        if (maxSkip > 0) {
          desiredX += maxSkip * (maxNodeWidth + o.hGap) * o.skipMultiplier
        }

        return {
          id,
          order: index,
          size,
          desired: desiredX
        }
      })

      rowNodes.sort((a, b) => {
        if (!Number.isFinite(a.desired) && !Number.isFinite(b.desired)) return a.order - b.order
        if (!Number.isFinite(a.desired)) return 1
        if (!Number.isFinite(b.desired)) return -1
        if (a.desired === b.desired) return a.order - b.order
        return a.desired - b.desired
      })

      const rowHeight = rowNodes.reduce((max, node) => Math.max(max, node.size.height), DEFAULT_NODE_HEIGHT)
      let xCursor = o.baseX

      rowNodes.forEach((node) => {
        const safeDesired = Number.isFinite(node.desired) ? node.desired : xCursor
        const x = Math.max(safeDesired, xCursor)
        positions[node.id] = {
          x,
          y: yCursor + (rowHeight - node.size.height) / 2
        }
        xCursor = x + node.size.width + o.hGap
      })

      yCursor += rowHeight + o.vGap
    })
  }
  return positions
}

export default {
  computeLayout
}
