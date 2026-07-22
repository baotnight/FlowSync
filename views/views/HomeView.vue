<template>
  <el-container class="home-container">
    <el-container class="workspace-main full-workspace">
      <el-header class="top-header">
        <div class="topbar-main">
          <div class="brand-strip">
            <div class="brand-mark">F</div>
            <div>
              <strong>FlowSync</strong>
              <span>协同管理驾驶舱</span>
            </div>
          </div>

          <div class="header-right">
            <el-tag type="success">{{ currentUser.role }}</el-tag>
            <span class="user-name">{{ currentUser.realName }}</span>
            <el-button type="danger" size="small" plain @click="handleLogout">退出登录</el-button>
          </div>
        </div>

        <nav class="module-nav" aria-label="主功能导航">
          <button :class="{ active: currentPanel === 'overview' }" @click="handleSelectMenu('overview')">
            <el-icon><Odometer /></el-icon><span>总览</span>
          </button>
          <button :class="{ active: currentPanel === 'project' }" @click="handleSelectMenu('project')">
            <el-icon><Files /></el-icon><span>项目</span>
          </button>
          <button v-if="isLeader" :class="{ active: currentPanel === 'group-manage' }" @click="handleSelectMenu('group-manage')">
            <el-icon><UserFilled /></el-icon><span>小组</span>
          </button>
          <button v-if="isLeader" :class="{ active: currentPanel === 'task-ai' }" @click="handleSelectMenu('task-ai')">
            <el-icon><Cpu /></el-icon><span>AI 拆解</span>
          </button>
          <button :class="{ active: currentPanel === 'task' }" @click="handleSelectMenu('task')">
            <el-icon><List /></el-icon><span>任务</span>
          </button>
          <button :class="{ active: currentPanel === 'log' }" @click="handleSelectMenu('log')">
            <el-icon><Calendar /></el-icon><span>进度</span>
          </button>
          <button :class="{ active: currentPanel === 'summary' }" @click="handleSelectMenu('summary')">
            <el-icon><Document /></el-icon><span>复盘</span>
          </button>
          <button :class="{ active: currentPanel === 'members' }" @click="handleSelectMenu('members')">
            <el-icon><UserFilled /></el-icon><span>成员</span>
          </button>
          <button :class="{ active: currentPanel === 'profile' }" @click="handleSelectMenu('profile')">
            <el-icon><User /></el-icon><span>我的</span>
          </button>
        </nav>
      </el-header>

      <el-main class="main-content">
        <section v-if="currentPanel === 'overview'" class="screen overview-screen">
          <div class="overview-hero">
            <div class="hero-copy">
              <span class="eyebrow">Command Center</span>
              <h1>让项目、任务、成员和进度在同一个节奏里流动</h1>
              <p>新版结构把核心操作前置：从总览判断状态，从卡片进入对应模块，再用表格处理精确数据。</p>
            </div>
            <div class="orbit-map" aria-hidden="true">
              <span class="orbit-ring"></span>
              <span class="orbit-node node-project">项目</span>
              <span class="orbit-node node-task">任务</span>
              <span class="orbit-node node-user">成员</span>
            </div>
          </div>

          <div class="metric-grid">
            <article class="metric-card tone-blue">
              <span>项目数</span>
              <strong>{{ projectList.length }}</strong>
              <em>当前项目总量</em>
              <div class="metric-preview">
                <b>项目概况</b>
                <p>{{ activeProjectCount }} 个进行中 / {{ completedProjectCount }} 个已完成</p>
                <small>最近项目：{{ latestProjectName }}</small>
              </div>
            </article>
            <article class="metric-card tone-green">
              <span>任务数</span>
              <strong>{{ allTaskList.length }}</strong>
              <em>当前任务总量</em>
              <div class="metric-preview">
                <b>任务状态</b>
                <p>{{ pendingTaskCount }} 个待推进 / {{ doneTaskCount }} 个已完成</p>
                <small>最近任务：{{ latestTaskTitle }}</small>
              </div>
            </article>
            <article class="metric-card tone-amber">
              <span>进度记录</span>
              <strong>{{ logList.length }}</strong>
              <em>累计同步记录</em>
              <div class="metric-preview">
                <b>同步动态</b>
                <p>{{ latestLogOperator }} 最近更新</p>
                <small>{{ latestLogContent }}</small>
              </div>
            </article>
            <article class="metric-card tone-purple">
              <span>总结报告</span>
              <strong>{{ summaryList.length }}</strong>
              <em>累计归档报告</em>
              <div class="metric-preview">
                <b>报告沉淀</b>
                <p>{{ summaryList.length }} 份复盘内容</p>
                <small>最近报告：{{ latestSummaryProject }}</small>
              </div>
            </article>
          </div>

          <div class="overview-grid">
            <article class="surface surface-large">
              <div class="surface-head">
                <div>
                  <span class="eyebrow">Task Flow</span>
                  <h2>近期任务</h2>
                </div>
                <el-button size="small" @click="handleSelectMenu('task')">全部任务</el-button>
              </div>
              <div class="flow-list">
                <div class="flow-row" v-for="task in allTaskList.slice(0, 5)" :key="task.id">
                  <span class="flow-dot"></span>
                  <div>
                    <strong>{{ task.title }}</strong>
                    <p>{{ getProjectName(task.projectId) }} · {{ getUserRealName(task.assigneeId) }} · {{ task.dueDate || '未设置截止日期' }}</p>
                  </div>
                  <el-tag size="small" :type="task.status === '已完成' ? 'success' : 'primary'">{{ task.status }}</el-tag>
                </div>
                <el-empty v-if="allTaskList.length === 0" description="暂无任务" />
              </div>
            </article>

            <article class="surface">
              <div class="surface-head">
                <div>
                  <span class="eyebrow">Pulse</span>
                  <h2>协同脉冲</h2>
                </div>
              </div>
              <div class="pulse-stack">
                <div class="pulse-card">
                  <span>成员</span>
                  <strong>{{ userList.length }}</strong>
                  <div class="metric-preview pulse-preview">
                    <b>成员分布</b>
                    <p>{{ leaderCount }} 位负责人 / {{ memberCount }} 位成员</p>
                    <small>当前用户：{{ currentUser.realName || currentUser.username }}</small>
                  </div>
                </div>
                <div class="pulse-card" v-if="isLeader">
                  <span>小组</span>
                  <strong>{{ groupList.length }}</strong>
                  <div class="metric-preview pulse-preview">
                    <b>小组结构</b>
                    <p>{{ groupList.length }} 个协作小组</p>
                    <small>最近小组：{{ latestGroupName }}</small>
                  </div>
                </div>
                <div class="pulse-card">
                  <span>复盘</span>
                  <strong>{{ summaryList.length }}</strong>
                  <div class="metric-preview pulse-preview">
                    <b>复盘节奏</b>
                    <p>{{ summaryList.length }} 份总结报告</p>
                    <small>{{ latestSummaryProject }}</small>
                  </div>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section v-if="currentPanel === 'project'" class="screen">
          <div class="screen-title">
            <div><span class="eyebrow">Projects</span><h1>项目编排</h1><p>管理项目、归属小组、优先级和周期。</p></div>
            <el-button type="primary" v-if="isLeader" @click="openAddProjectDialog">新建项目</el-button>
          </div>
          <div class="surface table-surface">
            <el-table :data="projectList" style="width: 100%" v-loading="loading">
              <el-table-column prop="id" label="项目ID" width="80" />
              <el-table-column prop="name" label="项目名称" width="180" />
              <el-table-column prop="groupId" label="绑定小组" width="150"><template #default="scope"><el-tag type="info">{{ getGroupName(scope.row.groupId) }}</el-tag></template></el-table-column>
              <el-table-column prop="description" label="项目说明" show-overflow-tooltip />
              <el-table-column prop="status" label="状态" width="120"><template #default="scope"><el-tag :type="scope.row.status === '已完成' ? 'success' : 'primary'">{{ scope.row.status }}</el-tag></template></el-table-column>
              <el-table-column prop="priority" label="优先级" width="100"><template #default="scope"><el-tag :type="scope.row.priority === '高' ? 'danger' : scope.row.priority === '中' ? 'warning' : 'info'">{{ scope.row.priority }}</el-tag></template></el-table-column>
              <el-table-column prop="startDate" label="开始日期" width="120" />
              <el-table-column prop="endDate" label="结束日期" width="120" />
              <el-table-column label="操作" width="180" v-if="isLeader"><template #default="scope"><el-button size="small" @click="openEditProjectDialog(scope.row)">编辑</el-button><el-button size="small" type="danger" @click="handleDeleteProject(scope.row.id)">删除</el-button></template></el-table-column>
            </el-table>
          </div>
        </section>

        <section v-if="currentPanel === 'group-manage'" class="screen">
          <div class="screen-title">
            <div><span class="eyebrow">Groups</span><h1>小组调度</h1><p>创建小组并维护成员分配关系。</p></div>
            <el-button type="primary" @click="openAddGroupDialog">创建小组</el-button>
          </div>
          <div class="surface table-surface">
            <el-table :data="groupList" style="width: 100%">
              <el-table-column prop="id" label="小组ID" width="100" />
              <el-table-column prop="name" label="小组名称" width="200" />
              <el-table-column prop="description" label="描述" />
              <el-table-column prop="createTime" label="创建时间" width="200" />
              <el-table-column label="操作" width="260"><template #default="scope"><el-button size="small" type="success" @click="openManageMembersDialog(scope.row)">分配成员</el-button><el-button size="small" @click="openEditGroupDialog(scope.row)">编辑</el-button><el-button size="small" type="danger" @click="handleDeleteGroup(scope.row.id)">解散</el-button></template></el-table-column>
            </el-table>
          </div>
        </section>

        <section v-if="currentPanel === 'task-ai'" class="screen ai-screen">
          <div class="ai-compose">
            <div class="ai-copy"><span class="eyebrow">AI Planner</span><h1>从目标到可执行任务</h1><p>选择所属项目，输入目标后生成任务草稿；执行人限定在该项目小组内。</p></div>
            <div class="surface ai-form-card">
              <el-form :model="aiForm" label-width="100px">
                <el-form-item label="选择项目"><el-select v-model="aiForm.projectId" placeholder="选择项目" style="width: 100%" @change="handleAiProjectChange"><el-option v-for="p in projectList" :key="p.id" :label="p.name" :value="p.id" /></el-select></el-form-item>
                <el-form-item label="任务目标"><el-input v-model="aiForm.goal" placeholder="例如：开发小组协同系统的用户登录与主页框架" /></el-form-item>
                <el-form-item label="补充说明"><el-input v-model="aiForm.description" type="textarea" placeholder="可选" /></el-form-item>
                <el-form-item><el-button type="primary" :loading="aiLoading" @click="handleGenerateAiPlan">调用 AI 智能拆解</el-button></el-form-item>
              </el-form>
            </div>
          </div>
          <div v-if="aiPlanResult" class="surface ai-result-area">
            <el-alert :title="aiPlanResult.summary" type="info" :closable="false" show-icon style="margin-bottom: 20px;" />
            <h4>AI 拆解出的任务草稿</h4>
            <el-table :data="aiPlanResult.items" style="width: 100%; margin-bottom: 20px;" @selection-change="handleSelectionChange">
              <el-table-column type="selection" width="55" />
              <el-table-column prop="title" label="任务标题" width="220" />
              <el-table-column prop="description" label="任务详情" />
              <el-table-column label="优先级" width="120"><template #default="scope"><el-select v-model="scope.row.priority" size="small"><el-option label="高" value="高" /><el-option label="中" value="中" /><el-option label="低" value="低" /></el-select></template></el-table-column>
              <el-table-column label="执行人" width="150"><template #default="scope"><el-select v-model="scope.row.assigneeId" size="small" placeholder="指派执行人"><el-option v-for="u in projectFilteredUsers" :key="u.id" :label="u.realName" :value="u.id" /></el-select></template></el-table-column>
              <el-table-column prop="suggestedDays" label="建议工期(天)" width="120" />
            </el-table>
            <div class="table-actions"><el-button type="success" :disabled="selectedAiItems.length === 0" @click="handleImportAiTasks">导入选中任务到项目</el-button></div>
          </div>
        </section>

        <section v-if="currentPanel === 'task'" class="screen">
          <div class="screen-title">
            <div><span class="eyebrow">Tasks</span><h1>任务流</h1><p>处理责任人、状态、优先级和截止日期。</p></div>
            <el-button type="primary" v-if="isLeader" @click="openAddTaskDialog">手动创建任务</el-button>
          </div>
          <div class="surface table-surface">
            <el-table :data="allTaskList" style="width: 100%" v-loading="loading">
              <el-table-column prop="id" label="ID" width="70" />
              <el-table-column prop="projectId" label="项目" width="150"><template #default="scope">{{ getProjectName(scope.row.projectId) }}</template></el-table-column>
              <el-table-column prop="title" label="任务标题" width="180" />
              <el-table-column prop="description" label="任务详情" show-overflow-tooltip />
              <el-table-column prop="assigneeId" label="执行人" width="110"><template #default="scope">{{ getUserRealName(scope.row.assigneeId) }}</template></el-table-column>
              <el-table-column prop="status" label="任务状态" width="110"><template #default="scope"><el-tag :type="scope.row.status === '已完成' ? 'success' : 'primary'">{{ scope.row.status }}</el-tag></template></el-table-column>
              <el-table-column prop="priority" label="优先级" width="90"><template #default="scope"><el-tag :type="scope.row.priority === '高' ? 'danger' : 'info'">{{ scope.row.priority }}</el-tag></template></el-table-column>
              <el-table-column prop="dueDate" label="截止日期" width="120" />
              <el-table-column label="操作" width="160"><template #default="scope"><el-button v-if="isLeader || scope.row.assigneeId === currentUser.id" size="small" @click="openEditTaskDialog(scope.row)">{{ isLeader ? '编辑' : '更新状态' }}</el-button><el-button v-if="isLeader" size="small" type="danger" @click="handleDeleteTask(scope.row.id)">删除</el-button></template></el-table-column>
            </el-table>
          </div>
        </section>

        <section v-if="currentPanel === 'log'" class="screen">
          <div class="screen-title"><div><span class="eyebrow">Timeline</span><h1>进度轨迹</h1><p>以时间线方式查看成员对任务的进度同步。</p></div><el-button type="warning" @click="openAddLogDialog">报进度 / 记日志</el-button></div>
          <div class="surface timeline-surface">
            <el-timeline>
              <el-timeline-item v-for="log in logList" :key="log.id" :timestamp="log.createTime" placement="top" type="primary">
                <el-card><h4>{{ getUserRealName(log.operatorId) }} 更新了进度</h4><p class="timeline-progress">任务 ID: {{ log.taskId }} - 进度已达：{{ log.progressPercent }}%</p><p class="timeline-copy">说明：{{ log.content }}</p></el-card>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-if="logList.length === 0" description="暂无进度记录" />
          </div>
        </section>

        <section v-if="currentPanel === 'summary'" class="screen">
          <div class="screen-title"><div><span class="eyebrow">Reports</span><h1>复盘归档</h1><p>沉淀项目阶段总结和最终总结。</p></div><el-button type="success" @click="openAddSummaryDialog">撰写总结</el-button></div>
          <div class="surface table-surface">
            <el-table :data="summaryList" style="width: 100%">
              <el-table-column prop="id" label="总结ID" width="80" />
              <el-table-column prop="projectId" label="所属项目" width="180"><template #default="scope">{{ getProjectName(scope.row.projectId) }}</template></el-table-column>
              <el-table-column prop="summaryType" label="总结类型" width="120"><template #default="scope"><el-tag :type="scope.row.summaryType === '最终总结' ? 'danger' : 'info'">{{ scope.row.summaryType }}</el-tag></template></el-table-column>
              <el-table-column prop="content" label="总结详情内容" />
              <el-table-column prop="createdBy" label="撰写人" width="120"><template #default="scope">{{ getUserRealName(scope.row.createdBy) }}</template></el-table-column>
              <el-table-column prop="createTime" label="提交时间" width="180" />
            </el-table>
          </div>
        </section>

        <section v-if="currentPanel === 'members'" class="screen">
          <div class="screen-title"><div><span class="eyebrow">Members</span><h1>成员矩阵</h1><p>查看系统成员与角色信息。</p></div></div>
          <div class="surface table-surface">
            <el-table :data="userList" style="width: 100%">
              <el-table-column prop="id" label="工号/ID" width="100" />
              <el-table-column prop="username" label="用户名" width="180" />
              <el-table-column prop="realName" label="真实姓名" width="180" />
              <el-table-column prop="role" label="系统角色"><template #default="scope"><el-tag :type="scope.row.role === '负责人' ? 'danger' : 'success'">{{ scope.row.role }}</el-tag></template></el-table-column>
            </el-table>
          </div>
        </section>

        <section v-if="currentPanel === 'profile'" class="screen">
          <div class="screen-title"><div><span class="eyebrow">Profile</span><h1>我的身份</h1><p>当前登录用户的账号、姓名和权限角色。</p></div></div>
          <div class="surface profile-surface">
            <el-descriptions border :column="1" style="max-width: 640px;">
              <el-descriptions-item label="当前账号">{{ currentUser.username }}</el-descriptions-item>
              <el-descriptions-item label="真实姓名">{{ currentUser.realName }}</el-descriptions-item>
              <el-descriptions-item label="您的权限角色">{{ currentUser.role }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </section>
      </el-main>
    </el-container>

    <el-dialog v-model="dialogVisible" :title="projectForm.id ? '编辑项目' : '新建项目'" width="500px">
      <el-form :model="projectForm" label-width="100px">
        <el-form-item label="项目名称">
          <el-input v-model="projectForm.name" />
        </el-form-item>
        <el-form-item label="归属协同组">
          <el-select v-model="projectForm.groupId" placeholder="请选择负责该项目的小组" style="width: 100%">
            <el-option v-for="g in groupList" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="projectForm.description" type="textarea" />
        </el-form-item>
        <el-form-item label="项目状态">
          <el-select v-model="projectForm.status" style="width: 100%">
            <el-option label="未开始" value="未开始" />
            <el-option label="进行中" value="进行中" />
            <el-option label="已完成" value="已完成" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="projectForm.priority" style="width: 100%">
            <el-option label="低" value="低" />
            <el-option label="中" value="中" />
            <el-option label="高" value="高" />
          </el-select>
        </el-form-item>
        <el-form-item label="起止日期">
          <el-date-picker v-model="projectDateRange" type="daterange" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSaveProject">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 1-2：小组新建/编辑 -->
    <el-dialog v-model="groupDialogVisible" :title="groupForm.isEdit ? '编辑小组信息' : '创建新协作小组'" width="500px">
      <el-form :model="groupForm" label-width="100px">
        <el-form-item label="小组 ID" required>
          <el-input-number 
            v-model="groupForm.id" 
            :disabled="groupForm.isEdit" 
            :controls="false"
            placeholder="请输入数字小组 ID" 
            style="width: 100%; text-align: left;"
          />
        </el-form-item>
        <el-form-item label="小组名称">
          <el-input v-model="groupForm.name" placeholder="请输入协同小组名称" />
        </el-form-item>
        <el-form-item label="描述说明">
          <el-input v-model="groupForm.description" type="textarea" placeholder="填写小组分工描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSaveGroup">保 存</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 1-3：小组分配组员 -->
    <el-dialog v-model="memberManageVisible" title="👥 分配成员加入小组" width="550px">
      <p style="margin-bottom: 15px; color: #e6a23c;">一个人可以被拉入多个不同的小组，项目指派只能选择小组成员。</p>
      <el-checkbox-group v-model="checkedUserIds">
        <el-checkbox v-for="u in userList" :key="u.id" :label="u.id" border style="margin-bottom: 10px;">
          {{ u.realName }} ({{ u.role }})
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="memberManageVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSaveGroupMembers">保 存 人 员</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 2：任务新建/编辑 -->
    <el-dialog 
      v-model="taskDialogVisible" 
      :title="isLeader ? (taskForm.id ? '编辑任务信息' : '手动创建任务') : '更新任务状态'" 
      width="500px"
    >
      <el-form :model="taskForm" label-width="100px">
        <el-form-item label="所属项目">
          <el-select v-model="taskForm.projectId" :disabled="!isLeader" style="width: 100%" @change="handleTaskProjectChange">
            <el-option v-for="p in projectList" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务标题">
          <el-input v-model="taskForm.title" :disabled="!isLeader" />
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input v-model="taskForm.description" type="textarea" :disabled="!isLeader" />
        </el-form-item>
        
        <el-form-item label="指派执行人">
          <el-select v-model="taskForm.assigneeId" :disabled="!isLeader" placeholder="请选择本小组执行人" style="width: 100%">
            <el-option v-for="u in projectFilteredUsers" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="任务状态">
          <el-select v-model="taskForm.status" style="width: 100%">
            <el-option label="未开始" value="未开始" />
            <el-option label="进行中" value="进行中" />
            <el-option label="已完成" value="已完成" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="taskForm.priority" :disabled="!isLeader" style="width: 100%">
            <el-option label="低" value="低" />
            <el-option label="中" value="中" />
            <el-option label="高" value="高" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="taskForm.dueDate" value-format="YYYY-MM-DD" :disabled="!isLeader" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSaveTask">保存提交</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 3：写进度记录 -->
    <el-dialog v-model="logDialogVisible" title="✍️ 报进度 / 记录更新" width="500px">
      <el-form :model="logForm" label-width="110px">
        <el-form-item label="选择任务">
          <el-select v-model="logForm.taskId" placeholder="请选择您正在执行的任务" style="width: 100%">
            <el-option v-for="t in allTaskList" :key="t.id" :label="t.title" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="当前完成度 (%)">
          <el-slider v-model="logForm.progressPercent" :min="0" :max="100" show-input />
        </el-form-item>
        <el-form-item label="工作日志说明">
          <el-input v-model="logForm.content" type="textarea" placeholder="请简要写几句日志" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="logDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSaveLog">确认报进度</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 4：撰写总结报告 -->
    <el-dialog v-model="summaryDialogVisible" title="📝 撰写总结报告" width="500px">
      <el-form :model="summaryForm" label-width="100px">
        <el-form-item label="所属项目">
          <el-select v-model="summaryForm.projectId" style="width: 100%">
            <el-option v-for="p in projectList" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="总结类型">
          <el-select v-model="summaryForm.summaryType" style="width: 100%">
            <el-option label="阶段总结" value="阶段总结" />
            <el-option label="最终总结" value="最终总结" />
          </el-select>
        </el-form-item>
        <el-form-item label="总结总结报告">
          <el-input v-model="summaryForm.content" type="textarea" :rows="5" placeholder="请撰写具体的总结" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="summaryDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSaveSummary">提交报告</el-button>
      </template>
    </el-dialog>

  </el-container>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import axios from 'axios'
import {
  Odometer, Files, Cpu, List, Calendar, Document, UserFilled, User
} from '@element-plus/icons-vue'

const router = useRouter()

// 👤 用户与权限状态
const currentUser = ref({ id: 0, username: '', realName: '', role: '' })
const isLeader = ref(false)
const currentPanel = ref('overview')

// 📁 数据列表
const loading = ref(false)
const projectList = ref([]) 
const allTaskList = ref([])
const logList = ref([])
const summaryList = ref([])
const userList = ref([]) 

// 👥 小组管理相关状态
const groupList = ref([])
const groupDialogVisible = ref(false)
const memberManageVisible = ref(false)
const currentGroupId = ref(null) 
const checkedUserIds = ref([]) 
const groupForm = reactive({ id: null, name: '', description: '', isEdit: false })

// 🎯 核心联动：存放经过项目小组强隔离过滤后的成员列表
const projectFilteredUsers = ref([])

// 弹窗可见度
const dialogVisible = ref(false) 
const taskDialogVisible = ref(false)
const logDialogVisible = ref(false)
const summaryDialogVisible = ref(false)

// 📁 1. 项目表单
const projectDateRange = ref([]) 
const projectForm = reactive({
  id: null, name: '', description: '', status: '未开始', priority: '中', startDate: '', endDate: '', groupId: null
})

// 📋 2. 任务表单
const taskForm = reactive({
  id: null, projectId: null, title: '', description: '', assigneeId: null, status: '未开始', priority: '中', dueDate: ''
})

// 📈 3. 进度日志表单
const logForm = reactive({ taskId: null, progressPercent: 0, content: '' })

// 📝 4. 总结报告表单
const summaryForm = reactive({ projectId: null, summaryType: '阶段总结', content: '' })

// 🤖 5. AI智能拆解相关的响应式数据
const aiLoading = ref(false)
const aiPlanResult = ref(null) 
const selectedAiItems = ref([]) 
const aiForm = reactive({ projectId: null, goal: '', description: '' })

onMounted(() => {
  const userStr = sessionStorage.getItem('currentUser')
  if (userStr) {
    currentUser.value = JSON.parse(userStr)
    isLeader.value = currentUser.value.role === '负责人'
    fetchAllData()
  } else {
    router.push('/login')
  }
})

// 同步获取全量数据
const fetchAllData = () => {
  fetchProjects()
  fetchUsers()
  fetchTasks()
  fetchLogs()
  fetchSummaries()
  fetchGroups()
}

const handleSelectMenu = (index) => {
  currentPanel.value = index
  fetchAllData()
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗?', '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    sessionStorage.removeItem('currentUser')
    ElMessage.success('退出成功')
    router.push('/login')
  }).catch(() => {})
}

// 翻译辅助方法
const getProjectName = (projectId) => {
  const p = projectList.value.find(item => item.id === projectId)
  return p ? p.name : '未知项目'
}
const getUserRealName = (userId) => {
  const u = userList.value.find(item => item.id === userId)
  return u ? u.realName : '未指派'
}
const getGroupName = (groupId) => {
  const g = groupList.value.find(item => item.id === groupId)
  return g ? g.name : '未绑定小组（全员可见）'
}

const activeProjectCount = computed(() => projectList.value.filter(item => item.status !== '已完成').length)
const completedProjectCount = computed(() => projectList.value.filter(item => item.status === '已完成').length)
const latestProjectName = computed(() => projectList.value.at(-1)?.name || '暂无项目')
const pendingTaskCount = computed(() => allTaskList.value.filter(item => item.status !== '已完成').length)
const doneTaskCount = computed(() => allTaskList.value.filter(item => item.status === '已完成').length)
const latestTaskTitle = computed(() => allTaskList.value.at(-1)?.title || '暂无任务')
const latestLogOperator = computed(() => getUserRealName(logList.value.at(-1)?.operatorId))
const latestLogContent = computed(() => logList.value.at(-1)?.content || '暂无进度内容')
const latestSummaryProject = computed(() => {
  const latest = summaryList.value.at(-1)
  return latest ? getProjectName(latest.projectId) : '暂无报告'
})
const leaderCount = computed(() => userList.value.filter(item => String(item.role || '').includes('负责')).length)
const memberCount = computed(() => Math.max(userList.value.length - leaderCount.value, 0))
const latestGroupName = computed(() => groupList.value.at(-1)?.name || '暂无小组')

// ==========================================
// 👥 【小组管理模块】
// ==========================================
const fetchGroups = async () => {
  try {
    const response = await axios.get(`/api/groups?currentUserId=${currentUser.value.id}`)
    if (response.data.success) { groupList.value = response.data.data }
  } catch (error) { console.error(error) }
}

const openAddGroupDialog = () => {
  groupForm.id = null
  groupForm.name = ''
  groupForm.description = ''
  groupForm.isEdit = false
  groupDialogVisible.value = true
}

const openEditGroupDialog = (row) => {
  groupForm.id = row.id
  groupForm.name = row.name
  groupForm.description = row.description
  groupForm.isEdit = true
  groupDialogVisible.value = true
}

const handleSaveGroup = async () => {
  if (groupForm.id === null || groupForm.id === undefined) { ElMessage.warning('小组 ID 必填'); return }
  if (!groupForm.name) { ElMessage.warning('名称必填'); return }
  try {
    const response = await axios.post(`/api/groups?currentUserId=${currentUser.value.id}&isEdit=${groupForm.isEdit}`, groupForm)
    if (response.data.success) {
      ElMessage.success(response.data.message); groupDialogVisible.value = false; fetchGroups()
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) { console.error(error) }
}

const handleDeleteGroup = (id) => {
  ElMessageBox.confirm('解散该组会同步清理所有成员和项目归属，确认吗？', '解散警告', { type: 'danger' }).then(async () => {
    try {
      const response = await axios.delete(`/api/groups/${id}`)
      if (response.data.success) { ElMessage.success(response.data.message); fetchGroups() }
    } catch (error) { console.error(error) }
  }).catch(() => {})
}

// 打开“分配小组成员”对话框
const openManageMembersDialog = async (row) => {
  currentGroupId.value = row.id
  checkedUserIds.value = [] 
  memberManageVisible.value = true
  
  try {
    const response = await axios.get(`/api/groups/${row.id}/members`)
    if (response.data.success) {
      checkedUserIds.value = response.data.data 
    }
  } catch (error) { console.error(error) }
}

// 提交保存小组成员
const handleSaveGroupMembers = async () => {
  try {
    const response = await axios.post(`/api/groups/${currentGroupId.value}/members`, checkedUserIds.value)
    if (response.data.success) {
      ElMessage.success(response.data.message)
      memberManageVisible.value = false
    }
  } catch (error) { console.error(error) }
}

// ==========================================
// 📁 【项目管理】
// ==========================================
const fetchProjects = async () => {
  try {
    const response = await axios.get(`/api/projects?currentUserId=${currentUser.value.id}`)
    if (response.data.success) { projectList.value = response.data.data }
  } catch (error) { console.error(error) }
}

const openAddProjectDialog = () => {
  projectForm.id = null; projectForm.name = ''; projectForm.description = ''; projectForm.status = '未开始'; projectForm.priority = '中'; projectForm.startDate = ''; projectForm.endDate = ''; projectForm.groupId = null; projectDateRange.value = []
  dialogVisible.value = true
}

const openEditProjectDialog = (row) => {
  projectForm.id = row.id; projectForm.name = row.name; projectForm.description = row.description; projectForm.status = row.status; projectForm.priority = row.priority; projectForm.startDate = row.startDate; projectForm.endDate = row.endDate; projectForm.groupId = row.groupId
  if (row.startDate && row.endDate) { projectDateRange.value = [row.startDate, row.endDate] }
  dialogVisible.value = true
}

const handleSaveProject = async () => {
  if (!projectForm.name) { ElMessage.warning('项目名称必填'); return }
  if (projectDateRange.value && projectDateRange.value.length === 2) {
    projectForm.startDate = projectDateRange.value[0]
    projectForm.endDate = projectDateRange.value[1]
  }
  try {
    const response = await axios.post(`/api/projects?currentUserId=${currentUser.value.id}`, projectForm)
    if (response.data.success) {
      ElMessage.success(response.data.message); dialogVisible.value = false; fetchProjects()
    }
  } catch (error) { console.error(error) }
}

const handleDeleteProject = (id) => {
  ElMessageBox.confirm('确定删除吗？', '警告', { type: 'danger' }).then(async () => {
    try {
      const response = await axios.delete(`/api/projects/${id}?currentUserId=${currentUser.value.id}`)
      if (response.data.success) { ElMessage.success(response.data.message); fetchProjects() }
    } catch (error) { console.error(error) }
  }).catch(() => {})
}

// ==========================================
// 👥 【全系统成员加载】
// ==========================================
const fetchUsers = async () => {
  try {
    const response = await axios.get('/api/users')
    if (response.data.success) { userList.value = response.data.data }
  } catch (error) { console.error(error) }
}

// ==========================================
// 📋 【任务管理】
// ==========================================
const fetchTasks = async () => {
  try {
    const response = await axios.get(`/api/tasks?currentUserId=${currentUser.value.id}`)
    if (response.data.success) { allTaskList.value = response.data.data }
  } catch (error) { console.error(error) }
}

const handleTaskProjectChange = async (projectId) => {
  taskForm.assigneeId = null 
  projectFilteredUsers.value = []
  if (!projectId) return

  try {
    const response = await axios.get(`/api/groups/project-users?projectId=${projectId}`)
    if (response.data.success) {
      projectFilteredUsers.value = response.data.data 
    }
  } catch (error) { console.error(error) }
}

const openAddTaskDialog = () => {
  taskForm.id = null; taskForm.projectId = null; taskForm.title = ''; taskForm.description = ''; taskForm.assigneeId = null; taskForm.status = '未开始'; taskForm.priority = '中'; taskForm.dueDate = ''
  projectFilteredUsers.value = [] 
  taskDialogVisible.value = true
}

const openEditTaskDialog = async (row) => {
  await handleTaskProjectChange(row.projectId)
  taskForm.id = row.id; taskForm.projectId = row.projectId; taskForm.title = row.title; taskForm.description = row.description; taskForm.assigneeId = row.assigneeId; taskForm.status = row.status; taskForm.priority = row.priority; taskForm.dueDate = row.dueDate
  taskDialogVisible.value = true
}

const handleSaveTask = async () => {
  if (!taskForm.title || !taskForm.projectId) { ElMessage.warning('项目和任务标题必填！'); return }
  try {
    const response = await axios.post(`/api/tasks?currentUserId=${currentUser.value.id}`, taskForm)
    if (response.data.success) {
      ElMessage.success(response.data.message); taskDialogVisible.value = false; fetchTasks()
    }
  } catch (error) { console.error(error) }
}

const handleDeleteTask = (id) => {
  ElMessageBox.confirm('确定删除任务吗？', '提示').then(async () => {
    try {
      const response = await axios.delete(`/api/tasks/${id}?currentUserId=${currentUser.value.id}`)
      if (response.data.success) { ElMessage.success(response.data.message); fetchTasks() }
    } catch (error) { console.error(error) }
  }).catch(() => {})
}

// ==========================================
// 📈 【进度日志】
// ==========================================
const fetchLogs = async () => {
  try {
    const response = await axios.get(`/api/task-logs?currentUserId=${currentUser.value.id}`)
    if (response.data.success) { logList.value = response.data.data }
  } catch (error) { console.error(error) }
}

const openAddLogDialog = () => {
  logForm.taskId = null; logForm.progressPercent = 0; logForm.content = ''
  logDialogVisible.value = true
}

const handleSaveLog = async () => {
  if (!logForm.taskId || !logForm.content) { ElMessage.warning('请选择任务并撰写日志！'); return }
  try {
    const response = await axios.post(`/api/task-logs?currentUserId=${currentUser.value.id}`, logForm)
    if (response.data.success) {
      ElMessage.success(response.data.message); logDialogVisible.value = false; fetchLogs()
    }
  } catch (error) { console.error(error) }
}

// ==========================================
// 📝 【总结中心】
// ==========================================
const fetchSummaries = async () => {
  try {
    const response = await axios.get(`/api/summaries?currentUserId=${currentUser.value.id}`)
    if (response.data.success) { summaryList.value = response.data.data }
  } catch (error) { console.error(error) }
}

const openAddSummaryDialog = () => {
  summaryForm.projectId = null; summaryForm.summaryType = '阶段总结'; summaryForm.content = ''
  summaryDialogVisible.value = true
}

const handleSaveSummary = async () => {
  if (!summaryForm.projectId || !summaryForm.content) { ElMessage.warning('请选择项目并撰写总结内容！'); return }
  try {
    const response = await axios.post(`/api/summaries?currentUserId=${currentUser.value.id}`, summaryForm)
    if (response.data.success) {
      ElMessage.success(response.data.message); summaryDialogVisible.value = false; fetchSummaries()
    }
  } catch (error) { console.error(error) }
}

// ==========================================
// 🤖 【AI智能拆解】
// ==========================================
const handleAiProjectChange = async (projectId) => {
  projectFilteredUsers.value = []
  if (!projectId) return
  try {
    const response = await axios.get(`/api/groups/project-users?projectId=${projectId}`)
    if (response.data.success) {
      projectFilteredUsers.value = response.data.data 
    }
  } catch (error) { console.error(error) }
}

const handleGenerateAiPlan = async () => {
  if (!aiForm.projectId || !aiForm.goal) { ElMessage.warning('请选择项目和输入目标！'); return }
  const selectedProj = projectList.value.find(p => p.id === aiForm.projectId)
  const projectName = selectedProj ? selectedProj.name : ''
  
  await handleAiProjectChange(aiForm.projectId)

  aiLoading.value = true; aiPlanResult.value = null
  try {
    const response = await axios.post(`/api/ai/task-plan?currentUserId=${currentUser.value.id}`, {
      projectId: aiForm.projectId, projectName: projectName, goal: aiForm.goal, description: aiForm.description
    })
    if (response.data.success) { 
      aiPlanResult.value = response.data.data
      ElMessage.success('AI拆解完成，推荐执行人已限制为本组人员！') 
    }
  } catch (error) { console.error(error) } finally { aiLoading.value = false }
}

const handleSelectionChange = (selection) => { selectedAiItems.value = selection }

const handleImportAiTasks = async () => {
  if (selectedAiItems.value.length === 0) { ElMessage.warning('请先勾选任务！'); return }
  const hasUnassigned = selectedAiItems.value.some(item => !item.assigneeId)
  if (hasUnassigned) { ElMessage.warning('选中的任务必须都有本组执行人才能导入！'); return }
  try {
    const response = await axios.post(`/api/ai/task-plan/import?currentUserId=${currentUser.value.id}`, {
      projectId: aiForm.projectId, items: selectedAiItems.value
    })
    if (response.data.success) {
      ElMessageBox.alert(response.data.message, '导入成功', {
        confirmButtonText: '确定',
        callback: () => {
          aiForm.goal = ''; aiForm.description = ''; aiPlanResult.value = null
          currentPanel.value = 'task'; fetchTasks()
        }
      })
    }
  } catch (error) { console.error(error) }
}
</script>

<style scoped>
.home-container { height: 100vh; color: #172033; background: radial-gradient(circle at 12% 8%, rgba(35,103,240,.16), transparent 30%), radial-gradient(circle at 88% 0%, rgba(24,164,107,.14), transparent 28%), linear-gradient(135deg, #f8fbff 0%, #edf3fa 46%, #e8eef7 100%); overflow: hidden; }
button { font: inherit; }
.aside-menu { position: relative; color: #eef5ff; display: flex; flex-direction: column; background: linear-gradient(180deg, #18263d 0%, #101827 100%); box-shadow: 16px 0 44px rgba(22,34,56,.16); overflow: hidden; }
.aside-menu::before { content: ""; position: absolute; width: 220px; height: 220px; left: -96px; bottom: 132px; border-radius: 50%; background: rgba(35,103,240,.28); filter: blur(30px); animation: sidebarGlow 6s ease-in-out infinite; }
.brand-block { position: relative; z-index: 1; display: flex; align-items: center; gap: 13px; padding: 24px 20px 18px; }
.brand-mark { display: grid; place-items: center; width: 44px; height: 44px; border-radius: 15px; background: linear-gradient(135deg, #3b82ff, #19bf8a); box-shadow: 0 14px 28px rgba(35,103,240,.32); font-weight: 900; }
.system-logo { font-size: 20px; font-weight: 900; letter-spacing: .2px; }
.system-subtitle { margin-top: 3px; color: rgba(238,245,255,.58); font-size: 12px; }
.el-menu-vertical { position: relative; z-index: 1; flex: 1; border-right: 0; padding: 8px 12px; background: transparent !important; }
:deep(.el-menu-item-group__title) { padding: 18px 12px 7px !important; color: rgba(238,245,255,.42) !important; font-size: 12px; letter-spacing: .08em; }
:deep(.el-menu-item) { height: 48px; margin: 5px 0; border-radius: 15px; color: rgba(238,245,255,.76) !important; background: transparent !important; transition: transform .22s ease, background .22s ease, color .22s ease; }
:deep(.el-menu-item:hover), :deep(.el-menu-item.is-active) { color: #fff !important; background: rgba(255,255,255,.11) !important; transform: translateX(5px); }
:deep(.el-menu-item.is-active) { box-shadow: inset 3px 0 0 #54a3ff; }
:deep(.el-menu-item .el-icon) { width: 28px; height: 28px; margin-right: 10px; border-radius: 10px; background: rgba(255,255,255,.09); }
.aside-insight { position: relative; z-index: 1; margin: 18px 16px 20px; padding: 16px; border: 1px solid rgba(255,255,255,.12); border-radius: 18px; background: rgba(255,255,255,.08); }
.aside-insight span { color: rgba(238,245,255,.52); font-size: 12px; }
.aside-insight strong { display: block; margin: 8px 0; }
.aside-insight p { margin: 0; color: rgba(238,245,255,.62); font-size: 13px; line-height: 1.6; }
.workspace-main { min-width: 0; }
.top-header { height: 76px; padding: 0 28px; display: grid; grid-template-columns: minmax(220px, 1fr) auto auto; align-items: center; gap: 18px; border-bottom: 1px solid rgba(28,42,68,.1); background: rgba(255,255,255,.62); backdrop-filter: blur(18px); }
.header-left { display: grid; gap: 4px; }
.header-left strong { font-size: 19px; }
.eyebrow { color: #2367f0; font-size: 12px; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; }
.quick-tabs { display: flex; gap: 7px; padding: 5px; border-radius: 16px; background: rgba(28,42,68,.06); }
.quick-tabs button { border: 0; padding: 9px 14px; border-radius: 12px; color: #6f7d91; background: transparent; cursor: pointer; transition: all .2s ease; }
.quick-tabs button.active, .quick-tabs button:hover { color: #2367f0; background: #fff; box-shadow: 0 10px 20px rgba(22,34,56,.08); }
.header-right { display: flex; align-items: center; gap: 10px; white-space: nowrap; }
.user-name { font-weight: 800; color: #25324a; }
.main-content { position: relative; padding: 26px; overflow: auto; background-image: linear-gradient(rgba(28,42,68,.035) 1px, transparent 1px), linear-gradient(90deg, rgba(28,42,68,.035) 1px, transparent 1px); background-size: 44px 44px; }
.screen { animation: screenIn .36s ease both; }
.screen-title { display: flex; align-items: end; justify-content: space-between; gap: 18px; margin-bottom: 18px; }
.screen-title h1 { margin: 4px 0 7px; font-size: 30px; }
.screen-title p { margin: 0; color: #6f7d91; }
.overview-hero { position: relative; display: grid; grid-template-columns: minmax(0, 1fr) 280px; gap: 24px; min-height: 268px; padding: 30px; border-radius: 30px; color: #fff; overflow: hidden; background: radial-gradient(circle at 78% 24%, rgba(24,164,107,.55), transparent 32%), linear-gradient(135deg, #17243b, #245fce 68%, #1ba777); box-shadow: 0 24px 60px rgba(31,72,147,.24); }
.overview-hero::after { content: ""; position: absolute; inset: auto -70px -90px auto; width: 240px; height: 240px; border: 38px solid rgba(255,255,255,.08); border-radius: 50%; animation: ringFloat 8s ease-in-out infinite; }
.hero-copy { position: relative; z-index: 1; max-width: 760px; }
.hero-copy .eyebrow { color: rgba(255,255,255,.72); }
.hero-copy h1 { margin: 10px 0 14px; font-size: clamp(34px, 5vw, 58px); line-height: 1.02; }
.hero-copy p { max-width: 680px; margin: 0; color: rgba(255,255,255,.72); line-height: 1.8; }
.orbit-map { position: relative; z-index: 1; min-height: 220px; }
.orbit-ring { position: absolute; inset: 18px; border: 2px dashed rgba(255,255,255,.22); border-radius: 44%; animation: spinSlow 18s linear infinite; }
.orbit-node { position: absolute; display: grid; place-items: center; width: 70px; height: 70px; border-radius: 24px; color: #17304a; background: rgba(255,255,255,.9); box-shadow: 0 18px 34px rgba(0,0,0,.18); font-weight: 900; animation: floatNode 5s ease-in-out infinite; }
.node-project { left: 8%; top: 20%; }
.node-task { right: 8%; top: 8%; animation-delay: -1.4s; }
.node-user { left: 36%; bottom: 6%; animation-delay: -2.6s; }
.metric-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; margin: 20px 0; }
.metric-card { position: relative; overflow: hidden; display: grid; gap: 8px; min-height: 150px; padding: 20px; border: 1px solid rgba(255,255,255,.72); border-radius: 24px; text-align: left; background: rgba(255,255,255,.82); box-shadow: 0 16px 38px rgba(22,34,56,.09); cursor: pointer; transition: transform .22s ease, box-shadow .22s ease; }
.metric-card:hover { transform: translateY(-6px); box-shadow: 0 24px 52px rgba(22,34,56,.14); }
.metric-card::after { content: ""; position: absolute; right: -30px; bottom: -44px; width: 120px; height: 120px; border-radius: 50%; background: var(--tone); }
.metric-card span { color: #6f7d91; font-weight: 700; }
.metric-card strong { font-size: 38px; }
.metric-card em { color: #6f7d91; font-style: normal; font-size: 13px; }
.tone-blue { --tone: rgba(35,103,240,.13); }
.tone-green { --tone: rgba(24,164,107,.15); }
.tone-amber { --tone: rgba(216,138,25,.16); }
.tone-purple { --tone: rgba(123,97,255,.14); }
.overview-grid { display: grid; grid-template-columns: minmax(0, 1.55fr) minmax(280px, .8fr); gap: 18px; }
.surface { position: relative; padding: 20px; border: 1px solid rgba(255,255,255,.76); border-radius: 24px; background: rgba(255,255,255,.84); box-shadow: 0 18px 46px rgba(22,34,56,.1); backdrop-filter: blur(16px); }
.surface-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.surface-head h2 { margin: 4px 0 0; }
.flow-list { display: grid; gap: 10px; }
.flow-row { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 13px; padding: 14px; border-radius: 17px; background: rgba(248,250,253,.82); transition: transform .2s ease, background .2s ease; }
.flow-row:hover { transform: translateX(5px); background: #fff; }
.flow-dot { width: 14px; height: 14px; border: 3px solid #2367f0; border-radius: 50%; box-shadow: 0 0 0 6px rgba(35,103,240,.12); }
.flow-row p { margin: 5px 0 0; color: #6f7d91; font-size: 13px; }
.pulse-stack { display: grid; gap: 12px; }
.pulse-card { display: flex; align-items: center; justify-content: space-between; padding: 18px; border: 0; border-radius: 18px; background: linear-gradient(135deg, rgba(35,103,240,.1), rgba(24,164,107,.1)); cursor: pointer; transition: transform .2s ease; }
.pulse-card:hover { transform: scale(1.02); }
.pulse-card span { color: #6f7d91; font-weight: 800; }
.pulse-card strong { font-size: 30px; }
.ai-compose { display: grid; grid-template-columns: minmax(280px, .8fr) minmax(0, 1.2fr); gap: 20px; align-items: stretch; }
.ai-copy { padding: 30px; border-radius: 30px; color: #fff; background: radial-gradient(circle at 82% 10%, rgba(255,255,255,.18), transparent 28%), linear-gradient(145deg, #20243f, #4733a3); box-shadow: 0 24px 58px rgba(71,51,163,.22); }
.ai-copy .eyebrow { color: rgba(255,255,255,.72); }
.ai-copy h1 { margin: 10px 0 14px; font-size: clamp(32px, 5vw, 54px); line-height: 1.05; }
.ai-copy p { margin: 0; color: rgba(255,255,255,.72); line-height: 1.8; }
.ai-result-area { margin-top: 18px; }
.table-actions { text-align: right; }
.timeline-surface { max-width: 840px; }
.timeline-progress { margin: 10px 0; color: #2367f0; font-weight: 800; }
.timeline-copy { color: #5f6e82; }
.profile-surface { max-width: 760px; }
:deep(.el-card), :deep(.el-table) { border-radius: 18px; overflow: hidden; box-shadow: none; }
:deep(.el-table) { --el-table-header-bg-color: rgba(35,103,240,.06); --el-table-row-hover-bg-color: rgba(35,103,240,.055); }
:deep(.el-table th.el-table__cell) { color: #34435a; font-weight: 900; }
:deep(.el-table .el-table__row) { transition: transform .18s ease; }
:deep(.el-table .el-table__row:hover) { transform: translateX(3px); }
:deep(.el-button) { border-radius: 13px; font-weight: 800; transition: transform .18s ease, box-shadow .18s ease, filter .18s ease; }
:deep(.el-button:hover) { transform: translateY(-2px); filter: saturate(1.05); }
:deep(.el-button--primary) { border: 0; background: linear-gradient(135deg, #2367f0, #18a46b); box-shadow: 0 12px 24px rgba(35,103,240,.18); }
:deep(.el-button--success) { border: 0; background: linear-gradient(135deg, #18a46b, #31c995); }
:deep(.el-button--warning) { border: 0; background: linear-gradient(135deg, #d88a19, #f0b24b); }
:deep(.el-tag) { border-radius: 999px; font-weight: 800; }
:deep(.el-input__wrapper), :deep(.el-textarea__inner), :deep(.el-select__wrapper) { border-radius: 13px; transition: transform .2s ease, box-shadow .2s ease; }
:deep(.el-input__wrapper:hover), :deep(.el-textarea__inner:hover), :deep(.el-select__wrapper:hover) { transform: translateY(-1px); }
:deep(.el-dialog) { border-radius: 24px; overflow: hidden; box-shadow: 0 30px 80px rgba(22,34,56,.24); }
:deep(.el-dialog__header) { margin: 0; padding: 20px 24px; background: linear-gradient(135deg, rgba(35,103,240,.08), rgba(24,164,107,.08)); }
:deep(.el-dialog__body) { padding: 24px; }
@keyframes sidebarGlow { 50% { transform: scale(1.18); opacity: .72; } }
@keyframes screenIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
@keyframes ringFloat { 50% { transform: translate(-14px, 16px) scale(1.05); } }
@keyframes spinSlow { to { transform: rotate(360deg); } }
@keyframes floatNode { 50% { transform: translateY(-13px); } }
@media (max-width: 1180px) { .overview-hero, .overview-grid, .ai-compose { grid-template-columns: 1fr; } .orbit-map { display: none; } .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 860px) { .aside-menu { width: 76px !important; } .brand-block { justify-content: center; padding-inline: 0; } .brand-block > div:last-child, .aside-insight, :deep(.el-menu-item span), :deep(.el-menu-item-group__title) { display: none; } :deep(.el-menu-item) { justify-content: center; padding: 0 !important; } :deep(.el-menu-item .el-icon) { margin-right: 0; } .top-header { height: auto; grid-template-columns: 1fr; padding: 16px; } .header-right { flex-wrap: wrap; } .main-content { padding: 16px; } .screen-title { align-items: flex-start; flex-direction: column; } }
@media (max-width: 560px) { .home-container { display: block; overflow: auto; } .aside-menu { display: none; } .metric-grid { grid-template-columns: 1fr; } .overview-hero, .surface { border-radius: 20px; padding: 18px; } .flow-row { grid-template-columns: auto 1fr; } .flow-row .el-tag { grid-column: 2; justify-self: start; } }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { animation-duration: .01ms !important; animation-iteration-count: 1 !important; transition-duration: .01ms !important; } }


/* Top navigation layout: all modules live in the header, no sidebar needed. */
.full-workspace {
  width: 100%;
  min-width: 0;
}

.top-header {
  height: auto;
  min-height: 118px;
  padding: 16px 28px 18px;
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
  align-items: stretch;
  border-bottom: 1px solid rgba(28, 42, 68, 0.1);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(18px);
}

.topbar-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.brand-strip {
  display: flex;
  align-items: center;
  gap: 13px;
}

.brand-strip strong {
  display: block;
  color: #172033;
  font-size: 21px;
  font-weight: 900;
}

.brand-strip span {
  display: block;
  margin-top: 3px;
  color: #6f7d91;
  font-size: 13px;
}

.module-nav {
  display: grid;
  grid-template-columns: repeat(9, minmax(0, 1fr));
  gap: 9px;
  padding: 7px;
  border: 1px solid rgba(28, 42, 68, 0.08);
  border-radius: 22px;
  background: rgba(28, 42, 68, 0.055);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.62);
}

.module-nav button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 46px;
  border: 0;
  border-radius: 16px;
  color: #5f6e82;
  background: transparent;
  cursor: pointer;
  font-weight: 800;
  white-space: nowrap;
  transition: transform 0.2s ease, background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.module-nav button:hover {
  transform: translateY(-2px);
  color: #2367f0;
  background: rgba(255, 255, 255, 0.72);
}

.module-nav button.active {
  color: #fff;
  background: linear-gradient(135deg, #2367f0, #18a46b);
  box-shadow: 0 14px 26px rgba(35, 103, 240, 0.2);
}

.module-nav .el-icon {
  font-size: 17px;
}

.main-content {
  padding: 24px 30px 30px;
}

@media (max-width: 1180px) {
  .module-nav {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .topbar-main {
    align-items: flex-start;
    flex-direction: column;
  }

  .module-nav {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .top-header {
    padding: 14px;
  }

  .module-nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .module-nav button {
    min-height: 42px;
  }
}



/* Compact, non-redundant navigation pass. */
.home-container {
  background:
    radial-gradient(circle at 16% 0%, rgba(42, 117, 255, 0.1), transparent 30%),
    linear-gradient(135deg, #f7f9fc 0%, #edf2f7 100%);
}

.top-header {
  min-height: 58px;
  padding: 10px 18px;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
}

.topbar-main {
  display: contents;
}

.brand-strip {
  min-width: 180px;
  grid-column: 1;
  grid-row: 1;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #2457c5, #1f9d80);
  box-shadow: 0 8px 18px rgba(36, 87, 197, 0.18);
}

.brand-strip strong {
  font-size: 17px;
}

.brand-strip span {
  margin-top: 1px;
  font-size: 12px;
}

.module-nav {
  display: flex;
  justify-content: center;
  grid-column: 2;
  grid-row: 1;
  gap: 4px;
  min-width: 0;
  padding: 4px;
  border-radius: 14px;
  background: rgba(30, 43, 64, 0.055);
  box-shadow: none;
}

.module-nav button {
  min-height: 34px;
  padding: 0 10px;
  border-radius: 10px;
  gap: 5px;
  color: #526174;
  font-size: 13px;
}

.module-nav button:hover {
  transform: translateY(-1px);
  color: #2457c5;
  background: rgba(255, 255, 255, 0.82);
}

.module-nav button.active {
  color: #ffffff;
  background: #2457c5;
  box-shadow: 0 8px 18px rgba(36, 87, 197, 0.18);
}

.module-nav .el-icon {
  font-size: 15px;
}

.header-right {
  grid-column: 3;
  grid-row: 1;
  justify-content: flex-end;
  justify-self: end;
}

.main-content {
  padding: 18px 22px 24px;
}

.overview-hero {
  min-height: 190px;
  padding: 24px 26px;
  border-radius: 22px;
  background:
    radial-gradient(circle at 86% 20%, rgba(31, 157, 128, 0.26), transparent 30%),
    linear-gradient(135deg, #1f2b3d, #2457c5);
  box-shadow: 0 18px 42px rgba(31, 43, 61, 0.16);
}

.hero-copy h1 {
  max-width: 820px;
  margin: 8px 0 10px;
  font-size: clamp(28px, 3.2vw, 44px);
  line-height: 1.08;
}

.hero-copy p {
  max-width: 760px;
  line-height: 1.65;
}

.orbit-map {
  min-height: 150px;
}

.orbit-node {
  width: 54px;
  height: 54px;
  border-radius: 16px;
}

.metric-grid {
  gap: 12px;
  margin: 14px 0;
}

.metric-card {
  min-height: 108px;
  padding: 16px;
  border-radius: 18px;
  cursor: default;
  box-shadow: 0 10px 24px rgba(31, 43, 61, 0.07);
}

.metric-card:hover {
  transform: none;
  box-shadow: 0 10px 24px rgba(31, 43, 61, 0.07);
}

.metric-card strong {
  font-size: 30px;
}

.metric-card::after {
  width: 86px;
  height: 86px;
  right: -24px;
  bottom: -34px;
}

.surface {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 12px 30px rgba(31, 43, 61, 0.08);
}

.pulse-card {
  cursor: default;
}

.pulse-card:hover {
  transform: none;
}

:deep(.el-button--primary) {
  background: #2457c5;
  box-shadow: 0 8px 18px rgba(36, 87, 197, 0.16);
}

:deep(.el-button--success) {
  background: #1f9d80;
}

:deep(.el-button--warning) {
  background: #b7791f;
}

@media (max-width: 1080px) {
  .top-header {
    grid-template-columns: 1fr;
  }

  .topbar-main {
    display: flex;
  }

  .module-nav {
    justify-content: flex-start;
    overflow-x: auto;
  }

  .module-nav button {
    flex: 0 0 auto;
  }
}

@media (max-width: 560px) {
  .top-header {
    padding: 10px;
  }

  .brand-strip span,
  .module-nav .el-icon {
    display: none;
  }
}

/* Final mica pass: match the login page and keep only the content area scrollable. */
.home-container {
  position: relative;
  height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at 12% 4%, rgba(255, 214, 234, 0.32), transparent 30%),
    radial-gradient(circle at 72% 0%, rgba(191, 228, 255, 0.34), transparent 28%),
    radial-gradient(circle at 96% 22%, rgba(255, 241, 184, 0.25), transparent 24%),
    linear-gradient(135deg, #fff8fb 0%, #f4f7ff 52%, #fffaf0 100%);
}

.home-container::before {
  content: "";
  position: absolute;
  inset: -18%;
  pointer-events: none;
  background:
    radial-gradient(circle at 24% 22%, rgba(255, 214, 234, 0.34), transparent 28%),
    radial-gradient(circle at 72% 18%, rgba(191, 228, 255, 0.34), transparent 26%),
    radial-gradient(circle at 78% 76%, rgba(255, 241, 184, 0.28), transparent 24%),
    radial-gradient(circle at 36% 82%, rgba(220, 203, 255, 0.3), transparent 26%);
  filter: blur(18px) saturate(1.15);
  animation: pageMicaFloat 12s ease-in-out infinite alternate;
}

.workspace-main,
.full-workspace {
  position: relative;
  z-index: 1;
  height: 100vh;
  min-height: 0;
  overflow: hidden;
}

@keyframes pageMicaFloat {
  from {
    transform: translate3d(-1.4%, -0.8%, 0) scale(1.02);
  }
  to {
    transform: translate3d(1.4%, 0.8%, 0) scale(1.04);
  }
}

.top-header {
  flex: 0 0 auto;
  min-height: 52px;
  padding: 8px 16px;
  grid-template-columns: auto minmax(0, 1fr) auto;
  overflow: visible;
  background: rgba(255, 255, 255, 0.62);
  border-bottom: 1px solid rgba(132, 118, 170, 0.14);
  box-shadow: 0 8px 24px rgba(129, 117, 160, 0.1);
}

.brand-mark {
  background: linear-gradient(135deg, #9fbfff, #d8c7ff 52%, #ffd0e4);
  box-shadow: 0 8px 18px rgba(159, 140, 255, 0.2);
}

.module-nav {
  justify-self: center;
  width: max-content;
  max-width: 100%;
  overflow: visible;
  border: 1px solid rgba(255, 255, 255, 0.68);
  background: rgba(255, 255, 255, 0.42);
  box-shadow: 0 8px 20px rgba(129, 117, 160, 0.09), inset 0 1px 0 rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(14px) saturate(145%);
}

.module-nav button {
  min-height: 32px;
  color: #6a6380;
}

.module-nav button:hover {
  color: #7a6ee6;
  background: rgba(255, 255, 255, 0.72);
}

.module-nav button.active {
  color: #ffffff;
  background: linear-gradient(135deg, #8ebcff, #a995ff 55%, #ffb8d7);
  box-shadow: 0 10px 20px rgba(159, 140, 255, 0.22);
}

.header-right {
  justify-self: end;
  margin-left: auto;
}

.main-content {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 22px 24px;
  background-image:
    linear-gradient(rgba(132, 118, 170, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(132, 118, 170, 0.035) 1px, transparent 1px);
}

.overview-hero {
  min-height: 176px;
  color: #28324a;
  background:
    radial-gradient(circle at 20% 20%, rgba(255, 214, 234, 0.72), transparent 34%),
    radial-gradient(circle at 72% 22%, rgba(191, 228, 255, 0.74), transparent 34%),
    radial-gradient(circle at 82% 82%, rgba(255, 241, 184, 0.58), transparent 34%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.72), rgba(245, 239, 255, 0.68));
  border: 1px solid rgba(255, 255, 255, 0.72);
  box-shadow: 0 18px 42px rgba(129, 117, 160, 0.14);
}

.hero-copy .eyebrow {
  color: #7a6ee6;
}

.hero-copy p {
  color: #677087;
}

.orbit-ring {
  border-color: rgba(122, 110, 230, 0.2);
}

.orbit-node {
  color: #4c4665;
  background: rgba(255, 255, 255, 0.78);
}

.metric-card,
.surface {
  border-color: rgba(255, 255, 255, 0.76);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 12px 30px rgba(129, 117, 160, 0.1);
  backdrop-filter: blur(14px) saturate(135%);
}

.tone-blue { --tone: rgba(191, 228, 255, 0.42); }
.tone-green { --tone: rgba(255, 214, 234, 0.36); }
.tone-amber { --tone: rgba(255, 241, 184, 0.42); }
.tone-purple { --tone: rgba(220, 203, 255, 0.42); }

.flow-dot {
  border-color: #a995ff;
  box-shadow: 0 0 0 6px rgba(169, 149, 255, 0.14);
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #8ebcff, #a995ff 55%, #ffb8d7);
  box-shadow: 0 10px 22px rgba(159, 140, 255, 0.2);
}

:deep(.el-button--success) {
  background: linear-gradient(135deg, #a995ff, #ffb8d7);
}

:deep(.el-button--warning) {
  background: linear-gradient(135deg, #f5ca74, #ffb8d7);
}

@media (max-width: 1080px) {
  .module-nav {
    justify-self: stretch;
    width: auto;
    overflow-x: auto;
    overflow-y: hidden;
    scrollbar-width: none;
  }

  .module-nav::-webkit-scrollbar {
    display: none;
  }
}

@media (max-width: 560px) {
  .top-header {
    overflow: hidden;
  }
}

/* Keep the app shell fixed; only the main work surface scrolls. */
.home-container,
.workspace-main,
.full-workspace {
  width: 100%;
  max-width: 100%;
  min-height: 0;
}

.top-header {
  height: 52px !important;
  max-height: 52px;
  overflow: hidden;
}

.topbar-main,
.brand-strip,
.header-right {
  min-height: 0;
  max-height: 36px;
  overflow: hidden;
}

.module-nav {
  max-height: 40px;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
}

.module-nav::-webkit-scrollbar {
  display: none;
}

.main-content {
  height: 0;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.overview-hero {
  isolation: isolate;
  grid-template-columns: minmax(0, 1fr) 220px;
  min-height: 164px;
  padding: 22px 26px;
  background:
    radial-gradient(ellipse at 18% 18%, rgba(255, 207, 229, 0.88), transparent 42%),
    radial-gradient(ellipse at 70% 16%, rgba(184, 224, 255, 0.9), transparent 43%),
    radial-gradient(ellipse at 82% 88%, rgba(255, 238, 167, 0.78), transparent 43%),
    linear-gradient(135deg, #fff8fd, #f2f6ff 52%, #fff9e9);
}

.overview-hero::before,
.overview-hero::after {
  content: "";
  position: absolute;
  z-index: -1;
  pointer-events: none;
  border: 0;
  border-radius: 45% 55% 62% 38% / 52% 42% 58% 48%;
  filter: blur(28px) saturate(1.22);
}

.overview-hero::before {
  inset: -55% 28% -65% -12%;
  background: rgba(220, 195, 255, 0.6);
  animation: heroSmokeA 10s ease-in-out infinite alternate;
}

.overview-hero::after {
  inset: -70% -8% -54% 52%;
  width: auto;
  height: auto;
  background: rgba(181, 231, 255, 0.62);
  animation: heroSmokeB 13s ease-in-out infinite alternate;
}

.hero-copy {
  min-width: 0;
}

.hero-copy h1 {
  max-width: none;
  margin: 7px 0 9px;
  white-space: nowrap;
  font-size: 32px;
  line-height: 1.18;
  letter-spacing: 0;
}

.orbit-map {
  position: relative;
  z-index: 1;
}

@keyframes heroSmokeA {
  0% { transform: translate3d(-4%, -5%, 0) rotate(-7deg) scale(0.92, 1.08); opacity: 0.5; }
  48% { transform: translate3d(9%, 7%, 0) rotate(8deg) scale(1.18, 0.9); opacity: 0.78; }
  100% { transform: translate3d(17%, -2%, 0) rotate(-2deg) scale(1.04, 1.2); opacity: 0.6; }
}

@keyframes heroSmokeB {
  0% { transform: translate3d(5%, 7%, 0) rotate(6deg) scale(1.12, 0.88); opacity: 0.42; }
  55% { transform: translate3d(-13%, -8%, 0) rotate(-9deg) scale(0.9, 1.2); opacity: 0.72; }
  100% { transform: translate3d(-4%, 5%, 0) rotate(3deg) scale(1.2, 1.02); opacity: 0.54; }
}

@media (max-width: 1180px) {
  .overview-hero {
    grid-template-columns: minmax(0, 1fr);
  }

  .hero-copy h1 {
    font-size: 29px;
  }
}

@media (max-width: 760px) {
  .top-header {
    height: auto !important;
    max-height: 96px;
  }

  .hero-copy h1 {
    white-space: normal;
    font-size: 26px;
  }
}

/* Floating previews for overview statistic cards. */
.metric-grid,
.pulse-stack {
  overflow: visible;
}

.metric-card,
.pulse-card {
  overflow: visible !important;
  isolation: isolate;
}

.metric-card:hover,
.pulse-card:hover {
  z-index: 12;
}

.metric-card {
  cursor: default;
  transition: transform 0.28s cubic-bezier(.2, .8, .2, 1), box-shadow 0.28s ease, border-color 0.28s ease;
}

.metric-card:hover {
  transform: translateY(-5px);
  border-color: rgba(255, 255, 255, 0.95);
  box-shadow: 0 22px 48px rgba(151, 133, 178, 0.2);
}

.metric-preview {
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: calc(100% - 10px);
  z-index: 20;
  display: grid;
  gap: 6px;
  min-height: 104px;
  padding: 14px 15px;
  border: 1px solid rgba(255, 255, 255, 0.78);
  border-radius: 18px;
  color: #4a4560;
  background:
    radial-gradient(120px 76px at 15% 20%, rgba(255, 214, 234, 0.72), transparent 72%),
    radial-gradient(132px 84px at 86% 16%, rgba(191, 228, 255, 0.7), transparent 74%),
    radial-gradient(140px 92px at 48% 95%, rgba(255, 241, 184, 0.6), transparent 72%),
    rgba(255, 255, 255, 0.74);
  box-shadow: 0 20px 46px rgba(120, 106, 148, 0.22);
  backdrop-filter: blur(20px) saturate(150%);
  pointer-events: none;
  opacity: 0;
  transform: translateY(12px) scale(0.94);
  transform-origin: bottom center;
  transition: opacity 0.22s ease, transform 0.34s cubic-bezier(.18, .88, .32, 1.16);
}

.metric-preview::before {
  content: "";
  position: absolute;
  inset: -18px;
  z-index: -1;
  border-radius: 28px 34px 26px 38px;
  background:
    radial-gradient(closest-side at 26% 30%, rgba(255, 214, 234, 0.42), transparent 74%),
    radial-gradient(closest-side at 78% 44%, rgba(220, 203, 255, 0.42), transparent 76%),
    radial-gradient(closest-side at 45% 78%, rgba(191, 228, 255, 0.36), transparent 78%);
  filter: blur(14px);
  opacity: 0;
  animation: previewSmoke 6.2s ease-in-out infinite alternate;
  transition: opacity 0.22s ease;
}

.metric-card:hover .metric-preview,
.pulse-card:hover .metric-preview {
  opacity: 1;
  transform: translateY(-7px) scale(1);
}

.metric-card:hover .metric-preview::before,
.pulse-card:hover .metric-preview::before {
  opacity: 1;
}

.metric-preview b {
  font-size: 14px;
  line-height: 1.15;
  color: #39324f;
}

.metric-preview p,
.metric-preview small {
  position: relative;
  z-index: 1;
  margin: 0;
}

.metric-preview p {
  font-size: 13px;
  font-weight: 800;
  color: #5b5470;
}

.metric-preview small {
  display: -webkit-box;
  overflow: hidden;
  color: #7d748f;
  font-size: 12px;
  line-height: 1.45;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.pulse-card {
  position: relative;
  transition: transform 0.28s cubic-bezier(.2, .8, .2, 1), box-shadow 0.28s ease;
}

.pulse-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 18px 40px rgba(151, 133, 178, 0.16);
}

.pulse-preview {
  left: 10px;
  right: 10px;
  bottom: calc(100% - 6px);
  min-height: 92px;
}

@keyframes previewSmoke {
  0% { transform: translate3d(-4%, 4%, 0) rotate(-5deg) scale(0.96, 1.06); }
  48% { transform: translate3d(6%, -3%, 0) rotate(4deg) scale(1.12, 0.92); }
  100% { transform: translate3d(-1%, -6%, 0) rotate(-2deg) scale(1.02, 1.14); }
}

@media (max-width: 760px) {
  .metric-preview,
  .pulse-preview {
    left: 10px;
    right: 10px;
    bottom: auto;
    top: 10px;
    transform-origin: top center;
  }

  .metric-card:hover .metric-preview,
  .pulse-card:hover .metric-preview {
    transform: translateY(0) scale(1);
  }
}

</style>
