<template>
  <div class="register-page">
    <div class="register-wrapper">
      <div class="register-card">
        <div class="register-card__header">
          <div class="register-card__logo">
            <el-icon class="register-card__icon"><UserFilled /></el-icon>
          </div>
          <h1 class="register-card__title">用户注册</h1>
          <p class="register-card__subtitle">加入反诈骗学习平台</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          @submit.prevent="handleRegister"
          class="register-card__form"
          label-position="top"
        >
          <div class="register-card__row">
            <el-form-item prop="username" class="register-card__item">
              <template #label>
                <span class="register-card__label">用户名</span>
              </template>
              <el-input
                v-model="form.username"
                placeholder="3-50位字母数字组合"
                size="large"
                maxlength="50"
                :prefix-icon="User"
              />
            </el-form-item>

            <el-form-item prop="studentNo" class="register-card__item">
              <template #label>
                <span class="register-card__label">学号 <span class="register-card__required">*</span></span>
              </template>
              <el-input
                v-model="form.studentNo"
                placeholder="请输入学号"
                size="large"
                maxlength="30"
                :prefix-icon="Postcard"
              />
            </el-form-item>
          </div>

          <div class="register-card__row">
            <el-form-item prop="password" class="register-card__item">
              <template #label>
                <span class="register-card__label">密码</span>
              </template>
              <el-input
                v-model="form.password"
                type="password"
                placeholder="至少6位字符"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>

            <el-form-item prop="confirmPassword" class="register-card__item">
              <template #label>
                <span class="register-card__label">确认密码</span>
              </template>
              <el-input
                v-model="form.confirmPassword"
                type="password"
                placeholder="再次输入密码"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
          </div>

          <div class="register-card__row register-card__row--three">
            <el-form-item prop="nickname" class="register-card__item">
              <template #label>
                <span class="register-card__label">昵称</span>
              </template>
              <el-input
                v-model="form.nickname"
                placeholder="选填，用于展示"
                size="large"
                maxlength="50"
                :prefix-icon="UserFilled"
              />
            </el-form-item>

            <el-form-item prop="phone" class="register-card__item">
              <template #label>
                <span class="register-card__label">手机号</span>
              </template>
              <el-input
                v-model="form.phone"
                placeholder="选填"
                size="large"
                :prefix-icon="Phone"
              />
            </el-form-item>

            <el-form-item prop="email" class="register-card__item">
              <template #label>
                <span class="register-card__label">邮箱</span>
              </template>
              <el-input
                v-model="form.email"
                placeholder="选填"
                size="large"
                :prefix-icon="Message"
              />
            </el-form-item>
          </div>

          <div class="register-card__row">
            <el-form-item prop="grade" class="register-card__item">
              <template #label>
                <span class="register-card__label">年级 <span class="register-card__required">*</span></span>
              </template>
              <el-select
                v-model="form.grade"
                placeholder="请选择年级"
                size="large"
                class="register-card__select"
              >
                <el-option label="大一" value="大一" />
                <el-option label="大二" value="大二" />
                <el-option label="大三" value="大三" />
                <el-option label="大四" value="大四" />
                <el-option label="大五" value="大五" />
                <el-option label="研究生" value="研究生" />
              </el-select>
            </el-form-item>

            <el-form-item prop="major" class="register-card__item">
              <template #label>
                <span class="register-card__label">专业</span>
              </template>
              <el-input
                v-model="form.major"
                placeholder="请输入专业"
                size="large"
                maxlength="100"
                :prefix-icon="Reading"
              />
            </el-form-item>
          </div>

          <el-form-item class="register-card__item register-card__item--btn">
            <el-button
              type="primary"
              :loading="loading"
              class="register-card__btn"
              @click="handleRegister"
            >
              <span v-if="!loading">注 册</span>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="register-card__footer">
          <span class="register-card__tip">已有账号？</span>
          <router-link to="/login" class="register-card__link">立即登录</router-link>
        </div>
      </div>

      <div class="register-decoration">
        <div class="register-decoration__circle register-decoration__circle--1"></div>
        <div class="register-decoration__circle register-decoration__circle--2"></div>
        <div class="register-decoration__circle register-decoration__circle--3"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { User, Lock, Postcard, UserFilled, Phone, Message, Reading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  studentNo: '',
  phone: '',
  email: '',
  grade: '',
  major: ''
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validatePhone = (_rule: any, value: string, callback: any) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

const validateEmail = (_rule: any, value: string, callback: any) => {
  if (value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
    callback(new Error('邮箱格式不正确'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度为3-50个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  studentNo: [
    { required: true, message: '请输入学号', trigger: 'blur' }
  ],
  grade: [
    { required: true, message: '请选择年级', trigger: 'change' }
  ],
  phone: [
    { validator: validatePhone, trigger: 'blur' }
  ],
  email: [
    { validator: validateEmail, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  if (!formRef.value || loading.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const username = form.username.trim()
      const nickname = form.nickname.trim()
      const studentNo = form.studentNo.trim()
      const phone = form.phone.trim()
      const email = form.email.trim()
      const major = form.major.trim()

      await userStore.register({
        username,
        password: form.password,
        nickname: nickname || username,
        studentNo,
        phone: phone || undefined,
        email: email || undefined,
        grade: form.grade,
        major: major || undefined
      })

      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (error: any) {
      ElMessage.error(error.message || '注册失败，请稍后重试')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.register-page {
  --auth-ink: #0a0a0b;
  --auth-muted: #5f6368;
  --auth-line: rgba(12, 14, 18, 0.1);
  --auth-glass: rgba(255, 255, 255, 0.6);
  --auth-danger: #d92d20;

  position: relative;
  isolation: isolate;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 20px;
  overflow: hidden;
  background:
    linear-gradient(120deg, rgba(255, 255, 255, 0.97), rgba(245, 247, 250, 0.91) 46%, rgba(255, 255, 255, 0.98)),
    repeating-linear-gradient(90deg, rgba(10, 10, 11, 0.045) 0 1px, transparent 1px 72px),
    repeating-linear-gradient(0deg, rgba(10, 10, 11, 0.04) 0 1px, transparent 1px 72px);

  &::before,
  &::after {
    content: "";
    position: absolute;
    pointer-events: none;
    z-index: -1;
  }

  &::before {
    inset: 0;
    background:
      linear-gradient(64deg, transparent 0 22%, rgba(12, 14, 18, 0.055) 22% 22.6%, transparent 22.6% 68%, rgba(117, 137, 166, 0.12) 68% 68.7%, transparent 68.7%),
      linear-gradient(170deg, rgba(255, 255, 255, 0.84), transparent 46%);
  }

  &::after {
    inset: auto auto 8% 5%;
    width: min(56vw, 680px);
    height: min(44vw, 500px);
    border-radius: 38px;
    border: 1px solid rgba(255, 255, 255, 0.68);
    background:
      linear-gradient(135deg, rgba(255, 255, 255, 0.56), rgba(255, 255, 255, 0.16)),
      linear-gradient(90deg, rgba(12, 14, 18, 0.05), transparent);
    -webkit-backdrop-filter: blur(18px) saturate(1.18);
    backdrop-filter: blur(18px) saturate(1.18);
    box-shadow:
      0 24px 80px rgba(15, 23, 42, 0.08),
      inset 0 1px 0 rgba(255, 255, 255, 0.92);
    transform: rotate(7deg);
  }
}

.register-wrapper {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 780px;
}

.register-card {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  border-radius: 30px;
  padding: 30px 40px 26px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.82), rgba(255, 255, 255, 0.5) 48%, rgba(245, 247, 250, 0.64)),
    var(--auth-glass);
  -webkit-backdrop-filter: blur(24px) saturate(1.24);
  backdrop-filter: blur(24px) saturate(1.24);
  box-shadow:
    0 24px 80px rgba(15, 23, 42, 0.16),
    0 2px 10px rgba(15, 23, 42, 0.06),
    inset 0 1px 1px rgba(255, 255, 255, 0.96),
    inset 0 -1px 1px rgba(15, 23, 42, 0.08);
  &::before,
  &::after {
    content: "";
    position: absolute;
    pointer-events: none;
    border-radius: inherit;
    z-index: -1;
  }

  &::before {
    inset: 10px;
    border: 1px solid rgba(255, 255, 255, 0.42);
    box-shadow:
      inset 0 12px 22px rgba(255, 255, 255, 0.46),
      inset 0 -18px 28px rgba(12, 14, 18, 0.06);
  }

  &::after {
    top: -22%;
    right: -14%;
    width: 56%;
    height: 42%;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.74), rgba(255, 255, 255, 0) 64%);
    filter: blur(18px);
    opacity: 0.86;
    transform: rotate(18deg);
  }

  &__header {
    text-align: center;
    margin-bottom: 20px;
  }

  &__logo {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 54px;
    height: 54px;
    color: var(--auth-ink);
    border-radius: 19px;
    margin-bottom: 12px;
    border: 1px solid rgba(255, 255, 255, 0.72);
    background:
      linear-gradient(145deg, rgba(255, 255, 255, 0.9), rgba(245, 247, 250, 0.62)),
      rgba(255, 255, 255, 0.52);
    -webkit-backdrop-filter: blur(14px);
    backdrop-filter: blur(14px);
    box-shadow:
      0 14px 34px rgba(15, 23, 42, 0.12),
      inset 0 1px 0 rgba(255, 255, 255, 0.96),
      inset 0 -1px 0 rgba(12, 14, 18, 0.08);
    transition: transform 260ms ease, box-shadow 260ms ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow:
        0 18px 42px rgba(15, 23, 42, 0.14),
        inset 0 1px 0 rgba(255, 255, 255, 0.96),
        inset 0 -1px 0 rgba(12, 14, 18, 0.08);
    }
  }

  &__icon {
    font-size: 28px;
  }

  &__title {
    font-size: 24px;
    font-weight: 700;
    color: var(--auth-ink);
    margin: 0 0 6px;
  }

  &__subtitle {
    font-size: 13px;
    color: var(--auth-muted);
    margin: 0;
  }

  &__form {
    margin-bottom: 0;
  }

  &__row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px;

    &--three {
      grid-template-columns: 1.15fr 1fr 1.15fr;
    }
  }

  &__item {
    margin-bottom: 12px;

    &--btn {
      margin-top: 4px;
      margin-bottom: 0;
    }

    :deep(.el-form-item__label) {
      padding-bottom: 4px;
    }
  }

  &__label {
    font-size: 13px;
    color: rgba(10, 10, 11, 0.78);
    font-weight: 600;
  }

  &__required {
    color: var(--auth-danger);
  }

  &__select {
    width: 100%;
  }

  :deep(.el-input__wrapper),
  :deep(.el-select__wrapper) {
    min-height: 44px;
    padding: 4px 16px;
    border-radius: 15px;
    background: rgba(255, 255, 255, 0.64);
    -webkit-backdrop-filter: blur(12px) saturate(1.12);
    backdrop-filter: blur(12px) saturate(1.12);
    box-shadow:
      0 0 0 1px var(--auth-line) inset,
      inset 0 1px 0 rgba(255, 255, 255, 0.9);
    transition: transform 220ms ease, box-shadow 220ms ease, background-color 220ms ease;

    &:hover {
      background: rgba(255, 255, 255, 0.78);
      box-shadow:
        0 8px 24px rgba(15, 23, 42, 0.08),
        0 0 0 1px rgba(12, 14, 18, 0.18) inset,
        inset 0 1px 0 rgba(255, 255, 255, 0.92);
    }

    &.is-focus,
    &.is-focused {
      background: rgba(255, 255, 255, 0.88);
      transform: translateY(-1px);
      box-shadow:
        0 12px 28px rgba(15, 23, 42, 0.1),
        0 0 0 2px rgba(10, 10, 11, 0.08),
        0 0 0 1px rgba(10, 10, 11, 0.36) inset,
        inset 0 1px 0 rgba(255, 255, 255, 0.96);
    }
  }

  :deep(.el-input__inner),
  :deep(.el-select__placeholder),
  :deep(.el-select__selected-item) {
    color: var(--auth-ink);
    font-weight: 500;
  }

  :deep(.el-input__prefix),
  :deep(.el-select__prefix),
  :deep(.el-select__suffix) {
    color: rgba(10, 10, 11, 0.72);
  }

  &__btn {
    width: 100%;
    height: 48px;
    font-size: 16px;
    font-weight: 700;
    border-radius: 999px;
    border: 1px solid rgba(10, 10, 11, 0.88);
    background:
      linear-gradient(180deg, rgba(255, 255, 255, 0.14), rgba(255, 255, 255, 0)),
      #0a0a0b;
    box-shadow:
      0 16px 36px rgba(10, 10, 11, 0.22),
      inset 0 1px 0 rgba(255, 255, 255, 0.22);
    transition: transform 220ms ease, box-shadow 220ms ease, background-color 220ms ease;

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      background:
        linear-gradient(180deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0)),
        #161719;
      box-shadow:
        0 20px 44px rgba(10, 10, 11, 0.26),
        inset 0 1px 0 rgba(255, 255, 255, 0.24);
    }

    &:active:not(:disabled) {
      transform: translateY(1px) scale(0.99);
      box-shadow:
        0 10px 24px rgba(10, 10, 11, 0.2),
        inset 0 1px 0 rgba(255, 255, 255, 0.18);
    }
  }

  &__footer {
    text-align: center;
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid rgba(12, 14, 18, 0.08);
  }

  &__tip {
    font-size: 14px;
    color: var(--auth-muted);
  }

  &__link {
    font-size: 14px;
    color: var(--auth-ink);
    font-weight: 700;
    margin-left: 4px;
    text-decoration: none;
    transition: opacity 200ms ease;

    &:hover {
      opacity: 0.72;
      text-decoration: underline;
    }
  }

  :deep(.el-form-item__error) {
    color: var(--auth-danger);
    padding-top: 5px;
  }
}

.register-decoration {
  display: none;
}

@supports not ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
  .register-card,
  .register-card__logo,
  .register-card :deep(.el-input__wrapper),
  .register-card :deep(.el-select__wrapper) {
    background: rgba(255, 255, 255, 0.96);
  }
}

@media (prefers-reduced-motion: reduce) {
  .register-card__logo,
  .register-card :deep(.el-input__wrapper),
  .register-card :deep(.el-select__wrapper),
  .register-card__btn,
  .register-card__link {
    transition: none;
  }
}

@media (max-width: 640px) {
  .register-page {
    padding: 20px 14px;
    align-items: flex-start;
    overflow-y: auto;

    &::after {
      display: none;
    }
  }

  .register-card {
    overflow: visible;
    border-radius: 24px;
    padding: 28px 20px 24px;

    &__row {
      grid-template-columns: 1fr;
      gap: 0;
    }

    &__title {
      font-size: 22px;
    }
  }
}
</style>
