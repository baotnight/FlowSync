package hgc.flowsyncapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import hgc.flowsyncapi.common.ApiResponse;
import hgc.flowsyncapi.entity.*;
import hgc.flowsyncapi.integration.GitHubApiClient;
import hgc.flowsyncapi.mapper.*;
import hgc.flowsyncapi.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskInfoService taskInfoService;
    private final ProjectInfoService projectInfoService;
    private final OperationLogService logService;
    private final ProjectGithubRepoMapper repoMapper;
    private final GitHubAuthService githubAuthService;
    private final GitHubApiClient githubApiClient;
    private final TaskSubtaskMapper subtaskMapper;
    private final TaskCommentMapper commentMapper;
    private final UserMapper userMapper;

    public TaskController(TaskInfoService taskInfoService,
                          ProjectInfoService projectInfoService,
                          OperationLogService logService,
                          ProjectGithubRepoMapper repoMapper,
                          GitHubAuthService githubAuthService,
                          GitHubApiClient githubApiClient,
                          TaskSubtaskMapper subtaskMapper,
                          TaskCommentMapper commentMapper,
                          UserMapper userMapper) {
        this.taskInfoService = taskInfoService;
        this.projectInfoService = projectInfoService;
        this.logService = logService;
        this.repoMapper = repoMapper;
        this.githubAuthService = githubAuthService;
        this.githubApiClient = githubApiClient;
        this.subtaskMapper = subtaskMapper;
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
    }

    @GetMapping
    public ApiResponse<List<TaskInfo>> list(@RequestParam(required = false) Long projectId,
                                             HttpServletRequest request) {
        Long userId = AuthController.getCurrentUserId(request);
        String role = (String) request.getAttribute("currentUserRole");
        List<TaskInfo> tasks = taskInfoService.listTasks(projectId);
        // 管理员看全部；组员/负责人按数据隔离规则过滤
        if (!"管理员".equals(role)) {
            if (projectId == null || !projectInfoService.isProjectOwner(projectId, userId)) {
                tasks.removeIf(t -> !userId.equals(t.getAssigneeId()) && !userId.equals(t.getCreatorId())
                        && !projectInfoService.isProjectOwner(t.getProjectId(), userId));
            }
        }
        return ApiResponse.ok(tasks);
    }

    @PostMapping
    public ApiResponse<TaskInfo> save(@RequestBody TaskInfo task, HttpServletRequest request) {
        Long userId = AuthController.getCurrentUserId(request);
        String role = (String) request.getAttribute("currentUserRole");
        if (!"管理员".equals(role) && task.getProjectId() != null
                && !projectInfoService.isProjectOwner(task.getProjectId(), userId)) {
            return ApiResponse.fail("无权操作：只有项目负责人可以创建/编辑任务");
        }
        TaskInfo saved = taskInfoService.saveTask(task, userId);
        String action = task.getId() == null ? "创建任务" : "编辑任务";
        logService.log(userId, action, "任务", saved.getId(), "任务：" + saved.getTitle());
        return ApiResponse.ok(saved);
    }

    @PostMapping("/{id}/status")
    public ApiResponse<TaskInfo> updateStatus(@PathVariable Long id,
                                               @RequestBody Map<String, String> body,
                                               HttpServletRequest request) {
        Long userId = AuthController.getCurrentUserId(request);
        TaskInfo updated = taskInfoService.updateTaskStatus(id, body.get("status"), userId);
        logService.log(userId, "更新任务状态", "任务", id, "状态 → " + body.get("status"));
        return ApiResponse.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = AuthController.getCurrentUserId(request);
        TaskInfo task = taskInfoService.getById(id);
        if (task == null) return ApiResponse.fail("任务不存在");
        String role = (String) request.getAttribute("currentUserRole");
        if (!"管理员".equals(role) && !projectInfoService.isProjectOwner(task.getProjectId(), userId))
            return ApiResponse.fail("无权操作：只有项目负责人可以删除任务");

        // 删除 GitHub 分支
        String token = githubAuthService.getToken(userId);
        if (token != null) {
            ProjectGithubRepo binding = repoMapper.selectOne(
                    new QueryWrapper<ProjectGithubRepo>().eq("project_id", task.getProjectId()));
            if (binding != null) {
                String branchName = "task/" + id + "-" + slugify(task.getTitle());
                try { githubApiClient.deleteBranch(token, binding.getOwner(), binding.getRepoName(), branchName); }
                catch (Exception ignored) {}
            }
        }
        taskInfoService.deleteTask(id);
        logService.log(userId, "删除任务", "任务", id, "删除任务：" + task.getTitle());
        return ApiResponse.ok(null);
    }

    private String slugify(String text) {
        return text.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "-")
                .replaceAll("-+", "-").replaceAll("^-|-$", "").toLowerCase();
    }

    @PostMapping("/batch-delete")
    public ApiResponse<Map<String, Integer>> batchDelete(@RequestBody Map<String, List<Long>> body,
                                                          HttpServletRequest request) {
        Long userId = AuthController.getCurrentUserId(request);
        String role = (String) request.getAttribute("currentUserRole");
        List<Long> ids = body.get("ids");
        int count = 0;
        for (Long id : ids) {
            TaskInfo task = taskInfoService.getById(id);
            if (task != null && ("管理员".equals(role) || projectInfoService.isProjectOwner(task.getProjectId(), userId))) {
                taskInfoService.deleteTask(id);
                count++;
            }
        }
        logService.log(userId, "批量删除任务", "任务", null, "删除 " + count + " 个任务");
        return ApiResponse.ok("成功删除 " + count + " 个任务", Map.of("deleted", count));
    }

    // ===== 子任务 =====

    @GetMapping("/{taskId}/subtasks")
    public ApiResponse<List<TaskSubtask>> getSubtasks(@PathVariable Long taskId) {
        return ApiResponse.ok(subtaskMapper.selectList(
                new QueryWrapper<TaskSubtask>().eq("task_id", taskId).orderByAsc("id")));
    }

    @PostMapping("/{taskId}/subtasks")
    public ApiResponse<TaskSubtask> addSubtask(@PathVariable Long taskId,
                                                @RequestBody Map<String, Object> body) {
        TaskSubtask st = new TaskSubtask();
        st.setTaskId(taskId);
        st.setTitle(body.get("title").toString());
        st.setCompleted(false);
        subtaskMapper.insert(st);
        return ApiResponse.ok("子任务已添加", st);
    }

    @PutMapping("/{taskId}/subtasks/{subtaskId}")
    public ApiResponse<Void> updateSubtask(@PathVariable Long taskId,
                                            @PathVariable Long subtaskId,
                                            @RequestBody Map<String, Object> body) {
        TaskSubtask st = subtaskMapper.selectById(subtaskId);
        if (st != null && st.getTaskId().equals(taskId)) {
            if (body.containsKey("title")) st.setTitle(body.get("title").toString());
            if (body.containsKey("completed")) st.setCompleted((Boolean) body.get("completed"));
            subtaskMapper.updateById(st);
        }
        return ApiResponse.ok("已更新", null);
    }

    @DeleteMapping("/{taskId}/subtasks/{subtaskId}")
    public ApiResponse<Void> deleteSubtask(@PathVariable Long taskId,
                                            @PathVariable Long subtaskId) {
        subtaskMapper.delete(new QueryWrapper<TaskSubtask>()
                .eq("id", subtaskId).eq("task_id", taskId));
        return ApiResponse.ok("已删除", null);
    }

    // ===== 评论 =====

    @GetMapping("/{taskId}/comments")
    public ApiResponse<List<Map<String, Object>>> getComments(@PathVariable Long taskId) {
        List<TaskComment> comments = commentMapper.selectList(
                new QueryWrapper<TaskComment>().eq("task_id", taskId).orderByAsc("create_time"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (TaskComment c : comments) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("taskId", c.getTaskId());
            item.put("userId", c.getUserId());
            item.put("content", c.getContent());
            item.put("createTime", c.getCreateTime() != null ? c.getCreateTime().toString() : "");
            User u = userMapper.selectById(c.getUserId());
            item.put("userName", u != null ? u.getRealName() : "未知");
            result.add(item);
        }
        return ApiResponse.ok(result);
    }

    @PostMapping("/{taskId}/comments")
    public ApiResponse<Map<String, Object>> addComment(@PathVariable Long taskId,
                                                        @RequestBody Map<String, String> body,
                                                        HttpServletRequest request) {
        Long userId = AuthController.getCurrentUserId(request);
        TaskComment c = new TaskComment();
        c.setTaskId(taskId);
        c.setUserId(userId);
        c.setContent(body.get("content"));
        commentMapper.insert(c);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", c.getId());
        result.put("content", c.getContent());
        result.put("createTime", c.getCreateTime() != null ? c.getCreateTime().toString() : "");
        User u = userMapper.selectById(userId);
        result.put("userName", u != null ? u.getRealName() : "未知");
        return ApiResponse.ok("评论已发送", result);
    }
}
