const { Sequelize, Model, DataTypes } = require('sequelize');
const express = require('express')
const app = express()

const sequelize = new Sequelize('postgres://mostima:iLuvExusiai1@localhost:15400/lattland');

const PowerStat = sequelize.define('PowerStat', {
    user: DataTypes.STRING,
    startTimestamp: DataTypes.BIGINT,
    endTimestamp: DataTypes.BIGINT,
    kiloWatts: DataTypes.FLOAT,
});

app.get('/update', async (req, res) => {
    let user = req.query.user;
    let startTimestamp = req.query.startTimestamp;
    let endTimestamp = req.query.endTimestamp;
    let kiloWatts = req.query.kiloWatts;
    await PowerStat.create({
        user,startTimestamp,endTimestamp,kiloWatts
    });
    res.send('OK')

})

app.get('/get', async (req, res) => {
    result = await PowerStat.findAll();
    res.send(result)
})

app.listen(3000, async () => {
    await sequelize.sync();
    console.log(`app listening.`)
})