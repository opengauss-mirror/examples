#include <vector>
#include <chrono>
#include <string>
#include <inference_engine.hpp>
#include <monitors/presenter.h>
#include <samples/images_capture.h>
#include <samples/ocv_common.hpp>
#include <fstream>
#include "human_pose_estimation_demo.hpp"
#include "human_pose_estimator.hpp"
#include "render_human_pose.hpp"
#include <librealsense2/rs.hpp>

#include <sys/types.h>
#include <sys/socket.h>
#include <stdio.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/shm.h>
#include <iostream>
#include <chrono>
#include <thread>
#include <future>
#include <opencv2/dnn.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/highgui.hpp>

#include "dlib_face.hpp"

#define MYPORT 7000
#define BUFFER_SIZE 1024

#define FRAME_WIDTH 640
#define FRAME_HIGH 480
#define FRAME_FPS 30

using namespace InferenceEngine;
using namespace human_pose_estimation;
using namespace cv;

bool ParseAndCheckCommandLine(int argc, char* argv[])
{
	// ---------------------------Parsing and validation of input args--------------------------------------

	gflags::ParseCommandLineNonHelpFlags(&argc, &argv, true);
	if (FLAGS_h)
	{
		showUsage();
		showAvailableDevices();
		return false;
	}
	std::cout << "Parsing input parameters" << std::endl;
	if (FLAGS_i.empty())
	{
		throw std::logic_error("Parameter -i is not set");
	}
	if (FLAGS_m.empty())
	{
		throw std::logic_error("Parameter -m is not set");
	}
	return true;
}

int initClient()
{
	///定义sockfd
	int sock_cli = socket(AF_INET, SOCK_STREAM, 0);

	///定义sockaddr_in
	struct sockaddr_in servaddr;
	memset(&servaddr, 0, sizeof(servaddr));
	servaddr.sin_family = AF_INET;
	servaddr.sin_port = htons(MYPORT);                    //服务器端口
	servaddr.sin_addr.s_addr = inet_addr("192.168.43.250"); //服务器ip（目前我用电脑跑处理），inet_addr用于IPv4的IP转换（十进制转换为二进制）
	//连接服务器，成功返回0，错误返回-1
	if (connect(sock_cli, (struct sockaddr*)&servaddr, sizeof(servaddr)) < 0)
	{
		perror("connect");
		exit(1);
	}
	return sock_cli;
}

void sendData(int sock_cli, std::string sendStr)
{
	int length = sendStr.length();
	char sendbuf[length];
	for (int i = 0; i < length; i++)
		sendbuf[i] = sendStr[i];
	std::cout << "send data" << sendbuf;
	send(sock_cli, sendbuf, strlen(sendbuf), 0); //发送
}

int initServer()
{
	int listenfd;
	struct sockaddr_in servaddr;

	if ((listenfd = socket(AF_INET, SOCK_STREAM, 0)) == -1)
	{
		printf("create socket error: %s(errno: %d)\n", strerror(errno), errno);
		return 0;
	}

	memset(&servaddr, 0, sizeof(servaddr));
	servaddr.sin_family = AF_INET;
	servaddr.sin_addr.s_addr = htonl(INADDR_ANY);
	servaddr.sin_port = htons(7000);

	if (bind(listenfd, (struct sockaddr*)&servaddr, sizeof(servaddr)) == -1)
	{
		printf("bind socket error: %s(errno: %d)\n", strerror(errno), errno);
		return 0;
	}

	if (listen(listenfd, 10) == -1)
	{
		printf("listen socket error: %s(errno: %d)\n", strerror(errno), errno);
		return 0;
	}
	return listenfd;
}

void acceptData(int listenfd)
{
	int connfd;
	char buff[4096];
	if ((connfd = accept(listenfd, (struct sockaddr*)NULL, NULL)) == -1)
	{
		printf("accept socket error: %s(errno: %d)", strerror(errno), errno);
		return;
	}
	int n = recv(connfd, buff, 4096, 0);
	if (n > 0)
	{
		buff[n] = '\0';
		std::cout << "疲劳结果: " << buff << std::endl;
	}
}

/*
int test() {
//    char sendbuf[BUFFER_SIZE];
	//int serverSocket = initServer();
	dlib::shape_predictor pos_model = loadModel();
	dlib::frontal_face_detector detector =loadDetector();
	try {
		//std::cout << "InferenceEngine: " << printable(*GetInferenceEngineVersion()) << std::endl;

		// ------------------------------ Parsing and validation of input args ---------------------------------
		if (!ParseAndCheckCommandLine(argc, argv)) {
			return EXIT_SUCCESS;
		}
		rs2::context ctx;
		//检查设备，rs深度摄像头
		auto list = ctx.query_devices();
		if (list.size() == 0)
			throw std::runtime_error("No device detected. Is it plugged in?");

		rs2::device dev = list.front();
		rs2::frameset frames;
		rs2::pipeline pipe;
		rs2::config cfg;

		//创建config，配置pipe
		cfg.enable_stream(RS2_STREAM_COLOR, FRAME_WIDTH, FRAME_HIGH, RS2_FORMAT_BGR8, FRAME_FPS);
		cfg.enable_stream(RS2_STREAM_DEPTH, FRAME_WIDTH, FRAME_HIGH, RS2_FORMAT_Z16, FRAME_FPS);
		pipe.start(cfg);

		//read frames
		frames = pipe.wait_for_frames();
		//进行深度和color对齐操作
		rs2::align align_to_depth(RS2_STREAM_DEPTH);
		frames = align_to_depth.process(frames);
		//得到深度和彩色图像
		rs2::frame color_frame = frames.get_color_frame();
		rs2::frame depth_frame = frames.get_depth_frame();
		//创建图像容器矩阵
		Mat color(Size(FRAME_WIDTH, FRAME_HIGH), CV_8UC3, (void*)color_frame.get_data(), Mat::AUTO_STEP);
		Mat pic_depth(Size(FRAME_WIDTH, FRAME_HIGH), CV_16U, (void*)depth_frame.get_data(), Mat::AUTO_STEP);

		//创建estimator对象
		HumanPoseEstimator estimator(FLAGS_m, FLAGS_d, FLAGS_pc);

		cv::Mat curr_frame = color;
		cv::Size graphSize{ curr_frame.cols / 4, 60 };
		Presenter presenter(FLAGS_u, curr_frame.rows - graphSize.height - 10, graphSize);

		estimator.reshape(curr_frame);

		std::cout << "To close the application, press 'CTRL+C' here";
		if (!FLAGS_no_show) {
			std::cout << " or switch to the output window and press ESC key" << std::endl;
			std::cout << "To pause execution, switch to the output window and press 'p' key" << std::endl;
		}
		std::cout << std::endl;

		int delay = 1;
		bool blackBackground = FLAGS_black;

		typedef std::chrono::duration<double, std::ratio<1, 1000>> ms;
		auto total_t0 = std::chrono::high_resolution_clock::now();
		auto wallclock = std::chrono::high_resolution_clock::now();
		double render_time = 0;
		bool firstFrame = true;
		//存取图片文件名
		std::string colorName = "color_";
		std::string depthName = "depth_";
		std::string style = ".png";
		//存取数据的路径
		std::string path = "/home/hit-ices/data_all";
		size_t count = 0;
		std::vector<int> compession_params;
		compession_params.push_back(IMWRITE_PNG_COMPRESSION);
		compession_params.push_back(1);
		std::ofstream destFile;
		//自己建立文件夹！！！！
		destFile.open(path + "/data/data.txt");

		Mat depthValue(5000, 54, CV_32FC1);
	//	imread(path + "/data/rowdate.png");
				Mat depth = imread(path+"/depth/depth_9.png", -1);
				std::cout <<"通道个数:"<< depth.channels() << std::endl;
		do {
			auto t0 = std::chrono::high_resolution_clock::now();
			//进行深度和rgb的对齐
			frames = pipe.wait_for_frames();
			rs2::align align_to_depth(RS2_STREAM_DEPTH);
			frames = align_to_depth.process(frames);

			//get depth and color frame
			rs2::frame color_frame = frames.get_color_frame();
			rs2::depth_frame depth_frame = frames.get_depth_frame();
			//从图片创建cv矩阵
			Mat color(Size(FRAME_WIDTH, FRAME_HIGH), CV_8UC3, (void*)color_frame.get_data(), Mat::AUTO_STEP);
			Mat pic_depth(Size(FRAME_WIDTH, FRAME_HIGH), CV_16U, (void*)depth_frame.get_data(), Mat::AUTO_STEP);
			//展示rgb和深度图像
			namedWindow("Display Image", WINDOW_AUTOSIZE);
			imshow("Display Image", color);
			waitKey(1);
			imshow("Display depth", pic_depth * 15);
			waitKey(1);
			//abandon firstframe
			if (firstFrame) firstFrame = false;
			else            curr_frame = color;
			//得到decode-time 高精度时间
			estimator.frameToBlobCurr(curr_frame);
			auto t1 = std::chrono::high_resolution_clock::now();
			double decode_time = std::chrono::duration_cast<ms>(t1 - t0).count();
			//detection-time
			t0 = std::chrono::high_resolution_clock::now();
			estimator.startCurr();
			estimator.waitCurr();
			t1 = std::chrono::high_resolution_clock::now();
			ms detection = std::chrono::duration_cast<ms>(t1 - t0);
			//wall-time
			t0 = std::chrono::high_resolution_clock::now();
			ms wall = std::chrono::duration_cast<ms>(t0 - wallclock);

			wallclock = t0;
			t0 = std::chrono::high_resolution_clock::now();
			if (blackBackground) {
				curr_frame = cv::Mat::zeros(curr_frame.size(), curr_frame.type());
			}
			std::ostringstream out;
			out << "OpenCV cap/render time: " << std::fixed << std::setprecision(1)
				<< (decode_time + render_time) << " ms";
			cv::putText(curr_frame, out.str(), cv::Point2f(0, 25),
				cv::FONT_HERSHEY_TRIPLEX, 0.6, cv::Scalar(0, 255, 0));
			out.str("");
			out << "Wall clock time " << "SYNC: ";
			out << std::fixed << std::setprecision(2) << wall.count()
				<< " ms (" << 1000.0 / wall.count() << " fps)";
			cv::putText(curr_frame, out.str(), cv::Point2f(0, 50),
				cv::FONT_HERSHEY_TRIPLEX, 0.6, cv::Scalar(0, 0, 255));
			//if (!isAsyncMode) {  // In the true async mode, there is no way to measure detection time directly
			out.str("");
			out << "Detection time  : " << std::fixed << std::setprecision(1) << detection.count()
				<< " ms ("
				<< 1000.0 / detection.count() << " fps)";
			cv::putText(curr_frame, out.str(), cv::Point2f(0, 75), cv::FONT_HERSHEY_TRIPLEX, 0.6,
				cv::Scalar(255, 0, 0));
			//}

			std::vector<HumanPose> poses = estimator.postprocessCurr();
			std::string datetime;
			if (FLAGS_r) {
				if (!poses.empty()) {
					std::time_t result = std::time(nullptr);
					char timeString[sizeof("2020-01-01 00:00:00: ")];
					std::strftime(timeString, sizeof(timeString), "%Y-%m-%d %H:%M:%S: ", std::localtime(&result));
					// std::cout << timeString;
					datetime = timeString;
				}
				//float* ptr = depthValue.ptr<float>(count);
				for (HumanPose const& pose : poses) {
					std::stringstream rawPose;
					//int i = 0;
					//rawPose << std::fixed << std::setprecision(0);
					// rawPose << datetime << "data-" << std::to_string(count) << ":";
					for (auto const& keypoint : pose.keypoints) {
						float depth_value;
						if (keypoint.x != -1 && keypoint.y != -1)
							//-------------------------------
							if(keypoint.x>=0 && keypoint.x <=639)
								depth_value = depth_frame.get_distance(keypoint.x, keypoint.y);
								//depth_value = depth_frame.get_distance(640, keypoint.y);

							else depth_value = -1;
						else
							depth_value = -1;//此时无法求得距离值
						//ptr[i++] = keypoint.x; ptr[i++] = keypoint.y; ptr[i++] = depth_value;
						rawPose << std::fixed << std::setprecision(0) << "(" << keypoint.x << "," << keypoint.y << ",";
						rawPose << std::fixed << std::setprecision(2) << depth_value << "), ";
					}
					// rawPose << pose.score;
					// std::cout << rawPose.str() << std::endl;
					// destFile << rawPose.str() << std::endl;
					// std::cout << "sendData start" << std::endl;
					// sendData(sock_cli, rawPose.str());
				}
			}

			presenter.drawGraphs(curr_frame);
			renderHumanPose(poses, curr_frame);
			//改，自己建立文件夹!!!!
			// imwrite(path + "/depth/" + depthName + std::to_string(count) + style, pic_depth * 15, compession_params);
			// imwrite(path + "/color/" + colorName + std::to_string(count) + style, color, compession_params);
			count++;


			t1 = std::chrono::high_resolution_clock::now();
			render_time = std::chrono::duration_cast<ms>(t1 - t0).count();

			if (!FLAGS_no_show) {
				const int key = cv::waitKey(delay) & 255;
				if (key == 'p') {
					delay = (delay == 0) ? 1 : 0;
				}
				else if (27 == key) { // Esc
					destFile.close();
					// close(sock_cli);
					//imwrite(path + "/data/rowdate.png", depthValue, compession_params);
					break;
				}
				else if (9 == key) { // Tab
				}
				else if (32 == key) { // Space
					blackBackground = !blackBackground;
				}
				presenter.handleKey(key);
			}
			// added by songjian, to detect face_image
			// std::async(eye_func, curr_frame, pos_model);
			 eye_func(curr_frame, pos_model, detector);

			//acceptData(serverSocket);
			//展示实时的图像
			if (!FLAGS_no_show) {
				cv::imshow("Human Pose Estimation on " + FLAGS_d, curr_frame);
			}
		} while (curr_frame.data);

		auto total_t1 = std::chrono::high_resolution_clock::now();
		ms total = std::chrono::duration_cast<ms>(total_t1 - total_t0);
		// std::cout << "Total Inference time: " << total.count() << std::endl;
		// std::cout << presenter.reportMeans() << '\n';
	}
	catch (const std::exception& error) {
		std::cerr << "[ ERROR ] " << error.what() << std::endl;
		return EXIT_FAILURE;
	}
	catch (...) {
		std::cerr << "[ ERROR ] Unknown/internal exception happened." << std::endl;
		return EXIT_FAILURE;
	}
	std::cout << "Execution successful" << std::endl;
	return EXIT_SUCCESS;
}
*/

int main(int argc, char* argv[])
{
	//    char sendbuf[BUFFER_SIZE];
	//int serverSocket = initServer();
	int sock_cli = initClient();

	dlib::shape_predictor pos_model = loadModel();
	dlib::frontal_face_detector detector = loadDetector();
	try {
		//std::cout << "InferenceEngine: " << printable(*GetInferenceEngineVersion()) << std::endl;

		// ------------------------------ Parsing and validation of input args ---------------------------------
		if (!ParseAndCheckCommandLine(argc, argv)) {
			return EXIT_SUCCESS;
		}
		rs2::context ctx;
		//检查设备，rs深度摄像头
		auto list = ctx.query_devices();
		if (list.size() == 0)
			throw std::runtime_error("No device detected. Is it plugged in?");

		rs2::device dev = list.front();
		rs2::frameset frames;
		rs2::pipeline pipe;
		rs2::config cfg;

		//创建config，配置pipe
		cfg.enable_stream(RS2_STREAM_COLOR, FRAME_WIDTH, FRAME_HIGH, RS2_FORMAT_BGR8, FRAME_FPS);
		cfg.enable_stream(RS2_STREAM_DEPTH, FRAME_WIDTH, FRAME_HIGH, RS2_FORMAT_Z16, FRAME_FPS);
		pipe.start(cfg);

		//read frames
		frames = pipe.wait_for_frames();
		//进行深度和color对齐操作
		rs2::align align_to_depth(RS2_STREAM_DEPTH);
		frames = align_to_depth.process(frames);
		//得到深度和彩色图像
		rs2::frame color_frame = frames.get_color_frame();
		rs2::frame depth_frame = frames.get_depth_frame();
		//创建图像容器矩阵
		Mat color(Size(FRAME_WIDTH, FRAME_HIGH), CV_8UC3, (void*)color_frame.get_data(), Mat::AUTO_STEP);
		Mat pic_depth(Size(FRAME_WIDTH, FRAME_HIGH), CV_16U, (void*)depth_frame.get_data(), Mat::AUTO_STEP);

		//创建estimator对象
		HumanPoseEstimator estimator(FLAGS_m, FLAGS_d, FLAGS_pc);

		cv::Mat curr_frame = color;
		cv::Size graphSize{ curr_frame.cols / 4, 60 };
		Presenter presenter(FLAGS_u, curr_frame.rows - graphSize.height - 10, graphSize);

		estimator.reshape(curr_frame);

		std::cout << "To close the application, press 'CTRL+C' here";
		if (!FLAGS_no_show) {
			std::cout << " or switch to the output window and press ESC key" << std::endl;
			std::cout << "To pause execution, switch to the output window and press 'p' key" << std::endl;
		}
		std::cout << std::endl;

		int delay = 1;
		bool blackBackground = FLAGS_black;

		typedef std::chrono::duration<double, std::ratio<1, 1000>> ms;
		auto total_t0 = std::chrono::high_resolution_clock::now();
		auto wallclock = std::chrono::high_resolution_clock::now();
		double render_time = 0;
		bool firstFrame = true;
		//存取图片文件名
		std::string colorName = "color_";
		std::string depthName = "depth_";
		std::string style = ".png";
		//存取数据的路径
		std::string path = "/home/hit-ices/Desktop";
		size_t count = 0;
		std::vector<int> compession_params;
		compession_params.push_back(IMWRITE_PNG_COMPRESSION);
		compession_params.push_back(1);
		std::ofstream destFile;
		//自己建立文件夹！！！！
		destFile.open(path + "/data.txt");

		Mat depthValue(5000, 54, CV_32FC1);
		//	imread(path + "/data/rowdate.png");
		Mat depth = imread(path + "/depth_9.png", -1);
		std::cout << "通道个数:" << depth.channels() << std::endl;
		do {
			auto t0 = std::chrono::high_resolution_clock::now();
			//进行深度和rgb的对齐
			frames = pipe.wait_for_frames();
			rs2::align align_to_depth(RS2_STREAM_DEPTH);
			frames = align_to_depth.process(frames);

			//get depth and color frame
			rs2::frame color_frame = frames.get_color_frame();
			rs2::depth_frame depth_frame = frames.get_depth_frame();
			//从图片创建cv矩阵
			Mat color(Size(FRAME_WIDTH, FRAME_HIGH), CV_8UC3, (void*)color_frame.get_data(), Mat::AUTO_STEP);
			Mat pic_depth(Size(FRAME_WIDTH, FRAME_HIGH), CV_16U, (void*)depth_frame.get_data(), Mat::AUTO_STEP);
			//展示rgb和深度图像
			namedWindow("Display Image", WINDOW_AUTOSIZE);
			imshow("Display Image", color);
			waitKey(1);
			imshow("Display depth", pic_depth * 15);
			waitKey(1);
			//abandon firstframe
			if (firstFrame) firstFrame = false;
			else            curr_frame = color;
			//得到decode-time 高精度时间
			estimator.frameToBlobCurr(curr_frame);
			auto t1 = std::chrono::high_resolution_clock::now();
			double decode_time = std::chrono::duration_cast<ms>(t1 - t0).count();
			//detection-time
			t0 = std::chrono::high_resolution_clock::now();
			estimator.startCurr();
			estimator.waitCurr();
			t1 = std::chrono::high_resolution_clock::now();
			ms detection = std::chrono::duration_cast<ms>(t1 - t0);
			//wall-time
			t0 = std::chrono::high_resolution_clock::now();
			ms wall = std::chrono::duration_cast<ms>(t0 - wallclock);

			wallclock = t0;
			t0 = std::chrono::high_resolution_clock::now();
			if (blackBackground) {
				curr_frame = cv::Mat::zeros(curr_frame.size(), curr_frame.type());
			}
			std::ostringstream out;
			out << "OpenCV cap/render time: " << std::fixed << std::setprecision(1)
				<< (decode_time + render_time) << " ms";
			cv::putText(curr_frame, out.str(), cv::Point2f(0, 25),
				cv::FONT_HERSHEY_TRIPLEX, 0.6, cv::Scalar(0, 255, 0));
			out.str("");
			out << "Wall clock time " << "SYNC: ";
			out << std::fixed << std::setprecision(2) << wall.count()
				<< " ms (" << 1000.0 / wall.count() << " fps)";
			cv::putText(curr_frame, out.str(), cv::Point2f(0, 50),
				cv::FONT_HERSHEY_TRIPLEX, 0.6, cv::Scalar(0, 0, 255));
			//if (!isAsyncMode) {  // In the true async mode, there is no way to measure detection time directly
			out.str("");
			out << "Detection time  : " << std::fixed << std::setprecision(1) << detection.count()
				<< " ms ("
				<< 1000.0 / detection.count() << " fps)";
			cv::putText(curr_frame, out.str(), cv::Point2f(0, 75), cv::FONT_HERSHEY_TRIPLEX, 0.6,
				cv::Scalar(255, 0, 0));
			//}

			std::vector<HumanPose> poses = estimator.postprocessCurr();
			std::string datetime;
			if (FLAGS_r) {
				if (!poses.empty()) {
					std::time_t result = std::time(nullptr);
					char timeString[sizeof("2020-01-01 00:00:00: ")];
					std::strftime(timeString, sizeof(timeString), "%Y-%m-%d %H:%M:%S: ", std::localtime(&result));
					std::cout << timeString;
					datetime = timeString;
				}
				//float* ptr = depthValue.ptr<float>(count);
				for (HumanPose const& pose : poses) {
					std::stringstream rawPose;
					//int i = 0;
					//rawPose << std::fixed << std::setprecision(0);
					rawPose << datetime << "data-" << std::to_string(count) << ":";
					for (auto const& keypoint : pose.keypoints) {
						float depth_value;
						if (keypoint.x != -1 && keypoint.y != -1)
							//-------------------------------
							if (keypoint.x >= 0 && keypoint.x <= 639)
								depth_value = depth_frame.get_distance(keypoint.x, keypoint.y);
						//depth_value = depth_frame.get_distance(640, keypoint.y);

							else depth_value = -1;
						else
							depth_value = -1;//此时无法求得距离值
						//ptr[i++] = keypoint.x; ptr[i++] = keypoint.y; ptr[i++] = depth_value;
						rawPose << std::fixed << std::setprecision(0) << "(" << keypoint.x << "," << keypoint.y << ",";
						rawPose << std::fixed << std::setprecision(2) << depth_value << "), ";
					}
					//rawPose << pose.score;
					std::cout << rawPose.str() << std::endl;
					//destFile << rawPose.str() << std::endl;
					std::cout << "sendData start" << std::endl;
					sendData(sock_cli, rawPose.str());
				}
			}

			presenter.drawGraphs(curr_frame);
			renderHumanPose(poses, curr_frame);
			//改，自己建立文件夹!!!!
			// imwrite(path + "/depth/" + depthName + std::to_string(count) + style, pic_depth * 15, compession_params);
			// imwrite(path + "/color/" + colorName + std::to_string(count) + style, color, compession_params);
			count++;


			t1 = std::chrono::high_resolution_clock::now();
			render_time = std::chrono::duration_cast<ms>(t1 - t0).count();

			if (!FLAGS_no_show) {
				const int key = cv::waitKey(delay) & 255;
				if (key == 'p') {
					delay = (delay == 0) ? 1 : 0;
				}
				else if (27 == key) { // Esc
					std::cout << "Esc exit" << std::endl;
					destFile.close();
					close(sock_cli);
					//imwrite(path + "/data/rowdate.png", depthValue, compession_params);
					break;
				}
				else if (9 == key) { // Tab
				}
				else if (32 == key) { // Space
					blackBackground = !blackBackground;
				}
				presenter.handleKey(key);
			}
			// added by songjian, to detect face_image
			// std::async(eye_func, curr_frame, pos_model);
			eye_func(curr_frame, pos_model, detector);

			//acceptData(serverSocket);
			//展示实时的图像
			if (!FLAGS_no_show) {
				cv::imshow("Human Pose Estimation on " + FLAGS_d, curr_frame);
			}
		} while (curr_frame.data);

		auto total_t1 = std::chrono::high_resolution_clock::now();
		ms total = std::chrono::duration_cast<ms>(total_t1 - total_t0);
		// std::cout << "Total Inference time: " << total.count() << std::endl;
		// std::cout << presenter.reportMeans() << '\n';
	}
	catch (const std::exception& error) {
		std::cerr << "[ ERROR ] " << error.what() << std::endl;
		return EXIT_FAILURE;
	}
	catch (...) {
		std::cerr << "[ ERROR ] Unknown/internal exception happened." << std::endl;
		return EXIT_FAILURE;
	}
	std::cout << "Execution successful" << std::endl;
	return EXIT_SUCCESS;
}

