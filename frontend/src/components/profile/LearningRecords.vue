<template>
  <div class="learning-records">
    <div v-if="loading" class="learning-records__loading">
      <el-skeleton :rows="5" animated />
    </div>

    <div v-else-if="records.length === 0" class="learning-records__empty">
      <el-empty description="暂无学习记录">
        <el-button type="primary" @click="$router.push('/challenge')">去学习</el-button>
      </el-empty>
    </div>

    <div v-else class="learning-records__list">
      <el-timeline>
        <el-timeline-item
          v-for="item in records"
          :key="item.id"
          :timestamp="item.time"
          :color="getItemColor(item.type)"
          :icon="getItemIcon(item.type)"
          placement="top"
        >
          <el-card shadow="hover" class="learning-records__item">
            <div class="learning-records__content">
              <span class="learning-records__type" :class="`learning-records__type--${item.type}`">
                {{ getTypeLabel(item.type) }}
              </span>
              <p class="learning-records__desc">{{ item.content }}</p>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <div class="learning-records__pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { get } from '@/utils/request'
import { Trophy, Reading, Collection, ChatDotRound } from '@element-plus/icons-vue'

interface Record {
  id: number
  type: 'challenge' | 'case' | 'forum' | 'news'
  content: string
  time: string
}

const loading = ref(false)
const records = ref<Record[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const getItemColor = (type: string) => {
  const colors: Record<string, string> = {
    challenge: 'hsl(45, 90%, 55%)',
    case: 'hsl(200, 70%, 50%)',
    forum: 'hsl(280, 60%, 55%)',
    news: 'hsl(150, 60%, 45%)'
  }
  return colors[type] || 'hsl(220, 10%, 50%)'
}

const getItemIcon = (type: string) => {
  const icons: Record<string, any> = {
    challenge: Trophy,
    case: Reading,
    forum: ChatDotRound,
    news: Collection
  }
  return icons[type]
}

const getTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    challenge: '闯关',
    case: '案例',
    forum: '社区',
    news: '资讯'
  }
  return labels[type] || type
}

const fetchRecords = async () => {
  loading.value = true
  try {
    const res: any = await get('/learning/records', {
      params: {
        page: currentPage.value,
        size: pageSize.value
      }
    })
    records.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    records.value = [
      { id: 1, type: 'challenge', content: '完成了"初识诈骗"闯关，获得50积分', time: '2026-03-20 14:30' },
      { id: 2, type: 'case', content: '浏览了"刷单返利诈骗"案例', time: '2026-03-19 10:20' },
      { id: 3, type: 'forum', content: '参与了"如何识别钓鱼网站"讨论', time: '2026-03-18 16:45' },
      { id: 4, type: 'news', content: '阅读了"最新反诈预警"资讯', time: '2026-03-17 09:00' },
      { id: 5, type: 'challenge', content: '完成了"情景模拟：快递诈骗"', time: '2026-03-16 20:15' }
    ]
    total.value = records.value.length
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchRecords()
}

onMounted(() => {
  fetchRecords()
})
</script>

<style scoped lang="scss">
.learning-records {
  &__loading {
    padding: 20px 0;
  }

  &__empty {
    padding: 60px 0;
  }

  &__list {
    :deep(.el-timeline-item__node) {
      background-color: hsl(225, 60%, 55%);
    }

    :deep(.el-timeline-item__timestamp) {
      color: hsl(220, 10%, 55%);
      font-size: 13px;
    }
  }

  &__item {
    border-radius: 8px;
    transition: transform 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px hsla(220, 10%, 85%, 0.5);
    }
  }

  &__content {
    display: flex;
    align-items: flex-start;
    gap: 12px;
  }

  &__type {
    flex-shrink: 0;
    padding: 2px 10px;
    border-radius: 12px;
    font-size: 12px;
    font-weight: 500;
    color: #fff;

    &--challenge {
      background: hsl(45, 90%, 55%);
    }

    &--case {
      background: hsl(200, 70%, 50%);
    }

    &--forum {
      background: hsl(280, 60%, 55%);
    }

    &--news {
      background: hsl(150, 60%, 45%);
    }
  }

  &__desc {
    margin: 0;
    font-size: 14px;
    color: hsl(220, 10%, 30%);
    line-height: 1.6;
  }

  &__pagination {
    display: flex;
    justify-content: center;
    margin-top: 24px;
    padding-top: 24px;
    border-top: 1px solid hsl(220, 10%, 92%);

    :deep(.el-pagination.is-background .el-pager li.is-active) {
      background-color: hsl(225, 60%, 55%);
    }
  }
}
</style>
