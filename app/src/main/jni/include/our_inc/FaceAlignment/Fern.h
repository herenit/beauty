#pragma once

#include "FaceAlignment/Utils.h"

class Fern{
    private:
        int fern_pixel_num_;
        int landmark_num_;
        cv::Mat_<int> selected_nearest_landmark_index_;
        cv::Mat_<double> threshold_;
        cv::Mat_<int> selected_pixel_index_;
        cv::Mat_<double> selected_pixel_locations_;
        std::vector<cv::Mat_<double> > bin_output_;
    public:
        std::vector<cv::Mat_<double> > Train(const std::vector<std::vector<double> >& candidate_pixel_intensity, 
                                             const cv::Mat_<double>& covariance,
                                             const cv::Mat_<double>& candidate_pixel_locations,
                                             const cv::Mat_<int>& nearest_landmark_index,
                                             const std::vector<cv::Mat_<double> >& regression_targets,
                                             int fern_pixel_num);
        cv::Mat_<double> Predict(const cv::Mat_<uchar>& image,
                                 const cv::Mat_<double>& shape,
                                 const cv::Mat_<double>& rotation,
                                 const BoundingBox& bounding_box,
                                 double scale);
        void Read(std::ifstream& fin);
        void Write(std::ofstream& fout);
};
