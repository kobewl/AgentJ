<template>
  <el-container class="config-center-container" style="height: 100vh;">
    <!-- Left Sidebar: Group List -->
    <el-aside width="240px" class="group-sidebar">
      <div class="sidebar-header">
        <h3>配置分组</h3>
      </div>
      <el-menu
        :default-active="activeGroup"
        class="group-menu"
        @select="handleGroupSelect"
      >
        <el-menu-item v-for="group in groups" :key="group" :index="group">
          <el-icon><Folder /></el-icon>
          <span>{{ group }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- Main Content -->
    <el-main class="main-content">
      <el-card v-if="activeGroup" class="config-card" shadow="never">
        <template #header>
          <div class="card-toolbar">
            <div class="header-left">
              <h2>{{ activeGroup }} 配置</h2>
              <el-tag type="info" class="ml-2">{{ groupedConfigs.total }} 项配置</el-tag>
            </div>
            <div class="header-actions">
              <el-button type="primary" :loading="saving" @click="saveConfigs">
                <el-icon><Check /></el-icon> 保存修改
              </el-button>
              <el-popconfirm title="确定重置当前分组的所有配置为默认值？" @confirm="resetGroup">
                <template #reference>
                  <el-button type="warning" plain>
                    <el-icon><RefreshLeft /></el-icon> 重置当前分组
                  </el-button>
                </template>
              </el-popconfirm>
              <el-popconfirm title="警告：确定重置系统中所有配置为默认值？" @confirm="resetAll">
                <template #reference>
                  <el-button type="danger" plain>
                    <el-icon><Delete /></el-icon> 重置全部
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>
        </template>

        <!-- Sub-group Tabs -->
        <el-tabs v-model="activeSubGroup" class="subgroup-tabs">
          <el-tab-pane label="全部" name="ALL">
            <div class="config-list">
              <div v-for="(subGroupConfigs, subGroupName) in groupedConfigs.map" :key="subGroupName" class="subgroup-section">
                <div class="subgroup-title" v-if="subGroupName !== 'default'">
                  {{ subGroupName }}
                </div>
                <el-table :data="subGroupConfigs" border style="width: 100%">
                  <el-table-column prop="description" label="配置项" min-width="250">
                    <template #default="{ row }">
                      <div class="config-name">{{ getConfigDisplayName(row) }}</div>
                      <div class="config-path">{{ getConfigDisplayHint(row) }}</div>
                    </template>
                  </el-table-column>
                  
                  <el-table-column label="配置值" min-width="300">
                    <template #default="{ row }">
                      <!-- TEXT / TEXTAREA -->
                      <el-input
                        v-if="isTextType(row.inputType)"
                        v-model="row.configValue"
                        :type="row.inputType === 'TEXTAREA' ? 'textarea' : 'text'"
                        :rows="2"
                        placeholder="请输入配置值"
                      />
                      
                      <!-- NUMBER -->
                      <el-input-number
                        v-else-if="row.inputType === 'NUMBER'"
                        v-model="row.configValue"
                        controls-position="right"
                        style="width: 100%"
                      />
                      
                      <!-- CHECKBOX / BOOLEAN -->
                      <el-switch
                        v-else-if="row.inputType === 'CHECKBOX' || row.inputType === 'BOOLEAN'"
                        v-model="row.configValue"
                        active-value="true"
                        inactive-value="false"
                        active-text="开启"
                        inactive-text="关闭"
                      />
                      
                      <!-- SELECT -->
                      <el-select
                        v-else-if="row.inputType === 'SELECT'"
                        v-model="row.configValue"
                        placeholder="请选择"
                        style="width: 100%"
                      >
                        <el-option
                          v-for="opt in parseOptions(row.optionsJson)"
                          :key="opt.value"
                          :label="opt.label"
                          :value="opt.value"
                        />
                      </el-select>
                      
                      <div v-else>{{ row.configValue }}</div>
                    </template>
                  </el-table-column>
                  
                  <el-table-column label="默认值" width="180">
                    <template #default="{ row }">
                      <el-tag type="info" size="small">{{ row.defaultValue }}</el-tag>
                    </template>
                  </el-table-column>
                  
                  <el-table-column label="操作" width="100" align="center">
                    <template #default="{ row }">
                      <el-tooltip content="恢复默认值" placement="top">
                        <el-button 
                          circle 
                          size="small" 
                          type="info" 
                          plain
                          @click="resetRow(row)"
                          :disabled="row.configValue === row.defaultValue"
                        >
                          <el-icon><RefreshLeft /></el-icon>
                        </el-button>
                      </el-tooltip>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </el-tab-pane>
          
          <!-- Individual Sub-group Tabs -->
          <el-tab-pane 
            v-for="(subGroupConfigs, subGroupName) in groupedConfigs.map" 
            :key="subGroupName" 
            :label="subGroupName === 'default' ? '通用' : subGroupName" 
            :name="subGroupName"
          >
            <el-table :data="subGroupConfigs" border style="width: 100%">
              <el-table-column prop="description" label="配置项" min-width="250">
                <template #default="{ row }">
                  <div class="config-name">{{ getConfigDisplayName(row) }}</div>
                  <div class="config-path">{{ getConfigDisplayHint(row) }}</div>
                </template>
              </el-table-column>
              
              <el-table-column label="配置值" min-width="300">
                <template #default="{ row }">
                  <!-- Same inputs as above, reused code could be componentized but inline for simplicity -->
                  <el-input
                    v-if="isTextType(row.inputType)"
                    v-model="row.configValue"
                    :type="row.inputType === 'TEXTAREA' ? 'textarea' : 'text'"
                    :rows="2"
                  />
                  <el-input-number
                    v-else-if="row.inputType === 'NUMBER'"
                    v-model="row.configValue"
                    controls-position="right"
                    style="width: 100%"
                  />
                  <el-switch
                    v-else-if="row.inputType === 'CHECKBOX' || row.inputType === 'BOOLEAN'"
                    v-model="row.configValue"
                    active-value="true"
                    inactive-value="false"
                    active-text="开启"
                    inactive-text="关闭"
                  />
                  <el-select
                    v-else-if="row.inputType === 'SELECT'"
                    v-model="row.configValue"
                    placeholder="请选择"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="opt in parseOptions(row.optionsJson)"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                    />
                  </el-select>
                </template>
              </el-table-column>
              
              <el-table-column label="默认值" width="180">
                <template #default="{ row }">
                   <el-tag type="info" size="small">{{ row.defaultValue }}</el-tag>
                </template>
              </el-table-column>

               <el-table-column label="操作" width="100" align="center">
                    <template #default="{ row }">
                      <el-tooltip content="恢复默认值" placement="top">
                        <el-button 
                          circle 
                          size="small" 
                          type="info" 
                          plain
                          @click="resetRow(row)"
                          :disabled="row.configValue === row.defaultValue"
                        >
                          <el-icon><RefreshLeft /></el-icon>
                        </el-button>
                      </el-tooltip>
                    </template>
                  </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-card>

      <div v-else class="empty-state">
        <el-empty description="请从左侧选择一个配置分组" />
      </div>
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Folder, Setting, Check, RefreshLeft, Delete } from '@element-plus/icons-vue';
import { listConfigGroups, getConfigsByGroup, batchUpdateConfigs, resetAllDefaults } from '@/api/config';
import type { ConfigEntity } from '@/api/types';

const groups = ref<string[]>([]);
const activeGroup = ref('');
const activeSubGroup = ref('ALL');
const configs = ref<ConfigEntity[]>([]);
const loading = ref(false);
const saving = ref(false);

const configDisplayMap: Record<string, { name: string; hint: string }> = {
  // manus.browser.*
  'manus.browser.headless': {
    name: '浏览器无界面模式',
    hint: '开启后在后台无界面运行浏览器，适合服务器环境'
  },
  'manus.browser.requestTimeout': {
    name: '浏览器请求超时时间（秒）',
    hint: '单次网页请求的最长等待时间，超出将中止本次请求'
  },

  // manus.general.*
  'manus.general.debugDetail': {
    name: 'Agent 调试详情模式',
    hint: '开启后输出更详细的调试信息，仅推荐在排查问题时使用'
  },
  'manus.baseDir': {
    name: '工作目录',
    hint: 'Agent 读写文件的基础工作目录，留空则使用默认目录'
  },

  // manus.interaction.*
  'manus.openBrowserAuto': {
    name: '自动打开浏览器页面',
    hint: '当任务需要浏览器时，是否自动在本机打开可视化浏览器窗口'
  },

  // manus.agent / manus.agents.*
  'manus.maxSteps': {
    name: '最大执行步数',
    hint: '限制单个任务中 Agent 的最大思考与工具调用轮数'
  },
  'manus.agents.forceOverrideFromYaml': {
    name: '强制使用 YAML 中的 Agent 定义',
    hint: '开启后优先生效配置文件中的 Agent 定义，覆盖数据库中的旧配置'
  },
  'manus.agent.userInputTimeout': {
    name: '用户输入超时时间（秒）',
    hint: '等待用户在页面填写表单的最长时间，超时将自动放弃本次输入'
  },
  'manus.agent.maxMemory': {
    name: 'Agent 记忆条数上限',
    hint: '单个任务内最多保留的历史对话和执行记录条数'
  },
  'manus.agent.parallelToolCalls': {
    name: '允许并行调用工具',
    hint: '允许 Agent 在单个步骤中同时调用多个工具以加快执行'
  },

  // manus.infiniteContext.*
  'manus.infiniteContext.enabled': {
    name: '无限上下文模式',
    hint: '开启后通过自动总结等方式延长对话上下文，适合超长会话'
  },
  'manus.infiniteContext.parallelThreads': {
    name: '无限上下文并行线程数',
    hint: '处理无限上下文任务时的并行线程数量，线程数越高处理越快但占用资源更多'
  },
  'manus.infiniteContext.taskContextSize': {
    name: '任务上下文最大字符数',
    hint: '单个任务在无限上下文模式下可保留的最大上下文长度（字符数）'
  },

  // manus.filesystem.*
  'manus.filesystem.allowExternalAccess': {
    name: '允许访问工作目录外文件',
    hint: '开启后 Agent 可以读写工作目录之外的文件，存在安全风险，慎用'
  },

  // manus.mcpServiceLoader.*
  'manus.mcpServiceLoader.connectionTimeoutSeconds': {
    name: 'MCP 连接超时时间（秒）',
    hint: '与 MCP 服务建立连接时的最长等待时间'
  },
  'manus.mcpServiceLoader.maxRetryCount': {
    name: 'MCP 最大重试次数',
    hint: '调用 MCP 服务失败时的最大自动重试次数'
  },
  'manus.mcpServiceLoader.maxConcurrentConnections': {
    name: 'MCP 最大并发连接数',
    hint: '同时允许的 MCP 服务连接数量上限'
  },

  // manus.imageRecognition.*
  'manus.imageRecognition.poolSize': {
    name: '图像识别线程池大小',
    hint: '用于处理图像识别任务的并行线程数量'
  },
  'manus.imageRecognition.modelName': {
    name: '图像识别模型名称',
    hint: '用于 OCR / 图像分析的模型标识，例如 qwen-vl-ocr-latest'
  },
  'manus.imageRecognition.dpi': {
    name: '图像识别 DPI',
    hint: '处理文档类图片时使用的分辨率（DPI），数值越高识别越清晰但更耗时'
  },
  'manus.imageRecognition.imageType': {
    name: '图像颜色类型',
    hint: '图像处理时使用的颜色模式，例如 RGB'
  },
  'manus.imageRecognition.maxRetryAttempts': {
    name: '图像识别最大重试次数',
    hint: '图像识别调用失败时的最大自动重试次数'
  },

  'lynxe.browser.headless': {
    name: '浏览器无界面模式',
    hint: '开启后在后台无界面运行浏览器，适合服务器环境'
  },
  'lynxe.browser.requestTimeout': {
    name: '浏览器请求超时时间（秒）',
    hint: '单次网页请求的最长等待时间，超出将中止本次请求'
  },
  'lynxe.general.debugDetail': {
    name: 'Agent 调试详情模式',
    hint: '开启后输出更详细的调试信息，仅推荐在排查问题时使用'
  },
  'lynxe.general.openBrowser': {
    name: '自动打开浏览器页面',
    hint: '当任务需要浏览器时，是否自动在本机打开可视化浏览器窗口'
  },
  'lynxe.browser.enableShortUrl': {
    name: '启用短链接跳转',
    hint: '访问页面时使用短链接形式，便于分享和跳转'
  },
  'lynxe.maxSteps': {
    name: '最大执行步数',
    hint: '限制单个任务中 Lynxe Agent 的最大思考与工具调用轮数'
  },
  'lynxe.agent.userInputTimeout': {
    name: '用户输入超时时间（秒）',
    hint: '等待用户在页面填写表单的最长时间，超时将自动放弃本次输入'
  },
  'lynxe.agent.maxMemory': {
    name: 'Agent 记忆条数上限',
    hint: '单个任务内最多保留的历史对话和执行记录条数'
  },
  'lynxe.general.enableConversationMemory': {
    name: '开启对话记忆',
    hint: '控制是否为 Lynxe Agent 记录历史对话作为上下文'
  },
  'lynxe.agent.conversationMemoryMaxChars': {
    name: '对话记忆最大字符数',
    hint: '限制持久化对话内容的字符上限，防止数据无限增长'
  },
  'lynxe.agent.parallelToolCalls': {
    name: '允许并行调用工具',
    hint: '允许 Lynxe 在单个步骤中同时调用多个工具以加快执行'
  },
  'lynxe.agent.executorPoolSize': {
    name: '执行线程池大小',
    hint: 'Lynxe 内部用于并行任务执行的线程数量，数值越高并发越强'
  },
  'lynxe.agent.llmReadTimeout': {
    name: 'LLM 响应超时时间（秒）',
    hint: '等待大模型接口返回结果的最长时间'
  },
  'lynxe.general.externalLinkedFolder': {
    name: '外部挂载目录',
    hint: '允许 Lynxe 访问的额外文件夹路径，用于挂载外部文件'
  },
  'lynxe.mcpServiceLoader.connectionTimeoutSeconds': {
    name: 'MCP 连接超时时间（秒）',
    hint: '与 MCP 服务建立连接时的最长等待时间'
  },
  'lynxe.mcpServiceLoader.maxRetryCount': {
    name: 'MCP 最大重试次数',
    hint: '调用 MCP 服务失败时的最大自动重试次数'
  },
  'lynxe.mcpServiceLoader.maxConcurrentConnections': {
    name: 'MCP 最大并发连接数',
    hint: '同时允许的 MCP 服务连接数量上限'
  },
  'lynxe.imageRecognition.poolSize': {
    name: '图像识别线程池大小',
    hint: '用于处理图像识别任务的并行线程数量'
  },
  'lynxe.imageRecognition.modelName': {
    name: '图像识别模型名称',
    hint: '用于 OCR / 图像分析的模型标识，例如 qwen-vl-ocr-latest'
  },
  'lynxe.imageRecognition.dpi': {
    name: '图像识别 DPI',
    hint: '处理文档类图片时使用的分辨率（DPI），数值越高识别越清晰但更耗时'
  },
  'lynxe.imageRecognition.imageType': {
    name: '图像颜色类型',
    hint: '图像处理时使用的颜色模式，例如 RGB'
  },
  'lynxe.imageRecognition.maxRetryAttempts': {
    name: '图像识别最大重试次数',
    hint: '图像识别调用失败时的最大自动重试次数'
  }
};

const groupedConfigs = computed(() => {
  const map: Record<string, ConfigEntity[]> = {};
  let total = 0;

  configs.value.forEach(config => {
    const subGroup = config.configSubGroup || 'default';
    if (!map[subGroup]) {
      map[subGroup] = [];
    }
    map[subGroup].push(config);
    total++;
  });

  return { map, total };
});

const loadGroups = async () => {
  try {
    const res = await listConfigGroups();
    groups.value = res.data || [];
    if (groups.value.length > 0 && !activeGroup.value) {
      activeGroup.value = groups.value[0];
      await handleGroupSelect(activeGroup.value);
    }
  } catch (error) {
    console.error(error);
    ElMessage.error('加载配置分组失败');
  }
};

const handleGroupSelect = async (group: string) => {
  activeGroup.value = group;
  activeSubGroup.value = 'ALL';
  loading.value = true;
  try {
    const res = await getConfigsByGroup(group);
    configs.value = res.data || [];
  } catch (error) {
    ElMessage.error(`加载 ${group} 配置失败`);
  } finally {
    loading.value = false;
  }
};

const saveConfigs = async () => {
  saving.value = true;
  try {
    await batchUpdateConfigs(configs.value);
    ElMessage.success('配置已保存');
    await handleGroupSelect(activeGroup.value);
  } catch (error) {
    ElMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
};

const resetGroup = async () => {
  configs.value.forEach(config => {
    if (config.defaultValue !== undefined) {
      config.configValue = config.defaultValue;
    }
  });
  await saveConfigs();
};

const resetAll = async () => {
  try {
    await resetAllDefaults();
    ElMessage.success('所有配置已重置为默认值');
    await handleGroupSelect(activeGroup.value);
  } catch (error) {
    ElMessage.error('重置失败');
  }
};

const resetRow = (row: ConfigEntity) => {
  if (row.defaultValue !== undefined) {
    row.configValue = row.defaultValue;
  }
};

const isTextType = (type?: string) => {
  return !type || type === 'TEXT' || type === 'TEXTAREA';
};

const parseOptions = (jsonStr?: string) => {
  if (!jsonStr) return [];
  try {
    return JSON.parse(jsonStr);
  } catch (e) {
    return [];
  }
};

const getConfigDisplayName = (row: ConfigEntity) => {
  const mapped = configDisplayMap[row.configPath];
  if (mapped) {
    return mapped.name;
  }
  return row.description || row.configKey;
};

const getConfigDisplayHint = (row: ConfigEntity) => {
  const mapped = configDisplayMap[row.configPath];
  if (mapped) {
    return mapped.hint;
  }
  return row.configPath;
};

onMounted(() => {
  loadGroups();
});
</script>

<style scoped>
.config-center-container {
  background-color: #f5f7fa;
}

.group-sidebar {
  background-color: #fff;
  border-right: 1px solid #e6e6e6;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.sidebar-header h3 {
  margin: 0;
  color: #303133;
  font-size: 16px;
}

.group-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
}

.main-content {
  padding: 20px;
  overflow-y: auto;
}

.config-card {
  min-height: 100%;
}

.card-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-left h2 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.ml-2 {
  margin-left: 10px;
}

.config-list {
  padding: 10px 0;
}

.subgroup-section {
  margin-bottom: 30px;
}

.subgroup-title {
  font-size: 14px;
  font-weight: bold;
  color: #606266;
  margin-bottom: 10px;
  padding-left: 10px;
  border-left: 3px solid #409eff;
}

.config-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.config-path {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
}

.empty-state {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
