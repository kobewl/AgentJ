<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="rules"
    :label-position="labelPosition"
    :size="size"
    class="enhanced-form"
    @submit.prevent="handleSubmit"
  >
    <slot></slot>
  </el-form>
</template>

<script setup lang="ts">
import { ref, type PropType } from 'vue';
import type { FormInstance, FormRules, FormItemProp } from 'element-plus';

const props = defineProps({
  formData: {
    type: Object as PropType<Record<string, any>>,
    required: true
  },
  rules: {
    type: Object as PropType<FormRules>,
    default: () => ({})
  },
  labelPosition: {
    type: String as PropType<'left' | 'right' | 'top'>,
    default: 'top'
  },
  size: {
    type: String as PropType<'large' | 'default' | 'small'>,
    default: 'default'
  }
});

const emit = defineEmits<{
  submit: [valid: boolean];
}>();

const formRef = ref<FormInstance>();

const validate = async () => {
  if (!formRef.value) return false;
  try {
    await formRef.value.validate();
    return true;
  } catch {
    return false;
  }
};

const validateField = async (props: FormItemProp) => {
  if (!formRef.value) return false;
  try {
    await formRef.value.validateField(props);
    return true;
  } catch {
    return false;
  }
};

const resetFields = () => {
  formRef.value?.resetFields();
};

const clearValidate = () => {
  formRef.value?.clearValidate();
};

const handleSubmit = async () => {
  const valid = await validate();
  emit('submit', valid);
};

defineExpose({
  validate,
  validateField,
  resetFields,
  clearValidate
});
</script>

<style scoped>
.enhanced-form {
  width: 100%;
}
</style>
