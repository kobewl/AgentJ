<template>
  <div class="login-page">
    <div class="login-card">
      <div class="header">
        <div class="brand">
          <el-icon size="32"><Cpu /></el-icon>
        </div>
        <h1 class="title">登录到 AgentJ</h1>
      </div>
      
      <el-tabs v-model="activeTab" type="border-card" class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @submit.prevent="handleLogin" class="login-form">
            <el-form-item label="" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名或邮箱" size="large">
                <template #prefix>
                  <el-icon><UserIcon /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large">
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item style="margin-bottom: 24px;">
              <el-button type="primary" :loading="loading" native-type="submit" @click="handleLogin" size="large" class="submit-btn">
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-position="top" @submit.prevent="handleRegister" class="register-form">
            <el-form-item label="" prop="username">
              <el-input v-model="registerForm.username" placeholder="请输入用户名" size="large">
                <template #prefix>
                  <el-icon><UserIcon /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="" prop="email">
              <el-input v-model="registerForm.email" placeholder="请输入邮箱" size="large">
                <template #prefix>
                  <el-icon><Message /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="" prop="displayName">
              <el-input v-model="registerForm.displayName" placeholder="用于显示的昵称" size="large">
                <template #prefix>
                  <el-icon><UserFilled /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="" prop="password">
              <el-input v-model="registerForm.password" type="password" show-password placeholder="请输入密码" size="large">
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="" prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="请再次输入密码" size="large">
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item style="margin-bottom: 24px;">
              <el-button type="primary" :loading="registerLoading" native-type="submit" @click="handleRegister" size="large" class="submit-btn">
                注册并登录
              </el-button>
            </el-form-item>
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
import { Cpu, User as UserIcon, Lock, Message, UserFilled } from '@element-plus/icons-vue';
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
  background: #f5f5f5;
  position: relative;
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 48px 32px;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  animation: slideUp 0.6s ease-out;
  position: relative;
  z-index: 1;
}

@keyframes slideUp {
  from { transform: translateY(30px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.header {
  text-align: center;
  margin-bottom: 32px;
}

.brand {
  margin-bottom: 16px;
}

.brand .el-icon {
  color: #667eea;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.title {
  font-size: 24px;
  font-weight: 600;
  color: #333333;
  margin: 0;
}

.el-tabs {
  margin-bottom: 24px;
}

.el-tabs__header {
  margin-bottom: 24px;
  text-align: center;
}

.el-tabs__item {
  font-size: 16px;
  font-weight: 500;
  color: #666666;
  padding: 12px 24px;
}

.el-tabs__item.is-active {
  color: #667eea;
  font-weight: 600;
}

.el-form-item {
  margin-bottom: 20px;
}

.el-form-item__label {
  display: none;
}

.el-input__wrapper {
  border-radius: 8px;
  box-shadow: none;
  transition: all 0.3s ease;
  border: 1px solid #e5e7eb;
}

.el-input__wrapper:hover {
  border-color: #667eea;
}

.el-input__wrapper.is-focus {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.el-input__prefix {
  color: #9ca3af;
}

.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  background: #667eea;
  border: none;
  transition: all 0.3s ease;
}

.submit-btn:hover:not(:disabled) {
  background: #5568d3;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.el-form-item__error {
  color: #ef4444;
  font-size: 12px;
  margin-top: 4px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-page {
    padding: 16px;
  }
  
  .login-card {
    padding: 32px 20px;
    max-width: 100%;
  }
  
  .title {
    font-size: 20px;
  }
  
  .el-tabs__item {
    font-size: 14px;
    padding: 10px 16px;
  }
}

@media (max-width: 480px) {
  .login-card {
    padding: 24px 16px;
  }
  
  .el-form-item {
    margin-bottom: 16px;
  }
  
  .submit-btn {
    height: 44px;
    font-size: 14px;
  }
}
</style>
