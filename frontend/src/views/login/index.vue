<template>
  <div class="login-page">
    <div class="login-wrapper">
      <div class="login-card">
        <div class="login-card__header">
          <div class="login-card__logo">
            <IconShield :size="34" />
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
            />
          </el-form-item>

          <el-form-item class="login-card__item login-card__item--row">
            <el-checkbox v-model="rememberUsername">记住用户名</el-checkbox>
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
import { IconShield } from '@/components/icons'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const rememberUsername = ref(false)

const form = reactive({
  username: localStorage.getItem('remember_username') || '',
  password: ''
})

if (form.username) {
  rememberUsername.value = true
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度为3-50个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!formRef.value || loading.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

      loading.value = true
    try {
      const username = form.username.trim()
      await userStore.login(username, form.password)

      localStorage.removeItem('remember_password')
      if (rememberUsername.value) {
        localStorage.setItem('remember_username', username)
      } else {
        localStorage.removeItem('remember_username')
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
  --auth-ink: #0a0a0b;
  --auth-muted: #5f6368;
  --auth-line: rgba(12, 14, 18, 0.1);
  --auth-soft: rgba(255, 255, 255, 0.72);
  --auth-glass: rgba(255, 255, 255, 0.58);

  position: relative;
  isolation: isolate;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  overflow: hidden;
  background:
    linear-gradient(120deg, rgba(255, 255, 255, 0.96), rgba(245, 247, 250, 0.9) 48%, rgba(255, 255, 255, 0.98)),
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
      linear-gradient(115deg, transparent 0 18%, rgba(12, 14, 18, 0.06) 18% 18.5%, transparent 18.5% 54%, rgba(117, 137, 166, 0.13) 54% 54.7%, transparent 54.7%),
      linear-gradient(165deg, rgba(255, 255, 255, 0.82), transparent 42%);
  }

  &::after {
    inset: 10% 8% auto auto;
    width: min(52vw, 620px);
    height: min(42vw, 480px);
    border-radius: 36px;
    border: 1px solid rgba(255, 255, 255, 0.68);
    background:
      linear-gradient(135deg, rgba(255, 255, 255, 0.52), rgba(255, 255, 255, 0.14)),
      linear-gradient(90deg, rgba(12, 14, 18, 0.05), transparent);
    -webkit-backdrop-filter: blur(18px) saturate(1.18);
    backdrop-filter: blur(18px) saturate(1.18);
    box-shadow:
      0 24px 80px rgba(15, 23, 42, 0.08),
      inset 0 1px 0 rgba(255, 255, 255, 0.92);
    transform: rotate(-8deg);
  }
}

.login-wrapper {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 430px;
}

.login-card {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  border-radius: 30px;
  padding: 42px;
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
    top: -28%;
    right: -18%;
    width: 68%;
    height: 54%;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.74), rgba(255, 255, 255, 0) 64%);
    filter: blur(18px);
    opacity: 0.86;
    transform: rotate(18deg);
  }

  &__header {
    text-align: center;
    margin-bottom: 34px;
  }

  &__logo {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 68px;
    height: 68px;
    color: var(--auth-ink);
    border-radius: 22px;
    margin-bottom: 18px;
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

  &__title {
    font-size: 25px;
    font-weight: 700;
    color: var(--auth-ink);
    margin: 0 0 8px;
  }

  &__subtitle {
    font-size: 14px;
    color: var(--auth-muted);
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
      margin-bottom: 22px;
    }
  }

  &__input {
    :deep(.el-input__wrapper) {
      min-height: 50px;
      padding: 4px 16px;
      border-radius: 18px;
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

      &.is-focus {
        background: rgba(255, 255, 255, 0.88);
        transform: translateY(-1px);
        box-shadow:
          0 12px 28px rgba(15, 23, 42, 0.1),
          0 0 0 2px rgba(10, 10, 11, 0.08),
          0 0 0 1px rgba(10, 10, 11, 0.36) inset,
          inset 0 1px 0 rgba(255, 255, 255, 0.96);
      }
    }

    :deep(.el-input__inner) {
      color: var(--auth-ink);
      font-weight: 500;
    }

    :deep(.el-input__prefix) {
      color: rgba(10, 10, 11, 0.72);
    }
  }

  &__btn {
    width: 100%;
    height: 50px;
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

  &__forgot {
    font-size: 14px;
    color: var(--auth-muted);
    cursor: pointer;
    transition: color 200ms ease;

    &:hover {
      color: var(--auth-ink);
    }
  }

  &__footer {
    text-align: center;
    margin-top: 24px;
    padding-top: 22px;
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

  :deep(.el-checkbox__label) {
    color: var(--auth-muted);
  }

  :deep(.el-checkbox__inner) {
    border-radius: 6px;
    border-color: rgba(10, 10, 11, 0.32);
    background: rgba(255, 255, 255, 0.72);
  }

  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    border-color: var(--auth-ink);
    background: var(--auth-ink);
  }

  :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
    color: var(--auth-ink);
  }
}

.login-decoration {
  display: none;
}

@supports not ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
  .login-card,
  .login-card__logo,
  .login-card__input :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.96);
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-card__logo,
  .login-card__input :deep(.el-input__wrapper),
  .login-card__btn,
  .login-card__forgot,
  .login-card__link {
    transition: none;
  }
}

@media (max-width: 480px) {
  .login-page {
    padding: 20px 14px;

    &::after {
      display: none;
    }
  }

  .login-card {
    border-radius: 24px;
    padding: 32px 22px;

    &__title {
      font-size: 22px;
    }
  }
}
</style>
