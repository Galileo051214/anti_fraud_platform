<template>
  <div class="agent-page">
    <header class="agent-header">
      <button class="back-btn" type="button" @click="router.push('/challenge')">
        <span aria-hidden="true">‹</span>
        返回
      </button>
      <div class="agent-header__main">
        <div class="agent-header__title-row">
          <h1>{{ session?.challengeTitle || detail?.title || 'Agent模拟挑战' }}</h1>
          <el-tag v-if="session?.status === 'completed'" :type="session.passed ? 'success' : 'danger'" effect="dark">
            {{ session.passed ? '已通过' : '未通过' }}
          </el-tag>
          <el-tag v-else type="warning" effect="plain">进行中</el-tag>
        </div>
        <p>{{ agentConfig?.scenarioBrief || detail?.description || '通过对话测试你的反诈处置能力。' }}</p>
        <div class="agent-header__meta">
          <span>{{ agentConfig?.fraudType || '诈骗模拟' }}</span>
          <span>第 {{ currentRound }} / {{ maxRounds }} 轮</span>
          <span>75分通过</span>
          <span>每日首次通过 +100积分</span>
        </div>
      </div>
    </header>

    <main class="agent-shell">
      <section class="chat-panel">
        <div class="chat-panel__top">
          <div>
            <strong>对话模拟</strong>
            <span>诈骗犯 Agent 会持续施压，请保持警惕并作出安全回应。</span>
          </div>
          <el-progress :percentage="roundProgress" :show-text="false" />
        </div>

        <div ref="messageListRef" class="message-list" @scroll="handleMessageScroll" v-loading="starting">
          <template v-if="messages.length">
            <div
              v-for="(message, index) in messages"
              :key="`${message.role}-${message.round}-${index}`"
              class="message"
              :class="`message--${message.role}`"
            >
              <div class="message__avatar">
                <IconBot v-if="message.role === 'agent'" :size="18" />
                <IconShield v-else :size="18" />
              </div>
              <div class="message__bubble">
                <div class="message__meta">
                  <span>{{ message.role === 'agent' ? agentName : '我的回应' }}</span>
                  <small>第{{ message.round }}轮</small>
                </div>
                <p>{{ message.content || '正在输入...' }}</p>
              </div>
            </div>
          </template>
          <el-empty v-else-if="!starting" description="暂未开始挑战" />
        </div>

        <form class="reply-box" @submit.prevent="sendReply">
          <el-input
            v-model="replyText"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            resize="none"
            :disabled="!canReply"
            placeholder="输入你的回应，例如：我不会转账，会通过官方渠道核实。"
            @keydown.ctrl.enter.prevent="sendReply"
          />
          <div class="reply-box__actions">
            <span>{{ replyHint }}</span>
            <el-button type="primary" native-type="submit" :loading="submitting" :disabled="!canReply || !replyText.trim()">
              发送回应
            </el-button>
          </div>
        </form>
      </section>

      <aside class="side-panel">
        <section class="info-block">
          <h2>风险线索</h2>
          <ul>
            <li v-for="item in agentConfig?.riskPoints || []" :key="item">{{ item }}</li>
          </ul>
        </section>

        <section class="info-block info-block--safe">
          <h2>安全应对</h2>
          <ul>
            <li v-for="item in agentConfig?.safeActions || []" :key="item">{{ item }}</li>
          </ul>
        </section>

        <section v-if="session?.status === 'completed'" class="result-panel" :class="{ 'result-panel--passed': session.passed }">
          <div class="result-panel__score">
            <span>{{ session.finalScore ?? 0 }}</span>
            <small>/100</small>
          </div>
          <strong>{{ session.scoringReport?.rating || ratingText }}</strong>
          <p>{{ session.scoringReport?.summary || session.summary }}</p>
          <div class="reward-line">
            <span>{{ session.rewardGranted ? `已获得 ${session.earnedScore || 100} 积分` : '今日未获得新奖励' }}</span>
          </div>
        </section>
      </aside>
    </main>

    <section v-if="session?.status === 'completed' && session.scoringReport" class="report-section">
      <div class="report-section__header">
        <h2>评分报告</h2>
        <el-button plain @click="restartChallenge" :loading="starting">重新挑战</el-button>
      </div>
      <div class="score-grid">
        <div v-for="item in scoreItems" :key="item.label" class="score-item">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
      <div class="report-columns">
        <div>
          <h3>需要改进</h3>
          <ul>
            <li v-for="item in session.scoringReport.keyMistakes || []" :key="item">{{ item }}</li>
          </ul>
        </div>
        <div>
          <h3>正确做法</h3>
          <ul>
            <li v-for="item in session.scoringReport.correctActions || []" :key="item">{{ item }}</li>
          </ul>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getChallengeDetail,
  replyAgentChallengeStream,
  startAgentChallenge,
  type AgentChallengeMessage,
  type AgentChallengeSessionVO,
  type ChallengeVO
} from '@/api/challenge'
import { IconBot, IconShield } from '@/components/icons'

type LocalMessage = AgentChallengeMessage & { streaming?: boolean }

const route = useRoute()
const router = useRouter()
const challengeId = Number(route.params.id)

const detail = ref<ChallengeVO | null>(null)
const session = ref<AgentChallengeSessionVO | null>(null)
const messages = ref<LocalMessage[]>([])
const replyText = ref('')
const starting = ref(false)
const submitting = ref(false)
const messageListRef = ref<HTMLElement | null>(null)
const autoScroll = ref(true)

const agentConfig = computed(() => session.value?.agentConfig || detail.value?.agentConfig)
const maxRounds = computed(() => session.value?.maxRounds || 5)
const currentRound = computed(() => session.value?.currentRound || 1)
const roundProgress = computed(() => Math.min(100, Math.round((currentRound.value / maxRounds.value) * 100)))
const canReply = computed(() => Boolean(session.value?.sessionId) && session.value?.status === 'in_progress' && !starting.value && !submitting.value)
const agentName = computed(() => agentConfig.value?.persona || '诈骗犯Agent')
const ratingText = computed(() => {
  const score = session.value?.finalScore || 0
  if (score >= 90) return '优秀'
  if (score >= 80) return '良好'
  if (score >= 60) return '需加强'
  return '高风险'
})
const replyHint = computed(() => {
  if (session.value?.status === 'completed') return '挑战已结束，可查看评分报告或重新挑战'
  if (!session.value) return '正在初始化挑战'
  return '按 Ctrl + Enter 也可发送'
})

const scoreItems = computed(() => {
  const report = session.value?.scoringReport
  return [
    { label: '风险识别', value: `${report?.riskIdentificationScore ?? 0}/30` },
    { label: '拒绝高危操作', value: `${report?.highRiskRejectionScore ?? 0}/30` },
    { label: '官方核验', value: `${report?.officialVerificationScore ?? 0}/20` },
    { label: '证据与求助', value: `${report?.evidenceAndHelpScore ?? 0}/10` },
    { label: '沟通稳定性', value: `${report?.communicationStabilityScore ?? 0}/10` }
  ]
})

const exportMessages = () => {
  return messages.value
    .filter((item) => item.content?.trim())
    .map((item) => ({
      role: item.role,
      round: item.round,
      content: item.content,
      createTime: item.createTime
    }))
}

const normalizeMessages = (data?: AgentChallengeSessionVO | null, preserveLocal = false) => {
  const incoming = (data?.messages || []).map((item) => ({ ...item }))
  if (preserveLocal && incoming.length < exportMessages().length) {
    return
  }
  messages.value = incoming
}

const maybeScrollToBottom = async () => {
  if (!autoScroll.value) return
  await nextTick()
  const el = messageListRef.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

const handleMessageScroll = () => {
  const el = messageListRef.value
  if (!el) return
  autoScroll.value = el.scrollHeight - el.scrollTop - el.clientHeight < 80
}

const loadDetail = async () => {
  detail.value = await getChallengeDetail(challengeId)
  if (detail.value.type !== 'agent_scenario') {
    ElMessage.warning('该关卡不是Agent模拟挑战')
    router.replace('/challenge')
  }
}

const start = async () => {
  starting.value = true
  try {
    const data = await startAgentChallenge(challengeId)
    session.value = data
    normalizeMessages(data)
    autoScroll.value = true
    maybeScrollToBottom()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Agent模拟挑战启动失败')
  } finally {
    starting.value = false
  }
}

const restartChallenge = async () => {
  replyText.value = ''
  await start()
}

const sendReply = async () => {
  const text = replyText.value.trim()
  if (!text || !session.value?.sessionId || !canReply.value) return

  const activeRound = currentRound.value
  replyText.value = ''
  submitting.value = true
  autoScroll.value = true
  messages.value.push({
    role: 'user',
    round: activeRound,
    content: text,
    createTime: new Date().toISOString()
  })
  const clientMessages = exportMessages()

  let agentPlaceholder: LocalMessage | null = null
  if (activeRound < maxRounds.value) {
    agentPlaceholder = {
      role: 'agent',
      round: activeRound + 1,
      content: '',
      createTime: new Date().toISOString(),
      streaming: true
    }
    messages.value.push(agentPlaceholder)
  }
  maybeScrollToBottom()

  try {
    await replyAgentChallengeStream(
      { sessionId: session.value.sessionId, message: text, clientMessages },
      {
        onDelta: (delta) => {
          if (agentPlaceholder) {
            agentPlaceholder.content += delta
            maybeScrollToBottom()
          }
        },
        onDone: (data) => {
          session.value = data
          normalizeMessages(data, true)
          maybeScrollToBottom()
        },
        onError: (message) => {
          ElMessage.error(message)
        }
      }
    )
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发送失败，请稍后再试')
    if (agentPlaceholder) {
      messages.value = messages.value.filter((item) => item !== agentPlaceholder)
    }
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadDetail()
  if (detail.value?.type === 'agent_scenario') {
    await start()
  }
})
</script>

<style scoped lang="scss">
.agent-page {
  min-height: 100vh;
  background: hsl(215, 24%, 96%);
  color: hsl(220, 22%, 16%);
  padding: 24px;
}

.agent-header,
.agent-shell,
.report-section {
  max-width: 1180px;
  margin: 0 auto;
}

.agent-header {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 20px;
  align-items: start;
  margin-bottom: 20px;
}

.back-btn {
  height: 38px;
  padding: 0 14px;
  border: 1px solid hsl(215, 18%, 84%);
  background: white;
  border-radius: 8px;
  color: hsl(215, 18%, 28%);
  cursor: pointer;
}

.agent-header__main {
  h1 {
    margin: 0;
    font-size: 26px;
    line-height: 1.25;
  }

  p {
    margin: 8px 0 0;
    color: hsl(218, 12%, 42%);
    line-height: 1.6;
  }
}

.agent-header__title-row,
.agent-header__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.agent-header__meta {
  margin-top: 12px;

  span {
    padding: 4px 8px;
    background: white;
    border: 1px solid hsl(215, 18%, 86%);
    border-radius: 6px;
    font-size: 12px;
    color: hsl(218, 14%, 38%);
  }
}

.agent-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
  align-items: start;
}

.chat-panel,
.side-panel > section,
.report-section {
  background: white;
  border: 1px solid hsl(215, 18%, 88%);
  border-radius: 8px;
  box-shadow: 0 8px 22px hsla(220, 20%, 30%, 0.06);
}

.chat-panel {
  overflow: hidden;
}

.chat-panel__top {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  gap: 18px;
  align-items: center;
  padding: 16px 18px;
  border-bottom: 1px solid hsl(215, 18%, 90%);

  strong {
    display: block;
    margin-bottom: 4px;
  }

  span {
    color: hsl(218, 12%, 48%);
    font-size: 13px;
  }
}

.message-list {
  height: min(58vh, 560px);
  min-height: 360px;
  overflow-y: auto;
  padding: 18px;
  background: linear-gradient(180deg, hsl(215, 24%, 98%) 0%, hsl(215, 24%, 96%) 100%);
}

.message {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  align-items: flex-start;

  &--user {
    flex-direction: row-reverse;

    .message__bubble {
      background: hsl(214, 74%, 52%);
      color: white;
      border-color: hsl(214, 74%, 52%);
    }

    .message__meta {
      color: hsla(0, 0%, 100%, 0.78);
    }
  }
}

.message__avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: hsl(168, 70%, 92%);
  color: hsl(168, 75%, 28%);
  flex: 0 0 auto;
}

.message__bubble {
  max-width: min(680px, 78%);
  padding: 11px 13px;
  border: 1px solid hsl(215, 18%, 86%);
  border-radius: 8px;
  background: white;

  p {
    margin: 4px 0 0;
    white-space: pre-wrap;
    word-break: break-word;
    line-height: 1.65;
  }
}

.message__meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: hsl(218, 12%, 46%);
}

.reply-box {
  padding: 16px 18px;
  border-top: 1px solid hsl(215, 18%, 90%);
}

.reply-box__actions {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;

  span {
    color: hsl(218, 12%, 48%);
    font-size: 12px;
  }
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.info-block,
.result-panel {
  padding: 16px;

  h2 {
    margin: 0 0 10px;
    font-size: 16px;
  }

  ul {
    margin: 0;
    padding-left: 18px;
    color: hsl(218, 12%, 42%);
    line-height: 1.7;
  }
}

.info-block--safe {
  border-color: hsl(145, 42%, 82%);
  background: hsl(145, 48%, 97%);
}

.result-panel {
  border-color: hsl(0, 66%, 86%);

  &--passed {
    border-color: hsl(145, 42%, 78%);
  }

  p {
    color: hsl(218, 12%, 42%);
    line-height: 1.6;
  }
}

.result-panel__score {
  display: flex;
  align-items: baseline;
  gap: 4px;

  span {
    font-size: 42px;
    font-weight: 800;
  }

  small {
    color: hsl(218, 12%, 48%);
  }
}

.reward-line {
  margin-top: 12px;
  padding: 8px 10px;
  border-radius: 6px;
  background: hsl(45, 90%, 94%);
  color: hsl(32, 80%, 32%);
  font-weight: 700;
}

.report-section {
  margin-top: 18px;
  padding: 18px;
}

.report-section__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;

  h2 {
    margin: 0;
    font-size: 20px;
  }
}

.score-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(5, minmax(120px, 1fr));
  gap: 10px;
}

.score-item {
  padding: 12px;
  border: 1px solid hsl(215, 18%, 88%);
  border-radius: 8px;
  background: hsl(215, 24%, 98%);

  span {
    display: block;
    color: hsl(218, 12%, 48%);
    font-size: 12px;
  }

  strong {
    display: block;
    margin-top: 6px;
    font-size: 20px;
  }
}

.report-columns {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;

  div {
    padding: 14px;
    border-radius: 8px;
    background: hsl(215, 24%, 98%);
  }

  h3 {
    margin: 0 0 10px;
    font-size: 15px;
  }

  ul {
    margin: 0;
    padding-left: 18px;
    color: hsl(218, 12%, 42%);
    line-height: 1.7;
  }
}

@media (max-width: 900px) {
  .agent-page {
    padding: 16px;
  }

  .agent-header,
  .agent-shell,
  .report-columns {
    grid-template-columns: 1fr;
  }

  .chat-panel__top {
    grid-template-columns: 1fr;
  }

  .score-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .message__bubble {
    max-width: 84%;
  }
}
</style>
