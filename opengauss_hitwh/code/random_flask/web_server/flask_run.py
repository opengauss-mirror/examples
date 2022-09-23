# coding=utf-8
# encoding:utf-8

from flask import Flask,jsonify,request
import numpy as np
# from utils import plot,calculate
from queue import Queue
import joblib
import base64
# 声明队列
data_queue = Queue(maxsize=5)
temp_dic = {}



app = Flask(__name__)
@app.route('/redirect',methods=['POST','GET'])
def get_pose():
    data = request.args.get("kw")
    rate = request.args.get("rate")
    #str转化成int
    temp1=data.split(".")
    data=temp1[0]
    data=int(data)
    #str转化成int
    temp2=rate.split(".")
    rate=temp2[0]
    rate=int(rate)
    
    print(rate)
    #pro_1为str类型
    #pro_1 = int(rate[1])
    #pro_2 = int(rate[2])
    #if data==2 and pro_1+pro_2>70 and pro_1 >10 :
    #   data =1
    
    temp_dic["result"]=data
    temp_dic["rate"] = rate
    print("rate:", rate)
    print(type(rate))
    # 队列已满
    if data_queue.full():
        data_queue.get()
    data_queue.put(temp_dic)
    print("data:", data)
    return  jsonify({
        "status":200
    })

@app.route('/getdata',methods=['POST','GET'])
def get():
    # 队列为空
    if data_queue.empty():
        return jsonify({
            "result": temp_dic,
            # "rate": temp_dic["rate"],
            "status": 200
        })
    else:
        data_dict = data_queue.get()
        return jsonify({
            "result": data_dict,
            # "rate": 0.0,
            "status": 200
        })





if __name__ == '__main__':
    #app.run(host='0.0.0.0', port=7779)
    app.run(host='0.0.0.0', port=8081, debug=True)
