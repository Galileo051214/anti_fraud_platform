package com.anti.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐算法工具类
 * 包含TF-IDF、余弦相似度、皮尔逊相关系数等算法实现
 */
public class RecommendationAlgorithmUtil {

    /**
     * 95%置信度对应的Z值
     */
    private static final double Z = 1.645;

    /**
     * 计算威尔逊置信度得分
     * 公式: score = (p + z²/2n - z√(p(1-p)/n + z²/4n²)) / (1 + z²/n)
     * 用于根据点赞率和浏览量计算一个考虑样本量的置信度得分
     *
     * @param positive 点赞数
     * @param total    总浏览量
     * @return 威尔逊得分
     */
    public static BigDecimal calculateWilsonScore(int positive, int total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        double p = (double) positive / total;
        double z2 = Z * Z;
        double n = total;

        double denominator = 1 + z2 / n;
        double sqrtTerm = Math.sqrt((p * (1 - p) / n) + (z2 / (4 * n * n)));
        double score = (p + z2 / (2 * n) - Z * sqrtTerm) / denominator;

        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算余弦相似度
     * 公式: cos(A,B) = (A·B) / (||A|| * ||B||)
     *
     * @param vectorA 向量A
     * @param vectorB 向量B
     * @return 余弦相似度
     */
    public static BigDecimal cosineSimilarity(List<BigDecimal> vectorA, List<BigDecimal> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.isEmpty() || vectorB.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (vectorA.size() != vectorB.size()) {
            return BigDecimal.ZERO;
        }

        double dotProduct = 0;
        double normA = 0;
        double normB = 0;

        for (int i = 0; i < vectorA.size(); i++) {
            double a = vectorA.get(i).doubleValue();
            double b = vectorB.get(i).doubleValue();
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0 || normB == 0) {
            return BigDecimal.ZERO;
        }

        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        return BigDecimal.valueOf(similarity).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算余弦相似度(基于Map向量)
     *
     * @param vectorA 标签ID到得分的Map
     * @param vectorB 标签ID到得分的Map
     * @return 余弦相似度
     */
    public static BigDecimal cosineSimilarity(Map<Long, BigDecimal> vectorA, Map<Long, BigDecimal> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.isEmpty() || vectorB.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Set<Long> allTags = new HashSet<>(vectorA.keySet());
        allTags.addAll(vectorB.keySet());

        List<BigDecimal> listA = new ArrayList<>();
        List<BigDecimal> listB = new ArrayList<>();

        for (Long tagId : allTags) {
            listA.add(vectorA.getOrDefault(tagId, BigDecimal.ZERO));
            listB.add(vectorB.getOrDefault(tagId, BigDecimal.ZERO));
        }

        return cosineSimilarity(listA, listB);
    }

    /**
     * 计算皮尔逊相关系数
     * 公式: r = Σ((xi - x̄)(yi - ȳ)) / √(Σ(xi - x̄)² * Σ(yi - ȳ)²)
     *
     * @param vectorA 向量A
     * @param vectorB 向量B
     * @return 皮尔逊相关系数
     */
    public static BigDecimal pearsonCorrelation(List<BigDecimal> vectorA, List<BigDecimal> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.isEmpty() || vectorB.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (vectorA.size() != vectorB.size()) {
            return BigDecimal.ZERO;
        }

        int n = vectorA.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;

        for (int i = 0; i < n; i++) {
            double x = vectorA.get(i).doubleValue();
            double y = vectorB.get(i).doubleValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
            sumY2 += y * y;
        }

        double numerator = n * sumXY - sumX * sumY;
        double denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));

        if (denominator == 0) {
            return BigDecimal.ZERO;
        }

        double r = numerator / denominator;
        return BigDecimal.valueOf(r).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算皮尔逊相关系数(基于Map向量)
     *
     * @param vectorA 标签ID到得分的Map
     * @param vectorB 标签ID到得分的Map
     * @return 皮尔逊相关系数
     */
    public static BigDecimal pearsonCorrelation(Map<Long, BigDecimal> vectorA, Map<Long, BigDecimal> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.isEmpty() || vectorB.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Set<Long> allTags = new HashSet<>(vectorA.keySet());
        allTags.addAll(vectorB.keySet());

        List<BigDecimal> listA = new ArrayList<>();
        List<BigDecimal> listB = new ArrayList<>();

        for (Long tagId : allTags) {
            listA.add(vectorA.getOrDefault(tagId, BigDecimal.ZERO));
            listB.add(vectorB.getOrDefault(tagId, BigDecimal.ZERO));
        }

        return pearsonCorrelation(listA, listB);
    }

    /**
     * 从文本中提取关键词(TF-IDF简化版)
     *
     * @param text 输入文本
     * @param topK 返回前K个关键词
     * @return 关键词列表
     */
    public static List<String> extractKeywords(String text, int topK) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        String[] words = text.toLowerCase()
                .replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", " ")
                .split("\\s+");

        Map<String, Integer> wordCount = new HashMap<>();
        for (String word : words) {
            if (word.length() >= 2) {
                wordCount.merge(word, 1, Integer::sum);
            }
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(wordCount.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<String> keywords = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, sorted.size()); i++) {
            keywords.add(sorted.get(i).getKey());
        }
        return keywords;
    }

    /**
     * 计算TF-IDF得分(简化版)
     *
     * @param term      关键词
     * @param document  文档文本
     * @param allDocs   所有文档列表
     * @return TF-IDF得分
     */
    public static double calculateTfIdf(String term, String document, List<String> allDocs) {
        String[] words = document.toLowerCase().split("\\s+");

        int termCount = 0;
        for (String word : words) {
            if (word.equalsIgnoreCase(term)) {
                termCount++;
            }
        }

        double tf = (double) termCount / words.length;

        int docsWithTerm = 0;
        for (String doc : allDocs) {
            if (doc.toLowerCase().contains(term.toLowerCase())) {
                docsWithTerm++;
            }
        }

        double idf = Math.log((double) allDocs.size() / (docsWithTerm + 1));

        return tf * idf;
    }

    /**
     * 热度排行计算
     * 公式: hotScore = viewCount * 0.3 + likeCount * 0.5 + commentCount * 0.2
     *
     * @param viewCount    浏览量
     * @param likeCount    点赞数
     * @param commentCount 评论数
     * @return 热度得分
     */
    public static BigDecimal calculateHotScore(int viewCount, int likeCount, int commentCount) {
        double score = viewCount * 0.3 + likeCount * 0.5 + commentCount * 0.2;
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 简化序列加权算法(SPM - Simplified Pattern Mining)
     * 根据序列模式计算标签间的转移概率
     *
     * @param currentTag   当前标签
     * @param tagHistory   标签浏览历史
     * @param associationRules 关联规则库
     * @return 预测的下一标签列表
     */
    public static List<String> spmPredict(String currentTag, List<String> tagHistory,
                                          Map<String, List<String>> associationRules) {
        List<String> predictions = new ArrayList<>();

        List<String> rules = associationRules.getOrDefault(currentTag, Collections.emptyList());
        predictions.addAll(rules);

        if (predictions.isEmpty() && !tagHistory.isEmpty()) {
            Set<String> recentTags = new HashSet<>(tagHistory.subList(
                    Math.max(0, tagHistory.size() - 3), tagHistory.size()));
            for (String tag : recentTags) {
                List<String> alt = associationRules.getOrDefault(tag, Collections.emptyList());
                for (String p : alt) {
                    if (!predictions.contains(p)) {
                        predictions.add(p);
                    }
                }
            }
        }

        return predictions;
    }

    /**
     * 归一化得分到指定范围
     *
     * @param score    原始得分
     * @param minScore 最小值
     * @param maxScore 最大值
     * @return 归一化得分(0-100)
     */
    public static BigDecimal normalizeScore(BigDecimal score, BigDecimal minScore, BigDecimal maxScore) {
        if (maxScore.compareTo(minScore) == 0) {
            return BigDecimal.valueOf(50);
        }
        double normalized = (score.subtract(minScore).doubleValue() /
                (maxScore.subtract(minScore).doubleValue())) * 100;
        return BigDecimal.valueOf(normalized).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 综合得分计算
     * 公式: finalScore = w1 * contentScore + w2 * contextScore + w3 * hotScore
     *
     * @param contentScore  内容匹配得分
     * @param contextScore  上下文相关得分
     * @param hotScore      热度得分
     * @param w1  内容权重
     * @param w2  上下文权重
     * @param w3  热度权重
     * @return 综合得分
     */
    public static BigDecimal calculateCompositeScore(BigDecimal contentScore,
                                                     BigDecimal contextScore,
                                                     BigDecimal hotScore,
                                                     double w1, double w2, double w3) {
        double score = contentScore.doubleValue() * w1 +
                contextScore.doubleValue() * w2 +
                hotScore.doubleValue() * w3;
        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 根据权重合并多个得分
     *
     * @param scores  得分列表
     * @param weights 权重列表
     * @return 加权平均得分
     */
    public static BigDecimal weightedAverage(List<BigDecimal> scores, List<Double> weights) {
        if (scores.isEmpty() || weights.isEmpty() || scores.size() != weights.size()) {
            return BigDecimal.ZERO;
        }

        double sumScore = 0;
        double sumWeight = 0;

        for (int i = 0; i < scores.size(); i++) {
            sumScore += scores.get(i).doubleValue() * weights.get(i);
            sumWeight += weights.get(i);
        }

        if (sumWeight == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(sumScore / sumWeight).setScale(4, RoundingMode.HALF_UP);
    }
}
