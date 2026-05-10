<template>
  <div class="home-page">
    <section class="hero-section">
      <div class="hero-content">
        <h1>守护财产安全 从学习开始</h1>
        <p>反诈骗学习平台 - 让你轻松识别各种诈骗手段</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="$router.push('/challenge')">开始学习</el-button>
          <el-button size="large" @click="$router.push('/case')">了解诈骗案例</el-button>
        </div>
      </div>
    </section>

    <!-- <section class="stats-section">
      <div class="stat-card">
        <div class="stat-icon"><IconBook :size="40" /></div>
        <div class="stat-info">
          <div class="stat-value">100+</div>
          <div class="stat-label">诈骗案例</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon"><IconTarget :size="40" /></div>
        <div class="stat-info">
          <div class="stat-value">10</div>
          <div class="stat-label">知识关卡</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon"><IconUsers :size="40" /></div>
        <div class="stat-info">
          <div class="stat-value">5000+</div>
          <div class="stat-label">学习用户</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon"><IconTrophy :size="40" /></div>
        <div class="stat-info">
          <div class="stat-value">95%</div>
          <div class="stat-label">防护能力提升</div>
        </div>
      </div>
    </section> -->

    <section class="features-section">
      <h2 class="section-title">平台特色</h2>
      <div class="features-grid">
        <div class="feature-card">
          <div class="feature-icon"><IconGame :size="48" /></div>
          <h3>游戏化学习</h3>
          <p>闯关答题、情景模拟，让学习变得有趣</p>
        </div>
        <div class="feature-card">
          <div class="feature-icon"><IconBot :size="48" /></div>
          <h3>智能客服</h3>
          <p>AI智能问答，随时解答您的疑惑</p>
        </div>
        <div class="feature-card">
          <div class="feature-icon"><IconChart :size="48" /></div>
          <h3>智能推荐</h3>
          <p>根据您的特点推荐最适合的防骗内容</p>
        </div>
        <div class="feature-card">
          <div class="feature-icon"><IconUsers :size="48" /></div>
          <h3>社区互动</h3>
          <p>分享经历、互相帮助、共同提高防骗意识</p>
        </div>
      </div>
    </section>

    <section class="hot-section" v-loading="hotLoading">
      <h2 class="section-title">热门案例</h2>
      <el-empty v-if="!hotLoading && !hotCases.length" description="暂无热门案例" />
      <el-row v-else :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" v-for="item in hotCases" :key="item.id">
          <div class="case-card" @click="$router.push(`/case/${item.id}`)">
            <div class="case-image"><component :is="caseCardIcon(item)" /></div>
            <div class="case-info">
              <h4>{{ item.title }}</h4>
              <p>{{ item.caseType }}</p>
              <div class="case-stats">
                <span class="case-stat-item"><IconEye :size="14" /> {{ item.viewCount ?? 0 }}</span>
                <span class="case-stat-item"><IconHeart :size="14" /> {{ item.likeCount ?? 0 }}</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getHotCases, type CaseVO } from '@/api/case'
import {
  IconBook, IconTarget, IconUsers, IconTrophy,
  IconGame, IconBot, IconChart, IconEye, IconHeart,
  IconCoin, IconPhone, IconCall, IconLove, IconBank,
  IconCart, IconPolice, IconPig, IconCredit, IconClipboard
} from '@/components/icons'

const hotCases = ref<CaseVO[]>([])
const hotLoading = ref(false)

const CASE_TYPE_ICONS: Record<string, any> = {
  网络诈骗: IconCoin,
  电信诈骗: IconPhone,
  电话诈骗: IconCall,
  情感诈骗: IconLove,
  金融诈骗: IconBank,
  刷单诈骗: IconCart,
  冒充公检法: IconPolice,
  杀猪盘: IconPig,
  贷款诈骗: IconCredit
}

function caseCardIcon(item: CaseVO) {
  if (CASE_TYPE_ICONS[item.caseType]) return CASE_TYPE_ICONS[item.caseType]
  return IconClipboard
}

async function loadHotCases() {
  hotLoading.value = true
  try {
    const res = await getHotCases(6)
    hotCases.value = Array.isArray(res.data) ? res.data : []
  } catch {
    hotCases.value = []
  } finally {
    hotLoading.value = false
  }
}

onMounted(() => {
  loadHotCases()
})
</script>

<style scoped lang="scss">
.home-page {
  .hero-section {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    padding: 80px 20px;
    text-align: center;
    border-radius: 12px;
    margin-bottom: 40px;

    .hero-content {
      max-width: 600px;
      margin: 0 auto;

      h1 {
        font-size: 36px;
        margin-bottom: 16px;
      }

      p {
        font-size: 18px;
        opacity: 0.9;
        margin-bottom: 32px;
      }

      .hero-actions {
        display: flex;
        gap: 16px;
        justify-content: center;
      }
    }
  }

  .stats-section {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    margin-bottom: 40px;

    .stat-card {
      background: #fff;
      padding: 24px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      gap: 16px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

      .stat-icon {
        font-size: 40px;
      }

      .stat-value {
        font-size: 28px;
        font-weight: 600;
        color: #303133;
      }

      .stat-label {
        color: #909399;
        font-size: 14px;
      }
    }
  }

  .features-section,
  .hot-section {
    margin-bottom: 40px;

    .section-title {
      font-size: 24px;
      color: #303133;
      margin-bottom: 24px;
    }

    .features-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 20px;

      .feature-card {
        background: #fff;
        padding: 24px;
        border-radius: 12px;
        text-align: center;
        box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
        transition: transform 0.3s;

        &:hover {
          transform: translateY(-4px);
        }

        .feature-icon {
          font-size: 48px;
          margin-bottom: 16px;
        }

        h3 {
          font-size: 18px;
          color: #303133;
          margin-bottom: 8px;
        }

        p {
          color: #909399;
          font-size: 14px;
        }
      }
    }

    .case-card {
      background: #fff;
      border-radius: 12px;
      overflow: hidden;
      cursor: pointer;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
      transition: transform 0.3s;

      &:hover {
        transform: translateY(-4px);
      }

      .case-image {
        height: 120px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);

        :deep(svg) {
          width: 60px;
          height: 60px;
          color: hsl(220, 15%, 45%);
        }
      }

      .case-info {
        padding: 16px;

        h4 {
          font-size: 16px;
          color: #303133;
          margin-bottom: 4px;
        }

        p {
          color: #909399;
          font-size: 14px;
          margin-bottom: 8px;
        }

        .case-stats {
          display: flex;
          gap: 16px;
          color: #909399;
          font-size: 12px;
        }

        .case-stat-item {
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }
    }
  }
}
</style>
