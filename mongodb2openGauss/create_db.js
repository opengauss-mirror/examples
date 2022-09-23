let report_models = require('./data_model.js')
var MongoConnection = require( './mongo_db.js' );
var OpengaussConnection = require( './openguass_db.js' );


// 从数据模型模块创建 Opengauss 数据库

// -------------------------------------


module.exports = async function (complete) {
    // 表名作为索引 0，后跟列和数据类型
    console.log('Creating Opengauss models')

    let all_models = Object.values(report_models.all_models)
    let model_comparison = []

    for (i = 0; i < all_models.length; i++) {
        try {
        console.log('checking table...')
        let all_cols = all_models[i].map(function (x) {
            return x.replace(/ \|.*/g, '')
        })
        let table_name = all_cols.shift()
        console.log(table_name)
        let check_text =
            "SELECT table_name, column_name, is_nullable, udt_name, character_maximum_length FROM information_schema.columns WHERE table_name = '" +
            table_name +
            "'"
        let res = await OpengaussConnection().query(check_text)
        model_comparison = []
        // 如果表存在，请创建数组以与现有表进行比较
        if (res.rows.length !== 0) {
            for (let t = 0; t < res.rows.length; t++) {
            model_comparison.push(
                res.rows[t].column_name
            )
            }
            // 对两个数组进行排序，使其匹配 - 以防模型顺序已更改
            var all_fields = all_cols.map(function (x) {
            return x.replace(/(^[^ ]+)/g, function (y) {
                return y.toLowerCase()
            })
            })
            all_fields_x = all_fields.slice()
            all_fields = all_fields.map(function (x) {
            //返回 x.replace（/ DEFAULT.*/g， ''）.replace（/ UNIQUE.*/g， ''）
                return x.replace(/ .*/g, '') 
            })
            all_fields_x = all_fields_x.map(function (x) {
                return x.replace(/ \|.*/g, '')
            })
            all_fields.sort()
            all_fields_x.sort()
            model_comparison.sort()
            // console.log('all_fields: ', all_fields)
            // console.log('model_comparison: ', model_comparison)

            var toAdd = []
            var toRemove = []
            
            // 删除已删除的列
            model_comparison.map(r => {
            if (all_fields.indexOf(r) < 0) toRemove.push(r.replace(/ .*/g, ''))
            })
            console.log('to remove: ' + toRemove)
            let combine_del = toRemove.join(', DROP COLUMN ')
            let del_text =
            'ALTER TABLE ' + table_name + ' DROP COLUMN ' + combine_del
            if (toRemove.length > 0) {
                console.log(del_text)
                await OpengaussConnection().query(del_text)
            }

            // 插入新列
            all_fields.map((r, i) => {
            if (model_comparison.indexOf(r) < 0) toAdd.push(all_fields_x[i])
            })
            console.log('to add: ' + toAdd)
            let combine_ins = toAdd.join(', ADD COLUMN ')
            let ins_text =
            'ALTER TABLE ' + table_name + ' ADD COLUMN ' + combine_ins
            if (toAdd.length > 0) {
                console.log(ins_text)
                await OpengaussConnection().query(ins_text)
            }
            
        }
        let combine_cols = all_cols.join(', ')
        // 如果表不存在，请创建它
        if (res.rows.length === 0) {
            console.log('creating table...')
            let text = 'CREATE TABLE ' + table_name + '(' + combine_cols + ')'
            console.log(text)
            await OpengaussConnection().query(text)
        }
        } catch (err) {
        console.log(err.stack)
        }
        }       

        console.log('complete')
        if (typeof complete === 'function') complete()
    }