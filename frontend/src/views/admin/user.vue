<template>
  <div class="admin-page">
    <h2>用户管理</h2>
    <div class="admin-page__search-bar search-bar">
      <el-input v-model="keyword" placeholder="搜索用户名/学号" clearable class="search-bar__input" @keyup.enter="handleSearch" />
      <el-select v-model="roleFilter" placeholder="选择角色" clearable class="search-bar__select">
        <el-option label="学生" value="student" />
        <el-option label="管理员" value="admin" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="选择状态" clearable class="search-bar__select">
        <el-option label="正常" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" :icon="Search" :loading="loading" @click="handleSearch">搜索</el-button>
    </div>
    <el-table v-loading="loading" :data="userList" border stripe class="admin-page__table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="studentNo" label="学号">
        <template #default="{ row }">{{ row.studentNo || '-' }}</template>
      </el-table-column>
      <el-table-column prop="grade" label="年级">
        <template #default="{ row }">{{ row.grade || '-' }}</template>
      </el-table-column>
      <el-table-column prop="major" label="专业">
        <template #default="{ row }">{{ row.major || '-' }}</template>
      </el-table-column>
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'admin' ? 'danger' : 'primary'" size="small">
            {{ row.role === 'admin' ? '管理员' : '学生' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="120">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" :loading="toggleLoading[row.id]" @click="handleToggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="admin-page__pagination"
      @current-change="loadUsers"
      @size-change="handleSizeChange"
    />

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑用户" width="480px" destroy-on-close @close="resetEditForm">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="editForm.studentNo" disabled />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="年级" prop="grade">
          <el-input v-model="editForm.grade" placeholder="请输入年级" />
        </el-form-item>
        <el-form-item label="专业" prop="major">
          <el-input v-model="editForm.major" placeholder="请输入专业" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  getUserList,
  getUserById,
  adminUpdateUser,
  enableUser,
  disableUser,
  type UserVO,
  type AdminUpdateUserRequest
} from '@/api/user'

const keyword = ref('')
const roleFilter = ref('')
const statusFilter = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const userList = ref<UserVO[]>([])
const toggleLoading = ref<Record<number, boolean>>({})

const editVisible = ref(false)
const editFormRef = ref<FormInstance>()
const editSubmitting = ref(false)
const editingUserId = ref(0)
const editForm = reactive({
  username: '',
  nickname: '',
  studentNo: '',
  phone: '',
  email: '',
  grade: '',
  major: ''
})
const editRules: FormRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

function formatDate(val: string | undefined): string {
  if (!val) return '-'
  const d = new Date(val)
  return isNaN(d.getTime()) ? val : d.toISOString().slice(0, 10)
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await getUserList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      role: roleFilter.value || undefined,
      status: statusFilter.value ?? undefined
    })
    userList.value = res.records || []
    total.value = res.total ?? 0
  } catch {
    userList.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadUsers()
}

function handleSizeChange() {
  currentPage.value = 1
  loadUsers()
}

async function handleEdit(row: UserVO) {
  editingUserId.value = row.id
  editForm.username = row.username
  editForm.nickname = row.nickname || ''
  editForm.studentNo = row.studentNo || '-'
  editForm.phone = row.phone || ''
  editForm.email = row.email || ''
  editForm.grade = row.grade || ''
  editForm.major = row.major || ''
  editVisible.value = true
  try {
    const detail = await getUserById(row.id)
    editForm.nickname = detail.nickname || ''
    editForm.phone = detail.phone || ''
    editForm.email = detail.email || ''
    editForm.grade = detail.grade || ''
    editForm.major = detail.major || ''
  } catch {
    ElMessage.error('获取用户详情失败')
  }
}

function resetEditForm() {
  editForm.username = ''
  editForm.nickname = ''
  editForm.studentNo = ''
  editForm.phone = ''
  editForm.email = ''
  editForm.grade = ''
  editForm.major = ''
  editingUserId.value = 0
  editFormRef.value?.resetFields()
}

async function submitEdit() {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    editSubmitting.value = true
    try {
      const data: AdminUpdateUserRequest = {
        nickname: editForm.nickname,
        phone: editForm.phone || undefined,
        email: editForm.email || undefined,
        grade: editForm.grade || undefined,
        major: editForm.major || undefined
      }
      await adminUpdateUser(editingUserId.value, data)
      ElMessage.success('更新成功')
      editVisible.value = false
      loadUsers()
    } finally {
      editSubmitting.value = false
    }
  })
}

async function handleToggleStatus(row: UserVO) {
  toggleLoading.value = { ...toggleLoading.value, [row.id]: true }
  try {
    if (row.status === 1) {
      await disableUser(row.id)
      ElMessage.success('已禁用该用户')
    } else {
      await enableUser(row.id)
      ElMessage.success('已启用该用户')
    }
    loadUsers()
  } finally {
    toggleLoading.value = { ...toggleLoading.value, [row.id]: false }
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped lang="scss">
.admin-page {
  padding: 0 4px;

  h2 {
    margin-bottom: 20px;
    color: hsl(220, 14%, 20%);
    font-size: clamp(1.25rem, 3vw, 1.5rem);
  }

  &__search-bar {
    margin-bottom: 20px;
  }

  &__table {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }

  &__pagination {
    margin-top: 20px;
    justify-content: flex-end;
  }
}

.search-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;

  &__input {
    width: 200px;
  }

  &__select {
    width: 150px;
  }
}
</style>
