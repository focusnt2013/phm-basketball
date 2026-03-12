package com.smartbasketball.app.util

import kotlin.math.sqrt
import kotlin.math.abs

class KdTreeNode(
    val userId: String,
    val feature: FloatArray,
    var left: KdTreeNode? = null,
    var right: KdTreeNode? = null,
    var dimension: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KdTreeNode) return false
        return userId == other.userId
    }

    override fun hashCode(): Int = userId.hashCode()
}

class KdTreeIndex(
    private val dimension: Int = 192
) {
    private var root: KdTreeNode? = null
    private val allNodes = mutableMapOf<String, KdTreeNode>()

    fun build(entries: Map<String, FloatArray>) {
        AppLogger.d("========== 开始构建KD-Tree索引 ==========")
        allNodes.clear()
        
        val nodes = entries.map { (userId, feature) ->
            KdTreeNode(userId, feature)
        }
        
        root = buildKdTree(nodes, 0)
        
        AppLogger.d("KD-Tree索引构建完成，共 ${allNodes.size} 个节点")
    }

    private fun buildKdTree(nodes: List<KdTreeNode>, depth: Int): KdTreeNode? {
        if (nodes.isEmpty()) return null
        
        val dim = depth % dimension
        
        val sorted = nodes.sortedBy { it.feature[dim] }
        val mid = sorted.size / 2
        
        val node = sorted[mid]
        node.dimension = dim
        
        allNodes[node.userId] = node
        
        node.left = buildKdTree(sorted.subList(0, mid), depth + 1)
        node.right = buildKdTree(sorted.subList(mid + 1, sorted.size), depth + 1)
        
        return node
    }

    fun search(query: FloatArray, k: Int = 50): List<Pair<String, Float>> {
        if (root == null) return emptyList()
        
        val startTime = System.currentTimeMillis()
        
        // 第一阶段：使用 KD-Tree 快速筛选出 k 个候选
        val candidates = mutableListOf<Pair<String, FloatArray>>()
        searchKnearest(root, query, k * 2, candidates)
        
        // 第二阶段：对候选使用真正的余弦相似度进行精确排序
        val results = candidates.mapNotNull { (userId, feature) ->
            val similarity = cosineSimilarity(query, feature)
            userId to similarity
        }.sortedByDescending { it.second }.take(k)
        
        val elapsed = System.currentTimeMillis() - startTime
        AppLogger.d("KDTree.search: 搜索完成，候选${candidates.size}个，返回 ${results.size} 个结果，耗时 ${elapsed}ms")
        
        return results
    }

    private fun searchKnearest(node: KdTreeNode?, query: FloatArray, k: Int, candidates: MutableList<Pair<String, FloatArray>>) {
        if (node == null) return
        
        val distance = euclideanDistance(query, node.feature)
        candidates.add(node.userId to node.feature)
        
        // 保持只保留最近的 k 个
        if (candidates.size > k) {
            candidates.sortBy { euclideanDistance(query, it.second) }
            while (candidates.size > k) {
                candidates.removeAt(candidates.size - 1)
            }
        }
        
        val dim = node.dimension
        val diff = query[dim] - node.feature[dim]
        
        val near = if (diff < 0) node.left else node.right
        val far = if (diff < 0) node.right else node.left
        
        searchKnearest(near, query, k, candidates)
        
        // 只有当有可能找到更近的点时才搜索另一侧
        if (candidates.size < k || abs(diff) < sqrt(candidates.maxOf { euclideanDistance(query, it.second) })) {
            searchKnearest(far, query, k, candidates)
        }
    }

    private fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        val sqrtNormA = sqrt(normA.toDouble())
        val sqrtNormB = sqrt(normB.toDouble())
        
        if (sqrtNormA == 0.0 || sqrtNormB == 0.0) return 0f
        
        val similarity = dotProduct / (sqrtNormA * sqrtNormB)
        // 转换到 0-1 范围
        return ((similarity + 1f) / 2f).toFloat().coerceIn(0f, 1f)
    }

    fun getSize(): Int = allNodes.size
}
