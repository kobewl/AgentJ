<template>
  <div class="dashboard">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <h1 class="page-title">AgentJ 控制台</h1>
          <p class="page-subtitle">AI 智能助手管理系统</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" @click="refreshData">
            <el-icon><Refresh /></el-icon>
            刷新数据
          </el-button>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid" v-loading="stats.length === 0">
      <el-card class="stat-card" v-for="stat in stats" :key="stat.title">
        <div class="stat-content">
          <div class="stat-icon" :style="{ background: stat.color + '20', color: stat.color }">
            <el-icon size="24">
              <component :is="stat.icon" />
            </el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-title">{{ stat.title }}</div>
            <div class="stat-trend" :class="stat.trend > 0 ? 'up' : 'down'">
              <el-icon size="12">
                <component :is="stat.trend > 0 ? 'Top' : 'Bottom'" />
              </el-icon>
              {{ Math.abs(stat.trend) }}%
            </div>
          </div>
        </div>
      </el-card>
      <div v-if="stats.length === 0" class="empty-stats">
        <el-icon size="32" class="empty-icon"><DataAnalysis /></el-icon>
        <p>暂无统计数据</p>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="content-grid">
      <!-- 左侧：快速操作 -->
      <div class="left-panel">
        <el-card class="quick-actions">
          <template #header>
            <div class="card-header">
              <el-icon><Operation /></el-icon>
              <span>快速操作</span>
            </div>
          </template>
          <div class="actions-grid">
            <el-button 
              v-for="action in quickActions" 
              :key="action.key"
              @click="handleQuickAction(action)"
              :type="action.type"
              class="action-btn"
            >
              <el-icon size="16">
                <component :is="action.icon" />
              </el-icon>
              <span>{{ action.title }}</span>
            </el-button>
          </div>
        </el-card>

        <!-- 系统状态 -->
        <el-card class="system-status" v-loading="systemStatus.length === 0">
          <template #header>
            <div class="card-header">
              <el-icon><Monitor /></el-icon>
              <span>系统状态</span>
            </div>
          </template>
          <div class="status-list" v-if="systemStatus.length > 0">
            <div 
              v-for="status in systemStatus" 
              :key="status.name"
              class="status-item"
            >
              <div class="status-indicator" :class="status.status">
                <el-icon size="8">
                  <component :is="getStatusIcon(status.status)" />
                </el-icon>
              </div>
              <span class="status-name">{{ status.name }}</span>
              <span class="status-value">{{ status.value }}</span>
            </div>
          </div>
          <div v-else class="empty-status">
            <el-icon size="32" class="empty-icon"><Monitor /></el-icon>
            <p>暂无系统状态信息</p>
          </div>
        </el-card>
      </div>

      <!-- 右侧：数据图表 -->
      <div class="right-panel">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <el-icon><TrendCharts /></el-icon>
              <span>对话统计</span>
              <el-select v-model="chartPeriod" size="small" class="period-select">
                <el-option label="今日" value="day" />
                <el-option label="本周" value="week" />
                <el-option label="本月" value="month" />
              </el-select>
            </div>
          </template>
          <div class="chart-container">
            <div class="chart-placeholder">
              <el-icon size="48" class="chart-icon"><DataAnalysis /></el-icon>
              <p>对话数据图表</p>
              <el-button type="primary" size="small" @click="loadChartData">
                加载数据
              </el-button>
            </div>
          </div>
        </el-card>

        <!-- 最近活动 -->
        <el-card class="recent-activity">
          <template #header>
            <div class="card-header">
              <el-icon><Clock /></el-icon>
              <span>最近活动</span>
            </div>
          </template>
          <div class="activity-list">
            <div 
              v-for="activity in recentActivities" 
              :key="activity.id"
              class="activity-item"
            >
              <div class="activity-icon" :style="{ background: activity.color + '20', color: activity.color }">
                <el-icon size="16">
                  <component :is="activity.icon" />
                </el-icon>
              </div>
              <div class="activity-content">
                <div class="activity-title">{{ activity.title }}</div>
                <div class="activity-time">{{ formatTime(activity.time) }}</div>
              </div>
              <el-tag :type="activity.statusType" size="small">
                {{ activity.status }}
              </el-tag>
            </div>
            <div v-if="recentActivities.length === 0" class="empty-activity">
              <el-icon size="32" class="empty-icon"><Inbox /></el-icon>
              <p>暂无活动记录</p>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 底部信息 -->
    <div class="footer-info">
      <el-card class="info-card">
        <div class="info-content">
          <div class="info-item">
            <el-icon><InfoFilled /></el-icon>
            <span>AgentJ v0.0.1 - AI智能助手管理系统</span>
          </div>
          <div class="info-item">
            <el-icon><Clock /></el-icon>
            <span>最后更新: {{ lastUpdateTime }}</span>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

const router = useRouter();
const chartPeriod = ref('day');
const lastUpdateTime = ref('');

// 统计数据 - 初始为空，等待后端数据
const stats = ref([]);

// 快速操作
const quickActions = ref([
  {
    key: 'chat',
    title: '开始对话',
    icon: 'ChatDotRound',
    type: 'primary'
  },
  {
    key: 'history',
    title: '查看历史',
    icon: 'Document',
    type: 'default'
  },
  {
    key: 'settings',
    title: '系统设置',
    icon: 'Setting',
    type: 'default'
  },
  {
    key: 'help',
    title: '使用帮助',
    icon: 'QuestionFilled',
    type: 'info'
  }
]);

// 系统状态 - 初始为空，等待后端数据
const systemStatus = ref([]);

// 最近活动 - 初始为空，等待后端数据
const recentActivities = ref([]);

// 获取状态图标
const getStatusIcon = (status: string) => {
  switch (status) {
    case 'success': return 'CircleCheck';
    case 'warning': return 'WarningFilled';
    case 'error': return 'CircleClose';
    default: return 'InfoFilled';
  }
};

// 格式化时间
const formatTime = (date: Date) => {
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const minutes = Math.floor(diff / 60000);
  
  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (minutes < 1440) return `${Math.floor(minutes / 60)}小时前`;
  
  return date.toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 处理快速操作
const handleQuickAction = (action: any) => {
  switch (action.key) {
    case 'chat':
      router.push('/chat');
      break;
    case 'history':
      ElMessage.info('历史功能开发中');
      break;
    case 'settings':
      ElMessage.info('设置功能开发中');
      break;
    case 'help':
      ElMessage.info('帮助功能开发中');
      break;
  }
};

// 刷新数据
const refreshData = () => {
  lastUpdateTime.value = new Date().toLocaleString('zh-CN');
  ElMessage.success('数据已刷新');
};

// 加载图表数据
const loadChartData = () => {
  ElMessage.info('图表数据加载中...');
};

// 初始化
onMounted(() => {
  lastUpdateTime.value = new Date().toLocaleString('zh-CN');
});
</script>

<style scoped>
.dashboard {
  padding: 24px;
  background: radial-gradient(circle at 20% 20%, rgb(37 99 235 / 0.04), transparent 35%), var(--bg-secondary);
  min-height: 100vh;
}

.page-header {
  margin-bottom: 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bg-glass);
  padding: 20px 24px;
  border-radius: 16px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(12px);
}

.title-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
}

.page-subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
}

.header-actions {
  display: flex;
  gap: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 16px;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border-color);
  transition: all 0.3s ease;
  background: linear-gradient(145deg, var(--bg-primary), rgb(37 99 235 / 0.03));
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.stat-title {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.stat-trend {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 2px;
}

.stat-trend.up {
  color: var(--success-color);
}

.stat-trend.down {
  color: var(--danger-color);
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 20px;
  margin-bottom: 24px;
}

.left-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.right-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.quick-actions,
.system-status,
.chart-card,
.recent-activity {
  border-radius: 16px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.actions-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  height: auto;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.action-btn:hover {
  transform: translateY(-1px);
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.status-indicator.status-success {
  background: var(--success-color);
  color: white;
}

.status-indicator.status-warning {
  background: var(--warning-color);
  color: white;
}

.status-indicator.status-error {
  background: var(--danger-color);
  color: white;
}

.status-name {
  flex: 1;
  font-size: 14px;
  color: var(--text-primary);
}

.status-value {
  font-size: 12px;
  color: var(--text-secondary);
}

.period-select {
  margin-left: auto;
  width: 100px;
}

.chart-container {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, rgb(37 99 235 / 0.05), rgb(124 58 237 / 0.04));
  border-radius: 12px;
  border: 1px dashed var(--border-color);
}

.chart-placeholder {
  text-align: center;
  color: var(--text-secondary);
}

.chart-icon {
  margin-bottom: 16px;
  color: var(--text-tertiary);
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-primary);
  border-radius: 12px;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.activity-item:hover {
  background: rgb(37 99 235 / 0.06);
  border-color: var(--border-color);
}

.activity-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.activity-content {
  flex: 1;
}

.activity-title {
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.activity-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.empty-activity {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-secondary);
}

.empty-stats,
.empty-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--text-secondary);
  text-align: center;
}

.empty-icon {
  color: var(--text-tertiary);
  margin-bottom: 12px;
}

.footer-info {
  margin-top: 24px;
}

.info-card {
  border-radius: 14px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.info-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-secondary);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
  
  .stats-grid {
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  }
}

@media (max-width: 768px) {
  .dashboard {
    padding: 16px;
  }
  
  .header-content {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .actions-grid {
    grid-template-columns: 1fr;
  }
  
  .info-content {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 20px;
  }
  
  .stat-value {
    font-size: 24px;
  }
  
  .chart-container {
    height: 250px;
  }
}
</style>
