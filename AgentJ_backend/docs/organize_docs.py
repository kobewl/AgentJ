#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AgentJ 文档整理脚本
自动整理docs目录，合并重复文档，归档过期文档
"""

import os
import shutil
from pathlib import Path

# 源目录
SOURCE_DIR = Path(r"D:\wangliang\AgentJ\AgentJ_backend\docs")

# 目录映射
CATEGORIES = {
    "core/01-architecture": [
        "为什么AgentJ使用模板系统-架构设计深度解析.md",
    ],
    "core/02-agent": [
        "BaseAgent完全解析.md",
        "BaseAgent学习指南.md",
        "ReActAgent完全解析.md",
        "ReActAgent学习指南.md",
        "DynamicAgent核心解析.md",
        "DynamicAgent学习指南.md",
    ],
    "core/03-planning": [
        "PlanDraftingService核心解析-计划草稿服务.md",
        "PlanDraftingService完整流程-步骤生成与执行详解.md",
    ],
    "core/04-tools": [
        "AbstractBaseTool基类深度解析.md",
        "ToolCallBiFunctionDef接口深度解析.md",
        "Tool注解与ToolCallback完全解析.md",
        "工具管理机制深度解析.md",
        "工具环境数据收集机制.md",
        "工具类功能完整解析.md",
        "PlanningFactory核心解析-为什么需要三种工具创建方式.md",
        "数据库工具学习指南.md",
    ],
    "core/05-optimization": [
        "更智能的模板选择方案-基于模板元数据.md",
        "智能模板选择器-实现总结.md",
        "智能模板选择器-性能与并发分析.md",
        "智能模板选择器-优化实现完成.md",
    ],
    "guides": [
        "工具系统学习指南-第1天-接口与基类.md",
        "工具系统学习指南-第2天-具体工具实现.md",
        "工具系统学习指南-第3天-工具管理核心机制.md",
        "AI自动选择模式设计方案.md",
        "AI自动选择模式使用指南.md",
        "AI自动选择模式-完成总结.md",
    ],
    "archive": [
        "当前三种模式并存的问题分析.md",
        "两种模式合并改进方案.md",
        "AgentJ两种模式合并改进-完成总结.md",
    ],
}

def move_files():
    """移动文件到对应目录"""
    print("🚀 开始整理文档...")

    for category, files in CATEGORIES.items():
        target_dir = SOURCE_DIR / category
        target_dir.mkdir(parents=True, exist_ok=True)

        for file in files:
            source_file = SOURCE_DIR / file
            if source_file.exists():
                target_file = target_dir / file
                shutil.move(str(source_file), str(target_file))
                print(f"✅ 移动: {file} → {category}/")
            else:
                print(f"⚠️  不存在: {file}")

    print("\n✨ 文档整理完成！")

def create_index():
    """创建各目录的README"""
    # Core目录README
    core_readme = """# 核心技术文档

本目录包含AgentJ项目的核心技术深度解析文档。

## 目录结构

- [01-architecture](01-architecture/) - 架构设计
- [02-agent](02-agent/) - 智能体系统
- [03-planning](03-planning/) - 计划与模板
- [04-tools](04-tools/) - 工具系统
- [05-optimization](05-optimization/) - 性能优化

## 阅读建议

适合有一定基础，希望深入了解技术细节的开发者。
"""
    (SOURCE_DIR / "core" / "README.md").write_text(core_readme, encoding="utf-8")

    # Guides目录README
    guides_readme = """# 学习指南

本目录包含AgentJ项目的系统学习指南，按学习路径组织。

## 目录

- [quick-start.md](quick-start.md) - 快速入门
- [agent-system.md](agent-system.md) - 智能体系统学习
- [tool-system.md](tool-system.md) - 工具系统学习
- [planning-system.md](planning-system.md) - 计划系统学习

## 学习路径

1. 快速入门 - 5分钟了解核心概念
2. 选择一个系统深入学习（智能体/工具/计划）
3. 阅读核心技术文档进行深入研究
"""
    (SOURCE_DIR / "guides" / "README.md").write_text(guides_readme, encoding="utf-8")

    print("✅ 已创建目录索引文件")

if __name__ == "__main__":
    move_files()
    create_index()
