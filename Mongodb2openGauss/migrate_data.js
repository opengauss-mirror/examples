let async = require('async')
let moment = require('moment')
let waterfall = require('async-waterfall')
let report_models = require('./data_model.js')
let MongoConnection = require('./mongo_db.js');
var OpenguassConnection = require('./openguass_db.js');

let db

// 一种数据迁移方法及模块。将数据从MongoDB迁移到opengauss的逻辑

// -------------------------------------

let MigrationIsRunning = false

module.exports = function (complete) {

    if (MigrationIsRunning) {
        console.log('Migration script is still running')
        MigrationIsRunning = true
        return
    }
    MigrationIsRunning = true
    console.log('start')

    //从data_model.js中提取所有模型
    let models = Object.getOwnPropertyNames(report_models.all_models)
    let now = moment().format('')
    let models_index = []

    for (let rm in models) {
        models_index.push(rm)
    }

    // 参数定义
    let table_name = ''
    let columns = []
    let latest_update_date = ''
    let latest_create_date = ''
    let columns_string = ''
    let og_columns_String = ''
    let ogUpdateStatement = ''
    let ogInsertStatement = ''
    let no_data_flag = ''
    let og_columns = []
    let updated_at_flag = false
    let created_at_flag = false
    let model_name = ''
    let model_data_type = []
    let og_update_res
    let og_create_res

    let model_transform = async function (base_model, cb) {

        console.log("--------------------------------------model_transform-start--------------------------------------------");


        // 拿到 table name
        let base_model_input = report_models.all_models[models[base_model]]
        let model_input = base_model_input.slice()
        model_data_type = model_input.map(function (x) {
            return x.split(/\s+/)[1]
        })

        table_name = model_input.shift()
        model_name = table_name.replace(/s$/, '')
        model_data_type.shift()

        console.log("table_name: " + table_name);
        console.log("model_name: " + model_name);

        // 检查在字段创建或更新时是否存在
        columns = []
        let index = 0
        for (let m = 0; m < model_input.length; m++) {
            index = model_input[m].indexOf(' ')
            columns.push(model_input[m].substring(0, index))
        }


        // 检查在字段创建或更新时是否存在
        created_at_flag = false
        updated_at_flag = false
        og_columns = []
        og_columns.push(columns[0])
        if (columns.includes('created_at')) {
            og_columns.push('created_at')
            created_at_flag = true
        }
        if (columns.includes('updated_at')) {
            og_columns.push('updated_at')
            updated_at_flag = true
        }

        latest_update_date = ''
        latest_create_date = ''

        columns_string = columns.join(', ')
        og_columns_String = og_columns.join(', ')
        ogUpdateStatement = ''
        ogInsertStatement = ''
        no_data_flag = ''

        
        console.log("columns: " + columns);
        console.log("og columns: " + og_columns);

        console.log("--------------------------------------model_transform-end--------------------------------------------");
       

        cb()
    }



    function json_key(object, key, k) {
        let out_array = [];

        if (typeof(object) === 'undefined') {
            return null
        }
        let json = JSON.parse(JSON.stringify(object))

        if (object === null) {
            return null
        } else {
            if (
                JSON.stringify(json[key]) === 'null' ||
                typeof json[key] === 'undefined'
            ) {

                return null
            } else if (
                JSON.stringify(json[key]).replace(/(\r\n|\n|\r)/gm, '').length > 0
            ) {
                // 不同数据类型的逻辑，即日期 - 时刻，整数 - 如果需要，转换数字
                if (model_data_type[k].indexOf('JSONB') !== -1) {
                    return JSON.stringify(json[key])
                }
                else if (model_data_type[k].indexOf('_') !== -1) {
                    for (j in (json[key]) ) {
                        out_array.push(json[key][j])
                    }
                    return out_array
                }
                else if (model_data_type[k].indexOf('TIMESTAMP') !== -1) {
                    if (json[key] === null) {
                        print('this time is null')
                    }
                    return moment(
                        JSON.stringify(json[key])
                            .replace(/(\r\n|\n|\r)/gm, '')
                            .replace(/"/gm, '')
                    ).format()
                } else if (
                    model_data_type[k].indexOf('INT') !== -1 ||
                    model_data_type[k].indexOf('NUMERIC') !== -1
                ) {
                    return Number(
                        JSON.stringify(json[key])
                            .replace(/(\r\n|\n|\r)/gm, '')
                            .replace(/"/g, '')
                    )
                } else {
                    return JSON.stringify(json[key])
                        .replace(/(\r\n|\n|\r)/gm, '')
                        .replace(/"/gm, '')
                }
            } else {
                return key + ' is an empty string'
            }
        }
        
    }

    function ogUpdateQuery(cols) {
        
        console.log("--------------------------------------ogUpdateQuery-start--------------------------------------------");
        // query静态启动
        let query = ['UPDATE ' + table_name]
        query.push('SET')

        // 创建要更新的列的数组 - 跳过第一列，该列用于 WHERE
        let set = []
        for (let col = 0; col < cols.length - 1; col++) {
            set.push(cols[col + 1] + ' = ($' + (col + 2) + ')')
        }
        query.push(set.join(', '))

        // 添加 WHERE 字段 by id
        query.push('WHERE ' + og_columns[0] + ' = $1')

        
        console.log("--------------------------------------ogUpdateQuery-end--------------------------------------------");


        console.log("query: " + query.join(' '));

        // 返回 查询字符串
        return query.join(' ')
        

    }

    let ogGenerate = async function (cb) {
        
        console.log("--------------------------------------ogGenerate-start--------------------------------------------");
        // 为 INSERT 语句创建 $1、$2、$3
        console.log('ogGenerate')
        let insert_values_array = Array.from({ length: columns.length }, (_v, k) =>
            String('$' + (k + 1))
        )
        let insert_values_string = insert_values_array.join(', ')
        ogInsertStatement =
            'INSERT into ' +
            table_name +
            ' (' +
            columns_string +
            ') VALUES (' +
            insert_values_string +
            ')'
        // 为 og UPDAte 语句创建 $1、$2、$3
        ogUpdateStatement = ogUpdateQuery(columns)
        
        console.log("--------------------------------------ogGenerate-end--------------------------------------------");
        cb()
    }

    // 提取Opengauss中当前存在的数据
    let ogExtract = async function (cb) {
        
        console.log("--------------------------------------ogExtract-start--------------------------------------------");

        console.log('ogExtract')
        let ogUpdateText =
            'SELECT ' +
            og_columns_String +
            ' from ' +
            table_name +
            ' WHERE updated_at IS NOT NULL ORDER BY updated_at DESC LIMIT 5'
        let ogCreateText =
            'SELECT ' +
            og_columns_String +
            ' from ' +
            table_name +
            ' WHERE created_at IS NOT NULL ORDER BY created_at DESC LIMIT 5'
        let ogAllText = 
            'SELECT ' +
            og_columns_String +
            ' from ' +
            table_name +
            ' LIMIT 5'

        if (updated_at_flag) {
            og_update_res = await OpenguassConnection().query(ogUpdateText)
            
            console.log(' og_update_res: ' + og_update_res)
        }
        if (created_at_flag) {
            og_create_res = await OpenguassConnection().query(ogCreateText)
            console.log(' og_update_res: ' + og_create_res)
        }
        og_all_res = await OpenguassConnection().query(ogAllText)

        if (typeof og_all_res.rows[0] === 'undefined') {
            no_data_flag = 'yes'
            console.log('no Opengauss data found')
        } else {
            if (updated_at_flag) {
                latest_update_date = moment(og_update_res.rows[0].updated_at)
                    .add(1, 'seconds')
                    .toISOString()
                console.log('latest updated_at date: ' + latest_update_date)
            }
            if (created_at_flag) {
                latest_create_date = moment(og_create_res.rows[0].created_at)
                    .add(1, 'seconds')
                    .toISOString()
                console.log('latest created_at date: ' + latest_create_date)
            }

        }
        console.log("--------------------------------------ogExtract-end--------------------------------------------");
        cb()
    }

    async function mongoConnect(cb) {
        console.log("--------------------------------------mongoConnect-start--------------------------------------------");
        MongoConnection.connectToServer(function (err, _client) {
            console.log('mongo connect')
            if (err) console.log(err);
            db = MongoConnection.getDb();
            
        console.log("--------------------------------------mongoConnect-end--------------------------------------------");
            cb()
        })

    }

    // -------------------------------------------------------
    async function startMongoExtract(queryType, cMessage, cb) {

        console.log("--------------------------------------startMongoExtract-start--------------------------------------------");
        console.log(queryType)

        if ((no_data_flag == 'yes' && queryType == 'all_data') || (no_data_flag != 'yes' && ((queryType == 'existing_data' && updated_at_flag) || (queryType == 'new_data' && created_at_flag) ))) {
            let count = null
            let found = null
            let limit = 250
            let max = 10000

            while ((found === null || found == limit) && count < max) {

                console.log(found, limit)
                console.log("table_name ===================" + table_name)
                console.log(queryType)

                mongo_data = await new Promise((resolve, reject) => {

                    if (queryType == 'new_data' && created_at_flag) {
                        db.collection(table_name)
                            .find({ created_at: { $gte: new Date(latest_create_date) } })
                            .sort({ created_at: 1 })
                            .skip(count === null ? 0 : count)
                            .limit(limit)
                            .toArray((_err, items) => {
                                resolve(items)
                            });
                    } else if (queryType == 'existing_data' && updated_at_flag) {
                        db.collection(table_name)
                            .find({ updated_at: { $gte: new Date(latest_update_date) } })
                            .sort({ updated_at: 1 })
                            .skip(count === null ? 0 : count)
                            .limit(limit)
                            .toArray((_err, items) => {
                                resolve(items)
                            });
                    } else if (queryType == 'all_data') {
                        db.collection(table_name)
                            .find({})
                            .skip(count === null ? 0 : count)
                            .limit(limit)
                            .toArray((_err, items) => {
                                resolve(items)
                            });
                    } else {
                        reject('error')
                    }

                })

                let rows = []

                for (let md in mongo_data) {
                    try {
                        let data_row = mongo_data[md]
                        console.log(md)

                        var insert_row = []
                        for (let j = 0; j < columns.length; j++) {
                            // 如果需要，在此语句中应用自定义规则，否则将使用默认值
                            // -------------------------------------------------------
                            switch (columns[j]) {
                                //用于自定义规则
                                case 'common_name':
                                    insert_row.push(
                                        json_key(data_row.name, 'common', j)
                                    )
                                    break
                                default:
                                    insert_row.push(
                                        json_key(data_row, columns[j], j)
                                    )
                            }
                            // -------------------------------------------------------
                        }
                    } catch (e) {
                        console.log(e)
                    }
                    rows.push(insert_row)
                }

                found = mongo_data.length
                count += found
                console.log('COUNT:' + count)
                console.log('FOUND:' + found)
                console.log('ROWS:' + rows.length)

                for (r in rows) {
                    try {
                        let values = rows[r]
                        if ( (queryType == 'new_data' && created_at_flag) || queryType == 'all_data') {
                            await OpenguassConnection().query(ogInsertStatement, values)
                        } else if (queryType == 'existing_data' && updated_at_flag) {
                            await OpenguassConnection().query(ogUpdateStatement, values)
                        } else {
                            console.log('No query type')
                        }
                    } catch (err) {
                        console.log(err.stack)
                    }
                }
                console.log(table_name + ' data copied successfully')
            }
            console.log(cMessage)
        }
        
        console.log("--------------------------------------startMongoExtract-end--------------------------------------------");
        cb()
        //})
    }
    // -----------------------------

    async.forEachLimit(
        models_index,
        1,
        function (m, modelcb) {
            
        console.log("--------------------------------------forEachLimit-start--------------------------------------------");
            waterfall([
                async.apply(model_transform, m),
                ogGenerate,
                ogExtract,
                mongoConnect,
                async.apply(startMongoExtract, 'all_data', 'All Data Inserted'),
                async.apply(startMongoExtract, 'new_data', 'New Data Inserted'),
                async.apply(startMongoExtract, 'existing_data', 'Updated Data Inserted'),
                function (cb) {
                    cb()
                }
            ],
                function (_err, _result) {
                    console.log('model ' + m + ' complete')
                    modelcb()
                }
            )
            
        console.log("--------------------------------------forEachLimit-end--------------------------------------------");
        },
        function (err) {
            console.log(err)
            console.log('complete')
            MigrationIsRunning = false
            if (typeof complete === 'function') setTimeout(complete, 1000)
        }
    )
}
