<template>
  <div class="login-page">
    <div class="login-card">
      <header class="header">
        <div class="brand">
          <el-icon size="32" aria-hidden="true"><Cpu /></el-icon>
        </div>
        <h1 class="title">登录到 AgentJ</h1>
        <p class="subtitle">AI智能助手管理系统</p>
      </header>
      
      <el-tabs v-model="activeTab" type="border-card" class="login-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="登录" name="login">
          <el-form 
            :model="form" 
            :rules="rules" 
            ref="formRef" 
            label-position="top" 
            @submit.prevent="handleLogin" 
            class="login-form"
            @validate="onValidate"
          >
            <el-form-item label="用户名或邮箱" prop="username">
              <el-input 
                v-model="form.username" 
                placeholder="请输入用户名或邮箱" 
                size="large"
                clearable
                @keyup.enter="handleLogin"
                aria-label="用户名或邮箱"
              >
                <template #prefix>
                  <el-icon aria-hidden="true"><UserIcon /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input 
                v-model="form.password" 
                type="password" 
                show-password 
                placeholder="请输入密码" 
                size="large"
                clearable
                @keyup.enter="handleLogin"
                aria-label="密码"
              >
                <template #prefix>
                  <el-icon aria-hidden="true"><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item style="margin-bottom: 24px;">
              <el-button 
                type="primary" 
                :loading="loading" 
                native-type="submit" 
                @click="handleLogin" 
                size="large" 
                class="submit-btn"
                :disabled="!isFormValid"
              >
                {{ loading ? '登录中...' : '登录' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form 
            :model="registerForm" 
            :rules="registerRules" 
            ref="registerFormRef" 
            label-position="top" 
            @submit.prevent="handleRegister" 
            class="register-form"
            @validate="onRegisterValidate"
          >
            <el-form-item label="用户名" prop="username">
              <el-input 
                v-model="registerForm.username" 
                placeholder="请输入用户名" 
                size="large"
                clearable
                @keyup.enter="handleRegister"
                aria-label="用户名"
              >
                <template #prefix>
                  <el-icon aria-hidden="true"><UserIcon /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input 
                v-model="registerForm.email" 
                placeholder="请输入邮箱" 
                size="large"
                clearable
                @keyup.enter="handleRegister"
                aria-label="邮箱"
              >
                <template #prefix>
                  <el-icon aria-hidden="true"><Message /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="昵称" prop="displayName">
              <el-input 
                v-model="registerForm.displayName" 
                placeholder="用于显示的昵称" 
                size="large"
                clearable
                @keyup.enter="handleRegister"
                aria-label="昵称"
              >
                <template #prefix>
                  <el-icon aria-hidden="true"><UserFilled /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input 
                v-model="registerForm.password" 
                type="password" 
                show-password 
                placeholder="请输入密码" 
                size="large"
                clearable
                @keyup.enter="handleRegister"
                aria-label="密码"
              >
                <template #prefix>
                  <el-icon aria-hidden="true"><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input 
                v-model="registerForm.confirmPassword" 
                type="password" 
                show-password 
                placeholder="请再次输入密码" 
                size="large"
                clearable
                @keyup.enter="handleRegister"
                aria-label="确认密码"
              >
                <template #prefix>
                  <el-icon aria-hidden="true"><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item style="margin-bottom: 24px;">
              <el-button 
                type="primary" 
                :loading="registerLoading" 
                native-type="submit" 
                @click="handleRegister" 
                size="large" 
                class="submit-btn"
                :disabled="!isRegisterFormValid"
              >
                {{ registerLoading ? '注册中...' : '注册并登录' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      
      <footer class="footer">
        <p class="footer-text">© 2024 AgentJ. All rights reserved.</p>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, FormInstance, FormRules, type FormItemProp } from 'element-plus';
import { Cpu, User as UserIcon, Lock, Message, UserFilled } from '@element-plus/icons-vue';
import http from '@/api/http';
import { setToken, setUser, type User } from '@/utils/auth';

const router = useRouter();
const loading = ref(false);
const registerLoading = ref(false);
const formRef = ref<FormInstance>();
const registerFormRef = ref<FormInstance>();
const activeTab = ref<'login' | 'register'>('login');
const loginValidFields = ref<Set<string>>(new Set());
const registerValidFields = ref<Set<string>>(new Set());

const isFormValid = computed(() => {
  return loginValidFields.value.size === 2 && form.username && form.password;
});

const isRegisterFormValid = computed(() => {
  return registerValidFields.value.size === 5 && 
    registerForm.username && 
    registerForm.email && 
    registerForm.displayName && 
    registerForm.password && 
    registerForm.confirmPassword;
});

const form = reactive({
  username: '',
  password: '',
});

const rules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名或邮箱', trigger: 'blur' },
    { min: 3, max: 50, message: '长度在 3 到 50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 个字符', trigger: 'blur' }
  ],
});

const registerForm = reactive({
  username: '',
  email: '',
  displayName: '',
  password: '',
  confirmPassword: '',
});

const registerRules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  displayName: [
    { required: true, message: '请输入展示名称', trigger: 'blur' },
    { min: 2, max: 30, message: '长度在 2 到 30 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 个字符', trigger: 'blur' },
    { pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, message: '密码需包含大小写字母和数字', trigger: 'blur' }
  ],
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

const onValidate = (prop: FormItemProp, isValid: boolean) => {
  if (isValid) {
    loginValidFields.value.add(prop as string);
  } else {
    loginValidFields.value.delete(prop as string);
  }
};

const onRegisterValidate = (prop: FormItemProp, isValid: boolean) => {
  if (isValid) {
    registerValidFields.value.add(prop as string);
  } else {
    registerValidFields.value.delete(prop as string);
  }
};

const handleTabChange = () => {
  formRef.value?.clearValidate();
  registerFormRef.value?.clearValidate();
  loginValidFields.value.clear();
  registerValidFields.value.clear();
};

const handleLogin = async () => {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
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
};

const handleRegister = async () => {
  if (!registerFormRef.value) return;
  const valid = await registerFormRef.value.validate().catch(() => false);
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
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.login-page::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: moveBackground 20s linear infinite;
}

@keyframes moveBackground {
  0% { transform: translate(0, 0); }
  100% { transform: translate(50px, 50px); }
}

.login-card {
  width: 100%;
  max-width: 440px;
  padding: 48px 40px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
  z-index: 1;
}

@keyframes slideUp {
  from { transform: translateY(40px); opacity: 0; }
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
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.el-tabs {
  margin-bottom: 0;
}

.el-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
  background: transparent;
}

.el-tabs :deep(.el-tabs__nav-wrap) {
  padding: 0 16px;
}

.el-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 500;
  color: #6b7280;
  padding: 12px 20px;
  transition: all 0.3s ease;
}

.el-tabs :deep(.el-tabs__item:hover) {
  color: #667eea;
}

.el-tabs :deep(.el-tabs__item.is-active) {
  color: #667eea;
  font-weight: 600;
}

.el-form-item {
  margin-bottom: 20px;
}

.el-form-item :deep(.el-form-item__label) {
  font-weight: 500;
  color: #374151;
  font-size: 14px;
  margin-bottom: 8px;
}

.el-input__wrapper {
  border-radius: 12px;
  box-shadow: none;
  transition: all 0.3s ease;
  border: 2px solid #e5e7eb;
  padding: 8px 16px;
}

.el-input__wrapper:hover {
  border-color: #667eea;
}

.el-input__wrapper.is-focus {
  border-color: #667eea;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

.el-input__prefix {
  color: #9ca3af;
}

.el-input__inner {
  font-size: 15px;
}

.submit-btn {
  width: 100%;
  height: 52px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
  letter-spacing: 0.5px;
}

.submit-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #5568d3 0%, #6a3f9e 100%);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
  transform: translateY(-2px);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: #9ca3af;
}

.el-form-item :deep(.el-form-item__error) {
  color: #ef4444;
  font-size: 13px;
  margin-top: 6px;
  padding-left: 4px;
}

.footer {
  margin-top: 24px;
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid #e5e7eb;
}

.footer-text {
  margin: 0;
  font-size: 13px;
  color: #9ca3af;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-page {
    padding: 16px;
  }
  
  .login-card {
    padding: 32px 24px;
    max-width: 100%;
  }
  
  .title {
    font-size: 24px;
  }
  
  .el-tabs :deep(.el-tabs__item) {
    font-size: 14px;
    padding: 10px 16px;
  }
}

@media (max-width: 480px) {
  .login-card {
    padding: 24px 20px;
    border-radius: 20px;
  }
  
  .el-form-item {
    margin-bottom: 16px;
  }
  
  .submit-btn {
    height: 48px;
    font-size: 15px;
  }
  
  .title {
    font-size: 22px;
  }
}

/* 暗色主题支持 */
@media (prefers-color-scheme: dark) {
  .login-card {
    background: rgba(31, 41, 55, 0.95);
  }
  
  .title {
    color: #f9fafb;
  }
  
  .subtitle {
    color: #9ca3af;
  }
  
  .el-form-item :deep(.el-form-item__label) {
    color: #d1d5db;
  }
  
  .el-input__wrapper {
    background: rgba(17, 24, 39, 0.8);
    border-color: #374151;
  }
  
  .el-input__wrapper:hover {
    border-color: #818cf8;
  }
  
  .el-input__wrapper.is-focus {
    border-color: #818cf8;
    box-shadow: 0 0 0 4px rgba(129, 140, 248, 0.15);
  }
  
  .footer {
    border-top-color: #374151;
  }
}
</style>
