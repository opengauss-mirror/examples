import { useEditorStore } from "../store/editor";
import { buildExportDto } from "./exportDtoBuilder";
import { api } from '../boot/axios';
import { triggerDownload } from './download'; // ✅ Import the utility
import { Notify } from 'quasar';

export async function exportSqlFile(dialect) {
  const editor = useEditorStore();
  const database = editor.database;
  const exportDto = buildExportDto(database, dialect);

  try {
    const response = await api.post('/export/sql', exportDto, {
      responseType: 'text'
    });

    const sqlContent = response.data;
    const dataUrl = `data:text/sql;charset=utf-8,${encodeURIComponent(sqlContent)}`;

    // ✅ Use the shared download utility
    triggerDownload(dataUrl, 'er-diagram.sql');

  } catch (err) {
    console.error('导出 SQL 失败:', err);
  }
}

/**
 * 调用后端 API，将 DBML 文本转换为 SQL。
 * @param {string} dbmlText - 包含 DBML 定义的字符串。
 * @param {string} dialect - 目标数据库方言 (例如 "mysql", "postgresql", "opengauss")。
 */
export async function exportSqlFromDbml(dbmlText, dialect) {
  try {
    // 检查 dbmlText 是否为空
    if (!dbmlText || !dbmlText.trim()) {
      console.error('DBML content is empty. Cannot export SQL.');
      Notify.create({ type: 'warning', message: 'DBML内容为空，无法导出。' });
      return;
    }

    // 将数据库方言作为 URL 的查询参数
    const apiUrl = `/export/sql-from-dbml?dialect=${encodeURIComponent(dialect.toUpperCase())}`;

    const response = await api.post(apiUrl, dbmlText, {
      headers: { 'Content-Type': 'text/plain' },
      responseType: 'json'  // 改成json
    });

    console.log("response: ", response.data)
    console.log("response content: ", response.data.sqlContent)

    const sql = response.data.sqlContent;
    triggerDownload(`data:text/sql;charset=utf-t,${encodeURIComponent(sql)}`, `er-export-${dialect}.sql`);
  } catch (err) {
    console.error('导出 SQL 失败', err);
    // 向用户显示更友好的错误信息
    const errorMessage = err.response?.data || '导出失败，请检查后端服务是否正常。';
    Notify.create({ type: 'negative', message: `导出 SQL 失败: ${errorMessage}` });
  }
}
