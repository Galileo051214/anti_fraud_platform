<template>
  <div class="dashboard">
    <div class="dashboard__header">
      <h2 class="dashboard__title">数据看板</h2>
      <div class="dashboard__actions">
        <el-button type="primary" :loading="refreshLoading" @click="handleRefresh">
          <span class="btn-icon">&#8635;</span> 刷新数据
        </el-button>
        <el-dropdown @command="handleExport">
          <el-button type="success">
            <span class="btn-icon">&#8595;</span> 导出Excel
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="daily">导出每日统计</el-dropdown-item>
              <el-dropdown-item command="department">导出院系统计</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button type="warning" @click="handleExportPdf">
          <span class="btn-icon">&#128196;</span> 导出PDF
        </el-button>
      </div>
    </div>

    <div class="dashboard__stats">
      <div class="stat-card stat-card--blue">
        <div class="stat-card__icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="9" cy="7" r="4"></circle>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
          </svg>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ formatNumber(dashboardData.totalUsers || 0) }}</div>
          <div class="stat-card__label">总用户数</div>
        </div>
      </div>

      <div class="stat-card stat-card--green">
        <div class="stat-card__icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
            <polyline points="17 6 23 6 23 12"></polyline>
          </svg>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ formatNumber(dashboardData.todayActiveUsers || 0) }}</div>
          <div class="stat-card__label">今日活跃用户</div>
        </div>
      </div>

      <div class="stat-card stat-card--purple">
        <div class="stat-card__icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
            <circle cx="12" cy="12" r="3"></circle>
          </svg>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ formatNumber(dashboardData.todayViews || 0) }}</div>
          <div class="stat-card__label">今日浏览量</div>
        </div>
      </div>

      <div class="stat-card stat-card--orange">
        <div class="stat-card__icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
          </svg>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ formatNumber(dashboardData.totalCases || 0) }}</div>
          <div class="stat-card__label">案例总数</div>
        </div>
      </div>

      <div class="stat-card stat-card--red">
        <div class="stat-card__icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
          </svg>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ dashboardData.avgTestScore || 0 }}</div>
          <div class="stat-card__label">平均测试得分</div>
        </div>
      </div>

      <div class="stat-card stat-card--teal">
        <div class="stat-card__icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 20V10"></path>
            <path d="M18 20V4"></path>
            <path d="M6 20v-4"></path>
          </svg>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ formatNumber(dashboardData.totalChallengeCompletions || 0) }}</div>
          <el-tooltip
            placement="top"
            effect="dark"
            :show-after="300"
            content="今日内有多少个「用户×关卡」达成通关；同一用户同一天同一关卡多次通关只计 1 次。不是「已通关全部关卡的用户人数」。"
          >
            <div class="stat-card__label stat-card__label--tooltip">闯关完成数</div>
          </el-tooltip>
        </div>
      </div>
    </div>

    <div class="dashboard__charts">
      <div class="chart-card chart-card--wide">
        <div class="chart-card__header">
          <h3 class="chart-card__title">访问量趋势</h3>
          <el-radio-group v-model="visitDays" size="small" @change="fetchVisitTrend">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="14">近14天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
        <div ref="visitChartRef" class="chart-card__body"></div>
      </div>

      <div class="chart-card">
        <div class="chart-card__header">
          <h3 class="chart-card__title">诈骗类型分布</h3>
        </div>
        <div ref="pieChartRef" class="chart-card__body"></div>
      </div>
    </div>

    <div class="dashboard__charts">
      <div class="chart-card">
        <div class="chart-card__header">
          <h3 class="chart-card__title">各院系平均得分</h3>
        </div>
        <div ref="barChartRef" class="chart-card__body"></div>
      </div>

      <div class="chart-card">
        <div class="chart-card__header">
          <h3 class="chart-card__title">用户活跃度(24小时)</h3>
        </div>
        <div ref="heatmapChartRef" class="chart-card__body"></div>
      </div>
    </div>

    <div class="dashboard__table">
      <div class="chart-card">
        <div class="chart-card__header">
          <h3 class="chart-card__title">TOP案例排行榜</h3>
        </div>
        <el-table :data="dashboardData.topCases" stripe class="top-cases-table">
          <el-table-column type="index" label="排名" width="60" align="center">
            <template #default="{ $index }">
              <span class="rank-badge" :class="`rank-badge--${$index + 1}`">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="案例标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="caseType" label="类型" width="120">
            <template #default="{ row }">
              <el-tag size="small" type="danger">{{ row.caseType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="浏览量" width="100" align="center">
            <template #default="{ row }">
              <span class="count-value">{{ formatNumber(row.viewCount) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="likeCount" label="点赞数" width="100" align="center">
            <template #default="{ row }">
              <span class="count-value count-value--like">{{ formatNumber(row.likeCount) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="hotScore" label="热度得分" width="100" align="center">
            <template #default="{ row }">
              <span class="hot-score">{{ formatNumber(row.hotScore) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="dashboard__loading" v-if="loading">
      <el-icon class="is-loading"><loading /></el-icon>
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Loading } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getDashboardData,
  getVisitTrend,
  getFraudTypeDistribution,
  getDepartmentScores,
  getHourlyActivity,
  refreshStatistics,
  exportDailyStatistics,
  exportDepartmentStatistics
} from '@/api/statistics'
import type { DashboardVO, VisitTrendVO, FraudTypeDistVO, DepartmentScoreVO, HourlyActivityVO } from '@/api/statistics'

const loading = ref(false)
const refreshLoading = ref(false)
const visitDays = ref(7)

const dashboardData = reactive<DashboardVO>({
  todayViews: 0,
  todayNewUsers: 0,
  todayActiveUsers: 0,
  totalCases: 0,
  totalUsers: 0,
  totalChallengeCompletions: 0,
  avgTestScore: 0,
  visitTrend: { dates: [], pageViews: [], activeUsers: [], newUsers: [] },
  fraudTypeDist: { types: [], counts: [] },
  departmentScores: { departments: [], avgScores: [], userCounts: [], completionRates: [] },
  topCases: [],
  hourlyActivity: []
})

const visitChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
const barChartRef = ref<HTMLElement>()
const heatmapChartRef = ref<HTMLElement>()

let visitChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null
let heatmapChart: echarts.ECharts | null = null

const formatNumber = (num: number): string => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num.toLocaleString()
}

const fetchDashboard = async () => {
  loading.value = true
  try {
    const res = await getDashboardData()
    Object.assign(dashboardData, res.data)
    await nextTick()
    initAllCharts()
  } catch (error) {
    console.error('获取看板数据失败', error)
  } finally {
    loading.value = false
  }
}

const fetchVisitTrend = async () => {
  try {
    const res = await getVisitTrend(visitDays.value)
    dashboardData.visitTrend = res.data
    updateVisitChart()
  } catch (error) {
    console.error('获取访问趋势失败', error)
  }
}

const initVisitChart = () => {
  if (!visitChartRef.value) return
  visitChart = echarts.init(visitChartRef.value)
}

const updateVisitChart = () => {
  if (!visitChart) return
  visitChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e8e8e8',
      textStyle: { color: '#333' }
    },
    legend: {
      data: ['页面浏览量', '活跃用户', '新增用户'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dashboardData.visitTrend.dates,
      axisLine: { lineStyle: { color: '#e8e8e8' } },
      axisLabel: { color: '#666' }
    },
    yAxis: [
      {
        type: 'value',
        name: '浏览量',
        axisLine: { lineStyle: { color: '#e8e8e8' } },
        axisLabel: { color: '#666' },
        splitLine: { lineStyle: { color: '#f5f5f5' } }
      },
      {
        type: 'value',
        name: '用户数',
        axisLine: { lineStyle: { color: '#e8e8e8' } },
        axisLabel: { color: '#666' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '页面浏览量',
        type: 'line',
        smooth: true,
        data: dashboardData.visitTrend.pageViews,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ])
        },
        lineStyle: { color: '#409eff', width: 2 },
        itemStyle: { color: '#409eff' }
      },
      {
        name: '活跃用户',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: dashboardData.visitTrend.activeUsers,
        lineStyle: { color: '#67c23a', width: 2 },
        itemStyle: { color: '#67c23a' }
      },
      {
        name: '新增用户',
        type: 'bar',
        yAxisIndex: 1,
        barWidth: '40%',
        data: dashboardData.visitTrend.newUsers,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#e6a23c' },
            { offset: 1, color: '#f56c6c' }
          ]),
          borderRadius: [4, 4, 0, 0]
        }
      }
    ]
  })
}

const initPieChart = () => {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value)

  const colors = ['#ff6b6b', '#9b59b6', '#3498db', '#2ecc71', '#e74c3c', '#f39c12', '#1abc9c', '#e91e63']
  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e8e8e8',
      textStyle: { color: '#333' }
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { color: '#666' }
    },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold' }
        },
        labelLine: { show: false },
        data: dashboardData.fraudTypeDist.types.map((type, i) => ({
          name: type,
          value: dashboardData.fraudTypeDist.counts[i],
          itemStyle: { color: colors[i % colors.length] }
        }))
      }
    ]
  })
}

const initBarChart = () => {
  if (!barChartRef.value) return
  barChart = echarts.init(barChartRef.value)

  barChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e8e8e8',
      textStyle: { color: '#333' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dashboardData.departmentScores.departments,
      axisLabel: {
        color: '#666',
        rotate: dashboardData.departmentScores.departments.length > 5 ? 30 : 0
      },
      axisLine: { lineStyle: { color: '#e8e8e8' } }
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLine: { lineStyle: { color: '#e8e8e8' } },
      axisLabel: { color: '#666' },
      splitLine: { lineStyle: { color: '#f5f5f5' } }
    },
    series: [
      {
        type: 'bar',
        data: dashboardData.departmentScores.avgScores,
        barWidth: '50%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409eff' },
            { offset: 1, color: '#79bbff' }
          ]),
          borderRadius: [6, 6, 0, 0]
        },
        emphasis: {
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#66b1ff' },
              { offset: 1, color: '#a0cfff' }
            ])
          }
        }
      }
    ]
  })
}

const initHeatmapChart = () => {
  if (!heatmapChartRef.value) return
  heatmapChart = echarts.init(heatmapChartRef.value)

  const hours = Array.from({ length: 24 }, (_, i) => i)
  const activityMap = new Map(dashboardData.hourlyActivity.map(h => [h.hour, h.count]))
  const maxCount = Math.max(...dashboardData.hourlyActivity.map(h => h.count), 1)

  const data = hours.map(hour => [hour, 0, activityMap.get(hour) || 0])

  heatmapChart.setOption({
    tooltip: {
      position: 'top',
      formatter: (params: any) => {
        return `${params.value[0]}:00 - ${params.value[0] + 1}:00<br/>活跃次数: ${params.value[2]}`
      },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e8e8e8',
      textStyle: { color: '#333' }
    },
    grid: {
      left: '2%',
      right: '2%',
      bottom: '2%',
      top: '2%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: hours,
      splitArea: { show: true },
      axisLabel: {
        interval: 2,
        color: '#666'
      },
      axisLine: { lineStyle: { color: '#e8e8e8' } }
    },
    yAxis: {
      type: 'category',
      data: ['活跃度'],
      axisLabel: { show: false },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    visualMap: {
      min: 0,
      max: maxCount,
      calculable: false,
      orient: 'horizontal',
      left: 'center',
      bottom: '0%',
      inRange: {
        color: ['#e8f4fd', '#b3d8fd', '#67c23a', '#e6a23c', '#f56c6c']
      }
    },
    series: [
      {
        type: 'heatmap',
        data: data,
        label: { show: true, color: '#333', fontSize: 10 },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.3)'
          }
        }
      }
    ]
  })
}

const initAllCharts = () => {
  initVisitChart()
  initPieChart()
  initBarChart()
  initHeatmapChart()
  updateVisitChart()
}

const handleResize = () => {
  visitChart?.resize()
  pieChart?.resize()
  barChart?.resize()
  heatmapChart?.resize()
}

const handleRefresh = async () => {
  refreshLoading.value = true
  try {
    await refreshStatistics()
    ElMessage.success('数据刷新成功')
    await fetchDashboard()
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    refreshLoading.value = false
  }
}

const handleExport = async (command: string) => {
  try {
    if (command === 'daily') {
      await ElMessageBox.confirm('确定要导出每日统计数据吗？', '导出确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      })
      await exportDailyStatistics()
      ElMessage.success('导出成功')
    } else {
      await ElMessageBox.confirm('确定要导出院系统计数据吗？', '导出确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      })
      await exportDepartmentStatistics()
      ElMessage.success('导出成功')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('导出失败')
    }
  }
}

const handleExportPdf = async () => {
  try {
    await ElMessageBox.confirm('确定要导出数据看板PDF吗？包含所有图表和数据。', '导出确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })

    ElMessage.info('正在生成PDF，请稍候...')

    // 创建PDF内容
    const container = document.createElement('div')
    container.style.cssText = 'position: fixed; left: -9999px; top: 0; width: 1200px; background: #fff; padding: 40px; font-family: "Microsoft YaHei", "PingFang SC", sans-serif;'

    // 标题
    const title = document.createElement('h1')
    title.style.cssText = 'text-align: center; font-size: 28px; color: #1a1a1a; margin-bottom: 30px;'
    title.textContent = '反诈学习平台 - 数据看板'
    container.appendChild(title)

    // 生成日期
    const dateInfo = document.createElement('p')
    dateInfo.style.cssText = 'text-align: center; color: #666; margin-bottom: 30px;'
    dateInfo.textContent = `导出时间: ${new Date().toLocaleString('zh-CN')}`
    container.appendChild(dateInfo)

    // 统计数据卡片
    const statsGrid = document.createElement('div')
    statsGrid.style.cssText = 'display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 30px;'
    const statsData = [
      { label: '总用户数', value: dashboardData.totalUsers },
      { label: '今日活跃用户', value: dashboardData.todayActiveUsers },
      { label: '今日浏览量', value: dashboardData.todayViews },
      { label: '案例总数', value: dashboardData.totalCases },
      { label: '平均测试得分', value: dashboardData.avgTestScore },
      { label: '闯关完成数', value: dashboardData.totalChallengeCompletions }
    ]
    statsData.forEach(stat => {
      const card = document.createElement('div')
      card.style.cssText = 'background: #f5f7fa; padding: 20px; border-radius: 8px; text-align: center;'
      card.innerHTML = `<div style="font-size: 24px; font-weight: bold; color: #409eff;">${formatNumber(stat.value || 0)}</div><div style="color: #666; margin-top: 8px;">${stat.label}</div>`
      statsGrid.appendChild(card)
    })
    container.appendChild(statsGrid)

    // 图表区域标题
    const chartsTitle = document.createElement('h2')
    chartsTitle.style.cssText = 'font-size: 18px; color: #1a1a1a; margin: 30px 0 20px; border-left: 4px solid #409eff; padding-left: 12px;'
    chartsTitle.textContent = '数据图表'
    container.appendChild(chartsTitle)

    // 图表容器
    const chartsContainer = document.createElement('div')
    chartsContainer.style.cssText = 'display: flex; flex-direction: column; gap: 24px;'

    // 访问量趋势图
    if (visitChart) {
      const trendImg = document.createElement('img')
      trendImg.src = visitChart.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: '#fff' })
      trendImg.style.cssText = 'width: 100%; max-width: 800px; margin: 0 auto; display: block;'
      const trendTitle = document.createElement('h3')
      trendTitle.style.cssText = 'text-align: center; color: #333; margin: 16px 0;'
      trendTitle.textContent = '访问量趋势'
      chartsContainer.appendChild(trendTitle)
      chartsContainer.appendChild(trendImg)
    }

    // 诈骗类型分布图
    if (pieChart) {
      const pieImg = document.createElement('img')
      pieImg.src = pieChart.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: '#fff' })
      pieImg.style.cssText = 'width: 100%; max-width: 600px; margin: 0 auto; display: block;'
      const pieTitle = document.createElement('h3')
      pieTitle.style.cssText = 'text-align: center; color: #333; margin: 16px 0;'
      pieTitle.textContent = '诈骗类型分布'
      chartsContainer.appendChild(pieTitle)
      chartsContainer.appendChild(pieImg)
    }

    // 院系得分图
    if (barChart) {
      const barImg = document.createElement('img')
      barImg.src = barChart.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: '#fff' })
      barImg.style.cssText = 'width: 100%; max-width: 600px; margin: 0 auto; display: block;'
      const barTitle = document.createElement('h3')
      barTitle.style.cssText = 'text-align: center; color: #333; margin: 16px 0;'
      barTitle.textContent = '各院系平均得分'
      chartsContainer.appendChild(barTitle)
      chartsContainer.appendChild(barImg)
    }

    // 用户活跃度图
    if (heatmapChart) {
      const heatmapImg = document.createElement('img')
      heatmapImg.src = heatmapChart.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: '#fff' })
      heatmapImg.style.cssText = 'width: 100%; max-width: 600px; margin: 0 auto; display: block;'
      const heatmapTitle = document.createElement('h3')
      heatmapTitle.style.cssText = 'text-align: center; color: #333; margin: 16px 0;'
      heatmapTitle.textContent = '用户活跃度(24小时)'
      chartsContainer.appendChild(heatmapTitle)
      chartsContainer.appendChild(heatmapImg)
    }

    container.appendChild(chartsContainer)

    // TOP案例表格
    const tableTitle = document.createElement('h2')
    tableTitle.style.cssText = 'font-size: 18px; color: #1a1a1a; margin: 30px 0 20px; border-left: 4px solid #67c23a; padding-left: 12px;'
    tableTitle.textContent = 'TOP案例排行榜'
    container.appendChild(tableTitle)

    const table = document.createElement('table')
    table.style.cssText = 'width: 100%; border-collapse: collapse; margin-bottom: 40px;'
    table.innerHTML = `
      <thead>
        <tr style="background: #f5f7fa;">
          <th style="padding: 12px; border: 1px solid #e8e8e8; text-align: center;">排名</th>
          <th style="padding: 12px; border: 1px solid #e8e8e8; text-align: left;">案例标题</th>
          <th style="padding: 12px; border: 1px solid #e8e8e8; text-align: center;">类型</th>
          <th style="padding: 12px; border: 1px solid #e8e8e8; text-align: center;">浏览量</th>
          <th style="padding: 12px; border: 1px solid #e8e8e8; text-align: center;">点赞数</th>
          <th style="padding: 12px; border: 1px solid #e8e8e8; text-align: center;">热度得分</th>
        </tr>
      </thead>
      <tbody>
        ${dashboardData.topCases.map((c, i) => `
          <tr>
            <td style="padding: 12px; border: 1px solid #e8e8e8; text-align: center;">${i + 1}</td>
            <td style="padding: 12px; border: 1px solid #e8e8e8; text-align: left;">${c.title}</td>
            <td style="padding: 12px; border: 1px solid #e8e8e8; text-align: center;">${c.caseType}</td>
            <td style="padding: 12px; border: 1px solid #e8e8e8; text-align: center;">${formatNumber(c.viewCount)}</td>
            <td style="padding: 12px; border: 1px solid #e8e8e8; text-align: center;">${formatNumber(c.likeCount)}</td>
            <td style="padding: 12px; border: 1px solid #e8e8e8; text-align: center;">${formatNumber(c.hotScore)}</td>
          </tr>
        `).join('')}
      </tbody>
    `
    container.appendChild(table)

    document.body.appendChild(container)

    // 使用html2canvas生成图片，然后转PDF
    const canvas = document.createElement('canvas')
    const scale = 2
    canvas.width = 1200 * scale
    canvas.height = container.offsetHeight * scale
    const ctx = canvas.getContext('2d')!
    ctx.scale(scale, scale)
    ctx.fillStyle = '#fff'

    const opt = {
      scale: scale,
      useCORS: true,
      allowTaint: true,
      backgroundColor: '#ffffff'
    }

    // 动态加载html2canvas
    const html2canvasScript = document.createElement('script')
    html2canvasScript.src = 'https://cdn.bootcdn.net/ajax/libs/html2canvas/1.4.1/html2canvas.min.js'
    html2canvasScript.onload = async () => {
      try {
        const html2canvas = (window as any).html2canvas
        const canvasEl = await html2canvas(container, opt)

        // 创建PDF
        const imgData = canvasEl.toDataURL('image/png')
        const pdfWidth = 1200
        const pdfHeight = (canvasEl.height / canvasEl.width) * pdfWidth

        // 使用jsPDF
        const jspdfScript = document.createElement('script')
        jspdfScript.src = 'https://cdn.bootcdn.net/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js'
        jspdfScript.onload = async () => {
          try {
            const { jsPDF } = (window as any).jspdf
            const pdf = new jsPDF({
              orientation: pdfHeight > pdfWidth ? 'portrait' : 'landscape',
              unit: 'px',
              format: [pdfWidth, pdfHeight]
            })

            pdf.addImage(imgData, 'PNG', 0, 0, pdfWidth, pdfHeight)
            pdf.save(`数据看板_${new Date().toISOString().split('T')[0]}.pdf`)

            ElMessage.success('PDF导出成功')
          } catch (err) {
            console.error('PDF生成失败:', err)
            ElMessage.error('PDF生成失败')
          } finally {
            document.body.removeChild(container)
          }
        }
        document.head.appendChild(jspdfScript)
      } catch (err) {
        console.error('Canvas生成失败:', err)
        ElMessage.error('PDF生成失败')
        document.body.removeChild(container)
      }
    }
    html2canvasScript.onerror = () => {
      ElMessage.error('加载PDF生成库失败，请检查网络连接')
      document.body.removeChild(container)
    }
    document.head.appendChild(html2canvasScript)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('导出失败')
    }
  }
}

onMounted(() => {
  fetchDashboard()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  visitChart?.dispose()
  pieChart?.dispose()
  barChart?.dispose()
  heatmapChart?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
  }

  &__title {
    font-size: 22px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0;
  }

  &__actions {
    display: flex;
    gap: 12px;

    .btn-icon {
      margin-right: 4px;
    }
  }

  &__stats {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 16px;
    margin-bottom: 20px;
  }

  &__charts {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: 16px;
    margin-bottom: 20px;
  }

  &__table {
    margin-bottom: 20px;
  }

  &__loading {
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 16px 24px;
    background: rgba(255, 255, 255, 0.95);
    border-radius: 8px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    z-index: 1000;
  }
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  }

  &__icon {
    width: 52px;
    height: 52px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    svg {
      width: 26px;
      height: 26px;
    }
  }

  &__content {
    flex: 1;
    min-width: 0;
  }

  &__value {
    font-size: 26px;
    font-weight: 700;
    color: #1a1a1a;
    line-height: 1.2;
  }

  &__label {
    font-size: 13px;
    color: #8c8c8c;
    margin-top: 4px;
  }

  &__label--tooltip {
    cursor: help;
    text-decoration: underline dotted hsl(0 0% 60% / 0.85);
    text-underline-offset: 3px;
    width: fit-content;
  }

  &--blue .stat-card__icon {
    background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
    color: #fff;
  }

  &--green .stat-card__icon {
    background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
    color: #fff;
  }

  &--purple .stat-card__icon {
    background: linear-gradient(135deg, #9c27b0 0%, #ba68c8 100%);
    color: #fff;
  }

  &--orange .stat-card__icon {
    background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
    color: #fff;
  }

  &--red .stat-card__icon {
    background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
    color: #fff;
  }

  &--teal .stat-card__icon {
    background: linear-gradient(135deg, #1abc9c 0%, #48d1a8 100%);
    color: #fff;
  }
}

.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  &--wide {
    grid-column: span 1;
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0;
  }

  &__body {
    height: 280px;
  }
}

.top-cases-table {
  .rank-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;

    &--1 {
      background: linear-gradient(135deg, #ffd700, #ffb700);
      color: #fff;
    }

    &--2 {
      background: linear-gradient(135deg, #c0c0c0, #a8a8a8);
      color: #fff;
    }

    &--3 {
      background: linear-gradient(135deg, #cd7f32, #b87333);
      color: #fff;
    }

    &--4,
    &--5 {
      background: #f5f5f5;
      color: #666;
    }
  }

  .count-value {
    font-weight: 500;
    color: #409eff;

    &--like {
      color: #f56c6c;
    }
  }

  .hot-score {
    font-weight: 600;
    color: #67c23a;
  }
}

@media (max-width: 1200px) {
  .dashboard__charts {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .dashboard {
    padding: 16px;

    &__header {
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
    }

    &__stats {
      grid-template-columns: repeat(2, 1fr);
    }
  }

  .stat-card {
    padding: 16px;

    &__value {
      font-size: 20px;
    }

    &__icon {
      width: 44px;
      height: 44px;

      svg {
        width: 22px;
        height: 22px;
      }
    }
  }
}
</style>
