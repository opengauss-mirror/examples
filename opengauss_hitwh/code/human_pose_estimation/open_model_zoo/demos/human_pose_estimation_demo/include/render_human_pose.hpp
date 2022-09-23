// Copyright (C) 2018-2019 Intel Corporation
// SPDX-License-Identifier: Apache-2.0
//

#pragma once

#include <vector>

#include <opencv2/core/core.hpp>

#include "human_pose.hpp"

namespace human_pose_estimation {
    void renderHumanPose(const std::vector<HumanPose>& poses, cv::Mat& image);//Mat : juzhen Mat(int rows, int cols, int type)  Humanpose vector<cv::Point2f>& keypoints = std::vector<cv::Point2f>() group_4
}  // namespace human_pose_estimation
