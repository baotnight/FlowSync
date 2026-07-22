<template>
  <div>
    <!-- Hero 区域 -->
    <div class="overview-hero">
      <div class="hero-copy">
        <span class="hero-eyebrow">Command Center</span>
        <h1>让项目、任务和成员<br/>在同一个节奏里流动</h1>
        <p>从总览判断状态，从卡片进入模块，用表格处理精确数据。</p>
      </div>
    </div>

    <!-- 指标卡片 -->
    <el-row :gutter="16">
      <el-col :span="6" v-for="item in stats" :key="item.label">
        <el-card shadow="hover" class="stat-card metric-card" :class="item.tone" @click="goTo(item.panel)">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.desc }}</em>
          <div class="metric-preview">
            <b>{{ item.previewTitle }}</b>
            <p>{{ item.previewText }}</p>
            <small>{{ item.previewSmall }}</small>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 上传排队状态，仅当队列有数据时显示 -->
    <el-card v-if="queueItems.length > 0" style="margin-top:20px">
      <template #header>
        <span>文件上传队列</span>
      </template>
      <div v-for="item in queueItems" :key="item.path"
           style="display:flex;align-items:center;justify-content:space-between;padding:10px;margin-bottom:8px;border-radius:4px"
           :style="{ background: item.ready ? 'rgba(103,194,58,0.12)' : 'rgba(230,162,60,0.12)' }">
        <div>
          <div style="font-weight:bold">{{ item.owner }}/{{ item.repo }} — {{ item.path }}</div>
          <div style="font-size:12px;color:#e8ddd0;margin-top:4px">
            <template v-if="item.ready">
              <span style="color:#67C23A">轮到您了！请检查远端最新版本后确认上传</span>
            </template>
            <template v-else>
              {{ item.currentHolder || '未知' }} 正在上传，等待中...
            </template>
          </div>
        </div>
        <el-button v-if="item.ready" type="primary" size="small" @click="jumpToUpload(item)">
          去确认上传
        </el-button>
      </div>
    </el-card>

    <!-- 文件修改记录 -->
    <el-card style="margin-top:20px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>文件修改记录</span>
          <div style="display:flex;gap:8px">
            <el-select v-model="selectedProject" placeholder="选择项目" size="small" style="width:200px" @change="loadCommits" clearable>
              <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
            <el-select v-model="filterUser" placeholder="筛选成员" size="small" style="width:150px" @change="loadCommits" clearable>
              <el-option v-for="u in uniqueUsers" :key="u" :label="u" :value="u" />
            </el-select>
          </div>
        </div>
      </template>
      <div v-if="!selectedProject" style="text-align:center;padding:20px;color:#e8ddd0">选择项目查看 Git 提交历史</div>
      <div v-else-if="commits.length === 0" style="text-align:center;padding:20px;color:#e8ddd0" v-loading="commitLoading">暂无提交记录或未绑定仓库</div>
      <el-timeline v-else>
        <el-timeline-item v-for="c in commits" :key="c.sha" :timestamp="c.date" placement="top"
                          :color="c.flowSyncUser ? '#f0a838' : '#e8ddd0'">
          <div>{{ c.message }}</div>
          <div style="font-size:12px;color:#e8ddd0;margin-top:4px">
            SHA: <code>{{ c.sha }}</code>
            <template v-if="c.githubLogin"> | <strong>{{ c.githubLogin }}</strong></template>
            <template v-if="c.flowSyncUser"> | <el-tag size="small" type="success">{{ c.flowSyncUser }}</el-tag></template>
            <template v-if="c.author && !c.githubLogin"> | {{ c.author }}</template>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <!-- 待审核文件（仅项目负责人和管理员可见） -->
    <el-card style="margin-top:20px" v-if="isReviewer && pendingReviews.length > 0">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>待审核文件（{{ pendingReviews.length }}）</span>
          <el-button size="small" @click="loadPendingReviews">刷新</el-button>
        </div>
      </template>
      <el-table :data="pendingReviews" border size="small" v-loading="prLoading">
        <el-table-column prop="submitterName" label="提交人" width="100" />
        <el-table-column prop="filePath" label="文件路径" show-overflow-tooltip />
        <el-table-column prop="branch" label="分支" width="120" />
        <el-table-column prop="createTime" label="提交时间" width="160" />
        <el-table-column label="操作" width="210">
          <template #default="{ row }">
            <el-button size="small" @click="viewCacheContent(row)">查看</el-button>
            <el-button size="small" type="success" @click="handleApproveCache(row)">批准</el-button>
            <el-button size="small" type="danger" @click="handleRejectCache(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 文件内容预览弹窗 -->
    <el-dialog :title="'审核文件 — ' + previewFile.path" v-model="previewVisible" width="800px" top="2vh">
      <div style="font-size:12px;color:#e8ddd0;margin-bottom:8px">
        提交人：{{ previewFile.submitterName }} | 分支：{{ previewFile.branch }} | 时间：{{ previewFile.createTime }}
      </div>
      <pre style="background:#1e1e1e;color:#d4d4d4;padding:12px;border-radius:4px;overflow:auto;max-height:500px;font-size:13px;line-height:1.5">{{ previewFile.decoded }}</pre>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button type="success" @click="previewVisible = false; handleApproveCache(previewFile)">批准</el-button>
        <el-button type="danger" @click="previewVisible = false; handleRejectCache(previewFile)">拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getOverview, getProjects, githubProjectCommits, getFileCache, approveFileCache, rejectFileCache } from '../api'
import { pendingUploads } from '../store/uploadQueue'
import { ElMessage } from 'element-plus'

const stats = ref([
  { label: '系统用户', value: 0, panel: 'admin', tone: 'tone-blue', desc: '当前注册用户', previewTitle: '成员管理', previewText: '管理用户角色与权限', previewSmall: '点击进入成员管理面板' },
  { label: '项目总数', value: 0, panel: 'projects', tone: 'tone-green', desc: '协作项目总量', previewTitle: '项目管理', previewText: '创建与编排项目', previewSmall: '点击进入项目管理面板' },
  { label: '任务总数', value: 0, panel: 'tasks', tone: 'tone-amber', desc: '当前任务总量', previewTitle: '任务管理', previewText: '分配与追踪任务', previewSmall: '点击进入任务管理面板' },
  { label: '总结总数', value: 0, panel: 'summaries', tone: 'tone-purple', desc: '累计复盘报告', previewTitle: '总结管理', previewText: '沉淀项目阶段报告', previewSmall: '点击进入总结管理面板' }
])

const emit = defineEmits(['navigate'])
function goTo(panel) { emit('navigate', panel) }

const props = defineProps({ currentUser: Object })
const isReviewer = computed(() => props.currentUser?.role === '负责人' || props.currentUser?.role === '管理员')
const queueItems = pendingUploads
const projects = ref([])
const selectedProject = ref(null)
const commits = ref([])
const allCommits = ref([])
const filterUser = ref('')
const commitLoading = ref(false)

// 从所有提交记录中提取不重复的用户列表
const uniqueUsers = computed(() => {
  const names = new Set()
  for (const c of allCommits.value) {
    if (c.flowSyncUser) names.add(c.flowSyncUser)
  }
  return [...names].sort()
})

onMounted(loadInitialData)

// 加载选中项目的 Git 提交记录
async function loadCommits() {
  if (!selectedProject.value) { allCommits.value = []; commits.value = []; return }
  commitLoading.value = true
  try {
    const res = await githubProjectCommits(selectedProject.value)
    allCommits.value = res.success ? (res.data || []) : []
    applyFilter()
  } finally { commitLoading.value = false }
}

// 根据筛选条件过滤提交记录
function applyFilter() {
  commits.value = filterUser.value
    ? allCommits.value.filter(c => c.flowSyncUser === filterUser.value)
    : allCommits.value
}

// 跳转到上传确认页面
// 待审核文件
const pendingReviews = ref([])
const prLoading = ref(false)
const previewVisible = ref(false)
const previewFile = ref({})

async function loadPendingReviews() {
  prLoading.value = true
  try {
    const all = []
    for (const p of projects.value) {
      const res = await getFileCache(p.id)
      if (res.success) {
        for (const item of (res.data || [])) {
          if (item.status === 'pending') all.push({ ...item, projectName: p.name })
        }
      }
    }
    pendingReviews.value = all
  } finally { prLoading.value = false }
}

async function handleApproveCache(row) {
  const res = await approveFileCache(row.id)
  if (res.success) {
    ElMessage.success('已批准并上传到 GitHub')
    loadPendingReviews()
  }
}

function viewCacheContent(row) {
  try {
    previewFile.value = {
      ...row,
      decoded: decodeURIComponent(escape(atob(row.content)))
    }
  } catch {
    previewFile.value = { ...row, decoded: '[解码失败]' }
  }
  previewVisible.value = true
}

async function handleRejectCache(row) {
  const res = await rejectFileCache(row.id)
  if (res.success) {
    ElMessage.success('已拒绝')
    loadPendingReviews()
  }
}

function jumpToUpload(item) {
  window.dispatchEvent(new CustomEvent('nav-github-tree', { detail: item }))
}

// 加载时也获取待审核列表
async function loadInitialData() {
  try {
    const res = await getOverview()
    if (res.success) {
      stats.value[0].value = res.data.userCount || 0
      stats.value[1].value = res.data.projectCount || 0
      stats.value[2].value = res.data.taskCount || 0
      stats.value[3].value = res.data.summaryCount || 0
    }
    const pRes = await getProjects()
    if (pRes.success) projects.value = pRes.data || []
    loadPendingReviews()
  } catch (e) { /* ignore */ }
}
</script>
<style scoped>
/* Hero 区域 */
.overview-hero {
  position: relative;
  display: grid;
  min-height: 150px;
  padding: 22px 26px;
  margin-bottom: 16px;
  border-radius: 22px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(240, 168, 56, 0.22), rgba(248, 200, 96, 0.12), rgba(20, 14, 8, 0.3));
  border: 1px solid rgba(230, 162, 60, 0.25);
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.2);
}
.hero-eyebrow {
  color: #f8c860;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.hero-copy { position: relative; z-index: 1; }
.hero-copy h1 {
  margin: 8px 0 10px;
  font-size: 28px;
  line-height: 1.15;
  color: #fff8f2;
}
.hero-copy p {
  margin: 0;
  color: #e0d4c8;
  line-height: 1.6;
  font-size: 14px;
}

/* 指标卡片 */
.metric-card {
  position: relative !important;
  overflow: visible !important;
  display: grid;
  gap: 6px;
  padding: 18px 16px !important;
  cursor: pointer;
  isolation: isolate;
  transition: transform 0.28s cubic-bezier(.2,.8,.2,1), box-shadow 0.28s, border-color 0.28s;
}
.metric-card:hover {
  transform: translateY(-6px);
  z-index: 12;
  border-color: rgba(240, 168, 56, 0.5) !important;
}
.metric-card span {
  font-size: 14px;
  font-weight: 700;
  color: #e0d4c8;
}
.metric-card strong {
  font-size: 38px;
  color: #f0a838;
}
.metric-card em {
  font-style: normal;
  font-size: 12px;
  color: #e0d4c8;
}
/* 悬浮预览 */
.metric-preview {
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: calc(100% - 10px);
  z-index: 20;
  display: grid;
  gap: 6px;
  min-height: 90px;
  padding: 14px 15px;
  border: 1px solid rgba(240, 168, 56, 0.3);
  border-radius: 18px;
  color: #f5e5d8;
  background: rgba(30, 18, 10, 0.92);
  box-shadow: 0 20px 46px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(16px);
  pointer-events: none;
  opacity: 0;
  transform: translateY(12px) scale(0.94);
  transform-origin: bottom center;
  transition: opacity 0.22s ease, transform 0.34s cubic-bezier(.18,.88,.32,1.16);
}
.metric-card:hover .metric-preview {
  opacity: 1;
  transform: translateY(-7px) scale(1);
}
.metric-preview b {
  font-size: 14px;
  line-height: 1.15;
  color: #f0a838;
}
.metric-preview p {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: #f5e5d8;
}
.metric-preview small {
  display: block;
  color: #e0d4c8;
  font-size: 12px;
  line-height: 1.45;
}
</style>
