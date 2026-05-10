<template>
  <div class="login-page">
    <div class="login-wrapper">
      <div class="login-card">
        <div class="login-card__header">
          <div class="login-card__logo">
            <svg class="login-card__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
          </div>
          <h1 class="login-card__title">反诈骗学习平台</h1>
          <p class="login-card__subtitle">守护校园安全，提升防骗意识</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          @submit.prevent="handleLogin"
          class="login-card__form"
        >
          <el-form-item prop="username" class="login-card__item">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
              class="login-card__input"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item prop="password" class="login-card__item">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              class="login-card__input"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item class="login-card__item login-card__item--row">
            <el-checkbox v-model="rememberPwd">记住密码</el-checkbox>
            <span class="login-card__forgot">忘记密码？</span>
          </el-form-item>

          <el-form-item class="login-card__item">
            <el-button
              type="primary"
              :loading="loading"
              class="login-card__btn"
              @click="handleLogin"
            >
              <span v-if="!loading">登 录</span>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-card__footer">
          <span class="login-card__tip">还没有账号？</span>
          <router-link to="/register" class="login-card__link">立即注册</router-link>
        </div>
      </div>

      <div class="login-decoration">
        <div class="login-decoration__circle login-decoration__circle--1"></div>
        <div class="login-decoration__circle login-decoration__circle--2"></div>
        <div class="login-decoration__circle login-decoration__circle--3"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const rememberPwd = ref(false)

const form = reactive({
  username: localStorage.getItem('remember_username') || '',
  password: localStorage.getItem('remember_password') || ''
})

if (form.username) {
  rememberPwd.value = true
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await userStore.login(form.username, form.password)

      if (rememberPwd.value) {
        localStorage.setItem('remember_username', form.username)
        localStorage.setItem('remember_password', form.password)
      } else {
        localStorage.removeItem('remember_username')
        localStorage.removeItem('remember_password')
      }

      ElMessage.success('登录成功')
      const redirect = route.query.redirect as string
      if (redirect) {
        router.push(redirect)
      } else if (userStore.isAdmin) {
        router.push('/admin/dashboard')
      } else {
        router.push('/home')
      }
    } catch (error: any) {
      ElMessage.error(error.message || '登录失败，请检查用户名和密码')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, hsl(225, 60%, 55%) 0%, hsl(270, 50%, 45%) 100%);
  padding: 20px;
}

.login-wrapper {
  position: relative;
  width: 100%;
  max-width: 420px;
}

.login-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 8px 32px hsla(0, 0, 0, 0.15);

  &__header {
    text-align: center;
    margin-bottom: 32px;
  }

  &__logo {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 64px;
    height: 64px;
    background: linear-gradient(135deg, hsl(225, 60%, 55%), hsl(270, 50%, 45%));
    border-radius: 16px;
    margin-bottom: 16px;
  }

  &__icon {
    width: 32px;
    height: 32px;
    color: #fff;
  }

  &__title {
    font-size: 24px;
    font-weight: 600;
    color: hsl(220, 10%, 20%);
    margin: 0 0 8px;
  }

  &__subtitle {
    font-size: 14px;
    color: hsl(220, 10%, 55%);
    margin: 0;
  }

  &__form {
    margin-bottom: 0;
  }

  &__item {
    margin-bottom: 20px;

    &--row {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
  }

  &__input {
    :deep(.el-input__wrapper) {
      padding: 4px 16px;
      border-radius: 8px;
      box-shadow: 0 0 0 1px hsl(220, 10%, 85%) inset;
      transition: box-shadow 0.2s ease;

      &:hover {
        box-shadow: 0 0 0 1px hsl(225, 60%, 55%) inset;
      }

      &.is-focus {
        box-shadow: 0 0 0 2px hsla(225, 60%, 55%, 0.2), 0 0 0 1px hsl(225, 60%, 55%) inset;
      }
    }
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

  &__forgot {
    font-size: 14px;
    color: hsl(220, 10%, 55%);
    cursor: pointer;
    transition: color 0.2s ease;

    &:hover {
      color: hsl(225, 60%, 55%);
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

.login-decoration {
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
      width: 300px;
      height: 300px;
      top: -100px;
      right: -100px;
    }

    &--2 {
      width: 200px;
      height: 200px;
      bottom: 10%;
      left: -80px;
    }

    &--3 {
      width: 150px;
      height: 150px;
      bottom: -50px;
      right: 20%;
    }
  }
}

@media (max-width: 480px) {
  .login-card {
    padding: 32px 24px;

    &__title {
      font-size: 20px;
    }
  }
}
</style>
