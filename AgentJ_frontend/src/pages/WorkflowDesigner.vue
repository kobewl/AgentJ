<template>
  <div class="workflow-designer">
    <!-- Header Toolbar -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button @click="createNewWorkflow" type="primary" :icon="Plus" class="new-btn">新建</el-button>
        <el-divider direction="vertical" />
        <el-input
          v-model="workflowName"
          placeholder="工作流名称"
          class="workflow-name-input"
        />
        <el-input
          v-model="workflowDescription"
          placeholder="工作流描述（可选）"
          class="workflow-desc-input"
        />
        <el-tag :type="statusTagType" size="small">{{ statusText }}</el-tag>
      </div>
      <div class="toolbar-right">
        <el-button @click="saveWorkflow" type="primary" :icon="Check" :loading="saving">
          保存
        </el-button>
        <el-button @click="publishWorkflow" type="warning" :icon="Upload" :disabled="!workflowId">
          发布
        </el-button>
        <el-button @click="openExecutePanel" type="success" :icon="VideoPlay">
          运行
        </el-button>
      </div>
    </div>

    <div class="designer-container">
      <!-- Left Panel: Workflow History + Node Types -->
      <div class="left-panel">
        <!-- Workflow History -->
        <div class="panel-section">
          <div class="section-header">
            <span>历史工作流</span>
            <el-button text size="small" @click="refreshWorkflowList" :icon="Refresh" />
          </div>
          <div class="workflow-history-list">
            <div
              v-for="wf in workflowList"
              :key="wf.id"
              class="history-item"
              :class="{ active: workflowId === wf.id }"
              @click="selectWorkflow(wf)"
            >
              <div class="history-item-name">{{ wf.name }}</div>
              <div class="history-item-desc" v-if="wf.description">{{ wf.description }}</div>
              <div class="history-item-meta">
                <el-tag size="small" :type="wf.status === 'PUBLISHED' ? 'success' : 'info'">
                  {{ wf.status === 'PUBLISHED' ? '已发布' : '草稿' }}
                </el-tag>
              </div>
            </div>
            <div v-if="workflowList.length === 0" class="empty-history">
              暂无保存的工作流
            </div>
          </div>
        </div>

        <el-divider />

        <!-- Node Types -->
        <div class="panel-section">
          <div class="section-header">节点类型</div>
          <div class="node-list">
            <div
              v-for="nodeType in nodeTypes"
              :key="nodeType.type"
              class="node-item"
              :style="{ '--node-color': nodeType.color }"
              draggable="true"
              @dragstart="onDragStart($event, nodeType)"
            >
              <div class="node-item-icon">
                <el-icon :size="18"><component :is="getIcon(nodeType.icon)" /></el-icon>
              </div>
              <div class="node-item-info">
                <div class="node-item-label">{{ nodeType.label }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Center: Vue Flow Canvas -->
      <div class="canvas-container" @drop="onDrop" @dragover.prevent>
        <VueFlow
          v-model:nodes="nodes"
          v-model:edges="edges"
          :default-viewport="{ zoom: 1 }"
          :min-zoom="0.2"
          :max-zoom="4"
          :connect-on-click="true"
          :default-edge-options="defaultEdgeOptions"
          :edges-updatable="true"
          :delete-key-code="['Backspace', 'Delete']"
          fit-view-on-init
          @connect="onConnect"
          @node-click="onNodeClick"
          @edge-click="onEdgeClick"
          @pane-click="onPaneClick"
        >
          <Background :gap="20" :size="1" pattern-color="rgba(0,0,0,0.03)" />
          <Controls position="bottom-left" />
          <MiniMap position="bottom-right" />

          <!-- Custom Node Templates -->
          <template #node-start="nodeProps">
            <div class="flow-node start-node" @click.stop="selectNode(nodeProps.id)">
              <div class="node-icon"><el-icon :size="20"><VideoPlay /></el-icon></div>
              <div class="node-label">开始</div>
              <Handle type="source" :position="Position.Right" id="source" class="handle-dot" />
            </div>
          </template>

          <template #node-end="nodeProps">
            <div class="flow-node end-node" @click.stop="selectNode(nodeProps.id)">
              <Handle type="target" :position="Position.Left" id="target" class="handle-dot" />
              <div class="node-icon"><el-icon :size="20"><CircleCheck /></el-icon></div>
              <div class="node-label">结束</div>
            </div>
          </template>

          <template #node-llm="nodeProps">
            <div 
              class="flow-node llm-node" 
              :class="{ selected: selectedNodeId === nodeProps.id }"
              @click.stop="selectNode(nodeProps.id)"
            >
              <Handle type="target" :position="Position.Left" id="target" class="handle-dot" />
              <div class="node-icon"><el-icon :size="20"><ChatLineRound /></el-icon></div>
              <div class="node-body">
                <div class="node-title">LLM</div>
                <div class="node-subtitle">{{ nodeProps.data.label || '大语言模型' }}</div>
              </div>
              <Handle type="source" :position="Position.Right" id="source" class="handle-dot" />
            </div>
          </template>

          <template #node-condition="nodeProps">
            <div 
              class="flow-node condition-node" 
              :class="{ selected: selectedNodeId === nodeProps.id }"
              @click.stop="selectNode(nodeProps.id)"
            >
              <Handle type="target" :position="Position.Left" id="target" class="handle-dot" />
              <div class="node-icon"><el-icon :size="20"><Switch /></el-icon></div>
              <div class="node-body">
                <div class="node-title">条件</div>
                <div class="node-subtitle">{{ nodeProps.data.label || '条件分支' }}</div>
              </div>
              <Handle type="source" :position="Position.Right" id="true" class="handle-dot" style="top: 35%;" />
              <Handle type="source" :position="Position.Right" id="false" class="handle-dot" style="top: 65%;" />
            </div>
          </template>

          <template #node-tool="nodeProps">
            <div 
              class="flow-node tool-node" 
              :class="{ selected: selectedNodeId === nodeProps.id }"
              @click.stop="selectNode(nodeProps.id)"
            >
              <Handle type="target" :position="Position.Left" id="target" class="handle-dot" />
              <div class="node-icon"><el-icon :size="20"><Tools /></el-icon></div>
              <div class="node-body">
                <div class="node-title">工具</div>
                <div class="node-subtitle">{{ nodeProps.data.toolName || '选择工具' }}</div>
              </div>
              <Handle type="source" :position="Position.Right" id="source" class="handle-dot" />
            </div>
          </template>

          <!-- Edge label template for deletion -->
          <template #edge-label="edgeProps">
            <div class="edge-label-wrapper">
              <el-button
                type="danger"
                size="small"
                circle
                :icon="Delete"
                class="edge-delete-btn"
                @click.stop="deleteEdge(edgeProps.id)"
              />
            </div>
          </template>
        </VueFlow>

        <!-- Edge delete hint -->
        <div class="canvas-hint" v-if="selectedEdgeId">
          <el-tag type="info" closable @close="selectedEdgeId = null">
            已选中连线 - 点击删除按钮或按 Delete 键删除
          </el-tag>
        </div>
      </div>

      <!-- Right Panel: Node Properties -->
      <div class="right-panel" v-if="selectedNodeId">
        <div class="panel-header">
          <span>节点配置</span>
          <el-button text size="small" @click="selectedNodeId = null" :icon="Close" />
        </div>
        
        <el-form label-position="top" size="default" class="property-form">
          <el-form-item label="节点 ID">
            <el-input :value="selectedNodeId" disabled />
          </el-form-item>

          <!-- LLM Node Config -->
          <template v-if="getNodeType(selectedNodeId) === 'llm'">
            <el-form-item label="节点名称">
              <el-input
                :value="getNodeDataValue('label')"
                @input="setNodeDataValue('label', $event)"
                placeholder="LLM 节点"
              />
            </el-form-item>
            <el-form-item label="选择模型">
              <el-select
                :model-value="getNodeDataValue('modelName')"
                @update:model-value="setNodeDataValue('modelName', $event)"
                placeholder="选择 AI 模型"
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="model in availableModels"
                  :key="model.modelName"
                  :label="model.modelName"
                  :value="model.modelName"
                >
                  <span>{{ model.modelName }}</span>
                  <span v-if="model.isDefault" style="color: #67c23a; margin-left: 8px;">(默认)</span>
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="系统提示词">
              <el-input
                :value="getNodeDataValue('systemPrompt')"
                @input="setNodeDataValue('systemPrompt', $event)"
                type="textarea"
                :rows="3"
                placeholder="定义 AI 的角色和行为"
              />
            </el-form-item>
            <el-form-item label="提示词模板">
              <el-input
                :value="getNodeDataValue('promptTemplate')"
                @input="setNodeDataValue('promptTemplate', $event)"
                type="textarea"
                :rows="4"
                placeholder="使用 {{input}} 引用用户输入"
              />
            </el-form-item>
            <el-form-item label="温度">
              <el-slider
                :model-value="Number(getNodeDataValue('temperature')) || 0.7"
                @update:model-value="setNodeDataValue('temperature', String($event))"
                :min="0"
                :max="2"
                :step="0.1"
                show-input
              />
            </el-form-item>
            <el-form-item label="Top P">
              <el-slider
                :model-value="Number(getNodeDataValue('topP')) || 1"
                @update:model-value="setNodeDataValue('topP', String($event))"
                :min="0"
                :max="1"
                :step="0.1"
                show-input
              />
            </el-form-item>
            <el-form-item label="输出变量">
              <el-input
                :value="getNodeDataValue('outputKey') || 'llm_output'"
                @input="setNodeDataValue('outputKey', $event)"
              />
            </el-form-item>
          </template>

          <!-- Condition Node Config -->
          <template v-if="getNodeType(selectedNodeId) === 'condition'">
            <el-form-item label="节点名称">
              <el-input
                :value="getNodeDataValue('label')"
                @input="setNodeDataValue('label', $event)"
                placeholder="条件节点"
              />
            </el-form-item>
            <el-form-item label="条件表达式">
              <el-input
                :value="getNodeDataValue('expression')"
                @input="setNodeDataValue('expression', $event)"
                type="textarea"
                :rows="3"
                placeholder="JavaScript 表达式，如: input.length > 100"
              />
            </el-form-item>
          </template>

          <!-- Tool Node Config -->
          <template v-if="getNodeType(selectedNodeId) === 'tool'">
            <el-form-item label="节点名称">
              <el-input
                :value="getNodeDataValue('label')"
                @input="setNodeDataValue('label', $event)"
                placeholder="工具节点"
              />
            </el-form-item>
            <el-form-item label="工具名称">
              <el-input
                :value="getNodeDataValue('toolName')"
                @input="setNodeDataValue('toolName', $event)"
                placeholder="选择要调用的工具"
              />
            </el-form-item>
            <el-form-item label="输出变量">
              <el-input
                :value="getNodeDataValue('outputKey') || 'tool_output'"
                @input="setNodeDataValue('outputKey', $event)"
              />
            </el-form-item>
          </template>

          <!-- Delete button -->
          <el-form-item v-if="getNodeType(selectedNodeId) !== 'start' && getNodeType(selectedNodeId) !== 'end'">
            <el-button type="danger" @click="deleteSelectedNode" style="width: 100%">
              <el-icon><Delete /></el-icon>
              删除节点
            </el-button>
          </el-form-item>
        </el-form>
      </div>
      <div class="right-panel empty" v-else>
        <div class="empty-state">
          <el-icon :size="48" color="#dcdfe6"><SetUp /></el-icon>
          <p>点击节点编辑配置</p>
          <p class="hint">提示: 点击连线可删除</p>
        </div>
      </div>
    </div>

    <!-- Execute Panel (Coze/Dify Style) -->
    <el-drawer
      v-model="executeDrawerVisible"
      title="运行工作流"
      direction="rtl"
      size="420px"
      :with-header="true"
    >
      <div class="execute-panel">
        <div class="execute-config">
          <div class="config-section">
            <label>用户输入</label>
            <el-input
              v-model="userPrompt"
              type="textarea"
              :rows="4"
              placeholder="输入您的问题或指令..."
            />
          </div>
        </div>

        <div class="execute-actions">
          <el-button
            type="primary"
            size="large"
            :loading="executing"
            @click="doExecute"
            style="width: 100%"
          >
            <el-icon v-if="!executing"><VideoPlay /></el-icon>
            {{ executing ? '运行中...' : '开始运行' }}
          </el-button>
        </div>

        <div v-if="executeResult" class="execute-result">
          <div class="result-header">
            <el-icon color="#67c23a"><CircleCheck /></el-icon>
            <span>执行结果</span>
          </div>
          <div class="result-content">
            <pre>{{ formatResult(executeResult) }}</pre>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { VueFlow, Position, Handle, MarkerType } from '@vue-flow/core';
import type { Connection, Node, Edge, DefaultEdgeOptions } from '@vue-flow/core';
import { Background } from '@vue-flow/background';
import { Controls } from '@vue-flow/controls';
import { MiniMap } from '@vue-flow/minimap';
import '@vue-flow/core/dist/style.css';
import '@vue-flow/core/dist/theme-default.css';
import '@vue-flow/controls/dist/style.css';
import '@vue-flow/minimap/dist/style.css';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  Plus, Check, VideoPlay, ChatLineRound, Switch, Tools,
  Refresh, Close, CircleCheck, SetUp, Delete, Upload
} from '@element-plus/icons-vue';
import * as workflowApi from '@/api/workflow';
import type { Workflow, NodeType, ModelInfo } from '@/api/workflow';

// Default edge options with arrow
const defaultEdgeOptions: DefaultEdgeOptions = {
  type: 'smoothstep',
  animated: true,
  style: { stroke: '#1890ff', strokeWidth: 2 },
  markerEnd: {
    type: MarkerType.ArrowClosed,
    color: '#1890ff',
  },
  labelBgPadding: [8, 4] as [number, number],
  labelBgBorderRadius: 4,
  labelBgStyle: { fill: '#fff', fillOpacity: 0.9 },
};

// State
const workflowId = ref<number | null>(null);
const workflowName = ref('未命名工作流');
const workflowDescription = ref('');
const workflowStatus = ref('DRAFT');
const nodes = ref<Node[]>([]);
const edges = ref<Edge[]>([]);
const selectedNodeId = ref<string | null>(null);
const selectedEdgeId = ref<string | null>(null);
const nodeTypes = ref<NodeType[]>([]);
const saving = ref(false);
const executing = ref(false);
const executeDrawerVisible = ref(false);
const workflowList = ref<Workflow[]>([]);
const userPrompt = ref('');
const executeResult = ref<any>(null);
const availableModels = ref<ModelInfo[]>([]);

// Computed helpers - must use computed() for reactivity in template
const statusTagType = computed(() => {
  switch (workflowStatus.value) {
    case 'PUBLISHED': return 'success';
    case 'ARCHIVED': return 'info';
    default: return 'warning';
  }
});

const statusText = computed(() => {
  switch (workflowStatus.value) {
    case 'PUBLISHED': return '已发布';
    case 'ARCHIVED': return '已归档';
    default: return '草稿';
  }
});

// Node data helpers - using functions for reactivity
function getNodeType(nodeId: string): string {
  const node = nodes.value.find(n => n.id === nodeId);
  return node?.type || '';
}

function getNodeDataValue(key: string): string {
  if (!selectedNodeId.value) return '';
  const node = nodes.value.find(n => n.id === selectedNodeId.value);
  return node?.data?.[key] || '';
}

function setNodeDataValue(key: string, value: string) {
  if (!selectedNodeId.value) return;
  const nodeIndex = nodes.value.findIndex(n => n.id === selectedNodeId.value);
  if (nodeIndex === -1) return;
  
  // Create new array to trigger reactivity
  const newNodes = [...nodes.value];
  newNodes[nodeIndex] = {
    ...newNodes[nodeIndex],
    data: {
      ...newNodes[nodeIndex].data,
      [key]: value,
    },
  };
  nodes.value = newNodes;
}

// Icon helper
const getIcon = (iconName: string) => {
  const icons: Record<string, any> = {
    play: VideoPlay,
    stop: CircleCheck,
    robot: ChatLineRound,
    branch: Switch,
    tool: Tools,
  };
  return icons[iconName] || ChatLineRound;
};

// Lifecycle
onMounted(async () => {
  await loadNodeTypes();
  await loadAvailableModels();
  await refreshWorkflowList();
  initDefaultNodes();
  
  // Add keyboard listener for delete
  window.addEventListener('keydown', handleKeyDown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown);
});

function handleKeyDown(e: KeyboardEvent) {
  if (e.key === 'Delete' || e.key === 'Backspace') {
    if (selectedEdgeId.value) {
      deleteEdge(selectedEdgeId.value);
    }
  }
}

// Methods
async function loadNodeTypes() {
  try {
    nodeTypes.value = await workflowApi.getNodeTypes();
  } catch (e) {
    nodeTypes.value = [
      { type: 'start', label: '开始', description: '工作流入口', icon: 'play', color: '#52c41a' },
      { type: 'end', label: '结束', description: '工作流结束', icon: 'stop', color: '#ff4d4f' },
      { type: 'llm', label: 'LLM', description: '大语言模型', icon: 'robot', color: '#1890ff' },
      { type: 'condition', label: '条件', description: '条件分支', icon: 'branch', color: '#faad14' },
      { type: 'tool', label: '工具', description: '调用工具', icon: 'tool', color: '#722ed1' },
    ];
  }
}

async function refreshWorkflowList() {
  try {
    workflowList.value = await workflowApi.getWorkflows();
  } catch (e) {
    console.error('Failed to load workflows', e);
  }
}

async function loadAvailableModels() {
  try {
    availableModels.value = await workflowApi.getAvailableModels();
  } catch (e) {
    console.error('Failed to load models', e);
  }
}

function initDefaultNodes() {
  nodes.value = [
    { id: 'start', type: 'start', position: { x: 100, y: 200 }, data: {} },
    { id: 'end', type: 'end', position: { x: 700, y: 200 }, data: {} },
  ];
  edges.value = [];
}

function createNewWorkflow() {
  workflowId.value = null;
  workflowName.value = '未命名工作流';
  workflowDescription.value = '';
  workflowStatus.value = 'DRAFT';
  initDefaultNodes();
  selectedNodeId.value = null;
  selectedEdgeId.value = null;
}

let nodeIdCounter = 0;

function onDragStart(event: DragEvent, nodeType: NodeType) {
  if (event.dataTransfer) {
    event.dataTransfer.setData('application/vueflow', JSON.stringify(nodeType));
    event.dataTransfer.effectAllowed = 'move';
  }
}

function onDrop(event: DragEvent) {
  event.preventDefault();
  const data = event.dataTransfer?.getData('application/vueflow');
  if (!data) return;

  const nodeType: NodeType = JSON.parse(data);
  const canvasEl = event.currentTarget as HTMLElement;
  const bounds = canvasEl.getBoundingClientRect();
  const x = event.clientX - bounds.left;
  const y = event.clientY - bounds.top;

  const newNode: Node = {
    id: `${nodeType.type}_${++nodeIdCounter}`,
    type: nodeType.type,
    position: { x, y },
    data: { label: nodeType.label },
  };

  nodes.value = [...nodes.value, newNode];
}

function onConnect(connection: Connection) {
  if (!connection.source || !connection.target) return;
  
  console.log('Creating edge:', connection);
  
  const newEdge: Edge = {
    id: `e-${connection.source}-${connection.target}-${Date.now()}`,
    source: connection.source,
    target: connection.target,
    sourceHandle: connection.sourceHandle || undefined,
    targetHandle: connection.targetHandle || undefined,
    type: 'smoothstep',
    animated: true,
    style: { stroke: '#1890ff', strokeWidth: 2 },
    markerEnd: { type: MarkerType.ArrowClosed, color: '#1890ff' },
  };
  
  edges.value = [...edges.value, newEdge];
  console.log('Edges after add:', edges.value.length, edges.value);
  ElMessage.success('连接已创建');
}

function onNodeClick(event: any) {
  selectNode(event.node.id);
}

function selectNode(nodeId: string) {
  selectedNodeId.value = nodeId;
  selectedEdgeId.value = null;
}

function onEdgeClick(event: any) {
  selectedEdgeId.value = event.edge.id;
  selectedNodeId.value = null;
  ElMessage.info('已选中连线，点击删除按钮或按 Delete 键删除');
}

function onPaneClick() {
  selectedNodeId.value = null;
  selectedEdgeId.value = null;
}

function deleteEdge(edgeId: string) {
  edges.value = edges.value.filter(e => e.id !== edgeId);
  selectedEdgeId.value = null;
  ElMessage.success('连线已删除');
}

function deleteSelectedNode() {
  if (!selectedNodeId.value) return;
  const nodeType = getNodeType(selectedNodeId.value);
  if (nodeType === 'start' || nodeType === 'end') {
    ElMessage.warning('开始和结束节点不能删除');
    return;
  }
  
  // Remove node
  nodes.value = nodes.value.filter(n => n.id !== selectedNodeId.value);
  // Remove connected edges
  edges.value = edges.value.filter(e => 
    e.source !== selectedNodeId.value && e.target !== selectedNodeId.value
  );
  selectedNodeId.value = null;
  ElMessage.success('节点已删除');
}

function selectWorkflow(row: Workflow) {
  workflowId.value = row.id ?? null;
  workflowName.value = row.name;
  workflowDescription.value = row.description || '';
  workflowStatus.value = row.status ?? 'DRAFT';
  nodes.value = row.nodes || [];
  edges.value = row.edges || [];
  selectedNodeId.value = null;
  selectedEdgeId.value = null;
}

async function saveWorkflow() {
  saving.value = true;
  try {
    const workflow: Workflow = {
      id: workflowId.value ?? undefined,
      name: workflowName.value,
      description: workflowDescription.value || undefined,
      status: workflowStatus.value,
      nodes: nodes.value.map(n => ({
        id: n.id,
        type: n.type || 'default',
        position: n.position,
        data: n.data,
      })),
      edges: edges.value.map(e => {
        console.log('Saving edge:', e);
        return {
          id: e.id,
          source: e.source,
          target: e.target,
          sourceHandle: e.sourceHandle ?? undefined,
          targetHandle: e.targetHandle ?? undefined,
        };
      }),
    };

    let result: Workflow;
    if (workflowId.value) {
      result = await workflowApi.updateWorkflow(workflowId.value, workflow);
    } else {
      result = await workflowApi.createWorkflow(workflow);
      workflowId.value = result.id ?? null;
    }

    await refreshWorkflowList();
    ElMessage.success('保存成功');
  } catch (e: any) {
    ElMessage.error('保存失败: ' + e.message);
  } finally {
    saving.value = false;
  }
}

async function publishWorkflow() {
  if (!workflowId.value) {
    ElMessage.warning('请先保存工作流');
    return;
  }
  try {
    const result = await workflowApi.publishWorkflow(workflowId.value);
    workflowStatus.value = result.status || 'PUBLISHED';
    await refreshWorkflowList();
    ElMessage.success('发布成功！');
  } catch (e: any) {
    ElMessage.error('发布失败: ' + e.message);
  }
}

function openExecutePanel() {
  if (!workflowId.value) {
    ElMessage.warning('请先保存工作流');
    return;
  }
  userPrompt.value = '';
  executeResult.value = null;
  executeDrawerVisible.value = true;
}

async function doExecute() {
  if (!userPrompt.value.trim()) {
    ElMessage.warning('请输入您的问题或指令');
    return;
  }

  executing.value = true;
  try {
    const inputs = {
      input: userPrompt.value,
    };
    executeResult.value = await workflowApi.executeWorkflow(workflowId.value!, { inputs });
    ElMessage.success('执行完成');
  } catch (e: any) {
    ElMessage.error('执行失败: ' + e.message);
  } finally {
    executing.value = false;
  }
}

function formatResult(result: any): string {
  if (typeof result === 'string') return result;
  if (result.final_output) return result.final_output;
  if (result.llm_output) return result.llm_output;
  if (result.output) return result.output;
  return JSON.stringify(result, null, 2);
}
</script>

<style scoped>
.workflow-designer {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f7f8fa;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.workflow-name-input {
  width: 180px;
}

.workflow-desc-input {
  width: 280px;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.designer-container {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* Left Panel */
.left-panel {
  width: 240px;
  background: #fff;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-section {
  padding: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.workflow-history-list {
  max-height: 200px;
  overflow-y: auto;
}

.history-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 6px;
  background: #f5f7fa;
  transition: all 0.2s;
}

.history-item:hover {
  background: #ecf5ff;
}

.history-item.active {
  background: #ecf5ff;
  border: 1px solid #409eff;
}

.history-item-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 2px;
}

.history-item-desc {
  font-size: 11px;
  color: #909399;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item-meta {
  display: flex;
  gap: 6px;
}

.empty-history {
  color: #909399;
  font-size: 12px;
  text-align: center;
  padding: 20px;
}

.node-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.node-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: grab;
  transition: all 0.2s;
  border-left: 3px solid var(--node-color);
}

.node-item:hover {
  background: #ebeef5;
  transform: translateX(4px);
}

.node-item-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--node-color);
  border-radius: 6px;
  color: #fff;
}

.node-item-label {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

/* Canvas */
.canvas-container {
  flex: 1;
  position: relative;
  background: #fafbfc;
}

.canvas-hint {
  position: absolute;
  bottom: 60px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
}

/* Right Panel */
.right-panel {
  width: 300px;
  background: #fff;
  border-left: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
}

.right-panel.empty {
  justify-content: center;
  align-items: center;
}

.empty-state {
  text-align: center;
  color: #909399;
}

.empty-state p {
  margin-top: 12px;
  font-size: 13px;
}

.empty-state .hint {
  font-size: 11px;
  color: #c0c4cc;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
  font-weight: 600;
  color: #303133;
}

.property-form {
  padding: 16px;
  flex: 1;
  overflow-y: auto;
}

/* Flow Node Styles */
.flow-node {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  min-width: 140px;
  border: 2px solid transparent;
  transition: all 0.2s;
  cursor: pointer;
}

.flow-node:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.12);
}

.flow-node.selected {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64,158,255,0.2);
}

.flow-node .node-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #fff;
}

.flow-node .node-body {
  flex: 1;
}

.flow-node .node-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.flow-node .node-subtitle {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.flow-node .node-label {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.start-node .node-icon { background: linear-gradient(135deg, #52c41a, #73d13d); }
.end-node .node-icon { background: linear-gradient(135deg, #ff4d4f, #ff7875); }
.llm-node .node-icon { background: linear-gradient(135deg, #1890ff, #40a9ff); }
.condition-node .node-icon { background: linear-gradient(135deg, #faad14, #ffc53d); }
.tool-node .node-icon { background: linear-gradient(135deg, #722ed1, #9254de); }

.start-node { border-color: #52c41a; }
.end-node { border-color: #ff4d4f; }
.llm-node { border-color: #1890ff; }
.condition-node { border-color: #faad14; }
.tool-node { border-color: #722ed1; }

/* Handle Styles - fixed position to prevent jittering */
.handle-dot {
  width: 12px !important;
  height: 12px !important;
  border-radius: 50% !important;
  background: #1890ff !important;
  border: 2px solid #fff !important;
  box-shadow: 0 0 4px rgba(0,0,0,0.2) !important;
  transition: background 0.2s !important;
}

.handle-dot:hover {
  background: #40a9ff !important;
  cursor: crosshair !important;
}

/* Edge delete button */
.edge-label-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
}

.edge-delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
}

:deep(.vue-flow__edge:hover) .edge-delete-btn {
  opacity: 1;
}

/* Execute Panel */
.execute-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px;
}

.execute-config {
  flex: 1;
}

.config-section {
  margin-bottom: 20px;
}

.config-section label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
}

.execute-actions {
  padding: 16px 0;
}

.execute-result {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  margin-top: 16px;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #67c23a;
  margin-bottom: 12px;
}

.result-content {
  background: #fff;
  border-radius: 6px;
  padding: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.result-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-size: 13px;
  line-height: 1.6;
}

/* Vue Flow Overrides */
:deep(.vue-flow__edge-path) {
  stroke: #1890ff;
  stroke-width: 2;
}

:deep(.vue-flow__edge.animated path) {
  stroke-dasharray: 5;
  animation: dash 0.5s linear infinite;
}

:deep(.vue-flow__edge.selected path) {
  stroke: #ff4d4f;
  stroke-width: 3;
}

@keyframes dash {
  to {
    stroke-dashoffset: -10;
  }
}
</style>
