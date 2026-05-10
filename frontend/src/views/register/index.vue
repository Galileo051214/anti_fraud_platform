<template>
  <div class="register-page">
    <div class="register-wrapper">
      <div class="register-card">
        <div class="register-card__header">
          <div class="register-card__logo">
            <svg class="register-card__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
              <line x1="19" y1="8" x2="19" y2="14"/>
              <line x1="22" y1="11" x2="16" y2="11"/>
            </svg>
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
                placeholder="3-20位字母数字组合"
                size="large"
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

          <el-form-item prop="nickname" class="register-card__item">
            <template #label>
              <span class="register-card__label">昵称</span>
            </template>
            <el-input
              v-model="form.nickname"
              placeholder="选填，用于展示"
              size="large"
              :prefix-icon="UserFilled"
            />
          </el-form-item>

          <div class="register-card__row">
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
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' },
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
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await userStore.register({
        username: form.username,
        password: form.password,
        nickname: form.nickname || form.username,
        studentNo: form.studentNo,
        phone: form.phone || undefined,
        email: form.email || undefined,
        grade: form.grade,
        major: form.major || undefined
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
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, hsl(225, 60%, 55%) 0%, hsl(270, 50%, 45%) 100%);
  padding: 40px 20px;
}

.register-wrapper {
  position: relative;
  width: 100%;
  max-width: 560px;
}

.register-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 8px 32px hsla(0, 0, 0, 0.15);
  max-height: 90vh;
  overflow-y: auto;

  &__header {
    text-align: center;
    margin-bottom: 28px;
  }

  &__logo {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 56px;
    height: 56px;
    background: linear-gradient(135deg, hsl(225, 60%, 55%), hsl(270, 50%, 45%));
    border-radius: 14px;
    margin-bottom: 14px;
  }

  &__icon {
    width: 28px;
    height: 28px;
    color: #fff;
  }

  &__title {
    font-size: 22px;
    font-weight: 600;
    color: hsl(220, 10%, 20%);
    margin: 0 0 6px;
  }

  &__subtitle {
    font-size: 14px;
    color: hsl(220, 10%, 55%);
    margin: 0;
  }

  &__form {
    margin-bottom: 0;
  }

  &__row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
  }

  &__item {
    margin-bottom: 16px;

    &--btn {
      margin-top: 8px;
      margin-bottom: 0;
    }

    :deep(.el-form-item__label) {
      padding-bottom: 4px;
    }
  }

  &__label {
    font-size: 14px;
    color: hsl(220, 10%, 30%);
    font-weight: 500;
  }

  &__required {
    color: hsl(0, 70%, 55%);
  }

  &__select {
    width: 100%;
  }

  &__btn {
    width: 100%;
    height: 48px;
    font-size: 16px;
    font-weight: 500;
    border-radius: 8px;
    background: linear-gradient(135deg, hsl(225, 60%, 55%), hsl(270, 50%, 45%));
    border: none;
    transition: transform 0.2s ease, box-shadow 0.2s ease;

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 4px 16px hsla(225, 60%, 55%, 0.4);
    }

    &:active:not(:disabled) {
      transform: translateY(0);
    }
  }

  &__footer {
    text-align: center;
    margin-top: 24px;
    padding-top: 24px;
    border-top: 1px solid hsl(220, 10%, 92%);
  }

  &__tip {
    font-size: 14px;
    color: hsl(220, 10%, 55%);
  }

  &__link {
    font-size: 14px;
    color: hsl(225, 60%, 55%);
    margin-left: 4px;
    text-decoration: none;
    transition: color 0.2s ease;

    &:hover {
      color: hsl(270, 50%, 45%);
      text-decoration: underline;
    }
  }
}

.register-decoration {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;

  &__circle {
    position: absolute;
    border-radius: 50%;
    background: hsla(0, 0%, 100%, 0.1);

    &--1 {
      width: 250px;
      height: 250px;
      top: -80px;
      left: -80px;
    }

    &--2 {
      width: 180px;
      height: 180px;
      bottom: 5%;
      right: -60px;
    }

    &--3 {
      width: 120px;
      height: 120px;
      top: 20%;
      right: 10%;
    }
  }
}

@media (max-width: 640px) {
  .register-card {
    padding: 28px 20px;

    &__row {
      grid-template-columns: 1fr;
      gap: 0;
    }
  }
}
</style>
