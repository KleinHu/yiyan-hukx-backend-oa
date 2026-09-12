package com.cac.oa.service.planexport;

import com.cac.oa.vo.planexport.PlanPptRowVO;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 导出统计口径：按状态关键字计数、逾期判定（截止日早于今天且未完成/未取消）、平均进度。
 */
final class PptStats {

    /** 状态分布（保持出现顺序） */
    final Map<String, Integer> statusCount = new LinkedHashMap<String, Integer>();
    /** 分类分布 */
    final Map<String, Integer> categoryCount = new LinkedHashMap<String, Integer>();
    /** 分类 -> 进度合计（算平均用） */
    private final Map<String, int[]> categoryProgressSum = new LinkedHashMap<String, int[]>();
    int total;
    int doneCount;
    int inProgressCount;
    int overdueCount;
    double avgProgress;
    /** 重点关注：逾期或 进度<60 且未终结的计划，按进度升序，最多 10 条 */
    final List<PlanPptRowVO> focusRows = new ArrayList<PlanPptRowVO>();

    private final List<PlanPptRowVO> plans;
    private final Date today;

    PptStats(List<PlanPptRowVO> plans) {
        this.plans = plans;
        this.today = new Date();
        compute();
    }

    private void compute() {
        long progressSum = 0;
        Date now = today;
        for (PlanPptRowVO p : plans) {
            total++;
            String status = PptKit.safe(p.getStatus());
            String bucket = statusBucket(status);
            Integer c = statusCount.get(bucket);
            statusCount.put(bucket, c == null ? 1 : c + 1);

            String cat = p.getCategory() == null || p.getCategory().trim().isEmpty() ? "未分类" : p.getCategory();
            Integer cc = categoryCount.get(cat);
            categoryCount.put(cat, cc == null ? 1 : cc + 1);
            int[] sum = categoryProgressSum.get(cat);
            if (sum == null) {
                sum = new int[2];
                categoryProgressSum.put(cat, sum);
            }
            sum[0] += p.getProgress() == null ? 0 : p.getProgress();
            sum[1] += 1;

            progressSum += p.getProgress() == null ? 0 : p.getProgress();
            if (status.contains("完成")) {
                doneCount++;
            } else if (status.contains("执行中") || status.contains("进行中")) {
                inProgressCount++;
            }

            boolean finished = status.contains("完成") || status.contains("取消");
            boolean overdue = false;
            Date end = parseDate(p.getEndDate());
            if (end != null && end.before(now) && !finished) {
                overdue = true;
                overdueCount++;
            }
            if ((overdue || (status.contains("执行中") || status.contains("进行中"))
                    && (p.getProgress() == null || p.getProgress() < 60)) && !finished) {
                focusRows.add(p);
            }
        }
        avgProgress = total == 0 ? 0 : (double) progressSum / total;
        java.util.Collections.sort(focusRows, new java.util.Comparator<PlanPptRowVO>() {
            public int compare(PlanPptRowVO a, PlanPptRowVO b) {
                int pa = a.getProgress() == null ? 0 : a.getProgress();
                int pb = b.getProgress() == null ? 0 : b.getProgress();
                return Integer.compare(pa, pb);
            }
        });
        if (focusRows.size() > 10) {
            focusRows.subList(10, focusRows.size()).clear();
        }
    }

    private String statusBucket(String status) {
        for (String key : PptKit.STATUS_COLORS.keySet()) {
            if (status.contains(key)) {
                return key;
            }
        }
        return status.isEmpty() ? "未知" : status;
    }

    /** 分类平均进度 */
    double categoryAvgProgress(String cat) {
        int[] sum = categoryProgressSum.get(cat);
        return sum == null || sum[1] == 0 ? 0 : (double) sum[0] / sum[1];
    }

    /** 供模板 {{tag}} 使用的全局上下文 */
    Map<String, String> globalTags(String title, String subtitle, String orgName) {
        Map<String, String> tags = new LinkedHashMap<String, String>();
        tags.put("title", PptKit.safe(title));
        tags.put("subtitle", PptKit.safe(subtitle));
        tags.put("orgName", PptKit.safe(orgName));
        tags.put("date", new SimpleDateFormat("yyyy-MM-dd").format(today));
        tags.put("planCount", String.valueOf(total));
        tags.put("doneCount", String.valueOf(doneCount));
        tags.put("inProgressCount", String.valueOf(inProgressCount));
        tags.put("overdueCount", String.valueOf(overdueCount));
        tags.put("avgProgress", fmt1(avgProgress) + "%");
        return tags;
    }

    static String fmt1(double v) {
        return String.format("%.1f", v);
    }

    static Date parseDate(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        String v = s.trim();
        String[] patterns = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd"};
        for (String pat : patterns) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(pat);
                f.setLenient(false);
                return f.parse(v.length() > 10 ? v.substring(0, 10) : v);
            } catch (Exception ignore) {
                // try next
            }
        }
        return null;
    }
}
