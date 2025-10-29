import { defineStore } from "pinia";
import { markRaw } from "vue";
import { computeLayout } from "../utils/autoLayout";

const DEFAULT_LAYOUT_WIDTH = 220;
const DEFAULT_LAYOUT_HEIGHT = 120;
const DEFAULT_BASE_X = 160;
const DEFAULT_BASE_Y = 120;
const DEFAULT_H_GAP = 160;
const DEFAULT_V_GAP = 140;

export const useChartStore = defineStore("chart", {
  state: () => ({
    zoom: 1.0,
    pan: { x: 0, y: 0 },
    ctm: [1, 0, 0, 1, 0, 0],
    inverseCtm: [1, 0, 0, 1, 0, 0],
    tableGroups: {},
    tables: {},
    refs: {},
    grid: {
      size: 100,
      divisions: 10,
      snap: 5
    },
    loaded: false,
    panEnabled: true,
    schemaDefinition: {
      tables: [],
      refs: [],
      tableGroups: []
    },
    tooltip: {
      x: 0,
      y: 0,
      show: false,
      target: null,
      component: null,
      binds: null,
      width: 0,
      height: 0
    },
    highlightedRefs: []
  }),
  getters: {
    subGridSize(state) {
      return state.grid.size / state.grid.divisions;
    },
    persistenceData(state) {
      const { zoom, pan, ctm, inverseCtm, tables, refs } = state;
      return  { zoom, pan, ctm, inverseCtm, tables, refs };
    },
    getPan(state) {
      return state.pan;
    },
    getZoom(state) {
      return state.zoom;
    },
    getCTM(state) {
      return state.ctm;
    },
    getTable(state) {
      return (tableId) => {
        if (!(tableId in state.tables))
          state.tables[tableId] = {
            x: 0,
            y: 0,
            width: 200,
            height: 32
          };
        return state.tables[tableId];
      };
    },
    getTableGroup(state) {
      return (tableGroupId) => {
        if (!(tableGroupId in state.tableGroups))
          state.tableGroups[tableGroupId] = {
            x: 0,
            y: 0,
            width: 200,
            height: 32
          };
        return state.tableGroups[tableGroupId];
      };
    },
    getRef(state) {
      return (refId) => {
        if (!(refId in state.refs))
          state.refs[refId] = {
            endpoints: [],
            vertices: [],
            auto: true
          };
        return state.refs[refId];
      };
    },
    save(state) {
      return {
        zoom: state.zoom,
        pan: state.pan,
        ctm: state.ctm,
        inverseCtm: state.inverseCtm,
        tables: state.tables,
        refs: state.refs,
        grid: state.grid
      };
    }
  },
  actions: {
    showTooltip(target, component, binds) {
      const width = target?.width ?? 220;
      const height = target?.height ?? 72;
      const offsetX = target?.x ?? 0;
      const offsetY = target?.y ?? 0;
      this.tooltip = {
        x: offsetX,
        y: offsetY,
        width,
        height,
        component: markRaw(component),
        binds,
        show: true
      };
    },
    hideTooltip() {
      this.tooltip = {
        x: 0,
        y: 0,
        width: 0,
        height: 0,
        component: null,
        binds: null,
        show: false
      };
    },
    loadDatabase(database = {}) {
      this.tableGroups = {};
      this.tables = {};
      this.refs = {};
      this.loaded = false;

      const schema = database.schemas && database.schemas.length > 0
        ? database.schemas[0]
        : { tableGroups: [], tables: [], refs: [] };

      for (const tableGroup of schema.tableGroups || []) {
        if (tableGroup?.id != null) {
          this.getTableGroup(tableGroup.id);
        }
      }
      for (const table of schema.tables || []) {
        if (table?.id != null) {
          this.getTable(table.id);
        }
      }
      for (const ref of schema.refs || []) {
        if (ref?.id != null) {
          this.getRef(ref.id);
        }
      }
      this.schemaDefinition = {
        tables: schema.tables || [],
        refs: schema.refs || [],
        tableGroups: schema.tableGroups || []
      };
      this.loaded = true;
      requestAnimationFrame(() => {
        this.autoLayoutGrid();
      });
    },
    load(state) {
      this.$reset();
      this.$patch({
        ...state,
        ctm: DOMMatrix.fromMatrix(state.ctm),
        inverseCtm: DOMMatrix.fromMatrix(state.inverseCtm).inverse()
      });
    },
    updatePan(newPan) {
      this.$patch({
        pan: {
          x: newPan.x,
          y: newPan.y
        }
      });
    },

    updateZoom(newZoom) {
      this.$patch({
        zoom: newZoom
      });
    },

    updateCTM(newCTM) {
      this.$patch({
        ctm: DOMMatrix.fromMatrix(newCTM),
        inverseCtm: DOMMatrix.fromMatrix(newCTM).inverse()
      });
    },

    updateTable(tableId, newTable) {
      this.tables.$patch({
        [tableId]: newTable
      });
    },
    updateRef(refId, newRef) {
      this.refs.$patch({
        [refId]: newRef
      });
    },
    setPanEnabled(enabled) {
      this.panEnabled = enabled
    },
    enablePan() {
      this.panEnabled = true
    },
    disablePan() {
      this.panEnabled = false
    },
    setHighlightedRefs(ids) {
      this.highlightedRefs = Array.from(new Set(ids || []))
    },
    clearHighlightedRefs() {
      this.highlightedRefs = []
    },
    // Apply positions from an auto-layout result: { [tableId]: {x, y} }
    applyLayoutPositions(positions) {
      Object.entries(positions).forEach(([id, p]) => {
        const t = this.getTable(Number(id))
        t.x = p.x
        t.y = p.y
      })
    },
    autoLayoutGrid() {
      const schema = this.schemaDefinition || { tables: [], refs: [] }
      const tables = schema.tables || []
      if (!tables.length) return

      const refs = schema.refs || []
      const getSize = (id) => getTableSize(this, id)

      if (!refs.length) {
        const fallbackPositions = buildBalancedGrid(tables, getSize, {
          baseX: DEFAULT_BASE_X,
          baseY: DEFAULT_BASE_Y,
          hGap: DEFAULT_H_GAP,
          vGap: DEFAULT_V_GAP
        })
        this.applyLayoutPositions(fallbackPositions)
        return
      }

      const positions = computeLayout(
        {
          tables,
          refs,
          getSize
        },
        {
          direction: "LR",
          baseX: DEFAULT_BASE_X,
          baseY: DEFAULT_BASE_Y,
          hGap: DEFAULT_H_GAP,
          vGap: DEFAULT_V_GAP
        }
      )

      this.applyLayoutPositions(positions)
    },
    autoLayoutVertical() {
      const schema = this.schemaDefinition || { tables: [], refs: [] }
      const tables = schema.tables || []
      if (!tables.length) return

      const refs = schema.refs || []
      const getSize = (id) => getTableSize(this, id)

      if (!refs.length) {
        const fallbackStack = buildVerticalStack(tables, getSize, {
          baseX: DEFAULT_BASE_X + 40,
          baseY: DEFAULT_BASE_Y,
          vGap: DEFAULT_V_GAP + 40
        })
        this.applyLayoutPositions(fallbackStack)
        return
      }

      const positions = computeLayout(
        {
          tables,
          refs,
          getSize
        },
        {
          direction: "TB",
          baseX: DEFAULT_BASE_X,
          baseY: DEFAULT_BASE_Y,
          hGap: DEFAULT_H_GAP,
          vGap: DEFAULT_V_GAP + 40,
          skipMultiplier: 1.25
        }
      )

      this.applyLayoutPositions(positions)
    }
  }
});

function getTableSize(store, id) {
  const state = store.getTable(Number(id))
  const width = Number.isFinite(state?.width) ? state.width : DEFAULT_LAYOUT_WIDTH
  const height = Number.isFinite(state?.height) ? state.height : DEFAULT_LAYOUT_HEIGHT
  return { width, height }
}

function buildBalancedGrid(tables, getSize, opts = {}) {
  const {
    baseX = DEFAULT_BASE_X,
    baseY = DEFAULT_BASE_Y,
    hGap = DEFAULT_H_GAP,
    vGap = DEFAULT_V_GAP
  } = opts

  if (!tables.length) return {}

  const cols = Math.ceil(Math.sqrt(tables.length))
  const rows = Math.ceil(tables.length / cols)

  const columnWidths = Array(cols).fill(0)
  const rowHeights = Array(rows).fill(0)
  const meta = []

  tables.forEach((table, index) => {
    const col = index % cols
    const row = Math.floor(index / cols)
    const size = getSize(table.id)

    columnWidths[col] = Math.max(columnWidths[col], size.width)
    rowHeights[row] = Math.max(rowHeights[row], size.height)
    meta.push({ id: table.id, col, row, size })
  })

  const colOffsets = []
  let xCursor = baseX
  columnWidths.forEach((width, idx) => {
    colOffsets[idx] = xCursor
    xCursor += width + hGap
  })

  const rowOffsets = []
  let yCursor = baseY
  rowHeights.forEach((height, idx) => {
    rowOffsets[idx] = yCursor
    yCursor += height + vGap
  })

  const positions = {}
  meta.forEach(({ id, col, row, size }) => {
    positions[id] = {
      x: colOffsets[col] + (columnWidths[col] - size.width) / 2,
      y: rowOffsets[row] + (rowHeights[row] - size.height) / 2
    }
  })

  return positions
}

function buildVerticalStack(tables, getSize, opts = {}) {
  const {
    baseX = DEFAULT_BASE_X,
    baseY = DEFAULT_BASE_Y,
    vGap = DEFAULT_V_GAP
  } = opts

  if (!tables.length) return {}

  const widths = tables.map(table => getSize(table.id).width)
  const maxWidth = widths.reduce((max, width) => Math.max(max, width), DEFAULT_LAYOUT_WIDTH)

  let yCursor = baseY
  const positions = {}

  tables.forEach(table => {
    const size = getSize(table.id)
    positions[table.id] = {
      x: baseX + (maxWidth - size.width) / 2,
      y: yCursor
    }
    yCursor += size.height + vGap
  })

  return positions
}
