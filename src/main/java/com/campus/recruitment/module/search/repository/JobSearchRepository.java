package com.campus.recruitment.module.search.repository;

import com.campus.recruitment.module.search.document.JobDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface JobSearchRepository extends ElasticsearchRepository<JobDocument, Long> {
}
