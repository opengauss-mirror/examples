# coding=utf-8
# encoding:utf-8

from socket import *
import joblib,math
import numpy as np
import requests
import re

def get_result(data):
    rf = joblib.load("/home/hit-ices/randomforest/test_model.m")
    r = rf.predict_proba(data)
    #return rf.predict(data), (r* 100).tolist()
    return rf.predict(data), (r[:, 1] + r[:, 2]) * 100

def get_feature(dataset):
    left_head_num = 0
    right_head_num = 0
    nod_num = 0
    last_node = 0
    left_turn = 0
    right_turn = 0
    turn = 0
    eye_left = 0
    eye_left_last_frame = 0
    eye_right = 0
    eye_right_last_frame = 0
    mouth_sec = 0
    last_cos =0
    
    numberDataSet = [] # 二维数组，共7行，每行为一帧的9个特征值
    for str in dataset:
    	tmp = str.split(",")
    	tmpNumberData = []
    	for tmpStr in tmp:
    	    tmpNumberData.append(float(tmpStr))
    	numberDataSet.append(tmpNumberData)
    
    print("接收的7条数据", numberDataSet, "\n")
    for i in numberDataSet:
        if (math.fabs(last_cos-i[0])>0.09 and math.fabs(last_cos-i[0])<2 and last_cos!=0):
            left_head_num+=1
        #if i[0] > 0.6:
        #    right_head_num+=1
        if (math.fabs(i[1]-last_node)>0.1 and math.fabs(i[1]-last_node)<2 and last_node!=0) or (i[1]<0.4 and i[1]>-2):
            print("nod ,math.fabs(i[1]-last_node): ", math.fabs(i[1]-last_node))
            nod_num+=1
        # elif i[2]-i[3] > 0.20 and i[2]-i[3] <0.6:
        #     left_turn+=1
        # elif i[3]-i[2] > 0.20 and i[3]-i[2] <0.6:
        #     right_turn+=1
        if math.fabs(i[5]-i[4]) > 0.5 and math.fabs(i[5]-i[4])<5:
            print("turn ,math.fabs(i[5]-i[4]): ", math.fabs(i[5]-i[4]))
            turn+=1
        if (i[6]!=-1 and eye_left_last_frame != -1 and math.fabs(eye_left_last_frame-i[6])>0.18) or (i[6]!=-1 and i[6]<0.20):
            eye_left+=1
        if (i[7]!=-1 and eye_right_last_frame != -1 and math.fabs(eye_right_last_frame-i[7])>0.18) or (i[7]!=-1 and i[7]<0.20):
            eye_right+=1
        if i[8]!=-1 and i[8]>0.5:
            print("mouth_sec ,i[8]: ", i[8])
            mouth_sec+=1
        if math.fabs(i[2] - i[3])<5:
            left_turn += math.fabs(i[2] - i[3])
        if math.fabs(i[5] - i[4])<5:
            right_turn += math.fabs(i[5] - i[4])
        eye_left_last_frame = i[6]
        eye_right_last_frame = i[7]
        last_node = i[1]
        last_cos = i[0]
        right_head_num += i[0]
        #print(left_head_num)
    left_head_num = int(left_head_num)
    right_head_num = int(right_head_num)
    nod_num = int(nod_num)
    left_turn = int(left_turn)
    right_turn = int(right_turn)
    turn = int(turn)
    eye_left = int(eye_left)
    eye_right = int(eye_right)
    mouth_sec = int(mouth_sec)
    feature = [left_head_num, right_head_num, nod_num,
               left_turn, right_turn, turn,
               eye_left, eye_right, mouth_sec]
    return feature

def initServer():
    # baseurl = 'http://118.195.157.233:8081/redirect'
    # baseurl = 'http://192.168.137.1:8081/redirect'
    server = socket()  # 声明socket类型，并且生成socket连接对象
    uploadHOST = '192.168.1.108'  # 数据上传服务器地址
    uploadPORT = 10244
    uploadADDR = (uploadHOST, uploadPORT)
    tcpCliSock = socket(AF_INET, SOCK_STREAM)
    tcpCliSock.connect(uploadADDR)
    serverHost = '192.168.1.108'
    serverPort = 7000
    server.setsockopt(SOL_SOCKET, SO_REUSEADDR, 1)
    server.bind((serverHost, serverPort))  # 把服务器绑定到7000端口上
    server.listen(5)  # 开始监听
    old_result = 0
    print("等待连接中……")
    temp = []
    while True:
        conn, addr = server.accept()  # 接收连接
        print("***连接成功***")
        print("addr: ", addr)
        while True:
            dataLength = conn.recv(2)
            dataLength = dataLength.decode('UTF-8')
            # print("data length： ", dataLength)
            tmpList = re.findall(r"\d+\.?\d*",dataLength)
            if len(tmpList) == 0:
            	continue
            dataLength = tmpList[0]
            data = conn.recv(int(dataLength))  # 接收客户发来的数据
            data = data.decode('UTF-8')
            print("接收到的data为：", data)
            # print("data中的个数：", len(data.split(",")))
            if (len(data.split(",")) != 9):
                continue
            temp.append(data)
            if len(temp)==7:
                result = get_result(np.array([get_feature(temp)]))
                #print(result[1])
                #r=tolist(result[1])
                #print(result[1])
                #print(type(result[1]))
                params = {
                    'kw': result[0],
                    "rate":result[1]
                }
                #print("result: ",result)
                #print("result.type: ",result[0].dtype())
                predict_result = result[0]
                print("result[0]: ",result[0])
                if old_result==[1] and predict_result==[0]:
                    predict_result = [1]
                if old_result==[2]:
                    predict_result = [2]
                str_result = str(predict_result)
                old_result = predict_result
                #print(type(str_result))
                tcpCliSock.send(bytes(str_result[1], 'utf-8'))
                # res = requests.get(baseurl,params=params)
                temp= []
            if not data:
                print("客户断开连接")
                break
    server.close()


if __name__ == '__main__':
   initServer()

