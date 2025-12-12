<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <el-icon size="24"><Cpu /></el-icon>
        <span>AgentJ</span>
      </div>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="登录" name="login">
          <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @submit.prevent="handleLogin">
            <el-form-item label="用户名或邮箱" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名或邮箱" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-button type="primary" :loading="loading" style="width: 100%" native-type="submit" @click="handleLogin">
              登录
            </el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-position="top" @submit.prevent="handleRegister">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="展示名称" prop="displayName">
              <el-input v-model="registerForm.displayName" placeholder="用于显示的昵称" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="请再次输入密码" />
            </el-form-item>
            <el-button type="primary" :loading="registerLoading" style="width: 100%" native-type="submit" @click="handleRegister">
              注册并登录
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, FormInstance, FormRules } from 'element-plus';
import { Cpu } from '@element-plus/icons-vue';
import http from '@/api/http';
import { setToken, setUser, type User } from '@/utils/auth';

const router = useRouter();
const loading = ref(false);
const registerLoading = ref(false);
const formRef = ref<FormInstance>();
const registerFormRef = ref<FormInstance>();
const activeTab = ref<'login' | 'register'>('login');

const form = reactive({
  username: '',
  password: '',
});

const rules = reactive<FormRules>({
  username: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
});

const registerForm = reactive({
  username: '',
  email: '',
  displayName: '',
  password: '',
  confirmPassword: '',
});

const registerRules = reactive<FormRules>({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  displayName: [{ required: true, message: '请输入展示名称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
});

const handleLogin = () => {
  if (!formRef.value) return;
  formRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      const resp = await http.post('/api/auth/login', {
        username: form.username,
        password: form.password,
      });
      const token = resp.data.token;
      const user = resp.data.user;
      if (!token || !user) {
        throw new Error('未获取到登录令牌或用户信息');
      }
      setToken(token);
      setUser(user as User);
      ElMessage.success('登录成功');
      router.replace('/');
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.error || e?.message || '登录失败');
    } finally {
      loading.value = false;
    }
  });
};

const handleRegister = () => {
  if (!registerFormRef.value) return;
  registerFormRef.value.validate(async (valid) => {
    if (!valid) return;
    registerLoading.value = true;
    try {
      const resp = await http.post('/api/auth/register', {
        username: registerForm.username,
        email: registerForm.email,
        displayName: registerForm.displayName,
        password: registerForm.password,
      });
      const token = resp.data.token;
      const user = resp.data.user;
      if (!token || !user) {
        throw new Error('未获取到登录令牌或用户信息');
      }
      setToken(token);
      setUser(user as User);
      ElMessage.success('注册并登录成功');
      router.replace('/');
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.error || e?.message || '注册失败');
    } finally {
      registerLoading.value = false;
    }
  });
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: radial-gradient(circle at 20% 20%, rgb(37 99 235 / 0.06), transparent 35%), radial-gradient(circle at 80% 0%, rgb(124 58 237 / 0.07), transparent 45%), var(--bg-secondary);
}
.login-card {
  width: 360px;
  padding: 32px 32px 28px;
  border-radius: 18px;
  background: var(--bg-glass);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-color);
  backdrop-filter: blur(12px);
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 18px;
  color: var(--primary-color);
  margin-bottom: 8px;
}
h2 {
  margin: 0 0 20px;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}
</style>
