#pragma once

#include "FaceAlignment/Fern.h"

class FernCascade{
    public:
        std::vector<cv::Mat_<double> > Train(const std::vector<cv::Mat_<uchar> >& images,
                                             const std::vector<cv::Mat_<double> >& current_shapes,
                                             const std::vector<cv::Mat_<double> >& ground_truth_shapes,
                                             const std::vector<BoundingBox> & bounding_box,
                                             const cv::Mat_<double>& mean_shape,
                                             int second_level_num,
                                             int candidate_pixel_num,
                                             int fern_pixel_num,
                                             int curr_level_num,
                                             int first_level_num);  
        cv::Mat_<double> Predict(const cv::Mat_<uchar>& image, 
                                 const BoundingBox& bounding_box, 
                                 const cv::Mat_<double>& mean_shape,
                                 const cv::Mat_<double>& shape);
        void Read(std::ifstream& fin);
        void Write(std::ofstream& fout);
        ~FernCascade();
    private:
        std::vector<Fern> ferns_;
        int second_level_num_;
};
