# Copyright (C) 2018-2019 Intel Corporation
# SPDX-License-Identifier: Apache-2.0
#
# set(dlib_DIR /opt/dlib/build/dlib/config/)   #dlib.cmake的搜索路径

file (GLOB_RECURSE SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/*.cpp)
file (GLOB_RECURSE HEADERS ${CMAKE_CURRENT_SOURCE_DIR}/*.hpp)

# macro(add_example name)
#    add_executable(${name} ${name}.cpp)
#    target_link_libraries(${name} dlib::dlib )
# endmacro()

ie_add_sample(NAME human_pose_estimation_demo_meng
              SOURCES ${SOURCES}
              HEADERS ${HEADERS}
              INCLUDE_DIRECTORIES "${CMAKE_CURRENT_SOURCE_DIR}/include"
              DEPENDENCIES monitors
              OPENCV_DEPENDENCIES highgui videoio imgproc
             )
             
            


# Find dlib, you may need to set dlib_DIR variable
# to the absolute path to the directory containing dlibConfig.cmake file
# via the command line or GUI
