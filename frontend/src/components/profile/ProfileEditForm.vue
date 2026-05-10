<template>
  <div class="profile-edit">
    <div class="profile-edit__header">
      <AvatarUploader v-model="form.avatar" :size="100" />
      <div class="profile-edit__header-info">
        <h3 class="profile-edit__username">{{ userInfo?.nickname || userInfo?.username }}</h3>
        <p class="profile-edit__meta">
          <span class="profile-edit__role" :class="`profile-edit__role--${userInfo?.role}`">
            {{ userInfo?.role === 'admin' ? '管理员' : '学生' }}
          </span>
          <span class="profile-edit__join">
            加入于 {{ formatDate(userInfo?.createTime) }}
          </span>
        </p>
      </div>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      class="profile-edit__form"
    >
      <div class="profile-edit__section">
        <h4 class="profile-edit__section-title">
          <span class="profile-edit__section-icon">
            <el-icon><User /></el-icon>
          </span>
          基础信息
        </h4>

        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" disabled placeholder="用户名不可修改">
            <template #suffix>
              <el-icon class="profile-edit__disabled-icon"><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="学号" prop="studentNo">
          <el-input v-model="form.studentNo" disabled placeholder="学号不可修改">
            <template #suffix>
              <el-icon class="profile-edit__disabled-icon"><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="form.nickname"
            placeholder="请输入昵称"
            maxlength="20"
            show-word-limit
            clearable
          >
            <template #prefix>
              <el-icon><UserFilled /></el-icon>
            </template>
          </el-input>
        </el-form-item>
      </div>

      <div class="profile-edit__section">
        <h4 class="profile-edit__section-title">
          <span class="profile-edit__section-icon">
            <el-icon><Message /></el-icon>
          </span>
          联系信息
        </h4>

        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="请输入手机号"
            maxlength="11"
            clearable
          >
            <template #prefix>
              <el-icon><Phone /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入邮箱"
            clearable
          >
            <template #prefix>
              <el-icon><Message /></el-icon>
            </template>
          </el-input>
        </el-form-item>
      </div>

      <div class="profile-edit__section">
        <h4 class="profile-edit__section-title">
          <span class="profile-edit__section-icon">
            <el-icon><School /></el-icon>
          </span>
          学校信息
        </h4>

        <el-form-item label="年级" prop="grade">
          <el-select v-model="form.grade" placeholder="请选择年级" class="profile-edit__select">
            <el-option label="大一" value="大一" />
            <el-option label="大二" value="大二" />
            <el-option label="大三" value="大三" />
            <el-option label="大四" value="大四" />
            <el-option label="大五" value="大五" />
            <el-option label="研究生" value="研究生" />
          </el-select>
        </el-form-item>

        <el-form-item label="专业" prop="major">
          <el-input
            v-model="form.major"
            placeholder="请输入专业"
            maxlength="50"
            clearable
          >
            <template #prefix>
              <el-icon><Reading /></el-icon>
            </template>
          </el-input>
        </el-form-item>
      </div>

      <div class="profile-edit__actions">
        <el-button @click="handleReset">重置</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          保存修改
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock, UserFilled, Message, Phone, School, Reading } from '@element-plus/icons-vue'
import AvatarUploader from './AvatarUploader.vue'
import dayjs from 'dayjs'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const formRef = ref<FormInstance>()
const loading = ref(false)
const initialForm = ref<any>({})

const form = reactive({
  username: '',
  studentNo: '',
  nickname: '',
  phone: '',
  email: '',
  grade: '',
  major: '',
  avatar: ''
})

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
  nickname: [
    { min: 2, max: 20, message: '昵称长度为2-20个字符', trigger: 'blur' }
  ],
  phone: [
    { validator: validatePhone, trigger: 'blur' }
  ],
  email: [
    { validator: validateEmail, trigger: 'blur' }
  ]
}

const formatDate = (date: string | undefined) => {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD')
}

const loadUserInfo = () => {
  if (userStore.userInfo) {
    const info = userStore.userInfo as any
    form.username = info.username || ''
    form.studentNo = info.studentNo || ''
    form.nickname = info.nickname || ''
    form.phone = info.phone || ''
    form.email = info.email || ''
    form.grade = info.grade || ''
    form.major = info.major || ''
    form.avatar = info.avatar || ''

    initialForm.value = { ...form }
  }
}

const handleReset = () => {
  if (formRef.value) {
    formRef.value.resetFields()
    Object.assign(form, initialForm.value)
  }
}

onMounted(() => {
  loadUserInfo()
})

watch(userInfo, () => {
  loadUserInfo()
}, { deep: true })

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await userStore.updateUser({
        nickname: form.nickname,
        phone: form.phone || undefined,
        email: form.email || undefined,
        grade: form.grade || undefined,
        major: form.major || undefined,
        avatar: form.avatar || undefined
      })

      ElMessage.success('资料更新成功')
      initialForm.value = { ...form }
    } catch (error: any) {
      ElMessage.error(error.message || '更新失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.profile-edit {
  &__header {
    display: flex;
    align-items: center;
    gap: 20px;
    padding: 24px;
    background: linear-gradient(135deg, hsl(225, 60%, 96%) 0%, hsl(270, 50%, 96%) 100%);
    border-radius: 12px;
    margin-bottom: 28px;
  }

  &__header-info {
    flex: 1;
  }

  &__username {
    margin: 0 0 8px;
    font-size: 20px;
    font-weight: 600;
    color: hsl(220, 10%, 20%);
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 12px;
    margin: 0;
    font-size: 14px;
    color: hsl(220, 10%, 55%);
  }

  &__role {
    padding: 2px 10px;
    border-radius: 10px;
    font-size: 12px;
    color: #fff;

    &--admin {
      background: hsl(0, 65%, 55%);
    }

    &--student {
      background: hsl(225, 60%, 50%);
    }
  }

  &__join {
    font-size: 13px;
  }

  &__section {
    padding: 20px 0;
    border-bottom: 1px dashed hsl(220, 10%, 90%);

    &:last-of-type {
      border-bottom: none;
    }
  }

  &__section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 20px;
    font-size: 15px;
    font-weight: 600;
    color: hsl(220, 10%, 25%);
  }

  &__section-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    background: hsl(225, 60%, 96%);
    border-radius: 6px;
    color: hsl(225, 60%, 50%);

    .el-icon {
      font-size: 16px;
    }
  }

  &__disabled-icon {
    color: hsl(220, 10%, 70%);
  }

  &__select {
    width: 100%;
  }

  &__form {
    :deep(.el-input.is-disabled .el-input__wrapper) {
      background-color: hsl(220, 10%, 96%);
      cursor: not-allowed;

      .el-input__inner {
        color: hsl(220, 10%, 50%);
      }
    }

    :deep(.el-input__wrapper) {
      border-radius: 8px;
      box-shadow: 0 0 0 1px hsl(220, 10%, 85%) inset;
      transition: box-shadow 0.2s ease;
      padding: 8px 16px;

      &:hover:not(.is-focus) {
        box-shadow: 0 0 0 1px hsl(225, 60%, 55%) inset;
      }

      &.is-focus {
        box-shadow: 0 0 0 2px hsla(225, 60%, 55%, 0.2), 0 0 0 1px hsl(225, 60%, 55%) inset;
      }
    }

    :deep(.el-select) {
      width: 100%;
    }

    :deep(.el-form-item__label) {
      font-weight: 500;
      color: hsl(220, 10%, 35%);
    }
  }

  &__actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    padding-top: 24px;
    margin-top: 8px;
    border-top: 1px solid hsl(220, 10%, 92%);

    .el-button--primary {
      min-width: 120px;
      background: linear-gradient(135deg, hsl(225, 60%, 55%), hsl(270, 50%, 45%));
      border: none;
      transition: transform 0.2s ease, box-shadow 0.2s ease;

      &:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px hsla(225, 60%, 55%, 0.4);
      }

      &:active:not(:disabled) {
        transform: translateY(0);
      }
    }
  }
}

@media (max-width: 640px) {
  .profile-edit {
    &__header {
      flex-direction: column;
      text-align: center;
    }

    &__meta {
      flex-direction: column;
      gap: 6px;
    }

    &__form {
      :deep(.el-form-item) {
        margin-bottom: 20px;
      }

      :deep(.el-form-item__label) {
        float: none;
        display: block;
        text-align: left;
        padding: 0 0 8px;
      }

      :deep(.el-form-item__content) {
        margin-left: 0 !important;
      }
    }
  }
}
</style>
