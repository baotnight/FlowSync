<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <div style="display:flex;align-items:center;gap:12px">
        <h2>任务管理</h2>
        <el-button v-if="(isLeader || isAdmin) && selectedIds.length > 0"
                   type="danger" @click="handleBatchDelete" :loading="batchDeleting">
          批量删除（{{ selectedIds.length }}）
        </el-button>
      </div>
      <div style="display:flex;gap:12px">
        <el-select v-model="filterProjectId" placeholder="筛选项目" clearable style="width:200px" @change="fetchTasks">
          <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-button v-if="isLeader || isAdmin" type="primary" @click="openDialog(null)">新建任务</el-button>
      </div>
    </div>

    <!-- 搜索筛选栏 -->
    <div style="display:flex;gap:12px;margin-bottom:16px;align-items:center;flex-wrap:wrap">
      <el-input v-model="searchKeyword" placeholder="搜索任务..." size="default" style="width:240px" clearable @clear="searchKeyword=''" @input="applyFilters">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="filterPriority" placeholder="优先级筛选" size="default" style="width:130px" clearable @change="applyFilters">
        <el-option label="高" value="高" /><el-option label="中" value="中" /><el-option label="低" value="低" />
      </el-select>
      <el-select v-model="filterAssignee" placeholder="负责人筛选" size="default" style="width:150px" clearable @change="applyFilters">
        <el-option v-for="u in users" :key="u.id" :label="u.realName" :value="u.id" />
      </el-select>
      <el-button-group style="margin-left:auto">
        <el-button :type="viewMode === 'table' ? 'primary' : ''" size="default" @click="viewMode = 'table'">表格</el-button>
        <el-button :type="viewMode === 'kanban' ? 'primary' : ''" size="default" @click="viewMode = 'kanban'">看板</el-button>
      </el-button-group>
    </div>

    <el-table v-if="viewMode === 'table'" :data="tasks" border stripe v-loading="loading" row-key="id"
              @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="任务标题" />
      <el-table-column prop="projectName" label="所属项目" width="120" />
      <el-table-column prop="description" label="任务说明" show-overflow-tooltip />
      <el-table-column prop="assigneeName" label="负责人" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="子任务" width="120">
        <template #default="{ row }">
          <template v-if="row._subtaskTotal > 0">
            <div style="display:flex;align-items:center;gap:6px">
              <el-progress :percentage="row._subtaskPercent" :stroke-width="6" style="flex:1" :color="row._subtaskPercent === 100 ? '#67C23A' : '#f0a838'" />
              <span style="font-size:11px;color:#c0b0a0;white-space:nowrap">{{ row._subtaskDone }}/{{ row._subtaskTotal }}</span>
            </div>
          </template>
          <span v-else style="color:#a09080;font-size:12px">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="priority" label="优先级" width="80" />
      <el-table-column prop="dueDate" label="截止日期" width="110" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-dropdown trigger="click" @command="(cmd) => handleRowAction(cmd, row)">
            <el-button size="small">操作<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="isLeader || isAdmin" command="edit">编辑</el-dropdown-item>
                <el-dropdown-item v-if="isAdmin || isProjectOwner(row)" command="assign">改负责人</el-dropdown-item>
                <el-dropdown-item v-if="isAdmin || isProjectOwner(row) || row.assigneeId === currentUser.id" command="code">
                  {{ isProjectOwner(row) || isAdmin ? '查看代码' : '我的代码' }}
                </el-dropdown-item>
                <el-dropdown-item v-if="canUpdateStatus(row)" command="status">更新状态</el-dropdown-item>
                <el-dropdown-item v-if="isLeader || isAdmin" command="delete" divided style="color:#F56C6C">删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建/编辑任务弹窗 -->
    <el-dialog :title="form.id ? '编辑任务' : '新建任务'" v-model="dialogVisible" width="550px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="任务标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="所属项目">
          <el-select v-model="form.projectId" style="width:100%">
            <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务说明">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.assigneeId" style="width:100%">
            <el-option v-for="u in users" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" style="width:100%">
            <el-option v-for="p in priorities" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="form.dueDate" type="date" style="width:100%" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 组员状态更新弹窗 -->
    <el-dialog title="更新任务状态" v-model="statusDialogVisible" width="400px">
      <el-form :model="statusForm" label-width="80px">
        <el-form-item label="任务标题">
          <el-input :model-value="statusForm.title" disabled />
        </el-form-item>
        <el-form-item label="当前状态">
          <el-tag :type="statusType(statusForm.oldStatus)">{{ statusForm.oldStatus }}</el-tag>
        </el-form-item>
        <el-form-item label="新状态">
          <el-select v-model="statusForm.status" style="width:100%">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStatusUpdate">确认更新</el-button>
      </template>
    </el-dialog>

    <!-- 看板视图 -->
    <div v-if="viewMode === 'kanban'" class="kanban-board">
      <div class="kanban-column" v-for="col in kanbanColumns" :key="col.status"
           @dragover.prevent @drop="handleDrop($event, col.status)"
           :style="{ borderTop: '3px solid ' + col.color }">
        <div class="kanban-col-header">
          <span>{{ col.label }}</span>
          <el-tag size="small" round>{{ getColumnTasks(col.status).length }}</el-tag>
        </div>
        <div class="kanban-cards">
          <div v-for="task in getColumnTasks(col.status)" :key="task.id"
               class="kanban-card" draggable="true"
               @dragstart="handleDragStart($event, task.id)"
               @click="openTaskDetail(task)"
               :style="{ borderLeft: '4px solid ' + priorityColor(task.priority) }">
            <div class="kc-title">{{ task.title }}</div>
            <div class="kc-meta">
              <span>{{ task.assigneeName || '未指派' }}</span>
              <el-tag :type="priorityType(task.priority)" size="small">{{ task.priority }}</el-tag>
            </div>
            <div class="kc-footer" v-if="task.dueDate">
              <span>{{ task.dueDate }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 任务详情抽屉 -->
    <TaskDetailDrawer :visible="drawerVisible" :task="detailTask"
                      @close="drawerVisible = false" @updated="fetchTasks" />

    <!-- 更改任务负责人弹窗 -->
    <el-dialog title="更改负责人" v-model="assignDialogVisible" width="400px">
      <el-form :model="assignForm" label-width="80px">
        <el-form-item label="任务">
          <el-input :model-value="assignForm.title" disabled />
        </el-form-item>
        <el-form-item label="当前负责人">
          <el-input :model-value="assignForm.oldAssignee" disabled />
        </el-form-item>
        <el-form-item label="新负责人">
          <el-select v-model="assignForm.newAssigneeId" style="width:100%">
            <el-option v-for="u in users" :key="u.id" :label="u.realName + ' (' + u.role + ')'"
                       :value="u.id" :disabled="u.role === '管理员'" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignSave" :loading="assignSaving">确认更改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getTasks, saveTask, updateTaskStatus, deleteTask, batchDeleteTasks, getProjects, getUsers, githubPublishTask, getSubtasks, getComments } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import TaskDetailDrawer from './TaskDetailDrawer.vue'

const props = defineProps({ currentUser: Object })
const isLeader = computed(() => props.currentUser?.role === '负责人')
const isAdmin = computed(() => props.currentUser?.role === '管理员')

const tasks = ref([])
const projects = ref([])
const users = ref([])
const loading = ref(false)
const batchDeleting = ref(false)
const selectedIds = ref([])
const filterProjectId = ref(null)
const dialogVisible = ref(false)
const statusDialogVisible = ref(false)
const assignDialogVisible = ref(false)
const assignSaving = ref(false)
const assignForm = ref({})
const form = ref({})
const statusForm = ref({})
const statuses = ['未开始', '进行中', '已完成']
const priorities = ['低', '中', '高']

// 看板视图
const viewMode = ref('table')
const searchKeyword = ref('')
const filterPriority = ref('')
const filterAssignee = ref('')
const drawerVisible = ref(false)
const detailTask = ref(null)
const draggedTaskId = ref(null)

const kanbanColumns = [
  { status: '未开始', label: '待办', color: '#f0a838' },
  { status: '进行中', label: '进行中', color: '#f8c860' },
  { status: '已完成', label: '已完成', color: '#67C23A' },
]

const filteredTasks = computed(() => {
  return tasks.value.filter(t => {
    if (searchKeyword.value && !t.title.toLowerCase().includes(searchKeyword.value.toLowerCase())) return false
    if (filterPriority.value && t.priority !== filterPriority.value) return false
    if (filterAssignee.value && t.assigneeId !== filterAssignee.value) return false
    return true
  })
})

function getColumnTasks(status) {
  return filteredTasks.value.filter(t => t.status === status)
}

function applyFilters() {}

async function openTaskDetail(task) {
  detailTask.value = task
  drawerVisible.value = true
  // 加载子任务数据到 task 上供表格展示
  const res = await getSubtasks(task.id)
  if (res.success) {
    const subs = res.data || []
    task._subtaskTotal = subs.length
    task._subtaskDone = subs.filter(s => s.completed).length
    task._subtaskPercent = task._subtaskTotal > 0 ? Math.round((task._subtaskDone / task._subtaskTotal) * 100) : 0
  }
}

// 拖拽
function handleDragStart(e, taskId) {
  draggedTaskId.value = taskId
  e.dataTransfer.effectAllowed = 'move'
}
async function handleDrop(e, newStatus) {
  const taskId = draggedTaskId.value
  if (!taskId) return
  draggedTaskId.value = null
  const task = tasks.value.find(t => t.id === taskId)
  if (!task || task.status === newStatus) return
  const res = await updateTaskStatus(taskId, newStatus)
  if (res.success) {
    task.status = newStatus
    ElMessage.success(`已移至「${newStatus}」`)
  }
}

function priorityColor(p) { return p === '高' ? '#F56C6C' : p === '中' ? '#f0a838' : '#67C23A' }
function priorityType(p) { return p === '高' ? 'danger' : p === '中' ? 'warning' : 'info' }

onMounted(async () => {
  loading.value = true
  try {
    const [taskRes, projRes, userRes] = await Promise.all([getTasks(), getProjects(), getUsers()])
    if (taskRes.success) tasks.value = taskRes.data || []
    if (projRes.success) projects.value = projRes.data || []
    if (userRes.success) users.value = userRes.data || []
  } finally {
    loading.value = false
  }
})

// 根据筛选条件重新获取任务列表
async function fetchTasks() {
  loading.value = true
  try {
    const res = await getTasks(filterProjectId.value || undefined)
    if (res.success) tasks.value = res.data || []
  } finally { loading.value = false }
}

// 判断当前用户是否可以更新某任务的状态（仅分配给自己的未完成任务）
function canUpdateStatus(row) {
  return row.assigneeId === props.currentUser?.id && row.status !== '已完成'
}

// 判断当前用户是否为任务所属项目的负责人
function isProjectOwner(row) {
  const p = projects.value.find(x => x.id === row.projectId)
  return p && p.ownerId === props.currentUser?.id
}

function handleRowAction(cmd, row) {
  switch (cmd) {
    case 'edit': openDialog(row); break
    case 'assign': openAssignDialog(row); break
    case 'code': openTaskCode(row); break
    case 'status': openStatusDialog(row); break
    case 'delete': handleDelete(row.id); break
  }
}

function openDialog(row) {
  form.value = row ? { ...row } : {
    title: '', description: '', projectId: null, assigneeId: null,
    status: '未开始', priority: '中', dueDate: ''
  }
  dialogVisible.value = true
}

async function handleSave() {
  const res = await saveTask(form.value)
  if (res.success) {
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchTasks()
  }
}

function openStatusDialog(row) {
  statusForm.value = { id: row.id, title: row.title, oldStatus: row.status, status: row.status }
  statusDialogVisible.value = true
}

// 更新任务状态
async function handleStatusUpdate() {
  const res = await updateTaskStatus(statusForm.value.id, statusForm.value.status)
  if (res.success) {
    ElMessage.success('状态更新成功')
    statusDialogVisible.value = false
    fetchTasks()
  }
}

function handleSelectionChange(rows) {
  selectedIds.value = rows.map(r => r.id)
}

// 批量删除任务
async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 个任务？`, '批量删除', { type: 'warning' })
  } catch { return }
  batchDeleting.value = true
  try {
    const res = await batchDeleteTasks(selectedIds.value)
    if (res.success) {
      ElMessage.success(res.message)
      selectedIds.value = []
      fetchTasks()
    }
  } finally { batchDeleting.value = false }
}

function openAssignDialog(row) {
  assignForm.value = {
    id: row.id, title: row.title, oldAssignee: row.assigneeName, newAssigneeId: row.assigneeId
  }
  assignDialogVisible.value = true
}

// 保存任务负责人更改
async function handleAssignSave() {
  assignSaving.value = true
  try {
    const task = tasks.value.find(t => t.id === assignForm.value.id)
    if (task) {
      task.assigneeId = assignForm.value.newAssigneeId
      const res = await saveTask(task)
      if (res.success) {
        ElMessage.success('负责人已更改')
        assignDialogVisible.value = false
        fetchTasks()
      }
    }
  } finally { assignSaving.value = false }
}

// 发布任务到 GitHub（创建 Issue 和分支）
async function handlePublishTask(row) {
  try {
    const res = await githubPublishTask(row.id)
    if (res.success) {
      row.githubPublished = true
      row._branchName = res.data.branchName
      ElMessage.success(`已发布：Issue #${res.data.issueNumber} + 分支 ${res.data.branchName}`)
    }
  } catch {}
}

// 查看代码：跳转到项目仓库（默认打开主分支，也可通过分支弹窗切换）
function openTaskCode(row) {
  window.dispatchEvent(new CustomEvent('nav-github-branch', {
    detail: { projectId: row.projectId, branchName: 'main', taskTitle: row.title }
  }))
}

async function handleDelete(id) {
  await deleteTask(id)
  ElMessage.success('删除成功')
  fetchTasks()
}

// 根据任务状态返回对应的标签颜色
function statusType(status) {
  return status === '已完成' ? 'success' : status === '进行中' ? 'warning' : 'info'
}
</script>

<style scoped>
/* 看板 */
.kanban-board {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  min-height: 400px;
}
.kanban-column {
  background: rgba(20, 14, 8, 0.2);
  border-radius: 16px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.kanban-col-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff8f2;
  font-weight: 700;
  font-size: 15px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(230,162,60,0.1);
}
.kanban-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  overflow-y: auto;
}
.kanban-card {
  background: rgba(20, 14, 8, 0.4);
  border-radius: 12px;
  padding: 14px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid rgba(230,162,60,0.1);
}
.kanban-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.3);
  border-color: rgba(240,168,56,0.3);
}
.kanban-card:active { cursor: grabbing; }
.kc-title {
  font-size: 14px;
  font-weight: 700;
  color: #f5e5d8;
  margin-bottom: 8px;
  line-height: 1.4;
}
.kc-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #c0b0a0;
  margin-bottom: 4px;
}
.kc-footer {
  font-size: 11px;
  color: #a09080;
}
</style>
