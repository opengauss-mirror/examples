const og = require('pg')
const og_json = require('./db_details.json')

// Postgres database connection module

// -------------------------------------


let og_config = og_json.opengauss_connection

let og_connect = new og.Pool(og_config)

module.exports = () => { return og_connect; }