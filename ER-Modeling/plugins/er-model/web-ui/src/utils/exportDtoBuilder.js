export function buildExportDto(database, dialect) {
  const schema = database.schemas[0];
  const tablesDtoMap = new Map();

  // === 阶段一：构建基础表结构 DTO ===
  schema.tables.forEach(table => {
    // 1. 收集主键列
    const primaryKeyColumns = [];
    table.fields.forEach(field => {
      if (field.pk) primaryKeyColumns.push(field.name);
    });
    if (primaryKeyColumns.length === 0) {
      (table.indexes || []).forEach(index => {
        if (index.pk) {
          index.columns.forEach(col => primaryKeyColumns.push(col.value));
        }
      });
    }

    // 2. 构建列信息，并做更健壮的类型 & 长度解析
    const columns = table.fields.map(field => {
      let dataType = (field.type.name || field.type.type_name).split('(')[0];
      let args = Array.isArray(field.type.args) ? field.type.args : (field.type.args ? [field.type.args] : []);

      if (table.name === 'posts') {
        console.log("post表信息：", table)
      }

      if (table.name === 'categories') {
        console.log("categories表信息：", table)
      }

      return {
        columnName:       field.name,
        dataType: dataType,
        length: parseInt(args[0], 10) || null,
        precision: parseInt(args[1], 10) || null,
        nullable:         !field.not_null,
        primaryKey:       !!field.pk,
        unique:           !!field.unique,
        defaultValue: field.dbdefault?.value ?? field.settings?.default ?? null,
        autoIncrement:    !!field.increment,
        comment:          field.note || ""
      };
    });

    // 3. 构建索引信息
    const indexes = (table.indexes || []).map(index => ({
      indexName:   index.name || "",
      columnNames: index.columns.map(col => col.value),
      unique:      !!index.unique,
      indexType:   index.type || "btree",
      comment:     index.note || "",
      pk:          !!index.pk
    }));

    // 4. 初始化 DTO
    tablesDtoMap.set(table.name, {
      tableName:   table.name,
      comment:     table.note || "",
      primaryKeys: primaryKeyColumns.length > 0 ? [primaryKeyColumns] : [],
      columns,
      indexes,
      foreignKeys: []
    });
  });

  // === 阶段二：重构！收集所有 Ref 并统一处理 ===
  const allRefs = new Map();
  const addedFkSignatures = new Set();

  // 1. 从全局 schema.refs 收集
  (schema.refs || []).forEach(ref => {
    // 假设 ref 有一个唯一的 id 属性，用于去重
    if (ref.id != null) allRefs.set(ref.id, ref);
  });

  // 2. 从每个表的每个字段内部收集（针对内联外键的特殊结构）
  schema.tables.forEach(table => {
    table.fields.forEach(field => {
      if (Array.isArray(field.endpoints) && field.endpoints.length > 0) {
        field.endpoints.forEach(endpoint => {
          // 核心！从字段的端点中提取完整的 ref 对象
          if (endpoint.ref && endpoint.ref.id != null) {
            allRefs.set(endpoint.ref.id, endpoint.ref);
          }
        });
      }
    });
  });

  // 3. 遍历收集到的所有唯一的 Ref
  allRefs.forEach(ref => {
    if (ref.endpoints.length !== 2) return;
    const [epA, epB] = ref.endpoints;

    // 3.1 判断 childEp / parentEp (使用我们之前已修正的健壮逻辑)
    let childEp, parentEp;
    if (epA.relation === '*' && epB.relation === '1') {
      childEp = epA; parentEp = epB;
    } else if (epB.relation === '*' && epA.relation === '1') {
      childEp = epB; parentEp = epA;
    } else {
      const tblA = schema.tables.find(t => t.name === epA.tableName);
      const hasInlineA = tblA.fields.some(f =>
        epA.fieldNames.includes(f.name) && (f.ref || (Array.isArray(f.endpoints) && f.endpoints.length > 0))
      );
      if (hasInlineA) { childEp = epA; parentEp = epB; }
      else            { childEp = epB; parentEp = epA; }
    }

    // 3.2 生成规范化签名并去重
    const signature = `${childEp.tableName}[${childEp.fieldNames.join(',')}]→`
      + `${parentEp.tableName}[${parentEp.fieldNames.join(',')}]`;
    if (addedFkSignatures.has(signature)) return;

    // 3.3 写入 DTO
    const dto = tablesDtoMap.get(childEp.tableName);
    if (dto) {
      dto.foreignKeys.push({
        constraintName:        ref.name || null,
        columnNames:           [...childEp.fieldNames],
        referencedTable:       parentEp.tableName,
        referencedColumnNames: [...parentEp.fieldNames],
        onUpdateAction:        (ref.onUpdate || 'NO ACTION').toUpperCase(),
        onDeleteAction:        (ref.onDelete || 'NO ACTION').toUpperCase()
      });
      addedFkSignatures.add(signature);
    }
  });

  return {
    dialect,
    tables: Array.from(tablesDtoMap.values())
  };
}
