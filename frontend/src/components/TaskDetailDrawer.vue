<template>
  <el-drawer :model-value="visible" @update:model-value="$emit('close')" title="任务详情" size="480px" direction="rtl">
    <div v-if="task" class="drawer-body" style="display:flex;flex-direction:column;gap:16px;height:100%">
      <!-- 任务标题和 meta -->
      <div>
        <h3 style="margin:0 0 8px;color:#fff8f2;font-size:20px">{{ task.title }}</h3>
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          <el-tag :type="statusType(task.status)">{{ task.status }}</el-tag>
          <el-tag :type="priorityType(task.priority)">{{ task.priority }}优先级</el-tag>
          <el-tag type="info">{{ task.assigneeName || '未指派' }}</el-tag>
        </div>
      </div>

      <!-- 描述 -->
      <div>
        <div style="color:#c0b0a0;font-size:13px;font-weight:700;margin-bottom:6px">描述</div>
        <p style="color:#f5e5d8;font-size:14px;line-height:1.6;margin:0">{{ task.description || '暂无描述' }}</p>
      </div>

      <!-- 子任务 -->
      <div style="flex:1;overflow-y:auto">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
          <span style="color:#c0b0a0;font-weight:700;font-size:13px">
            子任务（{{ completedCount }}/{{ subtasks.length }}）
          </span>
          <el-button size="small" @click="startAddSubtask">+ 添加</el-button>
        </div>
        <el-progress v-if="subtasks.length > 0" :percentage="subtaskPercent" :stroke-width="6" style="margin-bottom:10px" />
        <div v-if="subtasks.length === 0" style="color:#c0b0a0;font-size:13px;padding:20px 0;text-align:center">暂无子任务</div>
        <div v-for="st in subtasks" :key="st.id" style="display:flex;align-items:center;gap:8px;padding:8px 0;border-bottom:1px solid rgba(230,162,60,0.08)">
          <el-checkbox :model-value="st.completed" @change="toggleSubtask(st)" />
          <span :style="{ flex:1, textDecoration: st.completed ? 'line-through' : 'none', color: st.completed ? '#a09080' : '#f5e5d8' }">{{ st.title }}</span>
          <el-button link size="small" type="danger" @click="removeSubtask(st.id)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <div v-if="addingSubtask" style="display:flex;gap:8px;margin-top:8px">
          <el-input v-model="newSubtaskTitle" placeholder="子任务标题" size="small" @keyup.enter="confirmAddSubtask" />
          <el-button size="small" type="primary" @click="confirmAddSubtask">确定</el-button>
          <el-button size="small" @click="addingSubtask = false; newSubtaskTitle = ''">取消</el-button>
        </div>
      </div>

      <!-- 评论 -->
      <div style="border-top:1px solid rgba(230,162,60,0.1);padding-top:12px">
        <div style="color:#c0b0a0;font-weight:700;font-size:13px;margin-bottom:8px">评论（{{ comments.length }}）</div>
        <div v-if="comments.length === 0" style="color:#c0b0a0;font-size:13px;padding:12px 0;text-align:center">暂无评论</div>
        <div v-for="c in comments" :key="c.id" style="padding:8px 0;border-bottom:1px solid rgba(230,162,60,0.06)">
          <div style="display:flex;gap:8px;align-items:center;margin-bottom:4px">
            <span style="color:#f0a838;font-size:13px;font-weight:700">{{ c.userName }}</span>
            <span style="color:#a09080;font-size:11px">{{ c.createTime }}</span>
          </div>
          <p style="color:#f5e5d8;font-size:13px;line-height:1.5;margin:0">{{ c.content }}</p>
        </div>
        <div style="display:flex;gap:8px;margin-top:8px">
          <el-input v-model="newComment" placeholder="添加评论..." size="small" @keyup.enter="sendComment" />
          <el-button size="small" type="primary" @click="sendComment" :loading="sending">发送</el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { getSubtasks, addSubtask, updateSubtask, deleteSubtask, getComments, addComment, updateTaskStatus, saveTaskLog } from '../api'
import { ElMessage } from 'element-plus'

const props = defineProps({ visible: Boolean, task: Object })
const emit = defineEmits(['close', 'updated'])

const subtasks = ref([])
const comments = ref([])
const addingSubtask = ref(false)
const newSubtaskTitle = ref('')
const newComment = ref('')
const sending = ref(false)

const completedCount = computed(() => subtasks.value.filter(s => s.completed).length)
const subtaskPercent = computed(() => subtasks.value.length > 0 ? Math.round((completedCount.value / subtasks.value.length) * 100) : 0)

watch(() => props.visible, async (v) => {
  if (v && props.task) {
    const [sRes, cRes] = await Promise.all([getSubtasks(props.task.id), getComments(props.task.id)])
    subtasks.value = sRes.success ? (sRes.data || []) : []
    comments.value = cRes.success ? (cRes.data || []) : []
  }
})

async function toggleSubtask(st) {
  const res = await updateSubtask(props.task.id, st.id, { completed: !st.completed })
  if (res.success) {
    st.completed = !st.completed
    // 根据子任务完成情况自动更新任务状态
    const allDone = subtasks.value.every(s => s.completed)
    const anyProgress = subtasks.value.some(s => s.completed)
    const done = subtasks.value.filter(s => s.completed).length
    const progress = Math.round((done / subtasks.value.length) * 100)
    // 同步创建进度记录
    await saveTaskLog({ taskId: props.task.id, progressPercent: progress, content: `子任务「${st.title}」${st.completed ? '完成' : '取消完成'}，进度 ${done}/${subtasks.value.length}（${progress}%）` })
    if (allDone && props.task.status !== '已完成') {
      await updateTaskStatus(props.task.id, '已完成')
      props.task.status = '已完成'
    } else if (anyProgress && props.task.status === '未开始') {
      await updateTaskStatus(props.task.id, '进行中')
      props.task.status = '进行中'
    }
    emit('updated')
  }
}
function startAddSubtask() { addingSubtask.value = true; newSubtaskTitle.value = '' }
async function confirmAddSubtask() {
  if (!newSubtaskTitle.value.trim()) return
  const res = await addSubtask(props.task.id, newSubtaskTitle.value.trim())
  if (res.success) {
    subtasks.value.push(res.data)
    addingSubtask.value = false
    const done = subtasks.value.filter(s => s.completed).length
    await saveTaskLog({ taskId: props.task.id, progressPercent: Math.round((done / subtasks.value.length) * 100), content: `新增子任务「${newSubtaskTitle.value.trim()}」` })
    emit('updated')
  }
}
async function removeSubtask(id) {
  const st = subtasks.value.find(s => s.id === id)
  const res = await deleteSubtask(props.task.id, id)
  if (res.success) {
    subtasks.value = subtasks.value.filter(s => s.id !== id)
    const done = subtasks.value.filter(s => s.completed).length
    const progress = subtasks.value.length > 0 ? Math.round((done / subtasks.value.length) * 100) : 0
    await saveTaskLog({ taskId: props.task.id, progressPercent: progress, content: st ? `删除子任务「${st.title}」` : '删除子任务' })
    emit('updated')
  }
}
async function sendComment() {
  if (!newComment.value.trim()) return
  sending.value = true
  const res = await addComment(props.task.id, newComment.value.trim())
  if (res.success) { comments.value.push(res.data); newComment.value = '' }
  sending.value = false
}
function statusType(s) { return s === '已完成' ? 'success' : s === '进行中' ? 'warning' : 'info' }
function priorityType(p) { return p === '高' ? 'danger' : p === '中' ? 'warning' : 'info' }
</script>
