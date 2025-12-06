import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import Dashboard from '@/pages/Dashboard.vue';
import InitSetup from '@/pages/InitSetup.vue';
import ConfigCenter from '@/pages/ConfigCenter.vue';
import PlanTemplates from '@/pages/PlanTemplates.vue';
import Executor from '@/pages/Executor.vue';
import ChatDialog from '@/pages/ChatDialog.vue';
import Memories from '@/pages/Memories.vue';
import DatasourceConfigs from '@/pages/DatasourceConfigs.vue';
import CronTasks from '@/pages/CronTasks.vue';
import AgentTasks from '@/pages/AgentTasks.vue';

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
    path: '/executor',
    name: 'Executor',
    component: Executor,
    meta: { title: '执行与任务', icon: 'Promotion' },
  },
  {
    path: '/agents',
    name: 'AgentTasks',
    component: AgentTasks,
    meta: { title: 'Agent 任务', icon: 'Cpu' },
  },
  {
    path: '/chat',
    name: 'ChatDialog',
    component: ChatDialog,
    meta: { title: 'LLM对话', icon: 'ChatLineRound' },
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
];

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    ...menuRoutes,
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
});

export default router;
