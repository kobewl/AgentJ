<template>
  <div class="dashboard">
    <!-- 简洁的页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <div class="brand-logo">
            <el-icon size="32"><Cpu /></el-icon>
          </div>
          <div class="title-text">
            <h1 class="page-title">AgentJ</h1>
            <p class="page-subtitle">AI 智能助手</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 核心统计卡片 - 只保留最重要的数据 -->
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
          </div>
        </div>
      </el-card>
      <div v-if="stats.length === 0" class="empty-stats">
        <el-icon size="32" class="empty-icon"><DataAnalysis /></el-icon>
        <p>暂无统计数据</p>
      </div>
    </div>

    <!-- 简化的快速操作 -->
    <div class="quick-actions-section">
      <el-card class="quick-actions">
        <div class="actions-grid">
          <el-button 
            v-for="action in quickActions" 
            :key="action.key"
            @click="handleQuickAction(action)"
            :type="action.type"
            class="action-btn"
          >
            <el-icon size="20">
              <component :is="action.icon" />
            </el-icon>
            <span>{{ action.title }}</span>
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 底部信息 -->
    <div class="footer-info">
      <p class="footer-text">AgentJ v0.0.1 - AI智能助手管理系统</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { Cpu } from '@element-plus/icons-vue';

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
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: 100vh;
}

.page-header {
  margin-bottom: 24px;
}

.header-content {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 48px 32px;
  text-align: center;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand-logo {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin: 0 auto 16px;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
}

.title-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #333333;
}

.page-subtitle {
  margin: 0;
  font-size: 14px;
  color: #666666;
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
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: none;
  transition: all 0.4s ease;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
}

.stat-card:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.15);
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
  font-size: 32px;
  font-weight: 700;
  color: #333333;
  margin-bottom: 6px;
}

.stat-title {
  font-size: 14px;
  color: #666666;
  margin-bottom: 6px;
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
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: none;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
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
  gap: 10px;
  padding: 20px 12px;
  height: auto;
  border-radius: 12px;
  transition: all 0.4s ease;
  font-weight: 600;
}

.action-btn:hover {
  transform: translateY(-3px) scale(1.05);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
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
  gap: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  transition: all 0.4s ease;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.activity-item:hover {
  background: rgba(102, 126, 234, 0.1);
  border-color: rgba(102, 126, 234, 0.3);
  transform: translateX(8px);
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
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  }
}

@media (max-width: 768px) {
  .dashboard {
    padding: 16px;
  }
  
  .header-content {
    flex-direction: column;
    gap: 16px;
    align-items: center;
    text-align: center;
  }
  
  .title-section {
    flex-direction: column;
    gap: 12px;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .actions-grid {
    grid-template-columns: 1fr;
  }
  
  .info-content {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  .page-title {
    font-size: 24px;
  }
  
  .stat-value {
    font-size: 28px;
  }
}

@media (max-width: 480px) {
  .dashboard {
    padding: 12px;
  }
  
  .header-content {
    padding: 20px;
  }
  
  .page-title {
    font-size: 20px;
  }
  
  .stat-value {
    font-size: 24px;
  }
  
  .chart-container {
    height: 250px;
  }
  
  .stat-card {
    padding: 16px;
  }
  
  .action-btn {
    padding: 16px 8px;
  }
}
</style>
