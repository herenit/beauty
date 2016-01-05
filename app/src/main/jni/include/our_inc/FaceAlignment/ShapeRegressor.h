#pragma once

#include "FaceAlignment/FernCascade.h"

class ShapeRegressor{
    public:
        ShapeRegressor(); 
        void Train(const std::vector<cv::Mat_<uchar> >& images, 
                   const std::vector<cv::Mat_<double> >& ground_truth_shapes,
                   const std::vector<BoundingBox>& bounding_box,
                   int first_level_num, int second_level_num,
                   int candidate_pixel_num, int fern_pixel_num,
                   int initial_num);
        cv::Mat_<double> Predict(const cv::Mat_<uchar>& image, const BoundingBox& bounding_box, int initial_num);
        void Read(std::ifstream& fin);
        void Write(std::ofstream& fout);
        void Load(std::string &path);
        void Save(std::string &path);
        void Release();
    private:
        int first_level_num_;
        int landmark_num_;
        std::vector<FernCascade> fern_cascades_;
        cv::Mat_<double> mean_shape_;
        std::vector<cv::Mat_<double> > training_shapes_;
        std::vector<BoundingBox> bounding_box_;
};
