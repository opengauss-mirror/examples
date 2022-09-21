# import pyrealsense2 as rs
# import math
# import time
#
# """
# 俯仰角pitch：绕x轴旋转，重力加速度与"X-Y平面"的夹角，即重力加速度在Z轴上的分量与重力加速度在"X-Y平面"投影的正切
#             正角度是前倾，符合深度和彩色相机Z轴的正方向，负角度是后仰
#
# 翻滚角roll：绕z轴旋转，重力加速度与"Z-Y平面"的夹角，即重力加速度在X轴上的分量与重力加速度在"Z-Y平面"投影的正切
#             正角度是左相机到右相机，符合深度和彩色相机X轴的正方向 。
# """
#
# if __name__ == "__main__":
#     # 相机配置
#     pipeline = rs.pipeline()
#     config = rs.config()
#     config.enable_stream(rs.stream.accel, rs.format.motion_xyz32f)
#     config.enable_stream(rs.stream.gyro, rs.format.motion_xyz32f)
#     pipeline.start(config)
#
#     align_to = rs.stream.accel
#     align = rs.align(align_to)
#     while True:
#         frames = pipeline.wait_for_frames()
#         aligned_frames = align.process(frames)
#
#         accel = aligned_frames[0].as_motion_frame().get_motion_data()    # 获取加速度的数据
#         # gyro = aligned_frames[1].as_motion_frame().get_motion_data()
#
#         # ax = aligned_frames[0].as_motion_frame().get_motion_data().x
#         # ay = aligned_frames[0].as_motion_frame().get_motion_data().y
#         # az = aligned_frames[0].as_motion_frame().get_motion_data().z
#
#         # 重力加速度在X-Y面上的投影
#         pitch_xy = math.sqrt(accel.x * accel.x + accel.y * accel.y)
#         # 重力加速度在Z轴上的分量与在X-Y面上的投影的正切，即俯仰角
#         pitch = math.atan2(-accel.z, pitch_xy) * 57.3    # 57.3 = 180/3.1415
#         # 重力加速度在Z-Y面上的投影
#         roll_zy = math.sqrt(accel.z * accel.z + accel.y * accel.y)
#         # 重力加速度在X轴上的分量与在Z-Y面上的投影的正切，即翻滚角
#         roll = math.atan2(-accel.x, roll_zy) * 57.3
#
#         # 打印姿态角信息
#         print("roll:%.3f, pitch:%.3f" % (roll, pitch))
#         time.sleep(1)

import pyrealsense2 as rs
import numpy as np
import math
import time
from socket import *
import joblib
import requests
import re

def initialize_camera():
    # start the frames pipe
    p = rs.pipeline()
    conf = rs.config()
    conf.enable_stream(rs.stream.accel)
    conf.enable_stream(rs.stream.gyro)
    prof = p.start(conf)
    return p


def gyro_data(gyro):
    return np.asarray([gyro.x, gyro.y, gyro.z])


def accel_data(accel):
    return np.asarray([accel.x-0.0699096458233893, accel.y+9.057815720176697, accel.z-2.816199101638794])

p = initialize_camera()
aver_accelx = 0 # 平地平均值：0.0699096458233893
aver_accely = 0 # 平地平均值：-9.057815720176697
aver_accelz = 0 # 平地平均值：2.816199101638794

uploadHOST = '192.168.12.1'  # 数据上传服务器地址
uploadPORT = 7001 # 数据上传端口
uploadADDR = (uploadHOST, uploadPORT)
tcpCliSock = socket(AF_INET, SOCK_STREAM)
tcpCliSock.connect(uploadADDR)

try:
    while True:
        # for i in range(10000):
            f = p.wait_for_frames()
            accel  = accel_data(f[0].as_motion_frame().get_motion_data())
            # aver_accelx += accel[0]
            # aver_accely += accel[1]
            # aver_accelz += accel[2]
            print("accelerometer: ", accel)
            # gyro = gyro_data(f[1].as_motion_frame().get_motion_data())
            # print("gyro: ", gyro)

            # 重力加速度在X-Y面上的投影
            pitch_xy = math.sqrt(accel[0] * accel[0] + accel[1] * accel[1])
            # 重力加速度在Z轴上的分量与在X-Y面上的投影的正切，即俯仰角
            pitch = math.atan2(-accel[2], pitch_xy) * 57.3    # 57.3 = 180/3.1415
            # 重力加速度在Z-Y面上的投影
            roll_zy = math.sqrt(accel[2] * accel[2] + accel[1] * accel[1])
            # 重力加速度在X轴上的分量与在Z-Y面上的投影的正切，即翻滚角
            roll = math.atan2(-accel[0], roll_zy) * 57.3

            # 打印姿态角信息
            print("roll:%.3f, pitch:%.3f" % (roll, pitch))
            str_data = str(accel[0]) + ',' + str(accel[1]) + ',' + str(accel[2])
            print(str_data)
            tcpCliSock.send(bytes(str_data, 'utf-8'))
            time.sleep(1)
        # print("aver_accelx: ", aver_accelx/10000, "aver_accely: ", aver_accely/10000, "aver_accely: ", aver_accelz/10000)
        # time.sleep(20)
finally:
    p.stop()
