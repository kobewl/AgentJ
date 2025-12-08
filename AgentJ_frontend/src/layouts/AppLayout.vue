<template>
  <el-container style="min-height: 100vh">
    <!-- 移动端遮罩层 -->
    <div 
      v-if="isMobile && !isCollapsed" 
      class="mobile-overlay"
      @click="toggleSidebar"
    />
    
    <!-- 侧边栏 -->
    <el-aside 
      :width="isMobile ? '280px' : '260px'" 
      class="aside"
      :class="{ 'mobile-open': isMobile && !isCollapsed }"
    >
      <div class="logo">
        <el-icon size="24" class="logo-icon"><Cpu /></el-icon>
        <span class="logo-text">AgentJ</span>
      </div>
      
      <el-menu 
        :default-active="route.path" 
        router 
        class="menu" 
        :collapse="isCollapsed && !isMobile"
        :unique-opened="true"
      >
        <el-menu-item 
          v-for="item in menuRoutes" 
          :key="item.path" 
          :index="item.path"
          class="menu-item"
        >
          <el-icon v-if="item.meta?.icon" class="menu-icon">
            <component :is="resolveIcon(item.meta.icon as string)" />
          </el-icon>
          <template #title>
            <span class="menu-text">{{ item.meta?.title }}</span>
            <el-tag 
              v-if="getMenuBadge(item.path)" 
              size="small" 
              type="danger"
              class="menu-badge"
            >
              {{ getMenuBadge(item.path) }}
            </el-tag>
          </template>
        </el-menu-item>
      </el-menu>
      
      <!-- 侧边栏底部信息 -->
      <div class="aside-footer">
        <div class="connection-status">
          <div class="status-indicator" :class="connectionStatusClass">
            <el-icon size="12">
              <component :is="connectionStatusIcon" />
            </el-icon>
            <span>{{ connectionStatusText }}</span>
          </div>
        </div>
        <div class="version-info">
          <span>v{{ appVersion }}</span>
        </div>
      </div>
    </el-aside>

    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="header">
        <div class="header-left">
          <!-- 移动端菜单按钮 -->
          <el-button 
            v-if="isMobile" 
            text 
            @click="toggleSidebar"
            class="mobile-menu-btn"
          >
            <el-icon size="20">
              <component :is="isCollapsed ? 'Expand' : 'Fold'" />
            </el-icon>
          </el-button>
          
          <!-- 面包屑导航 -->
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <!-- 主题切换 -->
          <el-button 
            text 
            @click="toggleTheme"
            class="theme-toggle"
          >
            <el-icon size="18">
              <component :is="isDark ? 'Sunny' : 'Moon'" />
            </el-icon>
          </el-button>
          
          <!-- 全屏切换 -->
          <el-button 
            text 
            @click="toggleFullscreen"
            class="fullscreen-toggle"
          >
            <el-icon size="18">
              <FullScreen />
            </el-icon>
          </el-button>
          
          <!-- 用户信息 -->
          <el-dropdown trigger="click">
            <el-button text class="user-menu">
              <el-icon size="18"><User /></el-icon>
              <span class="username">{{ username }}</span>
              <el-icon size="12"><CaretBottom /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>
                  <el-icon><User /></el-icon>
                  个人设置
                </el-dropdown-item>
                <el-dropdown-item>
                  <el-icon><Setting /></el-icon>
                  系统设置
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <!-- 主内容区域 -->
      <el-main class="main">
        <RouterView v-slot="{ Component, route }">
          <transition name="fade" mode="out-in">
            <component v-if="Component" :is="Component" :key="route.fullPath" />
          </transition>
        </RouterView>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, RouterView, useRouter } from 'vue-router';
import { menuRoutes } from '@/router';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import {
  Cpu,
  FullScreen,
  User,
  CaretBottom,
  Setting,
  SwitchButton,
} from '@element-plus/icons-vue';
import { clearToken } from '@/utils/auth';

const route = useRoute();
const router = useRouter();
const isCollapsed = ref(false);
const isMobile = ref(false);
const isDark = ref(false);
const connectionStatus = ref<'connected' | 'connecting' | 'disconnected'>('connecting');
const appVersion = ref('0.0.1');
const username = ref('管理员');
const handleLogout = () => {
  clearToken();
  router.replace('/login');
};

// 响应式处理
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768;
  if (isMobile.value) {
    isCollapsed.value = true; // 移动端默认收起侧边栏
  }
};

// 切换侧边栏
const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value;
};

// 切换主题
const toggleTheme = () => {
  isDark.value = !isDark.value;
  document.documentElement.classList.toggle('dark', isDark.value);
  ElMessage.success(`已切换到${isDark.value ? '深色' : '浅色'}主题`);
};

// 全屏切换
const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen();
  } else {
    document.exitFullscreen();
  }
};

// 获取菜单徽标
const getMenuBadge = (path: string) => {
  // 这里可以根据实际需求返回徽标数量
  return null;
};

// 连接状态计算属性
const connectionStatusClass = computed(() => {
  return {
    'status-success': connectionStatus.value === 'connected',
    'status-warning': connectionStatus.value === 'connecting',
    'status-error': connectionStatus.value === 'disconnected'
  };
});

const connectionStatusIcon = computed(() => {
  switch (connectionStatus.value) {
    case 'connected': return 'CircleCheck';
    case 'connecting': return 'Loading';
    case 'disconnected': return 'CircleClose';
    default: return 'InfoFilled';
  }
});

const connectionStatusText = computed(() => {
  switch (connectionStatus.value) {
    case 'connected': return '已连接';
    case 'connecting': return '连接中...';
    case 'disconnected': return '未连接';
    default: return '未知';
  }
});

const currentTitle = computed(() => {
  const found = menuRoutes.find((item) => item.path === route.path);
  return found?.meta?.title ?? 'AgentJ';
});

const resolveIcon = (name: string) => {
  return (ElementPlusIconsVue as any)[name] || ElementPlusIconsVue.Menu;
};

// 模拟连接状态检测
const checkConnection = async () => {
  try {
    // 这里可以添加实际的连接检测逻辑
    await new Promise(resolve => setTimeout(resolve, 1000));
    connectionStatus.value = 'connected';
  } catch (error) {
    connectionStatus.value = 'disconnected';
  }
};

onMounted(() => {
  checkMobile();
  checkConnection();
  window.addEventListener('resize', checkMobile);
  
  // 初始化主题
  const savedTheme = localStorage.getItem('theme');
  if (savedTheme === 'dark') {
    isDark.value = true;
    document.documentElement.classList.add('dark');
  }
});

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile);
});

// 监听主题变化
watch(isDark, (newVal) => {
  localStorage.setItem('theme', newVal ? 'dark' : 'light');
});
</script>

<style scoped>
.aside {
  background: var(--bg-secondary);
  color: var(--text-primary);
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border-color);
  position: relative;
  overflow: hidden;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
}

.logo {
  padding: 20px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-primary);
  backdrop-filter: blur(10px);
}

.logo-icon {
  color: var(--primary-color);
  animation: pulse 2s infinite;
}

.logo-text {
  font-weight: 700;
  font-size: 20px;
  letter-spacing: 0.5px;
  color: var(--text-primary);
  background: linear-gradient(135deg, var(--primary-color), var(--primary-hover));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.menu {
  border-right: none;
  background: transparent;
  flex: 1;
  padding: 8px 0;
}

.menu-item {
  border-radius: 12px;
  margin: 4px 12px;
  transition: all 0.3s ease;
  border-left: 3px solid transparent;
}

.menu-item:hover {
  background: rgba(59, 130, 246, 0.15);
  border-left-color: var(--primary-color);
  transform: translateX(4px);
}

.menu-item.is-active {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-hover));
  color: white !important;
  border-left-color: #60a5fa;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.menu-icon {
  margin-right: 12px;
  font-size: 18px;
  color: var(--text-secondary);
  transition: color 0.3s ease;
}

.menu-item:hover .menu-icon {
  color: var(--primary-color);
}

.menu-item.is-active .menu-icon {
  color: white;
}

.menu-text {
  flex: 1;
  color: var(--text-primary);
  font-weight: 500;
  font-size: 14px;
}

.menu-badge {
  margin-left: 8px;
}

.aside-footer {
  padding: 16px;
  border-top: 1px solid var(--border-color);
  background: var(--bg-primary);
  backdrop-filter: blur(10px);
}

.connection-status {
  margin-bottom: 8px;
}

.version-info {
  text-align: center;
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-color);
  padding: 0 24px;
  backdrop-filter: blur(10px);
  background: rgba(255, 255, 255, 0.95);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.mobile-menu-btn {
  padding: 8px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.mobile-menu-btn:hover {
  background: var(--bg-tertiary);
  transform: scale(1.1);
}

.breadcrumb {
  font-size: 14px;
  color: var(--text-secondary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.theme-toggle,
.fullscreen-toggle {
  padding: 8px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.theme-toggle:hover,
.fullscreen-toggle:hover {
  background: var(--bg-tertiary);
  transform: scale(1.1);
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.user-menu:hover {
  background: var(--bg-tertiary);
}

.username {
  font-weight: 500;
  font-size: 14px;
}

.main {
  padding: 24px;
  background: var(--bg-secondary);
  min-height: calc(100vh - 60px);
}

/* 移动端遮罩层 */
.mobile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  backdrop-filter: blur(2px);
}

/* 深色主题 */
.dark {
  --bg-primary: #0f172a;
  --bg-secondary: #1e293b;
  --bg-tertiary: #334155;
  --text-primary: #f1f5f9;
  --text-secondary: #cbd5e1;
  --text-tertiary: #94a3b8;
  --border-color: #334155;
  --border-hover: #475569;
}

.dark .aside {
  background: var(--bg-secondary);
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.3);
}

.dark .header {
  background: rgba(15, 23, 42, 0.95);
  border-bottom-color: #334155;
}

.dark .main {
  background: #1e293b;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header {
    padding: 0 16px;
  }
  
  .main {
    padding: 16px;
  }
  
  .header-right {
    gap: 4px;
  }
  
  .username {
    display: none;
  }
}

@media (max-width: 480px) {
  .aside {
    position: fixed;
    left: 0;
    top: 0;
    height: 100vh;
    z-index: 1000;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
    width: 280px !important;
  }
  
  .aside.mobile-open {
    transform: translateX(0);
  }
}
</style>

