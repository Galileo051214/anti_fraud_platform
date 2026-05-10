<template>
  <div class="admin-case">
    <div class="admin-case__header">
      <h2 class="admin-case__title">案例管理</h2>
      <el-button type="primary" @click="handleCreate">新增案例</el-button>
    </div>

    <div class="admin-case__toolbar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索案例标题..."
        clearable
        style="width: 240px"
        @clear="fetchCases"
        @keyup.enter="fetchCases"
      >
        <template #prefix>
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <path d="M21 21l-4.35-4.35"/>
          </svg>
        </template>
      </el-input>
    </div>

    <el-table :data="caseList" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" min-width="200">
        <template #default="{ row }">
          <div class="case-title">
            <span v-if="row.isFeatured" class="featured-badge">精选</span>
            {{ row.title }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="caseType" label="类型" width="120" />
      <el-table-column prop="difficultyLevel" label="难度" width="100">
        <template #default="{ row }">
          <el-tag :type="getDifficultyType(row.difficultyLevel)" size="small">
            {{ row.difficultyName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览" width="80" />
      <el-table-column prop="likeCount" label="点赞" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publishTime" label="发布时间" width="160">
        <template #default="{ row }">
          {{ row.publishTime ? new Date(row.publishTime).toLocaleString() : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link :type="row.isFeatured ? 'warning' : 'primary'" @click="toggleFeatured(row)">
            {{ row.isFeatured ? '取消精选' : '设为精选' }}
          </el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.current"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      layout="total, prev, pager, next"
      @current-change="fetchCases"
      style="margin-top: 20px; justify-content: flex-end"
    />

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑案例' : '新增案例'" width="920px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入案例标题" />
        </el-form-item>
        <el-form-item label="类型" prop="caseType">
          <el-input v-model="form.caseType" placeholder="请输入案例类型" />
        </el-form-item>
        <el-form-item label="难度等级" prop="difficultyLevel">
          <el-rate v-model="form.difficultyLevel" :max="5" show-text :texts="['入门', '简单', '中等', '困难', '噩梦']" />
        </el-form-item>
        <el-form-item label="风险评分" prop="riskScore">
          <el-input-number v-model="form.riskScore" :min="0" :max="10" :step="0.5" />
        </el-form-item>
        <el-form-item label="目标年级" prop="targetGrades">
          <el-checkbox-group v-model="form.targetGrades">
            <el-checkbox label="大一" />
            <el-checkbox label="大二" />
            <el-checkbox label="大三" />
            <el-checkbox label="大四" />
            <el-checkbox label="all">全部</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="标签" prop="tagIds">
          <el-select v-model="form.tagIds" multiple placeholder="请选择标签" style="width: 100%">
            <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="案例内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入案例详细内容" />
        </el-form-item>
        <el-form-item label="剧本结构">
          <ScenarioScriptEditor v-model="scriptModel" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCasePage, getAllTags, createCase, updateCase, deleteCase, setCaseFeatured } from '@/api/case'
import ScenarioScriptEditor from '@/components/admin/ScenarioScriptEditor.vue'
import { emptyScenarioScript, parseScenarioScriptJson } from '@/types/scenario-script'
import type { ScenarioScriptModel } from '@/types/scenario-script'

const loading = ref(false)
const submitting = ref(false)
const caseList = ref<any[]>([])
const tags = ref<any[]>([])
const searchKeyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0
})

const scriptModel = ref<ScenarioScriptModel>(emptyScenarioScript())

const form = ref({
  id: null as number | null,
  title: '',
  caseType: '',
  content: '',
  targetGrades: [] as string[],
  difficultyLevel: 1,
  riskScore: 0,
  tagIds: [] as number[],
  status: 1
})

const rules = {
  title: [{ required: true, message: '请输入案例标题', trigger: 'blur' }],
  caseType: [{ required: true, message: '请输入案例类型', trigger: 'blur' }],
  content: [{ required: true, message: '请输入案例内容', trigger: 'blur' }],
  difficultyLevel: [{ required: true, message: '请选择难度等级', trigger: 'change' }]
}

const fetchCases = async () => {
  loading.value = true
  try {
    const res = await getCasePage({
      pageNum: pagination.value.current,
      pageSize: pagination.value.pageSize,
      keyword: searchKeyword.value || undefined
    })
    caseList.value = res.data.records ?? []
    pagination.value.total = res.data.total ?? 0
  } catch (error) {
    console.error('获取案例列表失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchTags = async () => {
  try {
    const res = await getAllTags()
    tags.value = res.data ?? []
  } catch (error) {
    console.error('获取标签失败:', error)
  }
}

const handleCreate = () => {
  scriptModel.value = emptyScenarioScript()
  form.value = {
    id: null,
    title: '',
    caseType: '',
    content: '',
    targetGrades: [],
    difficultyLevel: 1,
    riskScore: 0,
    tagIds: [],
    status: 1
  }
  isEdit.value = false
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  scriptModel.value = parseScenarioScriptJson(row.scripts)
  form.value = {
    id: row.id,
    title: row.title,
    caseType: row.caseType,
    content: row.content,
    targetGrades: row.targetGrades || [],
    difficultyLevel: row.difficultyLevel,
    riskScore: Number(row.riskScore),
    tagIds: row.tags?.map((t: any) => t.id) || [],
    status: row.status
  }
  isEdit.value = true
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      ...form.value,
      scripts: JSON.stringify(scriptModel.value)
    }
    if (isEdit.value) {
      await updateCase(form.value.id!, payload)
      ElMessage.success('更新成功')
    } else {
      await createCase(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchCases()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定删除案例"${row.title}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCase(row.id)
    ElMessage.success('删除成功')
    fetchCases()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const toggleFeatured = async (row: any) => {
  try {
    const newStatus = row.isFeatured ? 0 : 1
    await setCaseFeatured(row.id, newStatus)
    ElMessage.success(newStatus ? '已设为精选' : '已取消精选')
    fetchCases()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const getDifficultyType = (level: number) => {
  const types = ['', 'success', 'primary', 'warning', 'danger', 'danger']
  return types[level] || 'info'
}

onMounted(() => {
  fetchCases()
  fetchTags()
})
</script>

<style scoped lang="scss">
.admin-case {
  padding: 24px;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 24px;
  }

  &__title {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
  }

  &__toolbar {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
  }
}

.case-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.featured-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 6px;
  font-size: 11px;
  color: #fff;
  background: #f59e0b;
  border-radius: 4px;
}
</style>
