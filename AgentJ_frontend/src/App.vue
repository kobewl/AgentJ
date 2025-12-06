<template>
  <component :is="currentComponent" />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import AppLayout from './layouts/AppLayout.vue';
import Login from './pages/Login.vue';

const route = useRoute();
const currentComponent = computed(() => (route.path === '/login' ? Login : AppLayout));
</script>
