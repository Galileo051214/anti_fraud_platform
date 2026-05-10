<template>
  <div class="admin-news">
    <header class="admin-news__header">
      <h2 class="admin-news__title">资讯管理</h2>
      <el-button type="primary" @click="openEditor()">
        <Plus />
        发布资讯
      </el-button>
    </header>

    <div class="admin-news__filter">
      <el-select v-model="filterStatus" placeholder="全部状态" clearable @change="loadNews">
        <el-option label="已发布" :value="1" />
        <el-option label="草稿" :value="0" />
      </el-select>
      <el-select v-model="filterType" placeholder="全部类型" clearable @change="loadNews">
        <el-option label="新闻" value="news" />
        <el-option label="预警" value="warning" />
        <el-option label="政策" value="policy" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="搜索标题"
        clearable
        @clear="loadNews"
        @keyup.enter="loadNews"
      >
        <template #prefix>
          <Search />
        </template>
      </el-input>
    </div>

    <el-table :data="newsList" border stripe v-loading="loading" class="admin-news__table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="admin-news__title-cell">
            <span class="admin-news__title-text">{{ row.title }}</span>
            <span v-if="row.isTop" class="admin-news__badge admin-news__badge--top">置顶</span>
            <span v-if="row.isMandatory" class="admin-news__badge admin-news__badge--mandatory">必读</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column prop="newsType" label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getTagType(row.newsType)" size="small">{{ getTypeName(row.newsType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="authorName" label="作者" width="100" />
      <el-table-column prop="viewCount" label="浏览量" width="100" align="center" />
      <el-table-column prop="likeCount" label="点赞数" width="100" align="center" />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publishTime" label="发布时间" width="160" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <div class="admin-news__actions">
            <el-button size="small" @click="openEditor(row)">编辑</el-button>
            <el-button
              v-if="row.status === 0"
              size="small"
              type="success"
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button
              size="small"
              :type="row.isTop ? 'warning' : 'default'"
              @click="handleToggleTop(row)"
            >
              {{ row.isTop ? '取消置顶' : '置顶' }}
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="admin-news__pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <el-dialog
      v-model="editorVisible"
      :title="isEdit ? '编辑资讯' : '发布资讯'"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入资讯标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="formData.categoryId" placeholder="请选择分类">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="newsType">
          <el-radio-group v-model="formData.newsType">
            <el-radio value="news">新闻</el-radio>
            <el-radio value="warning">预警</el-radio>
            <el-radio value="policy">政策</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="封面图">
          <div class="cover-upload">
            <el-input v-model="formData.coverImage" placeholder="请输入封面图URL" />
            <el-button type="primary" @click="handleUploadClick">上传图片</el-button>
            <input
              ref="fileInputRef"
              type="file"
              accept="image/*"
              style="display: none"
              @change="handleFileChange"
            />
          </div>
          <div v-if="uploadProgress > 0 && uploadProgress < 100" class="cover-upload__progress">
            <el-progress :percentage="uploadProgress" :show-text="false" />
          </div>
          <div v-if="formData.coverImage" class="cover-upload__preview">
            <img :src="formData.coverImage" alt="封面预览" />
            <el-button
              type="danger"
              size="small"
              circle
              :icon="Delete"
              class="cover-upload__delete"
              @click="formData.coverImage = ''"
            />
          </div>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="formData.summary" type="textarea" :rows="2" placeholder="请输入摘要" maxlength="500" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="formData.content" type="textarea" :rows="10" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="formData.isTop" :width="40" />
        </el-form-item>
        <el-form-item label="全校必读">
          <el-switch v-model="formData.isMandatory" :width="40" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <!-- <el-button @click="handleSaveDraft">保存草稿</el-button> -->
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, Search, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import request from '@/utils/request'
import { getCategories } from '@/api/news'
import { uploadImage } from '@/api/upload'
import type { NewsCategory } from '@/api/news'

interface NewsItem {
  id: number
  title: string
  content: string
  summary: string
  coverImage: string
  categoryId: number
  categoryName: string
  authorId: number
  authorName: string
  newsType: string
  isTop: number
  isMandatory: number
  viewCount: number
  likeCount: number
  status: number
  publishTime: string
}

const loading = ref(false)
const submitLoading = ref(false)
const newsList = ref<NewsItem[]>([])
const categories = ref<NewsCategory[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref<number | null>(null)
const filterType = ref<string>('')
const keyword = ref('')
const editorVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const fileInputRef = ref<HTMLInputElement>()
const uploadProgress = ref(0)
const uploading = ref(false)

const formRef = ref<FormInstance>()
const formData = reactive({
  title: '',
  categoryId: null as number | null,
  newsType: 'news',
  coverImage: '',
  summary: '',
  content: '',
  isTop: false,
  isMandatory: false
})

const formRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  newsType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const getTagType = (type: string) => {
  const map: Record<string, string> = { news: '', warning: 'danger', policy: 'success' }
  return map[type] || 'info'
}

const getTypeName = (type: string) => {
  const map: Record<string, string> = { news: '新闻', warning: '预警', policy: '政策' }
  return map[type] || '其他'
}

const handleUploadClick = () => {
  fileInputRef.value?.click()
}

const handleFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    input.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过5MB')
    input.value = ''
    return
  }

  uploading.value = true
  uploadProgress.value = 10

  try {
    const res = await uploadImage(file)
    formData.coverImage = res.data
    uploadProgress.value = 100
    ElMessage.success('封面上传成功')
  } catch {
    ElMessage.error('上传失败，请重试')
    uploadProgress.value = 0
  } finally {
    uploading.value = false
    input.value = ''
  }
}

const loadNews = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
    if (filterStatus.value !== null) params.status = filterStatus.value
    if (filterType.value) params.newsType = filterType.value
    if (keyword.value) params.keyword = keyword.value

    const res = await request.get<{ data: { records: NewsItem[]; total: number } }>('/news/page', { params })
    newsList.value = res.data.records
    total.value = res.data.total
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    const res = await getCategories()
    categories.value = res.data
  } catch {
    // error handled by interceptor
  }
}

const openEditor = (row?: NewsItem) => {
  if (row) {
    isEdit.value = true
    editingId.value = row.id
    formData.title = row.title
    formData.categoryId = row.categoryId
    formData.newsType = row.newsType
    formData.coverImage = row.coverImage || ''
    formData.summary = row.summary || ''
    formData.content = row.content
    formData.isTop = row.isTop === 1
    formData.isMandatory = row.isMandatory === 1
  } else {
    isEdit.value = false
    editingId.value = null
    Object.assign(formData, {
      title: '',
      categoryId: null,
      newsType: 'news',
      coverImage: '',
      summary: '',
      content: '',
      isTop: false,
      isMandatory: false
    })
  }
  editorVisible.value = true
}

const handleSaveDraft = async () => {
  await handleSubmit(false)
}

const handleSubmit = async (publish = true) => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitLoading.value = true
    try {
      const topValue = formData.isTop ? 1 : 0
      const mandatoryValue = formData.isMandatory ? 1 : 0
      const data = {
        title: formData.title,
        categoryId: formData.categoryId,
        newsType: formData.newsType,
        coverImage: formData.coverImage,
        summary: formData.summary,
        content: formData.content,
        isTop: topValue,
        isMandatory: mandatoryValue
      }
      
      if (isEdit.value && editingId.value) {
        await request.put(`/news/${editingId.value}`, data)
        if (publish) {
          await request.post(`/news/${editingId.value}/publish`)
        }
        ElMessage.success(publish ? '更新并发布成功' : '保存草稿成功')
      } else {
        const res = await request.post<{ data: { id: number } }>('/news', data)
        if (publish) {
          await request.post(`/news/${res.data.id}/publish`)
        }
        ElMessage.success(publish ? '发布成功' : '保存草稿成功')
      }
      
      editorVisible.value = false
      loadNews()
    } catch {
      // error handled by interceptor
    } finally {
      submitLoading.value = false
    }
  })
}

const handlePublish = async (row: NewsItem) => {
  try {
    await request.post(`/news/${row.id}/publish`)
    ElMessage.success('发布成功')
    loadNews()
  } catch {
    // error handled by interceptor
  }
}

const handleToggleTop = async (row: NewsItem) => {
  try {
    await request.put(`/news/${row.id}/top`, null, { params: { isTop: row.isTop === 1 ? 0 : 1 } })
    ElMessage.success(row.isTop === 1 ? '已取消置顶' : '已设为置顶')
    loadNews()
  } catch {
    // error handled by interceptor
  }
}

const handleDelete = async (row: NewsItem) => {
  try {
    await ElMessageBox.confirm('确定要删除这条资讯吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.delete(`/news/${row.id}`)
    ElMessage.success('删除成功')
    loadNews()
  } catch {
    // error handled by interceptor
  }
}

const handleSizeChange = () => {
  currentPage.value = 1
  loadNews()
}

const handlePageChange = () => {
  loadNews()
}

onMounted(() => {
  loadNews()
  loadCategories()
})
</script>

<style scoped lang="scss">
.admin-news {
  padding: 24px;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }

  &__title {
    font-size: 20px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
  }

  &__filter {
    display: flex;
    gap: 12px;
    margin-bottom: 20px;
    flex-wrap: wrap;

    .el-input {
      width: 200px;
    }
  }

  &__table {
    margin-bottom: 20px;
  }

  &__title-cell {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__title-text {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__badge {
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 500;
    flex-shrink: 0;

    &--top {
      background: hsl(0, 70%, 55%);
      color: #fff;
    }

    &--mandatory {
      background: hsl(30, 80%, 50%);
      color: #fff;
    }
  }

  &__actions {
    display: flex;
    gap: 4px;
    flex-wrap: wrap;
  }

  &__pagination {
    display: flex;
    justify-content: flex-end;
  }
}

.cover-upload {
  display: flex;
  gap: 10px;
  align-items: flex-start;

  .el-input {
    flex: 1;
  }

  &__progress {
    margin-top: 8px;
    width: 300px;
  }

  &__preview {
    position: relative;
    margin-top: 10px;
    width: 200px;
    height: 120px;
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid var(--border-color);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    &:hover .cover-upload__delete {
      opacity: 1;
    }
  }

  &__delete {
    position: absolute;
    top: 4px;
    right: 4px;
    opacity: 0;
    transition: opacity 0.2s;
  }
}

@media (max-width: 768px) {
  .admin-news {
    padding: 16px;

    &__filter {
      flex-direction: column;

      .el-input {
        width: 100%;
      }
    }

    &__actions {
      flex-direction: column;

      .el-button {
        width: 100%;
      }
    }
  }
}
</style>
