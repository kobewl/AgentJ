<template>
  <div class="page-wrapper">
    <el-card shadow="never">
      <template #header>
        <div class="card-toolbar">
          <span>数据源配置</span>
          <div class="flex-row">
            <el-button type="primary" @click="openEdit()">新建</el-button>
            <el-button @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="items" v-loading="loading" border style="width: 100%">
        <el-table-column prop="name" label="名称" width="160" />
        <el-table-column prop="url" label="URL" min-width="220" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="enable" label="启用" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.enable ? 'success' : 'info'">{{ scope.row.enable ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
            <el-popconfirm title="确认删除?" @confirm="remove(scope.row.id)">
              <template #reference>
                <el-button size="small" type="danger" plain>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="drawer" title="数据源配置" size="40%">
      <el-form :model="form" label-width="120px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="数据库类型" required>
          <el-select v-model="form.type" placeholder="请选择数据库类型" style="width: 100%">
            <el-option v-for="opt in dbOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="IP/主机" required>
          <el-input v-model="form.host" placeholder="请输入数据库IP或主机名" />
        </el-form-item>
        <el-form-item label="数据库名称">
          <el-input v-model="form.database" placeholder="可选，留空则连接到实例" />
        </el-form-item>
        <el-form-item label="URL" required>
          <el-input v-model="form.url" disabled />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="Driver">
          <el-input v-model="form.driver_class_name" disabled />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enable" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save">保存</el-button>
          <el-button @click="test">测试连接</el-button>
        </el-form-item>
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {
  createDatasourceConfig,
  deleteDatasourceConfig,
  listDatasourceConfigs,
  testDatasourceConnection,
  updateDatasourceConfig,
} from '@/api/datasource';
import type { DatasourceConfig } from '@/api/types';

const items = ref<DatasourceConfig[]>([]);
const loading = ref(false);
const drawer = ref(false);

type DatasourceForm = DatasourceConfig & {
  host: string;
  database: string;
};

const driverMap: Record<string, string> = {
  mysql: 'com.mysql.cj.jdbc.Driver',
  mariadb: 'org.mariadb.jdbc.Driver',
  postgresql: 'org.postgresql.Driver',
  oracle: 'oracle.jdbc.OracleDriver',
  sqlserver: 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
  h2: 'org.h2.Driver',
};

const urlDefaults: Record<string, { prefix: string; port: number }> = {
  mysql: { prefix: 'jdbc:mysql://', port: 3306 },
  mariadb: { prefix: 'jdbc:mariadb://', port: 3306 },
  postgresql: { prefix: 'jdbc:postgresql://', port: 5432 },
  sqlserver: { prefix: 'jdbc:sqlserver://', port: 1433 },
  oracle: { prefix: 'jdbc:oracle:thin:@//', port: 1521 },
};

const dbOptions = [
  { value: 'mysql', label: 'MySQL' },
  { value: 'mariadb', label: 'MariaDB' },
  { value: 'postgresql', label: 'PostgreSQL' },
  { value: 'oracle', label: 'Oracle' },
  { value: 'sqlserver', label: 'SQL Server' },
  { value: 'h2', label: 'H2' },
];

const form = reactive<DatasourceForm>({
  name: '',
  type: 'mysql',
  host: '',
  database: '',
  url: '',
  username: '',
  password: '',
  driver_class_name: '',
  enable: true,
});

const applyDriverFromType = () => {
  const t = (form.type || '').toLowerCase();
  const driver = driverMap[t];
  if (driver) {
    form.driver_class_name = driver;
  }
};

const buildUrlFromParts = (type?: string, host?: string, database?: string) => {
  const t = (type || '').toLowerCase();
  const db = (database || '').trim();
  if (t === 'h2') {
    return db ? `jdbc:h2:mem:${db}` : 'jdbc:h2:mem:test';
  }
  const base = urlDefaults[t];
  const h = (host || '').trim();
  if (!base || !h) {
    return '';
  }
  if (t === 'sqlserver') {
    return `${base.prefix}${h}:${base.port}${db ? `;databaseName=${db}` : ''}`;
  }
  return `${base.prefix}${h}:${base.port}${db ? `/${db}` : ''}`;
};

const applyUrlFromHostAndDb = () => {
  const computed = buildUrlFromParts(form.type, form.host, form.database);
  if (computed) {
    form.url = computed;
  } else if (!form.id) {
    form.url = '';
  }
};

const parseHostAndDatabase = (url?: string, type?: string) => {
  const t = (type || '').toLowerCase();
  if (!url) return { host: '', database: '' };
  if (t === 'h2') {
    const m = url.match(/^jdbc:h2:mem:([^;]+)$/i);
    return { host: '', database: m?.[1] || '' };
  }
  if (t === 'sqlserver') {
    const m = url.match(/^jdbc:sqlserver:\/\/([^:;\/]+)(?::\d+)?(?:;.*databaseName=([^;]+))?/i);
    return { host: m?.[1] || '', database: m?.[2] || '' };
  }
  const m = url.match(/^jdbc:[a-z0-9]+:\/\/([^:/;]+)(?::\d+)?(?:\/([^?;]+))?/i);
  if (m) return { host: m[1], database: m[2] || '' };
  return { host: '', database: '' };
};

watch(
  () => [form.type, form.host, form.database],
  () => {
    applyDriverFromType();
    applyUrlFromHostAndDb();
  },
  { immediate: true },
);

const load = async () => {
  loading.value = true;
  try {
    const res = await listDatasourceConfigs();
    items.value = res.data || [];
  } catch (error) {
    ElMessage.error('加载失败');
  } finally {
    loading.value = false;
  }
};

const openEdit = (row?: DatasourceConfig) => {
  if (row) {
    Object.assign(form, row);
    form.password = undefined;
    const parsed = parseHostAndDatabase(form.url, form.type);
    form.host = parsed.host;
    form.database = parsed.database;
  } else {
    Object.assign(form, {
      id: undefined,
      name: '',
      type: 'mysql',
      host: '',
      database: '',
      url: '',
      username: '',
      password: '',
      driver_class_name: '',
      enable: true,
    });
  }
  applyDriverFromType();
  applyUrlFromHostAndDb();
  drawer.value = true;
};

const save = async () => {
  applyDriverFromType();
  applyUrlFromHostAndDb();
  if (!form.name || !form.type) {
    ElMessage.warning('请填写名称和数据库类型');
    return;
  }
  if (!form.url) {
    ElMessage.warning('请输入数据库IP/主机');
    return;
  }
  if (!form.driver_class_name) {
    ElMessage.warning('当前数据库类型没有可用 Driver');
    return;
  }
  try {
    if (form.id) {
      await updateDatasourceConfig(form.id, form);
    } else {
      await createDatasourceConfig(form);
    }
    ElMessage.success('保存成功');
    drawer.value = false;
    load();
  } catch (error) {
    ElMessage.error('保存失败');
  }
};

const test = async () => {
  try {
    applyDriverFromType();
    applyUrlFromHostAndDb();
    if (!form.type || !form.driver_class_name) {
      ElMessage.warning('请选择数据库类型');
      return;
    }
    if (!form.url) {
      ElMessage.warning('请输入数据库IP/主机');
      return;
    }
    const res = await testDatasourceConnection(form);
    if (res.data?.success) {
      ElMessage.success('连接测试成功');
    } else {
      ElMessage.error(res.data?.message || '连接测试失败');
    }
  } catch (error) {
    ElMessage.error('连接测试失败');
  }
};

const remove = async (id?: number) => {
  if (!id) return;
  try {
    await deleteDatasourceConfig(id);
    ElMessage.success('已删除');
    load();
  } catch (error) {
    ElMessage.error('删除失败');
  }
};

onMounted(load);
</script>
