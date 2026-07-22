<template>
  <!-- ==================== 登录/注册页面 ==================== -->
  <div v-if="!currentUser" class="login-container">
    <!-- 视频背景 -->
    <video class="login-video" autoplay muted loop playsinline>
      <source src="/login-bg.mp4" type="video/mp4" />
    </video>
    <!-- 背景大字标题 -->
    <div class="hero-title">FlowSync</div>
    <div class="hero-subtitle">小组任务协同管理系统</div>

    <!-- 翻转卡片 -->
    <div class="wrapper">
      <div class="switch" @click="isRegisterMode = !isRegisterMode">
        <div class="slider" :class="{ checked: isRegisterMode }"></div>
        <div class="card-side" :class="{ flipped: isRegisterMode }"></div>
      </div>
      <div class="flip-card__inner" :class="{ flipped: isRegisterMode }">
        <div class="flip-card__front">
          <div class="login-brand">
            <div class="brand-mark">F</div>
            <div><strong>FlowSync</strong><span>协同管理系统</span></div>
          </div>
          <div class="flip-title">欢迎回来</div>
          <div class="flip-subtitle">登录 FlowSync 工作台</div>
          <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" label-position="top"
                   class="flip-card__form" @keyup.enter="handleLogin">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" size="large" class="flip-input" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" size="large"
                        show-password class="flip-input" />
            </el-form-item>
            <el-form-item class="submit-row">
              <el-button color="#f0a838" size="large" class="flip-btn" @click="handleLogin" :loading="submitting">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
          <div class="flip-hint">没有账号？点击上方滑块切换注册</div>
        </div>
        <div class="flip-card__back">
          <div class="login-brand">
            <div class="brand-mark">F</div>
            <div><strong>FlowSync</strong><span>协同管理系统</span></div>
          </div>
          <div class="flip-title">创建账户</div>
          <div class="flip-subtitle">填写基础信息后加入协同流程</div>
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-position="top"
                   class="flip-card__form" @keyup.enter="handleRegister">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="请输入用户名" size="large" class="flip-input" />
            </el-form-item>
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="registerForm.realName" placeholder="请输入真实姓名" size="large" class="flip-input" />
            </el-form-item>
            <el-form-item label="系统角色" prop="role">
              <el-radio-group v-model="registerForm.role" class="role-group">
                <el-radio-button label="组员">组员（直接加入团队）</el-radio-button>
                <el-radio-button label="负责人">项目负责人（需邀请码）</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="registerForm.role === '负责人'" label="邀请码" prop="inviteCode">
              <el-input v-model="registerForm.inviteCode" placeholder="请输入管理员提供的邀请码" size="large" class="flip-input" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="至少4位" size="large"
                        show-password class="flip-input" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次输入密码" size="large"
                        show-password class="flip-input" />
            </el-form-item>
            <el-form-item class="submit-row">
              <el-button color="#f8c860" size="large" class="flip-btn" @click="handleRegister" :loading="submitting">
                注 册
              </el-button>
            </el-form-item>
          </el-form>
          <div class="flip-hint">已有账号？点击上方滑块切换登录</div>
        </div>
      </div>
    </div>
  </div>

  <!-- ==================== 主界面 ==================== -->
  <div v-else class="main-layout">
    <!-- 视频背景 -->
    <video class="main-video" autoplay muted loop playsinline>
      <source src="/login-bg.mp4" type="video/mp4" />
    </video>

    <!-- 顶部导航栏 -->
    <div class="top-nav">
      <div class="nav-left">
        <div class="nav-avatar" :style="{ background: !sidebarAvatarImg ? sidebarAvatarColor : 'transparent' }"
             @click="showProfile" title="个人信息">
          <img v-if="sidebarAvatarImg" :src="sidebarAvatarImg" style="width:100%;height:100%;border-radius:50%;object-fit:cover" />
          <span v-else>{{ sidebarAvatarText }}</span>
        </div>
        <div>
          <span class="nav-brand">FlowSync</span>
        </div>
      </div>

      <nav class="module-nav">
        <button :class="{ active: activeMenu === 'dashboard' }" @click="handleMenuSelect('dashboard')">控制台</button>
        <button v-if="isLeader || isAdmin" :class="{ active: activeMenu === 'ai-plan' }" @click="handleMenuSelect('ai-plan')">AI 拆解</button>
        <button :class="{ active: activeMenu === 'task-logs' }" @click="handleMenuSelect('task-logs')">进度</button>
        <button v-if="isLeader || isAdmin" :class="{ active: activeMenu === 'github' }" @click="handleMenuSelect('github')">GitHub</button>
      </nav>

      <div class="nav-right">
        <span class="nav-user">{{ currentUser.realName }}</span>
        <el-tag :type="isLeader ? 'danger' : 'info'" size="small">{{ currentUser.role }}</el-tag>
        <el-popconfirm title="确认退出登录？" @confirm="handleLogout">
          <template #reference>
            <el-button text size="small" class="nav-logout">退出</el-button>
          </template>
        </el-popconfirm>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="content">
      <component :is="currentPanel" :current-user="currentUser" @navigate="handleMenuSelect" />
    </div>

    <!-- 个人信息抽屉（点击头像展开） -->
    <el-drawer v-model="profileVisible" title="个人信息" direction="ltr" size="420px">
      <ProfilePanel :current-user="currentUser" />
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { login as apiLogin, register as apiRegister } from '../api'

// 面板组件
import DashboardPanel from '../components/DashboardPanel.vue'
import ProjectPanel from '../components/ProjectPanel.vue'
import AiTaskPlanPanel from '../components/AiTaskPlanPanel.vue'
import TaskPanel from '../components/TaskPanel.vue'
import TaskLogPanel from '../components/TaskLogPanel.vue'
import SummaryPanel from '../components/SummaryPanel.vue'
import AdminPanel from '../components/AdminPanel.vue'
import GitHubPanel from '../components/GitHubPanel.vue'
import ProfilePanel from '../components/ProfilePanel.vue'
import { avatarImage, avatarColor as storeAvatarColor } from '../store/avatarStore'

const currentUser = ref(null)
const activeMenu = ref('dashboard')
const profileVisible = ref(false)
function showProfile() { profileVisible.value = true }
const submitting = ref(false)
const isRegisterMode = ref(false)
const loginFormRef = ref(null)
const registerFormRef = ref(null)

const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', realName: '', role: '组员', inviteCode: '', password: '', confirmPassword: '' })

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const validateConfirmPwd = (rule, value, callback) => {
  if (value !== registerForm.value.password) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 4, message: '密码至少 4 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' }
  ]
}

const isLeader = computed(() => currentUser.value?.role === '负责人')
const isAdmin = computed(() => currentUser.value?.role === '管理员')

const sidebarAvatarText = computed(() => (currentUser.value?.realName || 'U').charAt(0).toUpperCase())
const sidebarAvatarImg = computed(() => avatarImage.value || currentUser.value?.avatar || '')
const sidebarAvatarColor = computed(() => {
  const sc = storeAvatarColor.value
  if (sc) return sc
  const h = (s) => { let h = 0; for (let i = 0; i < (s||'').length; i++) h = ((h << 5) - h) + s.charCodeAt(i); return Math.abs(h) }
  const colors = ['#f0a838','#67C23A','#f0a838','#F56C6C','#00d4ff','#8b5cf6']
  return colors[h(currentUser.value?.realName) % colors.length]
})

// 菜单 → 面板映射
const panelMap = {
  'dashboard': DashboardPanel,
  'projects': ProjectPanel,
  'ai-plan': AiTaskPlanPanel,
  'tasks': TaskPanel,
  'task-logs': TaskLogPanel,
  'summaries': SummaryPanel,
  'github': GitHubPanel,
  'admin': AdminPanel
}

const currentPanel = computed(() => panelMap[activeMenu.value] || DashboardPanel)

onMounted(() => {
  const stored = sessionStorage.getItem('currentUser')
  const token = sessionStorage.getItem('token')
  if (stored && token) {
    currentUser.value = JSON.parse(stored)
  }
  // 监听任务代码跳转事件
  window.addEventListener('nav-github-branch', (e) => {
    sessionStorage.setItem('githubOpenBranch', JSON.stringify(e.detail))
    activeMenu.value = 'github'
  })
})

async function handleLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await apiLogin(loginForm.value)
    if (res.success) {
      const { token, user } = res.data
      sessionStorage.setItem('token', token)
      sessionStorage.setItem('currentUser', JSON.stringify(user))
      currentUser.value = user
      ElMessage.success(`欢迎，${user.realName}`)
    }
  } finally {
    submitting.value = false
  }
}

async function handleRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await apiRegister({
      username: registerForm.value.username,
      password: registerForm.value.password,
      realName: registerForm.value.realName,
      role: registerForm.value.role,
      inviteCode: registerForm.value.inviteCode
    })
    if (res.success) {
      ElMessage.success('注册成功，请登录')
      isRegisterMode.value = false
      loginForm.value.username = registerForm.value.username
      // 清空注册表单
      registerForm.value = { username: '', realName: '', password: '', confirmPassword: '' }
    }
  } finally {
    submitting.value = false
  }
}

function handleMenuSelect(index) {
  activeMenu.value = index
}


function handleLogout() {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('currentUser')
  sessionStorage.removeItem('publishedTasks')
  currentUser.value = null
  activeMenu.value = 'dashboard'
}
</script>

<style>
/* 全局重置 */
* { margin: 0; padding: 0; box-sizing: border-box; }

/* 覆盖 Element Plus CSS 变量（从根源暗色化，组件内部不再引用白色变量） */
:root {
  --el-bg-color-overlay: rgba(20, 14, 8, 0.3);
  --el-bg-color: #1a1008;
  --el-bg-color-page: #1a1008;
  --el-fill-color-blank: rgba(20, 14, 8, 0.3);
  --el-fill-color: rgba(20, 14, 8, 0.3);
  --el-fill-color-light: rgba(230, 162, 60, 0.1);
  --el-fill-color-lighter: rgba(230, 162, 60, 0.05);
  --el-color-white: #0d162a;
  --el-color-black: #f5e5d8;
  --el-border-color: rgba(230, 162, 60, 0.15);
  --el-border-color-light: rgba(230, 162, 60, 0.1);
  --el-border-color-lighter: rgba(230, 162, 60, 0.06);
  --el-border-color-dark: rgba(230, 162, 60, 0.3);
  --el-text-color-primary: #fff8f2;
  --el-text-color-regular: #f5e5d8;
  --el-text-color-secondary: #c0b0a0;
  --el-text-color-placeholder: #c0b0a0;
  --el-text-color-disabled: #a09888;
  --el-disabled-bg-color: rgba(20, 14, 8, 0.3);
  --el-disabled-border-color: rgba(230, 162, 60, 0.08);
  --el-disabled-text-color: #a09888;
  --el-mask-color: rgba(5, 8, 16, 0.7);
  --el-box-shadow: 0 2px 16px rgba(0,0,0,0.25);
  --el-box-shadow-light: 0 2px 8px rgba(0,0,0,0.15);
}
</style>

<style scoped>
/* ======== 登录页 ======== */
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: #1a1008;
  position: relative;
  overflow: hidden;
}
.login-video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 0;
  pointer-events: none;
}
/* 背景暗色蒙层，让前景卡片更突出 */
.login-container::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(8, 5, 3, 0.1);
  z-index: 0;
  pointer-events: none;
}
/* ======== 翻转卡片容器 ======== */
.wrapper {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  --input-focus: #f0a838;
  --font-color: #f5e5d8;
  --font-color-sub: #c0b0a0;
  --bg-color: rgba(20, 14, 8, 0.3);
  --main-color: rgba(230, 162, 60, 0.45);
}

/* ======== 品牌标识 ======== */
.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 2px;
}
.brand-mark {
  display: grid;
  place-items: center;
  width: 38px; height: 38px;
  border-radius: 12px;
  color: #fff;
  background: linear-gradient(135deg, #f0a838, #f8c860 52%, #fdd89a);
  font-weight: 900;
  font-size: 18px;
}
.login-brand strong {
  display: block;
  color: #fff8f2;
  font-size: 18px;
}
.login-brand span {
  color: #c0b0a0;
  font-size: 12px;
}

/* ======== 切换滑块 ======== */
.switch {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 42px;
  height: 16px;
  margin-bottom: 14px;
}
.card-side::before {
  position: absolute;
  content: '登 录';
  left: -42px;
  top: -1px;
  width: 40px;
  text-decoration: underline;
  color: var(--font-color);
  font-weight: 500;
  font-size: 12px;
  letter-spacing: 1px;
  opacity: 0.7;
}
.card-side::after {
  position: absolute;
  content: '注 册';
  left: 46px;
  top: -1px;
  width: 40px;
  text-decoration: none;
  color: var(--font-color);
  font-weight: 500;
  font-size: 12px;
  letter-spacing: 1px;
  opacity: 0.7;
}
.slider {
  box-sizing: border-box;
  border-radius: 10px;
  border: 1px solid rgba(230, 162, 60, 0.25);
  position: absolute;
  cursor: pointer;
  top: 0; left: 0; right: 0; bottom: 0;
  background-color: rgba(20, 14, 8, 0.2);
  transition: 0.3s;
}
.slider:before {
  box-sizing: border-box;
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  border: 1px solid rgba(230, 162, 60, 0.3);
  border-radius: 8px;
  left: -1px;
  bottom: -1px;
  background-color: rgba(230, 162, 60, 0.15);
  transition: 0.3s;
}
.slider.checked {
  background-color: rgba(230, 162, 60, 0.15);
  border-color: rgba(240, 168, 56, 0.4);
}
.slider.checked:before {
  transform: translateX(26px);
  background-color: #f0a838;
  border-color: #f0a838;
  box-shadow: 0 0 6px rgba(240, 168, 56, 0.25);
}
.card-side.flipped:before {
  text-decoration: none;
}
.card-side.flipped:after {
  text-decoration: underline;
}

/* ======== 3D 翻转卡片 ======== */
.flip-card__inner {
  width: 440px;
  height: 620px;
  position: relative;
  background-color: transparent;
  perspective: 1000px;
  text-align: center;
  transition: transform 0.7s cubic-bezier(0.4, 0, 0.2, 1);
  transform-style: preserve-3d;
}
.flip-card__inner.flipped {
  transform: rotateY(180deg);
}
.flip-card__inner.flipped .flip-card__front {
  box-shadow: none;
}
.flip-card__front, .flip-card__back {
  padding: 24px 26px;
  position: absolute;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
  background: rgba(20, 14, 8, 0.3);
  gap: 4px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(230, 162, 60, 0.15) transparent;
  border-radius: 22px;
  border: 2px solid rgba(230, 162, 60, 0.35);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.05);
}
.flip-card__front {
  justify-content: center;
}
.flip-card__front::-webkit-scrollbar,
.flip-card__back::-webkit-scrollbar {
  width: 4px;
}
.flip-card__front::-webkit-scrollbar-thumb,
.flip-card__back::-webkit-scrollbar-thumb {
  background: rgba(230, 162, 60, 0.15);
  border-radius: 2px;
}
.flip-card__back {
  transform: rotateY(180deg);
}
.flip-card__form {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
}
.flip-title {
  font-size: 24px;
  font-weight: 900;
  text-align: center;
  color: #fff8f2;
  margin: 0;
}
.flip-subtitle {
  text-align: center;
  color: #c0b0a0;
  font-size: 13px;
  margin-bottom: 6px;
}
.flip-card__form :deep(.el-form-item) {
  margin-bottom: 10px;
}
.flip-card__form :deep(.el-form-item__label) {
  color: #fff8f2 !important;
  font-weight: 700;
  padding-bottom: 4px;
}
.flip-card__form :deep(.el-input__wrapper) {
  min-height: 42px;
  background: rgba(20, 14, 8, 0.3) !important;
  box-shadow: 0 0 0 1px rgba(230, 162, 60, 0.2) inset !important;
  border-radius: 11px !important;
}
.flip-card__form :deep(.el-input__inner) {
  color: #f5e5d8 !important;
}
.flip-card__form :deep(.el-input__inner::placeholder) {
  color: #c0b0a0 !important;
}
.flip-card__form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(230, 162, 60, 0.4) inset !important;
}
.flip-card__form :deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 1px #f0a838 inset, 0 0 0 4px rgba(240, 168, 56, 0.14) !important;
}
.flip-card__form :deep(.el-input__suffix .el-icon) {
  color: #c0b0a0;
}

/* 角色 radio 按钮组 */
.role-group {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  width: 100%;
}
:deep(.el-radio-button__inner) {
  width: 100%;
  border-radius: 10px !important;
}
:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #f0a838, #f8c860) !important;
  border-color: #f0a838 !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(240, 168, 56, 0.3) !important;
}

/* 提交按钮 */
.submit-row {
  margin-top: 4px;
}
.submit-row :deep(.el-button) {
  width: 100%;
  min-height: 42px;
}
.flip-btn {
  width: 100%;
  border-radius: 11px !important;
  font-weight: 700;
}
.flip-hint {
  text-align: center;
  color: #c0b0a0;
  font-size: 12px;
  line-height: 1.8;
}
.hero-title {
  position: absolute;
  z-index: 1;
  top: 2%;
  left: 50%;
  transform: translateX(-50%);
  font-size: 72px;
  font-weight: 900;
  color: rgba(255, 255, 255, 0.75);
  letter-spacing: 12px;
  pointer-events: none;
  user-select: none;
  text-shadow: 0 0 80px rgba(255,255,255,0.1);
}
.hero-subtitle {
  position: absolute;
  z-index: 1;
  top: calc(5% + 60px);
  left: 50%;
  transform: translateX(-50%);
  font-size: 18px;
  font-weight: 300;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 8px;
  pointer-events: none;
  user-select: none;
}
/* ======== 主界面 ======== */
.main-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #1a1008;
  position: relative;
  overflow: hidden;
}
.main-video {
  position: absolute;
  inset: 0;
  width: 100%; height: 100%;
  object-fit: cover;
  z-index: 0;
  pointer-events: none;
}

/* ======== 顶部悬浮导航栏 ======== */
.top-nav {
  position: relative;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 52px;
  padding: 8px 16px;
  margin: 10px 20px 0 20px;
  background: rgba(20, 14, 8, 0.3);
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
  border: 2px solid rgba(230, 162, 60, 0.35);
  border-radius: 22px;
  box-shadow: 0 0 30px rgba(230, 162, 60, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  flex-shrink: 0;
}
.nav-left { display: flex; align-items: center; gap: 8px; min-width: 170px; }
.nav-avatar {
  width: 34px; height: 34px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 15px; font-weight: bold;
  flex-shrink: 0; cursor: pointer;
  transition: transform 0.2s;
}
.nav-avatar:hover { transform: scale(1.1); }
.nav-brand {
  font-size: 17px; font-weight: 700;
  color: #faf3e8; letter-spacing: 2px;
  text-shadow: 0 0 16px rgba(230, 162, 60, 0.35);
}

/* 模块导航按钮组 */
.module-nav {
  display: flex;
  justify-content: center;
  gap: 4px;
  flex: 1;
  min-width: 0;
  padding: 4px;
  border-radius: 14px;
  background: rgba(20, 14, 8, 0.3);
}
.module-nav button {
  min-height: 34px;
  padding: 0 14px;
  border: 0;
  border-radius: 10px;
  color: #c0b0a0;
  background: transparent;
  cursor: pointer;
  font-family: inherit;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
  transition: all 0.2s ease;
}
.module-nav button:hover {
  color: #f8ede0;
  background: rgba(230, 162, 60, 0.12);
}
.module-nav button.active {
  color: #fff;
  background: linear-gradient(135deg, #f0a838, #f8c860);
  box-shadow: 0 8px 18px rgba(240, 168, 56, 0.22);
}

.nav-right { display: flex; align-items: center; gap: 10px; min-width: 170px; justify-content: flex-end; }
.nav-user { font-size: 13px; color: #e8ddd0; }
.nav-logout { color: #c0b0a0 !important; }
.nav-logout:hover { color: #F56C6C !important; }

/* ======== 内容区 ======== */
.content {
  position: relative; z-index: 1;
  flex: 1; padding: 20px;
  overflow-y: auto; color: #f5e5d8;
}
.content h2 { color: #fff8f2; }
.content h3 { color: #f5e5d8; }

/* 卡片毛玻璃 — 与登录卡片一致 */
.content :deep(.el-card) {
  background: rgba(20, 14, 8, 0.3) !important;
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
  border: 2px solid rgba(230, 162, 60, 0.35) !important;
  box-shadow: 0 0 30px rgba(230, 162, 60, 0.2), inset 0 0 30px rgba(230, 162, 60, 0.05) !important;
  color: #f5e5d8;
}
.content :deep(.el-card:hover) {
  box-shadow: 0 0 36px rgba(230, 162, 60, 0.28), inset 0 0 30px rgba(230, 162, 60, 0.08) !important;
}
.content :deep(.el-card__header) {
  color: #fff8f2;
  border-bottom-color: rgba(230, 162, 60, 0.12) !important;
}

/* 表格 */
.content :deep(.el-table) {
  background: rgba(20, 14, 8, 0.3) !important;
  color: #f5e5d8;
}
.content :deep(.el-table__header-wrapper) { background: rgba(20, 14, 8, 0.3); }
.content :deep(.el-table__body-wrapper) { background: rgba(20, 14, 8, 0.3); }
.content :deep(.el-table th.el-table__cell) {
  background-color: rgba(20, 14, 8, 0.3) !important;
  color: #e0d0b8 !important;
  border-bottom-color: rgba(230, 162, 60, 0.18) !important;
}
.content :deep(.el-table td.el-table__cell) {
  background-color: transparent !important;
  color: #f5e5d8 !important;
  border-bottom-color: rgba(230, 162, 60, 0.08) !important;
}
.content :deep(.el-table tr.el-table__row:hover > td.el-table__cell) {
  background-color: rgba(230, 162, 60, 0.08) !important;
}
.content :deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background-color: rgba(20, 14, 8, 0.3) !important;
}
.content :deep(.el-table--border td.el-table__cell),
.content :deep(.el-table--border th.el-table__cell) {
  border-right-color: rgba(230, 162, 60, 0.1) !important;
}
.content :deep(.el-table--border) {
  border-color: rgba(230, 162, 60, 0.15) !important;
}
.content :deep(.el-table__empty-text) { color: #c0b0a0; }
.content :deep(.el-table .el-loading-mask) { background-color: rgba(20, 14, 8, 0.3); }

/* 输入框 / 选择器 */
.content :deep(.el-input__wrapper) {
  background: rgba(20, 14, 8, 0.3);
  box-shadow: 0 0 0 1px rgba(230, 162, 60, 0.15) inset;
}
.content :deep(.el-input__inner) { color: #f5e5d8; }
.content :deep(.el-select .el-input__inner) { color: #f5e5d8; }

/* 描述列表 */
.content :deep(.el-descriptions__label) { color: #e0d0b8; }
.content :deep(.el-descriptions__content) { color: #f5e5d8; }

/* 时间线 */
.content :deep(.el-timeline-item__timestamp) { color: #c0b0a0; }

/* 弹窗 */
.content :deep(.el-dialog) {
  background: rgba(20, 14, 8, 0.3);
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
  border: 2px solid rgba(230, 162, 60, 0.35);
  border-radius: 12px;
  box-shadow: 0 0 30px rgba(230, 162, 60, 0.2), inset 0 0 30px rgba(230, 162, 60, 0.05);
}
.content :deep(.el-dialog__title) { color: #fff8f2; }
.content :deep(.el-dialog__body) { color: #f5e5d8; }

/* 抽屉 (个人信息) — rendered in-place, scoped OK */
:deep(.el-drawer) {
  background: rgba(20, 14, 8, 0.3) !important;
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
}
:deep(.el-drawer__title) { color: #fff8f2; }

/* 分页 */
.content :deep(.el-pagination .btn-prev),
.content :deep(.el-pagination .btn-next),
.content :deep(.el-pager li) {
  background: rgba(20, 14, 8, 0.3) !important;
  color: #e0d0b8 !important;
}
.content :deep(.el-pager li.is-active) {
  background: rgba(230, 162, 60, 0.3) !important;
  color: #fff !important;
}

/* 标签页 */
.content :deep(.el-tabs__item) { color: #c0b0a0; }
.content :deep(.el-tabs__item.is-active) { color: #f0a838; }
.content :deep(.el-tabs__nav-wrap::after) { background-color: rgba(230, 162, 60, 0.1); }

/* Tag 标签 */
.content :deep(.el-tag--info) {
  background-color: rgba(144,147,153,0.15);
  border-color: rgba(144,147,153,0.3);
  color: #b0b8c4;
}

/* 表单标签 */
.content :deep(.el-form-item__label) { color: #e0d0b8; }
.content :deep(.el-checkbox__label) { color: #f5e5d8; }

/* 级联/树选择器 */
.content :deep(.el-tree) { background: transparent; color: #f5e5d8; }
.content :deep(.el-tree-node__content:hover) { background-color: rgba(230,162,60,0.08); }
</style>

<style>
/* === 全局暗色覆盖：所有 Element Plus 弹出层 & 输入组件 === */

/* — 基础 Popper 容器 — */
.el-popper {
  background: rgba(20, 14, 8, 0.3) !important;
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
  border: 1px solid rgba(230, 162, 60, 0.2) !important;
  color: #f5e5d8 !important;
}

/* — 下拉选择器 — */
.el-select-dropdown {
  background: rgba(20, 14, 8, 0.3) !important;
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
  border: 1px solid rgba(230, 162, 60, 0.2) !important;
}
.el-select-dropdown__wrap {
  background: transparent !important;
}
.el-select-dropdown__list {
  background: transparent !important;
}
.el-select-dropdown__item {
  color: #f5e5d8 !important;
  background: transparent !important;
}
.el-select-dropdown__item:hover {
  background: rgba(230, 162, 60, 0.12) !important;
}
.el-select-dropdown__item.is-selected {
  color: #f0a838 !important;
  background: rgba(230, 162, 60, 0.15) !important;
  font-weight: 600;
}
.el-select-dropdown__item.is-hovering {
  background: rgba(230, 162, 60, 0.1) !important;
}
.el-select-dropdown__empty {
  color: #c0b0a0 !important;
}

/* — 选择器专用外壳（Element Plus 2.4+ 新增 el-select__wrapper） — */
.el-select__wrapper {
  background: rgba(20, 14, 8, 0.3) !important;
  box-shadow: 0 0 0 1px rgba(230, 162, 60, 0.15) inset !important;
}
.el-select__wrapper:hover {
  box-shadow: 0 0 0 1px rgba(230, 162, 60, 0.35) inset !important;
}
.el-select.is-focus .el-select__wrapper {
  box-shadow: 0 0 0 1px #f0a838 inset !important;
}
.el-select__placeholder {
  color: #c0b0a0 !important;
}
.el-select__placeholder.is-transparent {
  color: #c0b0a0 !important;
}
.el-select__caret {
  color: #c0b0a0 !important;
}

/* — 输入框全局暗色 — */
.el-input__wrapper {
  background: rgba(20, 14, 8, 0.3) !important;
  box-shadow: 0 0 0 1px rgba(230, 162, 60, 0.15) inset !important;
}
.el-input__wrapper:hover {
  box-shadow: 0 0 0 1px rgba(230, 162, 60, 0.35) inset !important;
}
.el-input.is-focus .el-input__wrapper,
.el-input.is-focus .el-input__wrapper:hover {
  box-shadow: 0 0 0 1px #f0a838 inset !important;
}
.el-input__inner {
  color: #f5e5d8 !important;
  background: transparent !important;
}
.el-input__inner::placeholder {
  color: #c0b0a0 !important;
}
.el-input__suffix .el-icon {
  color: #c0b0a0;
}
/* textarea */
.el-textarea__inner {
  background: rgba(20, 14, 8, 0.3) !important;
  color: #f5e5d8 !important;
  border-color: rgba(230, 162, 60, 0.15) !important;
}
.el-textarea__inner::placeholder {
  color: #c0b0a0 !important;
}
.el-textarea__inner:focus {
  border-color: #f0a838 !important;
}

/* — 多选 tag 标签 — */
.el-select .el-tag {
  background-color: rgba(230, 162, 60, 0.15) !important;
  border-color: rgba(230, 162, 60, 0.3) !important;
  color: #f5e5d8 !important;
}
.el-select .el-tag .el-tag__close {
  color: #c0b0a0 !important;
}
.el-select .el-tag .el-tag__close:hover {
  background-color: rgba(245, 108, 108, 0.3) !important;
  color: #F56C6C !important;
}

/* — Popconfirm 气泡 — */
.el-popconfirm {
  background: rgba(20, 14, 8, 0.3) !important;
  backdrop-filter: blur(3px);
  border: 1px solid rgba(230, 162, 60, 0.2) !important;
}
.el-popconfirm__title {
  color: #f5e5d8 !important;
}
.el-popconfirm__action .el-button--text {
  color: #c0b0a0 !important;
}
.el-popper__arrow::before {
  background: rgba(20, 14, 8, 0.3) !important;
  border: 1px solid rgba(230, 162, 60, 0.2) !important;
}

/* — 消息提示 — */
.el-message {
  background: rgba(20, 14, 8, 0.3) !important;
  backdrop-filter: blur(3px);
  border: 1px solid rgba(230, 162, 60, 0.2) !important;
}
.el-message__content { color: #f5e5d8 !important; }

/* — 消息弹窗 (MessageBox) — */
.el-message-box {
  background: rgba(20, 14, 8, 0.3) !important;
  backdrop-filter: blur(3px);
  border: 1px solid rgba(230, 162, 60, 0.2) !important;
}
.el-message-box__title { color: #fff8f2 !important; }
.el-message-box__message { color: #f5e5d8 !important; }
.el-message-box__input .el-input__inner { color: #f5e5d8 !important; }

/* — 级联选择器 — */
.el-cascader-node {
  color: #f5e5d8 !important;
  background: transparent !important;
}
.el-cascader-node:not(.is-disabled):hover {
  background: rgba(230, 162, 60, 0.1) !important;
}
.el-cascader-node.is-active {
  color: #f0a838 !important;
  background: rgba(230, 162, 60, 0.12) !important;
}
.el-cascader__dropdown {
  background: rgba(20, 14, 8, 0.3) !important;
  border: 1px solid rgba(230, 162, 60, 0.2) !important;
}

/* — 日期选择器 — */
.el-picker-panel {
  background: rgba(20, 14, 8, 0.3) !important;
  border: 1px solid rgba(230, 162, 60, 0.2) !important;
  color: #f5e5d8 !important;
}
.el-date-picker__header-label { color: #f5e5d8 !important; }
.el-date-table th { color: #c0b0a0 !important; }
.el-date-table td { color: #f5e5d8 !important; }
.el-date-table td.available:hover { color: #f0a838 !important; }
.el-date-table td.current:not(.disabled) span { background-color: #f0a838 !important; }
.el-date-table td.next-month, .el-date-table td.prev-month { color: #a09888 !important; }
.el-picker-panel__icon-btn { color: #c0b0a0 !important; }
.el-picker-panel__icon-btn:hover { color: #f0a838 !important; }

/* — 下拉菜单 — */
.el-dropdown-menu {
  background: rgba(20, 14, 8, 0.3) !important;
  border: 1px solid rgba(230, 162, 60, 0.2) !important;
}
.el-dropdown-menu__item {
  color: #f5e5d8 !important;
  background: transparent !important;
}
.el-dropdown-menu__item:hover {
  background: rgba(230, 162, 60, 0.12) !important;
  color: #f0a838 !important;
}

/* — 自动补全 — */
.el-autocomplete-suggestion {
  background: rgba(20, 14, 8, 0.3) !important;
  border: 1px solid rgba(230, 162, 60, 0.2) !important;
}
.el-autocomplete-suggestion li {
  color: #f5e5d8 !important;
}
.el-autocomplete-suggestion li:hover {
  background: rgba(230, 162, 60, 0.12) !important;
}

/* — 穿梭框 — */
.el-transfer-panel {
  background: rgba(20, 14, 8, 0.3) !important;
  border-color: rgba(230, 162, 60, 0.2) !important;
}
</style>
