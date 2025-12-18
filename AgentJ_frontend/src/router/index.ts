import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import Dashboard from '@/pages/Dashboard.vue';
import InitSetup from '@/pages/InitSetup.vue';
import ConfigCenter from '@/pages/ConfigCenter.vue';
import PlanTemplates from '@/pages/PlanTemplates.vue';
import ChatDialog from '@/pages/ChatDialog.vue';
import KnowledgeManage from '@/pages/KnowledgeManage.vue';
import KnowledgeChat from '@/pages/KnowledgeChat.vue';
import Memories from '@/pages/Memories.vue';
import DatasourceConfigs from '@/pages/DatasourceConfigs.vue';
import UnifiedAgent from '@/pages/UnifiedAgent.vue';
import CronTasks from '@/pages/CronTasks.vue';
import WorkflowDesigner from '@/pages/WorkflowDesigner.vue';
import Login from '@/pages/Login.vue';
import { isAuthenticated } from '@/utils/auth';
import { ElMessage } from 'element-plus';

export const menuRoutes: RouteRecordRaw[] = [
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: { title: '总览', icon: 'DataBoard' },
  },
  {
    path: '/init',
    name: 'InitSetup',
    component: InitSetup,
    meta: { title: '初始化向导', icon: 'MagicStick' },
  },
  {
    path: '/config',
    name: 'ConfigCenter',
    component: ConfigCenter,
    meta: { title: '系统配置', icon: 'Setting' },
  },
  {
    path: '/plan-templates',
    name: 'PlanTemplates',
    component: PlanTemplates,
    meta: { title: '计划模板', icon: 'Tickets' },
  },
  {
    path: '/agents',
    name: 'UnifiedAgent',
    component: UnifiedAgent,
    meta: { title: 'AI Agent', icon: 'Cpu' },
  },
  {
    path: '/chat',
    name: 'ChatDialog',
    component: ChatDialog,
    meta: { title: 'LLM对话', icon: 'ChatLineRound' },
  },
  {
    path: '/knowledge',
    name: 'KnowledgeManage',
    component: KnowledgeManage,
    meta: { title: '知识库管理', icon: 'FolderOpened' },
  },
  {
    path: '/knowledge-chat',
    name: 'KnowledgeChat',
    component: KnowledgeChat,
    meta: { title: '知识库对话', icon: 'ChatDotRound' },
  },
  {
    path: '/memories',
    name: 'Memories',
    component: Memories,
    meta: { title: '记忆管理', icon: 'Memo' },
  },
  {
    path: '/datasource',
    name: 'DatasourceConfigs',
    component: DatasourceConfigs,
    meta: { title: '数据源', icon: 'Coin' },
  },
  {
    path: '/cron',
    name: 'CronTasks',
    component: CronTasks,
    meta: { title: '定时任务', icon: 'Timer' },
  },
  {
    path: '/workflow',
    name: 'WorkflowDesigner',
    component: WorkflowDesigner,
    meta: { title: '工作流设计', icon: 'Share' },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/login', name: 'Login', component: Login, meta: { title: '登录', public: true } },
    // 兼容旧路由，重定向到统一 Agent 页面
    { path: '/database-ai', redirect: '/agents' },
    ...menuRoutes,
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
});

router.beforeEach((to, from, next) => {
  if (to.meta.public) {
    next();
    return;
  }
  if (isAuthenticated()) {
    next();
  } else {
    if (to.path !== '/login') {
      ElMessage.warning('请先登录');
    }
    next({ path: '/login', query: { redirect: to.fullPath } });
  }
});

export default router;
