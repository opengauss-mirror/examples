import { jsPDF } from 'jspdf';
import { triggerDownload } from './download'; // 仍然使用你现有的下载工具
import { api } from '../boot/axios';          // 新增：调用后端 AjaxResult 接口

/**
 * 内部工具：根据来源获取 PlantUML 文本
 * @param {'db'|'dbml'} plantumlSource 来源类型
 * @param {object} dbParams 当来源为 'db' 时需要：{ clusterId, nodeId, databaseName, schemaName }
 * @param {string} dbmlText 当来源为 'dbml' 时需要：纯 DBML 文本
 * @returns {Promise<string>} 纯 PlantUML 文本
 */
async function fetchPlantUml({ plantumlSource, dbParams, dbmlText }) {
  if (plantumlSource === 'db') {
    const res = await api.get('/export/plantuml-from-db', { params: dbParams });
    console.log("plantUML-res-db: ", res)
    if (res?.code !== 200) {
      throw new Error(res?.msg || '导出 PlantUML 失败（DB）');
    }
    return res.data; // AjaxResult.data
  }

  if (plantumlSource === 'dbml') {
    // 后端 ExportController 接口：@PostMapping("/plantuml-from-dbml")，@RequestBody String
    const res = await api.post('/export/plantuml-from-dbml', dbmlText, {
      headers: { 'Content-Type': 'text/plain; charset=utf-8' }
    });
    console.log("plantUML-res-dbml: ", res)
    if (res?.code !== 200) {
      throw new Error(res?.msg || '导出 PlantUML 失败（DBML）');
    }
    return res.data; // AjaxResult.data
  }

  throw new Error('plantumlSource 必须是 "db" 或 "dbml"');
}

/**
 * 将SVG元素及其所有子元素的计算样式（Computed Styles）应用为内联样式。
 */
function inlineAllStyles(svgElement) {
  for (const el of svgElement.querySelectorAll('*')) {
    const computedStyle = window.getComputedStyle(el);
    const inlineStyle = [];
    const propertiesToInline = [
      'fill', 'fill-opacity', 'stroke', 'stroke-width', 'stroke-opacity',
      'stroke-linecap', 'stroke-linejoin', 'font-family', 'font-size',
      'font-weight', 'font-style', 'color', 'text-anchor', 'dominant-baseline',
      'vertical-align', 'opacity', 'display', 'transform', 'cursor',
      'pointer-events', 'user-select',
    ];
    const forceInlineProperties = new Set(['fill', 'stroke', 'display']);
    for (const prop of propertiesToInline) {
      const value = computedStyle.getPropertyValue(prop);
      if (value && (forceInlineProperties.has(prop) || (value !== 'normal' && value !== 'auto'))) {
        inlineStyle.push(`${prop}:${value}`);
      }
    }
    if (inlineStyle.length) {
      const existingStyle = el.getAttribute('style') || '';
      el.setAttribute('style', `${existingStyle}; ${inlineStyle.join('; ')}`);
    }
  }
}

/**
 * 精确计算所有可见内容元素的视觉边界框（Bounding Box）。
 */
function calculateTightBBox(svgRoot, contentSelector) {
  const elements = svgRoot.querySelectorAll(contentSelector);
  if (elements.length === 0) {
    return { x: 0, y: 0, width: svgRoot.width.baseVal.value, height: svgRoot.height.baseVal.value };
  }
  const svgRect = svgRoot.getBoundingClientRect();
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
  elements.forEach(el => {
    const rect = el.getBoundingClientRect();
    minX = Math.min(minX, rect.left);
    minY = Math.min(minY, rect.top);
    maxX = Math.max(maxX, rect.right);
    maxY = Math.max(maxY, rect.bottom);
  });
  const padding = 5;
  return {
    x: (minX - svgRect.left) - padding,
    y: (minY - svgRect.top) - padding,
    width: (maxX - minX) + (padding * 2),
    height: (maxY - minY) + (padding * 2),
  };
}

/**
 * 根据SVG元素创建一个高分辨率的Canvas，并填充白色背景。
 */
function createHighResCanvas(svgElement, tightBBox, scale) {
  return new Promise((resolve, reject) => {
    const scaledWidth = tightBBox.width * scale;
    const scaledHeight = tightBBox.height * scale;

    const svgStr = `<svg xmlns="http://www.w3.org/2000/svg" width="${scaledWidth}" height="${scaledHeight}" viewBox="${tightBBox.x} ${tightBBox.y} ${tightBBox.width} ${tightBBox.height}">
                      ${svgElement.innerHTML}
                    </svg>`;

    const canvas = document.createElement('canvas');
    canvas.width = scaledWidth;
    canvas.height = scaledHeight;
    const ctx = canvas.getContext('2d');

    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    const img = new Image();
    const svgBlob = new Blob([svgStr], { type: 'image/svg+xml;charset=utf-8' });
    const url = URL.createObjectURL(svgBlob);

    img.onload = function () {
      ctx.drawImage(img, 0, 0);
      URL.revokeObjectURL(url);
      resolve(canvas);
    };
    img.onerror = function (e) {
      console.error("加载SVG图片到Image对象失败", e);
      URL.revokeObjectURL(url);
      reject(new Error("Image loading failed"));
    };
    img.src = url;
  });
}

/**
 * 导出视觉文件（PNG / PDF / PlantUML）
 *
 * @param {object} options
 * @param {'png'|'pdf'|'plantuml'} options.format
 * @param {string} options.fileName 不含扩展名
 * @param {number} options.scale PNG/PDF 分辨率倍数（默认 3）
 * @param {'db'|'dbml'} [options.plantumlSource] 当 format='plantuml' 时必填
 * @param {object} [options.dbParams] 当 plantumlSource='db'：{ clusterId, nodeId, databaseName, schemaName }
 * @param {string} [options.dbmlText] 当 plantumlSource='dbml'：DBML 文本
 */
export async function exportVisualFile({
                                         format = 'png',
                                         fileName = 'diagram',
                                         scale = 3,
                                         plantumlSource,
                                         dbParams,
                                         dbmlText
                                       }) {
  // --------- 新增：PlantUML 分支（不依赖 SVG/Canvas）---------
  if (format === 'plantuml') {
    try {
      const puml = await fetchPlantUml({ plantumlSource, dbParams, dbmlText });
      if (!puml || !puml.trim()) {
        throw new Error('后端未返回有效的 PlantUML 内容');
      }

      // 用 Blob/URL 触发下载（更稳，避免超长 data: URL）
      const blob = new Blob([puml], { type: 'text/plain;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      triggerDownload(url, `${fileName}.puml`);
      // 交给浏览器下载后再释放 URL
      setTimeout(() => URL.revokeObjectURL(url), 0);
    } catch (err) {
      console.error('导出 PlantUML 失败：', err);
    }
    return;
  }

  // --------- 以下为原有 PNG / PDF 流程（依赖 SVG/Canvas）---------
  const svgElement = document.querySelector('svg.db-chart');
  if (!svgElement) {
    console.error('未找到 SVG 元素');
    return;
  }

  const backgroundLayer = svgElement.querySelector('#background-layer');
  const toolsLayer = svgElement.querySelector('#tools-layer');
  const hitboxes = svgElement.querySelectorAll('.db-ref__hitbox');

  try {
    if (backgroundLayer) backgroundLayer.style.display = 'none';
    if (toolsLayer) toolsLayer.style.display = 'none';
    hitboxes.forEach(box => { box.style.display = 'none'; });

    inlineAllStyles(svgElement);
    const tightBBox = calculateTightBBox(svgElement, '.db-table, .db-ref__path');

    const canvas = await createHighResCanvas(svgElement, tightBBox, scale);
    if (!canvas) return;

    if (format === 'png') {
      const pngDataUrl = canvas.toDataURL('image/png');
      triggerDownload(pngDataUrl, `${fileName}.png`);

    } else if (format === 'pdf') {
      const imageData = canvas.toDataURL('image/jpeg', 0.9);
      const { width, height } = canvas;
      const orientation = width > height ? 'l' : 'p';

      const pdf = new jsPDF({
        orientation: orientation,
        unit: 'pt',
        format: [width, height]
      });

      pdf.addImage(imageData, 'JPEG', 0, 0, width, height);

      const pdfBlob = pdf.output('blob');
      const pdfUrl = URL.createObjectURL(pdfBlob);
      triggerDownload(pdfUrl, `${fileName}.pdf`);
      URL.revokeObjectURL(pdfUrl);
    } else {
      console.error('不支持的导出格式：', format);
    }

  } catch (error) {
    console.error("导出文件时发生错误:", error);
  } finally {
    if (backgroundLayer) backgroundLayer.style.display = '';
    if (toolsLayer) toolsLayer.style.display = '';
    hitboxes.forEach(box => { box.style.display = ''; });
  }
}
