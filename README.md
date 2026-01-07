# COS 418: Distributed Systems — Course README

> Princeton University

---

## 课程简介（Course Overview）

COS 418 是普林斯顿大学计算机科学系的核心高阶课程，主题为 **分布式系统（Distributed Systems）**。课程围绕真实世界中大规模分布式系统的设计与实现，系统性地讲解一致性、容错、复制、共识、并发控制等关键问题。

本课程强调：

* **原理 + 设计权衡（trade-offs）**
* **从失败中设计系统（Design for failure）**
* **理论模型与工程实践的结合**

---

## 学习目标（Learning Objectives）

完成本课程后，你将能够：

* 理解分布式系统中常见的失败模式（崩溃、网络分区、延迟）
* 掌握一致性模型（Linearizability、Sequential Consistency、Causal Consistency 等）
* 理解并实现核心协议（Paxos、Raft、2PC、Bayou、COPS 等）
* 分析 CAP、FLP 等经典不可能性与权衡结论
* 具备阅读和设计工业级分布式系统的能力

---

## 课程内容概览（Topics Covered）

### 1️⃣ 分布式系统基础

* 什么是分布式系统
* 为什么分布式系统“难”
* 时间、顺序与不确定性

### 2️⃣ 一致性与复制

* Linearizability vs Sequential Consistency
* Primary-backup replication
* Quorum systems

### 3️⃣ 共识协议（Consensus）

* Paxos（重点与难点）
* Raft（工程友好版本）
* Leader election

### 4️⃣ 事务与并发控制

* Two-Phase Commit (2PC)
* 分布式事务问题
* 死锁与恢复

### 5️⃣ 弱一致性系统

* Bayou（最终一致性）
* COPS（因果一致性）
* Conflict resolution

### 6️⃣ 容错与可用性

* Crash failures
* Network partitions
* CAP 定理

---

