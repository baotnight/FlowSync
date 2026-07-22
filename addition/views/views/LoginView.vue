<template>
  <div class="login-container">
    <canvas ref="meshCanvas" class="mesh-canvas" aria-hidden="true"></canvas>
    <div class="bg-glow"></div>
    <el-card class="login-card" shadow="never">
      <div class="login-brand">
        <div class="brand-mark">F</div>
        <div>
          <strong>FlowSync</strong>
          <span>协同管理系统</span>
        </div>
      </div>

      <div class="mode-switch">
        <button :class="{ active: !isRegisterMode }" @click="isRegisterMode = false">登录</button>
        <button :class="{ active: isRegisterMode }" @click="isRegisterMode = true">注册</button>
      </div>

      <div class="login-title">{{ isRegisterMode ? '创建账户' : '欢迎回来' }}</div>
      <div class="login-subtitle">{{ isRegisterMode ? '填写基础信息后加入协同流程' : '登录 FlowSync 工作台' }}</div>

      <el-form :model="authForm" label-position="top">
        <el-form-item label="工号/学号 (数字 ID)" v-if="isRegisterMode" required>
          <el-input v-model.number="authForm.id" placeholder="请输入您的学号或工号" @keyup.enter="handleAuthSubmit" />
        </el-form-item>

        <el-form-item label="用户名">
          <el-input v-model="authForm.username" placeholder="请输入用户名" @keyup.enter="handleAuthSubmit" />
        </el-form-item>

        <el-form-item label="密码">
          <el-input v-model="authForm.password" type="password" placeholder="请输入密码" show-password @keyup.enter="handleAuthSubmit" />
        </el-form-item>

        <template v-if="isRegisterMode">
          <el-form-item label="真实姓名">
            <el-input v-model="authForm.realName" placeholder="请输入您的真实姓名" @keyup.enter="handleAuthSubmit" />
          </el-form-item>

          <el-form-item label="系统角色">
            <el-radio-group v-model="authForm.role" class="role-group">
              <el-radio-button label="负责人">项目负责人</el-radio-button>
              <el-radio-button label="成员">普通成员</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </template>

        <el-form-item class="submit-row">
          <el-button type="primary" :loading="loading" @click="handleAuthSubmit">
            {{ isRegisterMode ? '立即注册' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="switch-link">
        <span @click="toggleMode">
          {{ isRegisterMode ? '已有账号？去登录' : '没有账号？立即注册' }}
        </span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const loading = ref(false)
const isRegisterMode = ref(false)
const meshCanvas = ref(null)
let meshFrameId = 0
let meshResizeHandler = null

const authForm = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  role: '成员'
})

const initMeshBackground = () => {
  const canvas = meshCanvas.value
  const gl = canvas?.getContext('webgl', { alpha: false, antialias: false })
  if (!canvas || !gl) return

  const config = {
    colors: ['#FFD6EA', '#BFE4FF', '#DCCBFF', '#FFF1B8', '#C8F4FF', '#F5D7FF'],
    positions: [[0.22, 0.24], [0.72, 0.24], [0.5, 0.78], [0.16, 0.7], [0.88, 0.58], [0.52, 0.34]],
    radii: [0.52, 0.5, 0.62, 0.5, 0.42, 0.46],
    softness: [0.78, 0.7, 0.74, 0.76, 0.68, 0.7],
    opacities: [1.0, 0.96, 1.0, 0.9, 0.86, 0.92],
    bgColor: '#FFF7FD',
    speed: 0.5,
    shimmer: 0.86
  }

  const resize = () => {
    const ratio = Math.min(window.devicePixelRatio || 1, 2)
    canvas.width = Math.max(1, Math.floor(canvas.clientWidth * ratio))
    canvas.height = Math.max(1, Math.floor(canvas.clientHeight * ratio))
    gl.viewport(0, 0, canvas.width, canvas.height)
  }

  const vertexShader = 'attribute vec2 p;varying vec2 vUv;void main(){vUv=p*0.5+0.5;gl_Position=vec4(p,0,1);}'
  const fragmentShader = `
precision mediump float;
uniform float u_time;
uniform vec4 u_colors[16];
uniform vec2 u_positions[16];
uniform float u_radii[16];
uniform float u_softness[16];
uniform float u_opacities[16];
uniform int u_count;
uniform vec3 u_bgColor;
uniform float u_shimmer;
varying vec2 vUv;

float hash21(vec2 p) {
  p = fract(p * vec2(123.34, 456.21));
  p += dot(p, p + 45.32);
  return fract(p.x * p.y);
}

float noise(vec2 p) {
  vec2 i = floor(p);
  vec2 f = fract(p);
  f = f * f * (3.0 - 2.0 * f);
  return mix(
    mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
    mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), f.x),
    f.y
  );
}

float fbm(vec2 p) {
  float value = 0.0;
  float amplitude = 0.52;
  for (int octave = 0; octave < 5; octave++) {
    value += amplitude * noise(p);
    p = mat2(1.72, 1.18, -1.18, 1.72) * p + 0.17;
    amplitude *= 0.5;
  }
  return value;
}

void main() {
  float t = u_time * 0.00034;
  vec2 aspectUv = vec2(vUv.x * 1.55, vUv.y);
  vec2 flow = vec2(
    fbm(aspectUv * 1.25 + vec2(t * 0.16, -t * 0.11)),
    fbm(aspectUv * 1.18 + vec2(-t * 0.13, t * 0.17) + 3.7)
  );
  vec2 secondaryFlow = vec2(
    fbm(aspectUv * 2.1 + flow * 1.7 - t * 0.09),
    fbm(aspectUv * 1.9 - flow * 1.5 + t * 0.08 + 7.1)
  );
  vec2 uv = vUv + (flow - 0.5) * 0.22 + (secondaryFlow - 0.5) * 0.08;
  vec3 totalColor = vec3(0.0);
  float totalW = 0.0;

  for (int i = 0; i < 16; i++) {
    if (i >= u_count) break;
    vec2 basePos = u_positions[i];
    float phase = float(i) * 1.91;
    vec2 offset = vec2(
      sin(t * (0.21 + float(i) * 0.027) + phase) * 0.12,
      cos(t * (0.17 + float(i) * 0.024) + phase * 1.37) * 0.12
    );
    vec2 delta = uv - basePos - offset;
    delta.x *= 0.82 + 0.18 * sin(phase + t * 0.11);
    delta.y *= 1.08 + 0.16 * cos(phase - t * 0.13);
    float edgeNoise = fbm(aspectUv * 3.1 + flow * 2.4 + phase + t * 0.06) - 0.5;
    float dist = length(delta) + edgeNoise * (0.13 + u_shimmer * 0.05);
    float spread = u_radii[i] * 0.42 + u_softness[i] * 0.18;
    float strength = exp(-(dist * dist) / (2.0 * spread * spread));
    float weight = strength * u_opacities[i];
    totalColor += u_colors[i].rgb * weight;
    totalW += weight;
  }

  vec3 finalColor = u_bgColor;
  if (totalW > 0.0) {
    finalColor = mix(u_bgColor, totalColor / totalW, min(0.94, totalW * 0.82));
  }
  float vapor = fbm(aspectUv * 4.2 + secondaryFlow * 2.0 + t * 0.05);
  finalColor += (vapor - 0.5) * 0.055 * u_shimmer;
  float vignette = smoothstep(1.05, 0.2, distance(vUv, vec2(0.5)));
  finalColor = mix(finalColor * 0.96, finalColor, vignette);
  gl_FragColor = vec4(finalColor, 1.0);
}`

  const compile = (type, source) => {
    const shader = gl.createShader(type)
    gl.shaderSource(shader, source)
    gl.compileShader(shader)
    return shader
  }

  const program = gl.createProgram()
  gl.attachShader(program, compile(gl.VERTEX_SHADER, vertexShader))
  gl.attachShader(program, compile(gl.FRAGMENT_SHADER, fragmentShader))
  gl.linkProgram(program)
  gl.useProgram(program)

  const buffer = gl.createBuffer()
  gl.bindBuffer(gl.ARRAY_BUFFER, buffer)
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]), gl.STATIC_DRAW)

  const positionLocation = gl.getAttribLocation(program, 'p')
  gl.enableVertexAttribArray(positionLocation)
  gl.vertexAttribPointer(positionLocation, 2, gl.FLOAT, false, 0, 0)

  const hexToRgb = (hex) => [
    parseInt(hex.slice(1, 3), 16) / 255,
    parseInt(hex.slice(3, 5), 16) / 255,
    parseInt(hex.slice(5, 7), 16) / 255
  ]

  const colors = new Float32Array(64)
  const positions = new Float32Array(32)
  const radii = new Float32Array(16)
  const softness = new Float32Array(16)
  const opacities = new Float32Array(16)

  config.colors.forEach((color, index) => {
    const rgb = hexToRgb(color)
    colors[index * 4] = rgb[0]
    colors[index * 4 + 1] = rgb[1]
    colors[index * 4 + 2] = rgb[2]
    colors[index * 4 + 3] = config.opacities[index]
    positions[index * 2] = config.positions[index][0]
    positions[index * 2 + 1] = config.positions[index][1]
    radii[index] = config.radii[index]
    softness[index] = config.softness[index]
    opacities[index] = config.opacities[index]
  })

  const bgRgb = hexToRgb(config.bgColor)
  const startedAt = performance.now()

  const draw = () => {
    gl.uniform1f(gl.getUniformLocation(program, 'u_time'), (performance.now() - startedAt) * config.speed)
    gl.uniform4fv(gl.getUniformLocation(program, 'u_colors'), colors)
    gl.uniform2fv(gl.getUniformLocation(program, 'u_positions'), positions)
    gl.uniform1fv(gl.getUniformLocation(program, 'u_radii'), radii)
    gl.uniform1fv(gl.getUniformLocation(program, 'u_softness'), softness)
    gl.uniform1fv(gl.getUniformLocation(program, 'u_opacities'), opacities)
    gl.uniform1i(gl.getUniformLocation(program, 'u_count'), config.colors.length)
    gl.uniform3f(gl.getUniformLocation(program, 'u_bgColor'), bgRgb[0], bgRgb[1], bgRgb[2])
    gl.uniform1f(gl.getUniformLocation(program, 'u_shimmer'), config.shimmer)
    gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4)
    meshFrameId = requestAnimationFrame(draw)
  }

  meshResizeHandler = resize
  resize()
  window.addEventListener('resize', meshResizeHandler)
  draw()
}

onMounted(() => {
  initMeshBackground()
})

onBeforeUnmount(() => {
  if (meshFrameId) cancelAnimationFrame(meshFrameId)
  if (meshResizeHandler) window.removeEventListener('resize', meshResizeHandler)
})

const toggleMode = () => {
  isRegisterMode.value = !isRegisterMode.value
  authForm.id = null
  authForm.username = ''
  authForm.password = ''
  authForm.realName = ''
}

const handleAuthSubmit = async () => {
  if (isRegisterMode.value) {
    handleRegister()
  } else {
    handleLogin()
  }
}

const handleLogin = async () => {
  if (!authForm.username || !authForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  
  loading.value = true
  try {
    const response = await axios.post('/api/auth/login', {
      username: authForm.username,
      password: authForm.password
    })
    
    const resData = response.data
    if (resData.success) {
      ElMessage.success('登录成功！')
      sessionStorage.setItem('currentUser', JSON.stringify(resData.data))
      router.push('/home')
    } else {
      ElMessage.error(resData.message || '登录失败')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('网络错误，请检查后端服务是否开启！')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!authForm.id || !authForm.username || !authForm.password || !authForm.realName) {
    ElMessage.warning('请将注册信息填写完整（包含数字工号/学号）！')
    return
  }

  if (typeof authForm.id !== 'number') {
    ElMessage.warning('工号/学号必须是纯数字！')
    return
  }

  loading.value = true
  try {
    const response = await axios.post('/api/auth/register', {
      id: authForm.id,
      username: authForm.username,
      password: authForm.password,
      realName: authForm.realName,
      role: authForm.role
    })

    if (response.data.success) {
      ElMessage.success('注册成功！已为您自动切换到登录页')
      isRegisterMode.value = false
      authForm.password = ''
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('注册接口异常，请确认后端是否存活！')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 24px;
  overflow: hidden;
  color: #172033;
  background:
    radial-gradient(circle at 18% 12%, rgba(255, 214, 234, 0.36), transparent 30%),
    radial-gradient(circle at 82% 14%, rgba(191, 228, 255, 0.34), transparent 28%),
    linear-gradient(135deg, #fff8fb 0%, #f3f7ff 48%, #fff9e8 100%) !important;
}

.bg-glow {
  position: absolute;
  inset: 0;
  width: auto;
  height: auto;
  border-radius: 0;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.2), transparent 44%, rgba(255, 255, 255, 0.18)),
    radial-gradient(circle at 50% 108%, rgba(255, 255, 255, 0.46), transparent 44%),
    linear-gradient(135deg, rgba(255, 214, 234, 0.12), rgba(191, 228, 255, 0.08), rgba(255, 241, 184, 0.12));
  filter: none;
  pointer-events: none;
  animation: micaSheen 7s ease-in-out infinite alternate;
}

.mesh-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
  filter: saturate(1.28) contrast(1.04) brightness(1.04);
}

.login-card {
  position: relative;
  z-index: 1;
  width: min(420px, 100%);
  padding: 24px 26px;
  border: 1px solid rgba(255, 255, 255, 0.72) !important;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.74) !important;
  box-shadow: 0 24px 64px rgba(129, 117, 160, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.82) !important;
  backdrop-filter: blur(22px) saturate(155%);
  -webkit-backdrop-filter: blur(22px) saturate(155%);
  animation: cardIn 0.32s ease both;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  color: #fff;
  background: linear-gradient(135deg, #9fbfff, #d8c7ff 52%, #ffd0e4);
  font-weight: 900;
}

.login-brand strong {
  display: block;
  color: #172033;
  font-size: 18px;
}

.login-brand span {
  color: #6f7d91;
  font-size: 12px;
}

.mode-switch {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
  padding: 4px;
  margin-bottom: 20px;
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.46);
}

.mode-switch button {
  min-height: 34px;
  border: 0;
  border-radius: 10px;
  color: #526174;
  background: transparent;
  cursor: pointer;
  font-weight: 700;
  transition: background 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.mode-switch button.active {
  color: #6d62c7;
  background: #fff;
  box-shadow: 0 8px 18px rgba(31, 43, 61, 0.08);
}

.login-title {
  margin-bottom: 6px;
  color: #172033 !important;
  font-size: 26px;
  font-weight: 900;
}

.login-subtitle {
  margin-bottom: 22px;
  color: #6f7d91 !important;
  font-size: 14px;
}

:deep(.el-form-item) {
  margin-bottom: 16px;
}

:deep(.el-form-item__label) {
  color: #34435a !important;
  font-weight: 700;
  padding-bottom: 5px;
}

:deep(.el-input__wrapper) {
  min-height: 42px;
  border-radius: 11px;
  background: #ffffff !important;
  box-shadow: 0 0 0 1px rgba(30, 43, 64, 0.12) inset !important;
  transition: box-shadow 0.18s ease;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #9f8cff inset, 0 0 0 4px rgba(159, 140, 255, 0.14) !important;
}

.role-group {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}

:deep(.el-radio-button__inner) {
  width: 100%;
  border-radius: 10px !important;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #8f83e8 !important;
  border-color: #8f83e8 !important;
  color: #fff !important;
}

.submit-row {
  margin-top: 8px;
}

.submit-row :deep(.el-button) {
  width: 100%;
  min-height: 42px;
}

:deep(.el-button--primary) {
  border: 0;
  border-radius: 11px;
  color: #ffffff;
  background: linear-gradient(135deg, #8ebcff, #a995ff 55%, #ffb8d7) !important;
  box-shadow: 0 12px 26px rgba(159, 140, 255, 0.24);
}

.switch-link {
  text-align: center;
  margin-top: 16px;
}

.switch-link span {
  color: #6d62c7 !important;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes micaSheen {
  from {
    opacity: 0.72;
    transform: translate3d(-2%, -1%, 0) scale(1.02);
  }
  to {
    opacity: 0.94;
    transform: translate3d(2%, 1%, 0) scale(1.04);
  }
}

@media (max-width: 520px) {
  .login-container {
    padding: 14px;
  }

  .login-card {
    padding: 20px;
  }
}
</style>
