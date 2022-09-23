import numpy as np
import math,requests
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
import joblib
from sklearn.metrics import accuracy_score
import numpy as np
def open_file(filename,flag):
    data = []
    with open(filename, "r", encoding="utf-8") as f:
        for line in f.readlines():
            try:
                line = [float(x) for x in line.split(',')[:-1]]
                line.append(flag)
                data.append(line)
            except AttributeError:
                print(line)
    return data
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
    for i in dataset:
        if (math.fabs(last_cos-i[0])>0.09):
            left_head_num+=1
        if i[0] > 0.6:
            right_head_num+=1
        elif math.fabs(i[1]-last_node)>0.1 or i[1]<0.4:
            nod_num+=1
        # elif i[2]-i[3] > 0.20 and i[2]-i[3] <0.6:
        #     left_turn+=1
        # elif i[3]-i[2] > 0.20 and i[3]-i[2] <0.6:
        #     right_turn+=1
        elif math.fabs(i[5]-i[4]) > 0.5:
            turn+=1
        elif (i[6]!=-1 and eye_left_last_frame != -1 and math.fabs(eye_left_last_frame-i[6])>0.18)or (i[6]!=-1 and i[6]<0.20):
            eye_left+=1
        elif (i[7]!=-1 and eye_right_last_frame != -1 and math.fabs(eye_right_last_frame-i[7])>0.18)or (i[7]!=-1 and i[7]<0.20 ):
            eye_right+=1
        if i[8]!=-1and i[8]<0.2:
            #print(i[8])
            mouth_sec+=1
        left_turn = math.fabs(i[2] - i[3])
        right_turn = math.fabs(i[5] - i[4])
        eye_left_last_frame = i[6]
        eye_right_last_frame = i[7]
        last_node = i[1]
        last_cos = i[0]
        #print(left_head_num)
    feature = [left_head_num,right_head_num,nod_num,left_turn,right_turn,turn,eye_left,eye_right,mouth_sec,dataset[0][9]]
    return feature

def get_feature1(dataset):
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
    for i in dataset:
        if (math.fabs(last_cos-i[0])>0.09):
            left_head_num+=1
        if i[0] > 0.6:
            right_head_num+=1
        elif math.fabs(i[1]-last_node)>0.1 or i[1]<0.4:
            nod_num+=1
        # elif i[2]-i[3] > 0.20 and i[2]-i[3] <0.6:
        #     left_turn+=1
        # elif i[3]-i[2] > 0.20 and i[3]-i[2] <0.6:
        #     right_turn+=1
        elif math.fabs(i[5]-i[4]) > 0.5:
            turn+=1
        elif (i[6]!=-1 and eye_left_last_frame != -1 and math.fabs(eye_left_last_frame-i[6])>0.18)or (i[6]!=-1 and i[6]<0.20):
            eye_left+=1
        elif (i[7]!=-1 and eye_right_last_frame != -1 and math.fabs(eye_right_last_frame-i[7])>0.18)or (i[7]!=-1 and i[7]<0.20 ):
            eye_right+=1
        if i[8]!=-1and i[8]<0.2:
            #print(i[8])
            mouth_sec+=1
        left_turn = math.fabs(i[2] - i[3])
        right_turn = math.fabs(i[5] - i[4])
        eye_left_last_frame = i[6]
        eye_right_last_frame = i[7]
        last_node = i[1]
        last_cos = i[0]
        #print(left_head_num)
    feature = [left_head_num,right_head_num,nod_num,left_turn,right_turn,turn,eye_left,eye_right,mouth_sec]
    return feature


def data_mining(dataset,frame_rate):
    temp = []
    mining_data = []
    for i in dataset:
        temp.append(i)
        if len(temp)==frame_rate:
            mining_data.append(get_feature(temp))
            temp = []
    return mining_data

def randomForest(feature,flag,a,b):
    X_train, X_test, y_train, y_test = train_test_split(feature,flag, test_size=0.2,random_state=0)
    rnd_clf = RandomForestClassifier(n_estimators=2000, max_leaf_nodes=12, n_jobs=-1)
    rnd_clf.fit(X_train,y_train)
    print(a)
    print(b)
    x =  rnd_clf.predict(X_test)
    y_pr_rf = rnd_clf.predict(a)
    i = rnd_clf.predict([b])
    print(y_pr_rf,i)
    #por = rnd_clf.predict_proba(X_test)
    #predict = y_pr_rf.tolist()
    #print((por[:,1]+por[:,2])*100)
    print("准确率",accuracy_score(y_test, x))
    print(x)
    print(y_test)
    # joblib.dump(rnd_clf, "train_model_7.m")

def get_result(data):
    rf = joblib.load(r'/train_model_7.m')
    r = rf.predict_proba(data)
    print(r)
    return rf.predict(data), (r[:, 1] + r[:, 2]) * 100

if __name__=='__main__':
    feature_data = []
    a = [[3.0, 0.0, 1.0, 0.009999999999999898, 0.0, 0.0, 4.0, 0.0, 7.0], [0.0, 0.0, 1.0, 0.010000000000000009, 0.010000000000000009, 0.0, 1.0, 0.0, 7.0], [0.0, 0.0, 1.0, 0.009999999999999898, 0.0, 0.0, 0.0, 0.0, 7.0], [3.0, 0.0, 1.0, 0.05999999999999994, 0.14, 0.0, 2.0, 2.0, 7.0], [3.0, 0.0, 1.0, 0.0, 0.020000000000000018, 0.0, 2.0, 0.0, 7.0], [1.0, 0.0, 1.0, 0.8899999999999999, 0.8999999999999999, 1.0, 5.0, 0.0, 7.0], [2.0, 0.0, 1.0, 0.009999999999999898, 0.010000000000000009, 2.0, 0.0, 0.0, 7.0]]
    b = [[1.0, 0.0, 1.0, 0.8599999999999999, 1.29, 1.0, 5.0, 0.0, 7.0], [1.0, 0.0, 1.0, 0.9, 1.3599999999999999, 6.0, 0.0, 0.0, 7.0], [3.0, 0.0, 1.0, 0.0, 0.010000000000000009, 3.0, 2.0, 0.0, 7.0], [0.0, 0.0, 1.0, 0.009999999999999898, 0.0, 0.0, 3.0, 0.0, 7.0], [0.0, 0.0, 1.0, 0.010000000000000009, 0.010000000000000009, 0.0, 4.0, 2.0, 6.0], [0.0, 0.0, 1.0, 0.010000000000000009, 0.0, 0.0, 0.0, 0.0, 7.0], [0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 2.0, 0.0, 7.0]]
    # data_set_0 = open_file(r"data1/正常.txt",0)
    # data_set_1 = open_file(r"data1/轻微疲劳.txt",1)
    # data_set_2 = open_file(r"data1/重度疲劳.txt",2)
    # data_set_0 =data_mining(open_file(r"../data1/正常.txt", 0), 7)
    #data_set_1 = data_mining(open_file(r"../data1/轻微疲劳.txt", 1), 7)
    data_set_2 = data_mining(open_file(r"data1/重度疲劳.txt", 2), 7)
    baseurl = 'http://127.0.0.1:8081/redirect'
    #feature_data.extend(data_set_0)
    #feature_data.extend(data_set_1)
    feature_data.extend(data_set_2)
    feature_data = np.array(feature_data)
    print(feature_data)
    temp = []
    data = feature_data[:,:9].tolist()
    print(data)
    for i in data:
        result = get_result([i])
        params = {
            'kw': result[0],
            "rate": result[1]
        }

        res = requests.get(baseurl, params=params)
        #print(res.content)
        temp = []

    #randomForest(feature_data[:,:9],feature_data[:,9],np.array(data_set_0)[:,:9],get_feature1(b))

    # f = open(r'feature','w') #把数据写入database_dict.txt中 可能总数据会导致内存不足 所以以后可能会把字符串切片储存
    # f.write(str(feature_data))
    # f.close()



