const MongoClient = require( 'mongodb' ).MongoClient;
const mongo_json = require('./db_details.json')

// MongoDB database connection module

// -------------------------------------


let url = mongo_json.mongo_connection.url;

let _db;

module.exports = {

  connectToServer: function( callback ) {
    MongoClient.connect( url,  {
      reconnectTries: 30, // 默认也是30次
      reconnectInterval: 6000, //默认是1000ms
      autoReconnect: true, //自动重试
      useUnifiedTopology: true,
    }, function( err, client ) {
      _db  = client.db(mongo_json.mongo_connection.database);
      return callback( err );
    } );
  },

  getDb: function() {
    return _db;
  }
};
