#ifndef DLIB_FACE_H
#define DLIB_FACE_H

// #include <QWidget>
// #include "v4l2.h"
// #include <QTimer>
#include <dlib/opencv.h>
#include <opencv2/opencv.hpp>
#include <iostream>
#include <vector>
#include <cmath>
#include <time.h>
#include <opencv2/core/core.hpp>
#include <opencv2/opencv.hpp>
#include "dlib/image_processing/frontal_face_detector.h"
#include "dlib/image_processing/render_face_detections.h"
#include "dlib/image_processing.h"
#include "dlib/gui_widgets.h"
#include "dlib/opencv/cv_image.h"
#include <iostream>
#include <vector>
#include <cmath>
#include <time.h>


extern std::string eye_func(cv::Mat temp, dlib::shape_predictor pos_model, dlib::frontal_face_detector detector);
extern dlib::shape_predictor loadModel();
extern dlib::frontal_face_detector loadDetector();


#endif // DLIB_FACE_H
