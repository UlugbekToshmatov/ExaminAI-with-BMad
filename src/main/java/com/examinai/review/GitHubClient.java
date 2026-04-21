package com.examinai.review;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/repos")
public interface GitHubClient {

    @GetExchange(value = "/{owner}/{repo}/pulls/{pullNumber}", accept = "application/vnd.github.v3.diff")
    String getPrDiff(
        @PathVariable String owner,
        @PathVariable String repo,
        @PathVariable("pullNumber") int pullNumber
    );
}
