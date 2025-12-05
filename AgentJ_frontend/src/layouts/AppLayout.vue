<template>
  <el-container style="min-height: 100vh">
    <el-aside width="230px" class="aside">
      <div class="logo">AgentJ Frontend</div>
      <el-menu :default-active="route.path" router class="menu" :collapse="isCollapsed">
        <el-menu-item v-for="item in menuRoutes" :key="item.path" :index="item.path">
          <el-icon v-if="item.meta?.icon">
            <component :is="resolveIcon(item.meta.icon as string)" />
          </el-icon>
          <span>{{ item.meta?.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="breadcrumb">
          <span class="page-title">{{ currentTitle }}</span>
        </div>
        <el-switch v-model="isCollapsed" active-text="折叠菜单" inactive-text="展开菜单" />
      </el-header>
      <el-main class="main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, RouterView } from 'vue-router';
import { menuRoutes } from '@/router';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';

const route = useRoute();
const isCollapsed = ref(false);

const currentTitle = computed(() => {
  const found = menuRoutes.find((item) => item.path === route.path);
  return found?.meta?.title ?? 'AgentJ';
});

const resolveIcon = (name: string) => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return (ElementPlusIconsVue as any)[name] || ElementPlusIconsVue.Menu;
};
</script>

<style scoped>
.aside {
  background: #0f172a;
  color: #e2e8f0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #1e293b;
}

.logo {
  padding: 20px 16px;
  font-weight: 700;
  font-size: 18px;
  letter-spacing: 0.3px;
  color: #e0f2fe;
}

.menu {
  border-right: none;
  flex: 1;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.breadcrumb {
  font-size: 14px;
  color: #475569;
}

.page-title {
  font-weight: 600;
  font-size: 16px;
}

.main {
  padding: 16px;
  background: #f5f6fa;
}
</style>
