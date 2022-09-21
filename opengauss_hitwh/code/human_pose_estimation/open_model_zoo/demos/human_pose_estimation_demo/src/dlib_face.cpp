#pragma GCC diagnostic push
// #pragma GCC diagnostic ignored "-Wunused-variable"
// #pragma GCC diagnostic ignored "-unused-but-variable-set"

#include "dlib_face.hpp"

using namespace cv;
using namespace dlib;
using namespace std;

std::string Convert(float Num)
{
    std::ostringstream oss;
    oss << Num;
    std::string str(oss.str());
    return str;
}

dlib::shape_predictor loadModel()
{

    dlib::shape_predictor pos_model;
    //下面用到的shape_predictor_68_face_landmarks/shape_predictor_68_face_landmarks.dat
    dlib::deserialize("/home/hit-ices/intel/shape_predictor_68_face_landmarks.dat") >> pos_model;
    return pos_model;
}


dlib::frontal_face_detector loadDetector()
{
    dlib::frontal_face_detector detector = dlib::get_frontal_face_detector();
    return detector;
}

std::string eye_func(cv::Mat temp, dlib::shape_predictor pos_model, dlib::frontal_face_detector detector)
{

    //  QThread::sleep(6);
    //画坐标轴
    //Mat Eye_Waveform = Mat::zeros(900, 900, CV_8UC3);    //用于记录眨眼的波形图
    //Point p1 = Point(10, 0);
    //Point p2 = Point(10, 900);
    //Point p3 = Point(0, 890);
    //Point p4 = Point(900, 890);
    //Scalar line_color = Scalar(255, 255, 255);
    //cv::line(Eye_Waveform, p1, p2, line_color, 1, LINE_AA);
    //cv::line(Eye_Waveform, p3, p4, line_color, 1, LINE_AA);

    //存储眼睛的上一个点的坐标
    //int eye_previous_x = 10;  //原点横坐标
    //int eye_previous_y = 890; //原点纵坐标
    //int eye_now_x = 1;
    //int eye_now_y = 1;

    //存储眨眼的次数
    //float count_blink = 0; //眨眼次数
    //每次眨眼EAR都要经历从  大于0.2-小于0.2-大于0.2 的过程
    //float blink_EAR_before = 0.0; //眨眼前
    //float blink_EAR_now = 0.2;    //眨眼中
    //float blink_EAR_after = 0.0;  //眨眼后
    
    //水平翻转图像
    //flip(temp, temp, 1);
    //dlib::frontal_face_detector detector = dlib::get_frontal_face_detector();
    //转化成gray图像
    Mat gray;
    cvtColor(temp,gray,COLOR_BGR2GRAY);
    //cv_image<bgr_pixel> cimg(temp);
    array2d<dlib::bgr_pixel> cimg;
    assign_image(cimg, cv_image<uchar>(gray)); 
   
    std::vector<dlib::rectangle> faces = detector(cimg);
    
    std::vector<full_object_detection> shapes;
    int faceNumber = faces.size(); //获取容器中向量的个数即人脸的个数
    for (int i = 0; i < faceNumber; i++)
    {
        shapes.push_back(pos_model(cimg, faces[i]));
    }
    //std::cout << "shapes_human_demo: " << shapes.size() << std::endl;
    std::string res;
    if (!shapes.empty())
    {
        //68个关键点的描绘
        int faceNumber = shapes.size();
        for (int j = 0; j < faceNumber; j++)
        {
            for (int i = 0; i < 68; i++)
            {
                //用来画特征值的点
                cv::circle(temp, cvPoint(shapes[j].part(i).x(), shapes[j].part(i).y()), 2, cv::Scalar(0, 0, 255), -1); //参数说明 图像 圆心 线条宽度 颜色 线的类型
                ////显示数字
                //cv::putText(temp, to_string(i), cvPoint(shapes[0].part(i).x(), shapes[0].part(i).y()), cv::FONT_HERSHEY_PLAIN, 1, cv::Scalar(0, 0, 255));
            }
        }

        //左眼
        //点36的坐标
        int x_36 = shapes[0].part(36).x();
        //  int y_36 = shapes[0].part(36).y();

        //点37的坐标
        //  int x_37 = shapes[0].part(37).x();
        int y_37 = shapes[0].part(37).y();

        //点38的坐标
        //  int x_38 = shapes[0].part(38).x();
        int y_38 = shapes[0].part(38).y();

        //点39的坐标
        int x_39 = shapes[0].part(39).x();
        //  int y_39 = shapes[0].part(39).y();

        //点40的坐标
        //  int x_40 = shapes[0].part(40).x();
        int y_40 = shapes[0].part(40).y();

        //点41的坐标
        //  int x_41 = shapes[0].part(41).x();
        int y_41 = shapes[0].part(41).y();

        int height_left_eye1 = y_41 - y_37; //37到41的纵向距离
        //cout << "左眼高度1\t" << height_left_eye1 << endl;
        int height_left_eye2 = y_40 - y_38; //38到40的纵向距离
        //cout << "左眼高度2\t" << height_left_eye2 << endl;
        float height_left_eye = (height_left_eye1 + height_left_eye2) / 2; //眼睛上下距离
        if(height_left_eye<=7){
           height_left_eye=3;
        }
        //if(height_left_eye>=8){
        //    height_left_eye=12;
        //}
        //cout << "左眼高度\t" << height_left_eye << endl;
        int length_left_eye = x_39 - x_36;
        //cout << "左眼长度\t" << length_left_eye << endl;
        //if (height_left_eye == 0) //当眼睛闭合的时候，距离可能检测为0，宽高比出错
        //    height_left_eye = 1;

        float EAR_left_eye; //眼睛宽高比
        EAR_left_eye = height_left_eye / length_left_eye;

        /*
		 //在屏幕上显示眼睛的高度及宽高比

		 cout << "左眼宽高比" << EAR_left_eye << endl;

		 显示height_left_eye、length_left_eye以及ERA_left_eye

		 把hight_left_eye从float类型转化成字符串类型
		 char height_left_eye_text[30];
		 char length_left_eye_text[30];
		 char ERA_left_eye_text[30];

		 _gcvt_s(height_left_eye_text, height_left_eye, 10);   //把hight_left_eye从float类型转化成字符串类型
		 _gcvt_s(length_left_eye_text, length_left_eye,10);
		 _gcvt_s(ERA_left_eye_text, EAR_left_eye, 10);

		 putText(temp, height_left_eye_text, Point(10, 100), FONT_HERSHEY_COMPLEX, 1.0, Scalar(12, 255, 200), 1, LINE_AA);
		 putText(temp,height_left_eye_text, Point(10, 200), FONT_HERSHEY_COMPLEX, 1.0, Scalar(12, 255, 200), 1, LINE_AA);
		 putText(temp, height_left_eye_text, Point(10, 300), FONT_HERSHEY_COMPLEX, 1.0, Scalar(12, 255, 200), 1, LINE_AA);
		 */

        //右眼
        //点42的坐标
        int x_42 = shapes[0].part(42).x();
        //  int y_42 = shapes[0].part(42).y();
        //点37的坐标
        //  int x_43 = shapes[0].part(43).x();
        int y_43 = shapes[0].part(43).y();
        //点38的坐标
        //  int x_44 = shapes[0].part(44).x();
        int y_44 = shapes[0].part(44).y();
        //点39的坐标
        int x_45 = shapes[0].part(45).x();
        //  int y_45 = shapes[0].part(45).y();
        //点40的坐标
        //  int x_46 = shapes[0].part(46).x();
        int y_46 = shapes[0].part(46).y();

        //点41的坐标
        //  int x_47 = shapes[0].part(47).x();
        int y_47 = shapes[0].part(47).y();

        int height_right_eye1 = y_47 - y_43;
        //std::cout << "height_right_eye1: " << Convert(height_right_eye1) << std::endl;                //37到41的纵向距离
        int height_right_eye2 = y_46 - y_44;                                  //38到40的纵向距离
        //std::cout << "height_right_eye2: " << Convert(height_right_eye2) << std::endl;  
        float height_right_eye = (height_right_eye1 + height_right_eye2) / 2; //眼睛上下距离
        if(height_right_eye<=7){
            height_right_eye=3;
        }
        //if(height_right_eye>=8){
        //    height_right_eye=12;
        //}
        //std::cout << "height_right_eye: " << Convert(height_right_eye) << std::endl;  
        //if (height_right_eye == 0)                                            //当眼睛闭合的时候，距离可能检测为0，宽高比出错
        //    height_right_eye = 1;

        int length_right_eye = x_45 - x_42;
       // std::cout << "length_right_eye: " << Convert(length_right_eye) << std::endl;
        float EAR_right_eye; //眼睛宽高比
        EAR_right_eye = height_right_eye / length_right_eye;
        //std::cout << "EAR" << Convert(EAR_right_eye) << std::endl;
        //取两只眼睛的平均宽高比作为眼睛的宽高比
        //float EAR_eyes = (EAR_left_eye + EAR_right_eye) / 2;

        //cout << "眼睛的宽高比为" << EAR_eyes << endl;
        //计算嘴部的MAR
        //计算MAR
        //计算length_mouth  
        int x_48 = shapes[0].part(48).x();
        int x_54 = shapes[0].part(54).x();
        int length_mouth = x_54 - x_48;//64-60
        //std::cout << "length: " << Convert(length_mouth) << std::endl;

        //计算height_mouth1
        int y_61 = shapes[0].part(61).y();
        int y_67 = shapes[0].part(67).y();
        int height_mouth1 = y_67 - y_61;//58-50
        //std::cout << "height1: " << Convert(height_mouth1) << std::endl;

        //计算height_mouth2
        int y_63 = shapes[0].part(63).y();
        int y_65 = shapes[0].part(65).y();
        int height_mouth2 = y_65 - y_63;//56-52
        //std::cout << "height2: " << Convert(height_mouth2) << std::endl;
        //计算height_mouth 
        float height_mouth = (height_mouth1 + height_mouth2) / 2;
        if(height_mouth<=5){
            height_mouth=5;
        }
       // std::cout << "height: " << Convert(height_mouth) << std::endl;

        ////计算MAR
        float MAR;
        MAR = height_mouth / length_mouth;
        //画眼睛的波形图
        //eye_now_x = eye_now_x + 1;                          //横坐标（每10个像素描一个点）
        //eye_now_y = 900 - (EAR_eyes * 900);                 //纵坐标
        //Point poi1 = Point(eye_previous_x, eye_previous_y); //上一个点
        //Point poi2 = Point(eye_now_x, eye_now_y);           //现在的点
        //Scalar eyes_color = Scalar(0, 255, 0);
        //cv::line(Eye_Waveform, poi1, poi2, eyes_color, 1, LINE_AA); //画线
        //eye_previous_x = eye_now_x;
        //eye_previous_y = eye_now_y;
        //namedWindow("Blink waveform figure", WINDOW_AUTOSIZE);

        //计算眨眼次数
         //if (blink_EAR_before < EAR_eyes)
 //       {
 //           blink_EAR_before = EAR_eyes;
 //       }
 //       if (blink_EAR_now > EAR_eyes)
 //       {
 //           blink_EAR_now = EAR_eyes;
 //       }
 //       if (blink_EAR_after < EAR_eyes)
 //       {
 //           blink_EAR_after = EAR_eyes;
 //       }
 //       if (blink_EAR_before > 0.2 && blink_EAR_now <= 0.2 && blink_EAR_after > 0.2)
 //       {
 //           count_blink = count_blink + 1.0;
 //           blink_EAR_before = 0.0;
 //           blink_EAR_now = 0.2;
 //           blink_EAR_after = 0.0;
 //       }

        //std::cout << "left: " << Convert(EAR_left_eye) << " right: " << Convert(EAR_right_eye) << std::endl;
        //std::cout << "MAR: " << Convert(MAR) << std::endl;
        
        res = Convert(EAR_left_eye);
        res.append(",");
        res.append(Convert(EAR_right_eye));
        res.append(",");
        res.append(Convert(MAR));
        // std::cout << "res:" << res << std::endl;
    }
    else{
        res="-1.00,-1.00,-1.00";
    }

    //计时一分钟（60秒）
    //clock_t start = clock();
    //clock_t finish = clock();
    //double consumeTime = (double)(finish - start); //注意转换为double的位置
    //if (count_blink >= 25)
    //{ // 眨眼次数
    //    if (consumeTime / 1000 < 60)
    //    {
    //        cout << "您已疲劳，请休息！！" << endl;
    //        count_blink = 0;
    //        return 0;
    //    }
    //}
    return res;
}

#pragma GCC diagnostic pop
