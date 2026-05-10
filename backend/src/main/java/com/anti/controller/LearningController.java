package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.CaseBrowseLog;
import com.anti.entity.ForumPost;
import com.anti.entity.News;
import com.anti.entity.UserChallengeRecord;
import com.anti.mapper.CaseBrowseLogMapper;
import com.anti.mapper.ForumPostMapper;
import com.anti.mapper.NewsMapper;
import com.anti.mapper.UserChallengeRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.anti.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "学习记录")
@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningController {

    private final CaseBrowseLogMapper caseBrowseLogMapper;
    private final NewsMapper newsMapper;
    private final UserChallengeRecordMapper challengeRecordMapper;
    private final ForumPostMapper forumPostMapper;

    @GetMapping("/records")
    @Operation(summary = "获取学习记录")
    public Result<Map<String, Object>> getLearningRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal LoginUser loginUser) {

        List<Map<String, Object>> allRecords = new ArrayList<>();

        Long userId = loginUser.getUserId();

        // 案例浏览记录
        List<CaseBrowseLog> caseLogs = caseBrowseLogMapper.selectList(
                new LambdaQueryWrapper<CaseBrowseLog>()
                        .eq(CaseBrowseLog::getUserId, userId)
                        .orderByDesc(CaseBrowseLog::getBrowseTime)
                        .last("LIMIT 100")
        );
        for (CaseBrowseLog log : caseLogs) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", log.getId());
            record.put("type", "case");
            record.put("content", "浏览了案例: " + log.getCaseId());
            record.put("time", log.getBrowseTime());
            allRecords.add(record);
        }

        // 闯关记录
        List<UserChallengeRecord> challengeRecords = challengeRecordMapper.selectList(
                new LambdaQueryWrapper<UserChallengeRecord>()
                        .eq(UserChallengeRecord::getUserId, userId)
                        .orderByDesc(UserChallengeRecord::getEndTime)
                        .last("LIMIT 100")
        );
        for (UserChallengeRecord record : challengeRecords) {
            Map<String, Object> r = new HashMap<>();
            r.put("id", record.getId());
            r.put("type", "challenge");
            r.put("content", "完成了闯关: " + record.getChallengeId() +
                    (record.getScore() != null ? "，得分: " + record.getScore() : ""));
            r.put("time", record.getEndTime());
            allRecords.add(r);
        }

        // 帖子记录
        List<ForumPost> posts = forumPostMapper.selectList(
                new LambdaQueryWrapper<ForumPost>()
                        .eq(ForumPost::getUserId, userId)
                        .orderByDesc(ForumPost::getCreateTime)
                        .last("LIMIT 100")
        );
        for (ForumPost post : posts) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", post.getId());
            record.put("type", "forum");
            record.put("content", "发布了帖子: " + post.getTitle());
            record.put("time", post.getCreateTime());
            allRecords.add(record);
        }

        // 按时间排序
        allRecords.sort((a, b) -> {
            Object timeA = a.get("time");
            Object timeB = b.get("time");
            if (timeA == null || timeB == null) return 0;
            return timeB.toString().compareTo(timeA.toString());
        });

        // 分页
        int total = allRecords.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<Map<String, Object>> pageRecords = start < total
                ? allRecords.subList(start, end)
                : Collections.emptyList();

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageRecords);
        result.put("total", total);
        result.put("size", size);
        result.put("current", page);
        result.put("pages", (total + size - 1) / size);

        return Result.success(result);
    }
}
