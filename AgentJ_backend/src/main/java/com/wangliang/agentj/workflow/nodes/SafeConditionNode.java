package com.wangliang.agentj.workflow.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 安全的条件节点实现
 *
 * 使用SpEL（Spring Expression Language）替代JavaScript引擎，
 * 避免代码注入风险，同时提供强大的表达式求值能力。
 *
 * 支持的条件类型：
 * - EXPRESSION: SpEL表达式（如 #score > 80）
 * - SIMPLE: 简单值匹配
 * - COMPARISON: 比较表达式（如 #value == 'target'）
 *
 * @author AgentJ
 */
@Slf4j
public class SafeConditionNode implements NodeAction {

    private final SafeConditionNodeConfig config;
    private final ExpressionParser parser;

    public SafeConditionNode(SafeConditionNodeConfig config) {
        this.config = config;
        this.parser = new SpelExpressionParser();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("SafeConditionNode [{}] evaluating condition type: {}", config.getNodeId(), config.getConditionType());

        String nextNode;

        switch (config.getConditionType()) {
            case EXPRESSION -> {
                nextNode = evaluateSpelExpression(state);
            }
            case COMPARISON -> {
                nextNode = evaluateComparison(state);
            }
            case SIMPLE -> {
                nextNode = evaluateSimpleCondition(state);
            }
            default -> {
                log.warn("Unknown condition type: {}, using default target", config.getConditionType());
                nextNode = config.getDefaultTarget();
            }
        }

        log.info("Condition evaluated, next node: {}", nextNode);

        Map<String, Object> result = new HashMap<>();
        result.put("_next_node", nextNode);
        result.put("_current_node", config.getNodeId());
        result.put("_condition_result", nextNode);

        return result;
    }

    /**
     * 使用SpEL表达式求值
     *
     * 示例表达式：
     * - #score > 80
     * - #status == 'approved'
     * - #items.?[#price > 100].size() > 5
     * - #user.age >= 18 and #user.hasPermission
     */
    private String evaluateSpelExpression(OverAllState state) {
        try {
            // 创建安全的求值上下文
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("state", state.data());

            // 将state中的所有变量注册到上下文
            for (Map.Entry<String, Object> entry : state.data().entrySet()) {
                // 只允许简单类型的变量，避免安全风险
                if (isSafeType(entry.getValue())) {
                    context.setVariable(entry.getKey(), entry.getValue());
                }
            }

            // 解析并求值表达式
            Expression expression = parser.parseExpression(config.getExpression(), ParserContext.TEMPLATE_EXPRESSION);
            Object result = expression.getValue(context);

            log.debug("SpEL expression '{}' evaluated to: {}", config.getExpression(), result);

            // 处理求值结果
            if (result instanceof Boolean) {
                return (Boolean) result ? config.getTrueTarget() : config.getFalseTarget();
            } else if (result instanceof String) {
                String resultStr = (String) result;
                // 检查是否在cases映射中
                if (config.getCases() != null && config.getCases().containsKey(resultStr)) {
                    return config.getCases().get(resultStr);
                }
                return resultStr;
            } else if (result instanceof Number) {
                // 数字结果转换为字符串映射
                String numStr = String.valueOf(result);
                if (config.getCases() != null && config.getCases().containsKey(numStr)) {
                    return config.getCases().get(numStr);
                }
            }

            return config.getDefaultTarget();

        } catch (Exception e) {
            log.error("Failed to evaluate SpEL expression: {}", config.getExpression(), e);
            return config.getDefaultTarget();
        }
    }

    /**
     * 比较表达式求值
     * 支持简单的比较操作：==, !=, >, <, >=, <=
     */
    private String evaluateComparison(OverAllState state) {
        try {
            Object value = state.value(config.getVariable()).orElse(null);
            Object targetValue = config.getTargetValue();

            if (value == null) {
                return config.getFalseTarget() != null ? config.getFalseTarget() : config.getDefaultTarget();
            }

            boolean result;
            String operator = config.getOperator() != null ? config.getOperator() : "==";

            switch (operator) {
                case "==" -> result = Objects.equals(value, targetValue);
                case "!=" -> result = !Objects.equals(value, targetValue);
                case ">" -> result = compareNumbers(value, targetValue) > 0;
                case "<" -> result = compareNumbers(value, targetValue) < 0;
                case ">=" -> result = compareNumbers(value, targetValue) >= 0;
                case "<=" -> result = compareNumbers(value, targetValue) <= 0;
                case "contains" -> result = value.toString().contains(targetValue.toString());
                case "startsWith" -> result = value.toString().startsWith(targetValue.toString());
                case "endsWith" -> result = value.toString().endsWith(targetValue.toString());
                default -> {
                    log.warn("Unknown operator: {}, using default", operator);
                    return config.getDefaultTarget();
                }
            }

            return result ? config.getTrueTarget() : config.getFalseTarget();

        } catch (Exception e) {
            log.error("Failed to evaluate comparison condition", e);
            return config.getDefaultTarget();
        }
    }

    /**
     * 简单值匹配求值
     */
    private String evaluateSimpleCondition(OverAllState state) {
        Object value = state.value(config.getVariable()).orElse(null);

        if (value == null) {
            return config.getDefaultTarget();
        }

        String stringValue = String.valueOf(value);

        // 检查cases映射
        if (config.getCases() != null) {
            for (Map.Entry<String, String> caseEntry : config.getCases().entrySet()) {
                if (caseEntry.getKey().equals(stringValue)) {
                    return caseEntry.getValue();
                }
            }
        }

        // 布尔值检查
        if ("true".equalsIgnoreCase(stringValue) || "1".equals(stringValue)) {
            return config.getTrueTarget() != null ? config.getTrueTarget() : config.getDefaultTarget();
        }

        if ("false".equalsIgnoreCase(stringValue) || "0".equals(stringValue)) {
            return config.getFalseTarget() != null ? config.getFalseTarget() : config.getDefaultTarget();
        }

        return config.getDefaultTarget();
    }

    /**
     * 比较两个数字
     */
    private int compareNumbers(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            double numA = ((Number) a).doubleValue();
            double numB = ((Number) b).doubleValue();
            return Double.compare(numA, numB);
        }
        throw new IllegalArgumentException("Both values must be numbers");
    }

    /**
     * 检查类型是否安全（避免注入风险）
     */
    private boolean isSafeType(Object value) {
        if (value == null) {
            return true;
        }
        Class<?> clazz = value.getClass();
        // 只允许基本类型和简单类型
        return clazz.isPrimitive()
                || Number.class.isAssignableFrom(clazz)
                || String.class.isAssignableFrom(clazz)
                || Boolean.class.isAssignableFrom(clazz)
                || Character.class.isAssignableFrom(clazz)
                || Map.class.isAssignableFrom(clazz)
                || Iterable.class.isAssignableFrom(clazz);
    }

    @Data
    @Builder
    public static class SafeConditionNodeConfig {
        private String nodeId;
        private ConditionType conditionType;

        // EXPRESSION 类型
        private String expression;

        // COMPARISON 类型
        private String variable;
        private String operator;        // ==, !=, >, <, >=, <=, contains, startsWith, endsWith
        private Object targetValue;

        // SIMPLE 类型
        private Map<String, String> cases;  // value -> target node

        // 通用目标配置
        private String trueTarget;
        private String falseTarget;
        private String defaultTarget;
    }

    public enum ConditionType {
        /** SpEL表达式 */
        EXPRESSION,
        /** 比较表达式 */
        COMPARISON,
        /** 简单值匹配 */
        SIMPLE
    }

    /**
     * 条件节点构建器，简化配置创建
     */
    public static class FluentBuilder {
        private final SafeConditionNodeConfig config;

        private FluentBuilder() {
            this.config = SafeConditionNodeConfig.builder().build();
        }

        public static FluentBuilder create(String nodeId) {
            return new FluentBuilder().nodeId(nodeId);
        }

        public FluentBuilder nodeId(String nodeId) {
            config.setNodeId(nodeId);
            return this;
        }

        public FluentBuilder expression(String expr) {
            config.setConditionType(ConditionType.EXPRESSION);
            config.setExpression(expr);
            return this;
        }

        public FluentBuilder comparison(String variable, String operator, Object targetValue) {
            config.setConditionType(ConditionType.COMPARISON);
            config.setVariable(variable);
            config.setOperator(operator);
            config.setTargetValue(targetValue);
            return this;
        }

        public FluentBuilder trueTarget(String target) {
            config.setTrueTarget(target);
            return this;
        }

        public FluentBuilder falseTarget(String target) {
            config.setFalseTarget(target);
            return this;
        }

        public FluentBuilder defaultTarget(String target) {
            config.setDefaultTarget(target);
            return this;
        }

        public FluentBuilder cases(Map<String, String> cases) {
            config.setCases(cases);
            return this;
        }

        public SafeConditionNode build() {
            return new SafeConditionNode(config);
        }
    }
}
