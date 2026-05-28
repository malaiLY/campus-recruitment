package com.campus.recruitment.module.search.controller;

import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.common.result.R;
import com.campus.recruitment.module.job.vo.JobVO;
import com.campus.recruitment.module.search.service.JobSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class SearchController {

    private final JobSearchService jobSearchService;

    @GetMapping("/search")
    public R<PageResult<JobVO>> search(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer salaryMin,
            @RequestParam(required = false) Integer salaryMax,
            @RequestParam(required = false) String education,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        PageResult<JobVO> result = jobSearchService.search(
                keyword, city, salaryMin, salaryMax, education, pageNum, pageSize);
        return R.ok(result);
    }
}
