<template>
  <div class="admin-challenge">
    <div class="admin-challenge__header">
      <h2 class="admin-challenge__title">闯关管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="handleCreate">
          <span class="btn-icon">+</span> 新增关卡
        </el-button>
        <el-button @click="handleViewStats">
          <span class="btn-icon">&#9679;</span> 查看统计
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="admin-challenge__stats" v-if="showStats">
      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--blue">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="3" y1="9" x2="21" y2="9"></line>
            <line x1="9" y1="21" x2="9" y2="9"></line>
          </svg>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ overview.totalChallenges }}</div>
          <div class="stat-card__label">总关卡数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--green">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
            <polyline points="22 4 12 14.01 9 11.01"></polyline>
          </svg>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ overview.enabledChallenges }}</div>
          <div class="stat-card__label">已启用</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--purple">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <path d="M12 6v6l4 2"></path>
          </svg>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ overview.totalAttempts }}</div>
          <div class="stat-card__label">总参与次数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--orange">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="9" cy="7" r="4"></circle>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
          </svg>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ overview.totalPassedUsers }}</div>
          <div class="stat-card__label">通关人数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--teal">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="20" x2="18" y2="10"></line>
            <line x1="12" y1="20" x2="12" y2="4"></line>
            <line x1="6" y1="20" x2="6" y2="14"></line>
          </svg>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ overview.overallPassRate }}%</div>
          <div class="stat-card__label">整体通过率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--red">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
          </svg>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ overview.todayPassed }}</div>
          <div class="stat-card__label">今日通关</div>
        </div>
      </div>
    </div>

    <!-- 筛选工具栏 -->
    <div class="admin-challenge__toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索关卡名称..."
          clearable
          style="width: 240px"
          @clear="fetchChallenges"
          @keyup.enter="fetchChallenges"
        >
          <template #prefix>
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <path d="M21 21l-4.35-4.35"/>
            </svg>
          </template>
        </el-input>
        <el-select v-model="filterType" placeholder="关卡类型" clearable style="width: 140px" @change="fetchChallenges">
          <el-option label="答题挑战" value="quiz" />
          <el-option label="情景模拟" value="scenario" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 120px" @change="fetchChallenges">
          <el-option label="已启用" :value="1" />
          <el-option label="已禁用" :value="0" />
        </el-select>
      </div>
      <div class="toolbar-right" v-if="selectedIds.length > 0">
        <span class="selection-info">已选择 {{ selectedIds.length }} 项</span>
        <el-button size="small" @click="handleBatchEnable" :loading="batchLoading">批量启用</el-button>
        <el-button size="small" @click="handleBatchDisable" :loading="batchLoading">批量禁用</el-button>
        <el-button size="small" type="danger" @click="handleBatchDelete" :loading="batchLoading">批量删除</el-button>
      </div>
    </div>

    <!-- 关卡列表 -->
    <el-table
      :data="challengeList"
      v-loading="loading"
      stripe
      @selection-change="handleSelectionChange"
      :row-class-name="tableRowClassName"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="levelOrder" label="顺序" width="80" />
      <el-table-column prop="title" label="关卡名称" min-width="200">
        <template #default="{ row }">
          <div class="challenge-title">
            <span class="type-badge" :class="`type--${row.type}`">
              {{ row.typeName }}
            </span>
            <span class="title-text">{{ row.title }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
      <el-table-column prop="difficulty" label="难度" width="100">
        <template #default="{ row }">
          <el-tag :type="getDifficultyType(row.difficulty)" size="small">
            {{ row.difficultyName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="passingScore" label="及格分" width="80" align="center">
        <template #default="{ row }">
          <span class="score-value">{{ row.passingScore }}分</span>
        </template>
      </el-table-column>
      <el-table-column prop="scoreReward" label="奖励" width="80" align="center">
        <template #default="{ row }">
          <span class="reward-text">+{{ row.scoreReward }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            @change="(val: boolean) => handleToggleStatus(row, val)"
            :loading="row._statusLoading"
            active-text="启用"
            inactive-text="禁用"
            inline-prompt
            style="--el-switch-on-color: #67c23a; --el-switch-off-color: #f56c6c"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="info" @click="handleViewDetail(row)">详情</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.current"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      layout="total, prev, pager, next"
      @current-change="fetchChallenges"
      style="margin-top: 20px; justify-content: flex-end"
    />

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑关卡' : '新增关卡'"
      width="800px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="关卡名称" prop="title">
          <el-input v-model="form.title" placeholder="请输入关卡名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="关卡描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入关卡描述" maxlength="200" show-word-limit />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关卡顺序" prop="levelOrder">
              <el-input-number v-model="form.levelOrder" :min="1" :max="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关卡类型" prop="type">
              <el-select v-model="form.type" placeholder="选择类型" style="width: 100%">
                <el-option label="答题挑战" value="quiz" />
                <el-option label="情景模拟" value="scenario" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="难度等级" prop="difficulty">
              <el-rate v-model="form.difficulty" :max="5" show-text :texts="['入门', '简单', '中等', '困难', '噩梦']" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="及格分数" prop="passingScore">
              <el-input-number v-model="form.passingScore" :min="0" :max="100" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="奖励积分" prop="scoreReward">
              <el-input-number v-model="form.scoreReward" :min="0" :max="1000" :step="5" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 题目编辑器 (答题挑战) -->
        <el-form-item label="题目内容" prop="content" v-if="form.type === 'quiz'">
          <div class="content-editor">
            <div class="question-list">
              <div v-for="(q, qIdx) in form.content?.questions || []" :key="qIdx" class="question-item">
                <div class="question-header">
                  <span class="question-num">题目 {{ qIdx + 1 }}</span>
                  <el-tag size="small" type="info">{{ getQuestionTypeLabel(q.questionType) }}</el-tag>
                  <el-button link type="danger" @click="removeQuestion(qIdx)">删除</el-button>
                </div>
                <el-form-item label="题目类型">
                  <el-select v-model="q.questionType" size="small" style="width: 120px">
                    <el-option label="单选" value="single" />
                    <el-option label="多选" value="multiple" />
                    <el-option label="判断" value="truefalse" />
                  </el-select>
                </el-form-item>
                <el-input
                  v-model="q.text"
                  type="textarea"
                  :rows="2"
                  placeholder="请输入题目内容"
                  size="small"
                  style="margin-bottom: 12px"
                />
                <div class="options-editor">
                  <div v-for="(opt, oIdx) in q.options" :key="oIdx" class="option-row">
                    <span class="option-label">{{ String.fromCharCode(65 + oIdx) }}.</span>
                    <el-input
                      v-model="opt.text"
                      :placeholder="`选项 ${String.fromCharCode(65 + oIdx)}`"
                      size="small"
                      style="flex: 1"
                    />
                    <el-checkbox
                      :model-value="q.correctIndexes?.includes(oIdx)"
                      @update:model-value="(val: boolean) => toggleCorrect(q, oIdx, val)"
                      size="small"
                    >
                      正确答案
                    </el-checkbox>
                    <el-button link type="danger" @click="removeOption(q, oIdx)" v-if="q.options.length > 2">
                      <span style="font-size: 16px; line-height: 1">&times;</span>
                    </el-button>
                  </div>
                </div>
                <div class="question-footer">
                  <span class="score-label">分值:</span>
                  <el-input-number v-model="q.score" :min="1" :max="100" size="small" style="margin-left: 8px" />
                  <el-button size="small" @click="addOption(q)" v-if="q.options.length < 6" style="margin-left: 12px">
                    + 添加选项
                  </el-button>
                </div>
              </div>
            </div>
            <el-button type="primary" plain @click="addQuestion" style="margin-top: 12px">
              + 添加题目
            </el-button>
          </div>
        </el-form-item>

        <!-- 情景模拟编辑器 -->
        <el-form-item label="剧本内容" prop="scripts" v-if="form.type === 'scenario'">
          <ScenarioScriptEditor v-model="form.scripts" style="width: 100%" />
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

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="关卡详情" width="700px">
      <div class="challenge-detail" v-if="currentChallenge">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="关卡ID">{{ currentChallenge.id }}</el-descriptions-item>
          <el-descriptions-item label="关卡顺序">{{ currentChallenge.levelOrder }}</el-descriptions-item>
          <el-descriptions-item label="关卡名称">{{ currentChallenge.title }}</el-descriptions-item>
          <el-descriptions-item label="关卡类型">
            <el-tag size="small" :type="currentChallenge.type === 'quiz' ? 'primary' : 'success'">
              {{ currentChallenge.typeName }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="难度">
            <el-tag :type="getDifficultyType(currentChallenge.difficulty)" size="small">
              {{ currentChallenge.difficultyName }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="及格分数">{{ currentChallenge.passingScore }}分</el-descriptions-item>
          <el-descriptions-item label="奖励积分">{{ currentChallenge.scoreReward }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentChallenge.status === 1 ? 'success' : 'danger'" size="small">
              {{ currentChallenge.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ currentChallenge.createTime }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ currentChallenge.description || '无' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 关卡统计 -->
        <div class="detail-stats" v-if="challengeStats">
          <h4>闯关统计</h4>
          <div class="stats-grid">
            <div class="stat-item">
              <span class="stat-label">参与人数</span>
              <span class="stat-value">{{ challengeStats.totalAttempts }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">通过人数</span>
              <span class="stat-value stat-value--success">{{ challengeStats.passedCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">通过率</span>
              <span class="stat-value stat-value--primary">{{ challengeStats.passRate }}%</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">平均分</span>
              <span class="stat-value">{{ challengeStats.avgScore }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">最高分</span>
              <span class="stat-value stat-value--success">{{ challengeStats.maxScore }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">最低分</span>
              <span class="stat-value">{{ challengeStats.minScore }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">平均用时</span>
              <span class="stat-value">{{ formatDuration(challengeStats.avgDuration) }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminChallengeList,
  createChallenge,
  updateChallenge,
  deleteChallenge,
  getChallengeOverview,
  getChallengeStats,
  batchUpdateChallengeStatus,
  batchDeleteChallenges,
  type ChallengeVO,
  type ChallengeOverviewVO,
  type ChallengeStatsVO
} from '@/api/challenge'
import ScenarioScriptEditor from '@/components/admin/ScenarioScriptEditor.vue'
import type { ScenarioScriptModel } from '@/types/scenario-script'

const loading = ref(false)
const submitting = ref(false)
const batchLoading = ref(false)
const challengeList = ref<ChallengeVO[]>([])
const searchKeyword = ref('')
const filterType = ref('')
const filterStatus = ref<number | null>(null)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const currentChallenge = ref<ChallengeVO | null>(null)
const challengeStats = ref<ChallengeStatsVO | null>(null)
const showStats = ref(false)
const selectedIds = ref<number[]>([])

const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0
})

const overview = reactive<ChallengeOverviewVO>({
  totalChallenges: 0,
  enabledChallenges: 0,
  disabledChallenges: 0,
  quizChallenges: 0,
  scenarioChallenges: 0,
  totalAttempts: 0,
  totalPassedUsers: 0,
  overallPassRate: 0,
  todayPassed: 0,
  challengeStats: []
})

const defaultForm = () => ({
  id: null as number | null,
  title: '',
  description: '',
  levelOrder: 1,
  difficulty: 1,
  type: 'quiz' as 'quiz' | 'scenario',
  passingScore: 60,
  scoreReward: 10,
  content: { questions: [] as any[] },
  scripts: {
    name: '',
    description: '',
    nodes: [] as any[],
    edges: [] as any[],
    startNodeId: '',
    endNodeIds: [] as string[]
  },
  status: 1 as 0 | 1
})

const form = ref<ReturnType<typeof defaultForm>>(defaultForm())

const rules = {
  title: [{ required: true, message: '请输入关卡名称', trigger: 'blur' }],
  levelOrder: [{ required: true, message: '请输入关卡顺序', trigger: 'blur' }],
  type: [{ required: true, message: '请选择关卡类型', trigger: 'change' }]
}

const fetchChallenges = async () => {
  loading.value = true
  try {
    const res = await getAdminChallengeList({
      pageNum: pagination.value.current,
      pageSize: pagination.value.pageSize,
      keyword: searchKeyword.value || undefined,
      type: filterType.value || undefined
    } as any)

    let records = (res.data as any).records

    // 前端过滤状态
    if (filterStatus.value !== null) {
      records = records.filter((c: ChallengeVO) => c.status === filterStatus.value)
    }

    // 添加临时状态用于开关
    records = records.map((c: ChallengeVO & { _statusLoading?: boolean }) => ({
      ...c,
      _statusLoading: false
    }))

    challengeList.value = records
    pagination.value.total = (res.data as any).total
  } catch (error) {
    console.error('获取关卡列表失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchOverview = async () => {
  try {
    const res = await getChallengeOverview()
    Object.assign(overview, res.data)
  } catch (error) {
    console.error('获取统计概览失败:', error)
  }
}

const handleViewStats = () => {
  showStats.value = !showStats.value
  if (showStats.value) {
    fetchOverview()
  }
}

const handleCreate = () => {
  form.value = defaultForm()
  isEdit.value = false
  dialogVisible.value = true
}

const handleEdit = (row: ChallengeVO) => {
  form.value = {
    id: row.id,
    title: row.title,
    description: row.description,
    levelOrder: row.levelOrder,
    difficulty: row.difficulty,
    type: row.type as 'quiz' | 'scenario',
    passingScore: row.passingScore,
    scoreReward: row.scoreReward,
    content: row.content ? JSON.parse(JSON.stringify(row.content)) : { questions: [] },
    scripts: row.scripts ? JSON.parse(JSON.stringify(row.scripts)) : {
      name: '',
      description: '',
      nodes: [],
      edges: [],
      startNodeId: '',
      endNodeIds: []
    },
    status: row.status as 0 | 1
  }
  isEdit.value = true
  dialogVisible.value = true
}

const handleViewDetail = async (row: ChallengeVO) => {
  currentChallenge.value = row
  detailVisible.value = true
  try {
    const res = await getChallengeStats(row.id)
    challengeStats.value = res.data
  } catch {
    challengeStats.value = null
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const submitData = {
      title: form.value.title,
      description: form.value.description,
      levelOrder: form.value.levelOrder,
      difficulty: form.value.difficulty,
      type: form.value.type,
      passingScore: form.value.passingScore,
      scoreReward: form.value.scoreReward,
      content: form.value.type === 'quiz' ? form.value.content : null,
      scripts: form.value.type === 'scenario' ? form.value.scripts : null,
      status: form.value.status
    }

    if (isEdit.value) {
      await updateChallenge(form.value.id!, submitData)
      ElMessage.success('更新成功')
    } else {
      await createChallenge(submitData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchChallenges()
    if (showStats.value) fetchOverview()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: ChallengeVO) => {
  try {
    await ElMessageBox.confirm(`确定删除关卡"${row.title}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteChallenge(row.id)
    ElMessage.success('删除成功')
    fetchChallenges()
    if (showStats.value) fetchOverview()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleToggleStatus = async (row: ChallengeVO, newStatus: boolean) => {
  const targetRow = challengeList.value.find(c => c.id === row.id)
  if (targetRow) {
    targetRow._statusLoading = true
    try {
      await updateChallenge(row.id, { status: newStatus ? 1 : 0 } as any)
      targetRow.status = newStatus ? 1 : 0
      ElMessage.success(`${newStatus ? '启用' : '禁用'}成功`)
      if (showStats.value) fetchOverview()
    } catch (error) {
      console.error('状态更新失败:', error)
      ElMessage.error('状态更新失败')
    } finally {
      if (targetRow) targetRow._statusLoading = false
    }
  }
}

const handleSelectionChange = (selection: ChallengeVO[]) => {
  selectedIds.value = selection.map(s => s.id)
}

const handleBatchEnable = async () => {
  if (selectedIds.value.length === 0) return
  batchLoading.value = true
  try {
    await batchUpdateChallengeStatus(selectedIds.value, 1)
    ElMessage.success('批量启用成功')
    fetchChallenges()
    if (showStats.value) fetchOverview()
  } catch (error) {
    console.error('批量启用失败:', error)
  } finally {
    batchLoading.value = false
  }
}

const handleBatchDisable = async () => {
  if (selectedIds.value.length === 0) return
  batchLoading.value = true
  try {
    await batchUpdateChallengeStatus(selectedIds.value, 0)
    ElMessage.success('批量禁用成功')
    fetchChallenges()
    if (showStats.value) fetchOverview()
  } catch (error) {
    console.error('批量禁用失败:', error)
  } finally {
    batchLoading.value = false
  }
}

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个关卡吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    batchLoading.value = true
    await batchDeleteChallenges(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    fetchChallenges()
    if (showStats.value) fetchOverview()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
    }
  } finally {
    batchLoading.value = false
  }
}

// 题目编辑器方法
const addQuestion = () => {
  if (!form.value.content) {
    form.value.content = { questions: [] }
  }
  form.value.content.questions.push({
    id: `q_${Date.now()}`,
    questionType: 'single',
    text: '',
    options: [
      { label: 'A', text: '' },
      { label: 'B', text: '' },
      { label: 'C', text: '' },
      { label: 'D', text: '' }
    ],
    correctIndexes: [],
    score: 10
  })
}

const removeQuestion = (idx: number) => {
  form.value.content.questions.splice(idx, 1)
}

const getQuestionTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    single: '单选',
    multiple: '多选',
    truefalse: '判断'
  }
  return map[type] || type
}

const toggleCorrect = (question: any, index: number, checked: boolean) => {
  if (!question.correctIndexes) question.correctIndexes = []
  if (checked) {
    if (!question.correctIndexes.includes(index)) {
      question.correctIndexes.push(index)
    }
  } else {
    const idx = question.correctIndexes.indexOf(index)
    if (idx > -1) question.correctIndexes.splice(idx, 1)
  }
}

const addOption = (question: any) => {
  if (question.options.length < 6) {
    const label = String.fromCharCode(65 + question.options.length)
    question.options.push({ label, text: '' })
  }
}

const removeOption = (question: any, index: number) => {
  question.options.splice(index, 1)
  // 重新标记 label
  question.options.forEach((opt: any, i: number) => {
    opt.label = String.fromCharCode(65 + i)
  })
  // 更新正确答案索引
  question.correctIndexes = question.correctIndexes
    .filter((idx: number) => idx < question.options.length)
}

const formatDuration = (seconds: number) => {
  if (!seconds) return '0秒'
  if (seconds < 60) return `${seconds}秒`
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}分${secs}秒`
}

const getDifficultyType = (level: number) => {
  const types = ['', 'success', 'primary', 'warning', 'danger', 'danger']
  return types[level] || 'info'
}

const tableRowClassName = ({ row }: { row: ChallengeVO }) => {
  return row.status === 0 ? 'disabled-row' : ''
}

onMounted(() => {
  fetchChallenges()
})
</script>

<style scoped lang="scss">
.admin-challenge {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;

    .header-actions {
      display: flex;
      gap: 12px;
    }
  }

  &__title {
    font-size: 20px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0;
  }

  &__stats {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    gap: 16px;
    margin-bottom: 20px;
    padding: 16px;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }

  &__toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding: 12px 16px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);

    .toolbar-left {
      display: flex;
      gap: 12px;
      align-items: center;
    }

    .toolbar-right {
      display: flex;
      gap: 8px;
      align-items: center;

      .selection-info {
        color: #409eff;
        font-size: 13px;
        margin-right: 8px;
      }
    }
  }
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;

  &__icon {
    width: 44px;
    height: 44px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    svg {
      width: 22px;
      height: 22px;
    }

    &--blue {
      background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
      color: #fff;
    }

    &--green {
      background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
      color: #fff;
    }

    &--purple {
      background: linear-gradient(135deg, #9c27b0 0%, #ba68c8 100%);
      color: #fff;
    }

    &--orange {
      background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
      color: #fff;
    }

    &--teal {
      background: linear-gradient(135deg, #1abc9c 0%, #48d1a8 100%);
      color: #fff;
    }

    &--red {
      background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
      color: #fff;
    }
  }

  &__content {
    flex: 1;
    min-width: 0;
  }

  &__value {
    font-size: 22px;
    font-weight: 700;
    color: #1a1a1a;
    line-height: 1.2;
  }

  &__label {
    font-size: 12px;
    color: #8c8c8c;
    margin-top: 2px;
  }
}

.challenge-title {
  display: flex;
  align-items: center;
  gap: 8px;

  .title-text {
    font-weight: 500;
  }
}

.type-badge {
  display: inline-flex;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 4px;
  flex-shrink: 0;

  &.type--quiz {
    background: hsl(210, 80%, 95%);
    color: hsl(210, 80%, 40%);
  }

  &.type--scenario {
    background: hsl(280, 60%, 95%);
    color: hsl(280, 60%, 40%);
  }
}

.score-value {
  color: #666;
  font-size: 13px;
}

.reward-text {
  color: hsl(142, 70%, 40%);
  font-weight: 600;
}

.content-editor {
  width: 100%;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-item {
  padding: 16px;
  background: hsl(220, 15%, 97%);
  border-radius: 8px;
  border: 1px solid hsl(220, 15%, 92%);
}

.question-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;

  .question-num {
    font-weight: 600;
    color: var(--el-text-color-primary);
  }
}

.options-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 8px;

  .option-label {
    width: 20px;
    font-weight: 600;
    color: #666;
  }
}

.question-footer {
  display: flex;
  align-items: center;
  margin-top: 12px;

  .score-label {
    font-size: 13px;
    color: #909399;
  }
}

.challenge-detail {
  .detail-stats {
    margin-top: 20px;
    padding: 16px;
    background: hsl(220, 15%, 97%);
    border-radius: 8px;

    h4 {
      margin: 0 0 12px;
      font-size: 14px;
      color: #333;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 12px;

      .stat-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 12px;
        background: #fff;
        border-radius: 6px;

        .stat-label {
          font-size: 12px;
          color: #999;
          margin-bottom: 4px;
        }

        .stat-value {
          font-size: 20px;
          font-weight: 700;
          color: #333;

          &--primary {
            color: #409eff;
          }

          &--success {
            color: #67c23a;
          }
        }
      }
    }
  }
}

:deep(.disabled-row) {
  opacity: 0.6;
  background: hsl(0, 0%, 98%);
}

:deep(.el-table .cell) {
  padding: 8px 12px;
}

.btn-icon {
  margin-right: 4px;
}
</style>
