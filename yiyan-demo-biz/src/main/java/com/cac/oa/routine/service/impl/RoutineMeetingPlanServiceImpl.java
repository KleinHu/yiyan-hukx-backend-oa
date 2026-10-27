package com.cac.oa.routine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cac.oa.routine.dao.RoutineMeetingPlanMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cac.oa.routine.dao.RoutineMeetingPlanWeekMapper;
import com.cac.oa.routine.entity.RoutineMeetingPlanEntity;
import com.cac.oa.routine.entity.RoutineMeetingPlanWeekEntity;
import com.cac.oa.routine.service.IRoutineMeetingPlanService;
import com.cac.oa.routine.vo.RoutineMeetingPlanCompletedDateRequest;
import com.cac.oa.routine.vo.RoutineMeetingPlanExtendRequest;
import com.cac.oa.routine.vo.RoutineMeetingPlanFeedbackDateRequest;
import com.cac.oa.routine.vo.RoutineMeetingPlanFeedbackRequest;
import com.cac.oa.routine.vo.RoutineMeetingPlanImportResultVO;
import com.cac.oa.routine.vo.RoutineMeetingPlanQuery;
import com.cac.oa.routine.vo.RoutineMeetingPlanSaveRequest;
import com.cac.oa.routine.vo.RoutineMeetingPlanStatisticsVO;
import com.cac.oa.routine.vo.RoutineMeetingPlanVO;
import com.cac.oa.routine.vo.RoutineMeetingPlanWeekVO;
import com.cac.yiyan.common.exception.service.ServiceException;
import com.cac.yiyan.common.page.PageData;
import com.cac.yiyan.security.core.user.UserDetailsImpl;
import com.cac.yiyan.security.core.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoutineMeetingPlanServiceImpl
        extends ServiceImpl<RoutineMeetingPlanMapper, RoutineMeetingPlanEntity>
        implements IRoutineMeetingPlanService {

    private static final String DRAFT = "DRAFT";
    private static final String NOT_STARTED = "NOT_STARTED";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String COMPLETED_ON_TIME = "COMPLETED_ON_TIME";
    private static final String COMPLETED_OVERDUE = "COMPLETED_OVERDUE";
    private static final Set<String> ADMIN_ROLE_IDS = new HashSet<>(Arrays.asList("2", "4"));
    private static final String SUPER_ADMIN_ROLE = "240-oa-routine-super-admin";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Map<String, SuperAdminCache> SUPER_ADMIN_CACHE = new ConcurrentHashMap<>();

    private final RoutineMeetingPlanWeekMapper weekMapper;

    @Value("${yiyan.security.token-info-uri:}")
    private String tokenInfoUri;

    @Value("${hr.employee-url:http://127.0.0.1:18085}")
    private String hrEmployeeUrl;

    @Override
    public PageData<RoutineMeetingPlanVO> page(RoutineMeetingPlanQuery query) {
        LambdaQueryWrapper<RoutineMeetingPlanEntity> wrapper = buildQueryWrapper(query);
        applyVisibility(wrapper);
        return queryPage(query, wrapper, false);
    }

    @Override
    public PageData<RoutineMeetingPlanVO> myPlans(RoutineMeetingPlanQuery query) {
        LambdaQueryWrapper<RoutineMeetingPlanEntity> wrapper = buildQueryWrapper(query);
        applyLeaderFilter(wrapper);
        return queryPage(query, wrapper, false);
    }

    @Override
    public PageData<RoutineMeetingPlanVO> myFeedback(RoutineMeetingPlanQuery query) {
        LambdaQueryWrapper<RoutineMeetingPlanEntity> wrapper = buildQueryWrapper(query);
        applyLeaderFilter(wrapper);
        wrapper.eq(RoutineMeetingPlanEntity::getDraft, 0);
        return queryPage(query, wrapper, true);
    }

    @Override
    public RoutineMeetingPlanStatisticsVO statistics() {
        LocalDate today = today();
        List<RoutineMeetingPlanEntity> plans = baseMapper.selectList(
                new LambdaQueryWrapper<RoutineMeetingPlanEntity>()
                        .select(RoutineMeetingPlanEntity::getDraft,
                                RoutineMeetingPlanEntity::getCompletedAt,
                                RoutineMeetingPlanEntity::getStartDate,
                                RoutineMeetingPlanEntity::getEndDate));
        long notStarted = 0L;
        long active = 0L;
        long completed = 0L;
        Set<Integer> yearSet = new HashSet<>();
        for (RoutineMeetingPlanEntity plan : plans) {
            if (plan.getStartDate() != null) {
                int startYear = plan.getStartDate().getYear();
                int endYear = plan.getEndDate() == null
                        ? startYear : plan.getEndDate().getYear();
                if (endYear < startYear) {
                    endYear = startYear;
                }
                for (int year = startYear; year <= endYear; year++) {
                    yearSet.add(year);
                }
            }
            if (Integer.valueOf(1).equals(plan.getDraft())) {
                continue;
            }
            if (plan.getCompletedAt() != null) {
                completed++;
            } else if (plan.getStartDate() != null && today.isBefore(plan.getStartDate())) {
                notStarted++;
            } else {
                active++;
            }
        }
        List<Integer> years = new ArrayList<>(yearSet);
        years.sort(Collections.reverseOrder());

        RoutineMeetingPlanStatisticsVO result = new RoutineMeetingPlanStatisticsVO();
        result.setNotStarted(notStarted);
        result.setActive(active);
        result.setCompleted(completed);
        result.setTotal(notStarted + active + completed);
        result.setYears(years);
        return result;
    }

    @Override
    public byte[] buildImportTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("计划导入");
            String[] headers = {
                    "计划名称", "计划目标", "责任领导工号",
                    "责任领导姓名", "开始日期", "结束日期"
            };
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            sheet.setColumnWidth(0, 22 * 256);
            sheet.setColumnWidth(1, 48 * 256);
            sheet.setColumnWidth(2, 18 * 256);
            sheet.setColumnWidth(3, 18 * 256);
            sheet.setColumnWidth(4, 14 * 256);
            sheet.setColumnWidth(5, 14 * 256);
            sheet.createFreezePane(0, 1);
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new ServiceException("导入模板生成失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoutineMeetingPlanImportResultVO importPlans(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择要导入的 Excel 文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new ServiceException("只支持 .xlsx 格式的 Excel 文件");
        }

        List<ImportRow> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int dataCount = 0;
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new ServiceException("Excel 中没有可读取的工作表");
            }
            String[] expected = {
                    "计划名称", "计划目标", "责任领导工号",
                    "责任领导姓名", "开始日期", "结束日期"
            };
            Row header = sheet.getRow(0);
            for (int i = 0; i < expected.length; i++) {
                if (header == null || !expected[i].equals(readCellText(header.getCell(i)))) {
                    throw new ServiceException("导入模板表头不正确，请下载最新模板");
                }
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankImportRow(row)) {
                    continue;
                }
                dataCount++;
                if (dataCount > 500) {
                    throw new ServiceException("单次最多导入 500 行计划");
                }
                int rowNum = rowIndex + 1;
                String name = readCellText(row.getCell(0));
                String goal = readCellText(row.getCell(1));
                String leaderId = readCellText(row.getCell(2));
                String leaderName = readCellText(row.getCell(3));
                if (!StringUtils.hasText(name)) {
                    errors.add("第" + rowNum + "行：计划名称不能为空");
                } else if (name.length() > 60) {
                    errors.add("第" + rowNum + "行：计划名称不能超过60字");
                }
                if (!StringUtils.hasText(goal)) {
                    errors.add("第" + rowNum + "行：计划目标不能为空");
                }
                if (!StringUtils.hasText(leaderId) && !StringUtils.hasText(leaderName)) {
                    errors.add("第" + rowNum + "行：责任领导工号或姓名至少填写一个");
                } else if (!StringUtils.hasText(leaderId)) {
                    String[] leader = lookupHrEmployee(null, leaderName);
                    if (leader == null) {
                        errors.add("第" + rowNum + "行：未找到唯一匹配的责任领导，请填写工号");
                    } else {
                        leaderId = leader[0];
                        leaderName = leader[1];
                    }
                } else if (!StringUtils.hasText(leaderName)) {
                    leaderName = leaderId;
                }
                LocalDate startDate = readImportDate(row.getCell(4), rowNum, "开始日期", errors);
                LocalDate endDate = readImportDate(row.getCell(5), rowNum, "结束日期", errors);
                if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
                    errors.add("第" + rowNum + "行：结束日期不能早于开始日期");
                }
                if (StringUtils.hasText(name) && StringUtils.hasText(goal)
                        && startDate != null && endDate != null
                        && (StringUtils.hasText(leaderId) || StringUtils.hasText(leaderName))) {
                    ImportRow importRow = new ImportRow();
                    importRow.rowNum = rowNum;
                    importRow.name = name;
                    importRow.goal = goal;
                    importRow.leaderId = leaderId;
                    importRow.leaderName = leaderName;
                    importRow.startDate = startDate;
                    importRow.endDate = endDate;
                    rows.add(importRow);
                }
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Excel 解析失败，请检查文件格式");
        }

        if (!errors.isEmpty()) {
            throw new ServiceException(String.join("；", errors));
        }
        if (rows.isEmpty()) {
            throw new ServiceException("Excel 中没有可导入的数据");
        }

        for (ImportRow row : rows) {
            RoutineMeetingPlanSaveRequest request = new RoutineMeetingPlanSaveRequest();
            request.setName(row.name);
            request.setGoal(row.goal);
            request.setLeaderId(row.leaderId);
            request.setLeaderName(row.leaderName);
            request.setStartDate(row.startDate);
            request.setEndDate(row.endDate);
            request.setDraft(false);
            try {
                savePlan(request);
            } catch (ServiceException e) {
                throw new ServiceException("第" + row.rowNum + "行：" + e.getMessage());
            } catch (Exception e) {
                throw new ServiceException("第" + row.rowNum + "行：保存失败");
            }
        }

        RoutineMeetingPlanImportResultVO result = new RoutineMeetingPlanImportResultVO();
        result.setSuccessCount(rows.size());
        return result;
    }

    private String readCellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        return new DataFormatter().formatCellValue(cell).trim();
    }

    private boolean isBlankImportRow(Row row) {
        for (int i = 0; i < 6; i++) {
            if (StringUtils.hasText(readCellText(row.getCell(i)))) {
                return false;
            }
        }
        return true;
    }

    private LocalDate readImportDate(Cell cell, int rowNum, String label,
                                    List<String> errors) {
        if (cell == null) {
            errors.add("第" + rowNum + "行：" + label + "不能为空");
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate();
                }
                if (DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
                    return DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalDate();
                }
            }
            String value = cell.getCellType() == CellType.STRING
                    ? cell.getStringCellValue().trim() : readCellText(cell);
            if (value.length() > 10) {
                value = value.substring(0, 10);
            }
            value = value.replace('/', '-').replace('.', '-')
                    .replace("年", "-").replace("月", "-").replace("日", "").trim();
            if (!StringUtils.hasText(value)) {
                errors.add("第" + rowNum + "行：" + label + "不能为空");
                return null;
            }
            try {
                return LocalDate.parse(value);
            } catch (Exception ignored) {
                return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-M-d"));
            }
        } catch (Exception e) {
            errors.add("第" + rowNum + "行：" + label + "格式应为 YYYY-MM-DD");
            return null;
        }
    }

    private String[] lookupHrEmployee(String userCode, String userName) {
        if (!StringUtils.hasText(hrEmployeeUrl)) {
            return null;
        }
        String token = SecurityUtils.obtainBearerToken();
        if (!StringUtils.hasText(token)) {
            return null;
        }
        HttpURLConnection connection = null;
        try {
            StringBuilder url = new StringBuilder(hrEmployeeUrl)
                    .append("/api/240/hr/employees?pageNum=1&pageSize=50");
            if (StringUtils.hasText(userCode)) {
                url.append("&userCode=")
                        .append(URLEncoder.encode(userCode, StandardCharsets.UTF_8.name()));
            }
            if (StringUtils.hasText(userName)) {
                url.append("&userName=")
                        .append(URLEncoder.encode(userName, StandardCharsets.UTF_8.name()));
            }
            connection = (HttpURLConnection) new URL(url.toString()).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestProperty("Authorization", "Bearer " + token);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            try (InputStream inputStream = connection.getInputStream()) {
                JsonNode list = OBJECT_MAPPER.readTree(inputStream).path("data").path("list");
                if (!list.isArray()) {
                    return null;
                }
                String foundCode = null;
                String foundName = null;
                int count = 0;
                for (JsonNode item : list) {
                    String code = item.path("userCode").asText("");
                    String name = item.path("userName").asText("");
                    boolean match = StringUtils.hasText(userCode)
                            ? userCode.equals(code)
                            : userName.equals(name);
                    if (match) {
                        count++;
                        foundCode = code;
                        foundName = name;
                    }
                }
                if (count != 1 || !StringUtils.hasText(foundCode)) {
                    return null;
                }
                return new String[]{foundCode, foundName};
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static class ImportRow {
        private int rowNum;
        private String name;
        private String goal;
        private String leaderId;
        private String leaderName;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Override
    public RoutineMeetingPlanVO detail(Long id) {
        RoutineMeetingPlanEntity plan = getPlan(id);
        assertVisible(plan);
        return buildVOs(Collections.singletonList(plan)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePlan(RoutineMeetingPlanSaveRequest request) {
        boolean draft = Boolean.TRUE.equals(request.getDraft());
        validatePlan(request, !draft);

        RoutineMeetingPlanEntity plan = new RoutineMeetingPlanEntity();
        copyPlanFields(plan, request);
        plan.setCreatorId(currentUserId());
        plan.setCreatorName(currentUserName());
        plan.setProgress(0);
        plan.setDraft(draft ? 1 : 0);
        if (!draft) {
            plan.setIssuedAt(today());
            plan.setOriginalEndDate(request.getEndDate());
        }
        baseMapper.insert(plan);

        if (!draft) {
            generateWeeks(plan);
        }
        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlan(Long id, RoutineMeetingPlanSaveRequest request) {
        RoutineMeetingPlanEntity plan = getPlan(id);
        if (plan.getCompletedAt() != null) {
            throw new ServiceException("计划完成后不能修改计划信息");
        }

        boolean wasDraft = Integer.valueOf(1).equals(plan.getDraft());
        boolean targetDraft = Boolean.TRUE.equals(request.getDraft());
        if (wasDraft) {
            if (!isCreator(plan) && !isLeader(plan) && !isAdmin()) {
                throw new ServiceException("无权修改该草稿");
            }
            validatePlan(request, !targetDraft);
        } else {
            if (!isLeader(plan) && !isAdmin()) {
                throw new ServiceException("只有责任领导或管理员可以修改已发布计划");
            }
            if (targetDraft) {
                throw new ServiceException("已发布计划不能改回草稿");
            }
            validatePlan(request, true);
        }

        String oldLeaderId = plan.getLeaderId();
        String oldLeaderName = plan.getLeaderName();
        LocalDate oldEnd = plan.getEndDate();
        boolean wasExtended = plan.getOriginalEndDate() != null && oldEnd != null
                && !plan.getOriginalEndDate().equals(oldEnd);

        copyPlanFields(plan, request);
        plan.setDraft(targetDraft ? 1 : 0);

        boolean publishNow = wasDraft && !targetDraft;
        if (publishNow) {
            plan.setIssuedAt(today());
            plan.setOriginalEndDate(request.getEndDate());
            plan.setProgress(0);
        } else if (!wasDraft && !wasExtended) {
            plan.setOriginalEndDate(request.getEndDate());
        }

        boolean leaderChanged = !equalsString(oldLeaderId, plan.getLeaderId())
                || !equalsString(oldLeaderName, plan.getLeaderName());

        baseMapper.updateById(plan);

        if (publishNow) {
            generateWeeks(plan);
            recalcPlan(plan);
        } else if (!wasDraft) {
            rebuildWeeks(plan);
            if (leaderChanged) {
                updatePendingWeekLeader(plan);
            }
            recalcPlan(plan);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(Long id) {
        RoutineMeetingPlanEntity plan = getPlan(id);
        String status = statusOf(plan);
        boolean allowed = isAdmin() || isLeader(plan);
        if (Integer.valueOf(1).equals(plan.getDraft())) {
            allowed = allowed || isCreator(plan);
        } else if (!NOT_STARTED.equals(status)) {
            throw new ServiceException("只有草稿或未开始计划可以删除");
        }
        if (!allowed) {
            throw new ServiceException("无权删除该计划");
        }
        weekMapper.delete(new LambdaQueryWrapper<RoutineMeetingPlanWeekEntity>()
                .eq(RoutineMeetingPlanWeekEntity::getPlanId, id));
        baseMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void extend(Long id, RoutineMeetingPlanExtendRequest request) {
        RoutineMeetingPlanEntity plan = getPlan(id);
        if (!isLeader(plan) && !isAdmin()) {
            throw new ServiceException("只有责任领导或管理员可以延期");
        }
        if (Integer.valueOf(1).equals(plan.getDraft()) || plan.getCompletedAt() != null) {
            throw new ServiceException("只有执行中的计划可以延期");
        }
        if (plan.getStartDate() != null && today().isBefore(plan.getStartDate())) {
            throw new ServiceException("计划尚未开始，不能延期");
        }
        if (request.getEndDate() == null || !request.getEndDate().isAfter(plan.getEndDate())) {
            throw new ServiceException("延期后的结束日期必须晚于当前结束日期");
        }
        if (plan.getStartDate() != null && request.getEndDate().isBefore(plan.getStartDate())) {
            throw new ServiceException("结束日期不能早于开始日期");
        }
        if (plan.getOriginalEndDate() == null) {
            plan.setOriginalEndDate(plan.getEndDate());
        }
        plan.setEndDate(request.getEndDate());
        baseMapper.updateById(plan);
        rebuildWeeks(plan);
        recalcPlan(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void feedback(Long id, RoutineMeetingPlanFeedbackRequest request) {
        RoutineMeetingPlanEntity plan = getPlan(id);
        if (Integer.valueOf(1).equals(plan.getDraft())) {
            throw new ServiceException("草稿不能填写反馈");
        }
        if (!isLeader(plan)) {
            throw new ServiceException("只有责任领导可以填写或修改周反馈");
        }
        if (plan.getCompletedAt() != null) {
            throw new ServiceException("计划已完成，不能修改反馈");
        }
        if (plan.getStartDate() != null && today().isBefore(plan.getStartDate())) {
            throw new ServiceException("计划尚未开始，不能填写反馈");
        }

        LocalDate weekStart = normalizeMonday(request.getWeekStart());
        if (weekStart.isAfter(currentMonday())) {
            throw new ServiceException("该周尚未到期，不能填写反馈");
        }

        RoutineMeetingPlanWeekEntity week = weekMapper.selectOne(
                new LambdaQueryWrapper<RoutineMeetingPlanWeekEntity>()
                        .eq(RoutineMeetingPlanWeekEntity::getPlanId, id)
                        .eq(RoutineMeetingPlanWeekEntity::getWeekStart, weekStart));
        if (week == null) {
            throw new ServiceException("该周不在计划周期内");
        }
        if (request.getProgress() != null && request.getProgress() == 100) {
            assertNoEarlierMissingWeeks(id, weekStart);
        }

        if (week.getFirstFeedbackAt() == null) {
            week.setFirstFeedbackAt(new Date());
            week.setLeaderId(currentUserId());
            week.setLeaderName(currentUserName());
        }
        week.setProgress(request.getProgress());
        week.setContent(request.getContent());
        weekMapper.updateById(week);

        recalcPlan(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void correctFeedbackDate(Long id, RoutineMeetingPlanFeedbackDateRequest request) {
        ensureSuperAdmin();
        RoutineMeetingPlanEntity plan = getPlan(id);
        LocalDate weekStart = normalizeMonday(request.getWeekStart());
        RoutineMeetingPlanWeekEntity week = weekMapper.selectOne(
                new LambdaQueryWrapper<RoutineMeetingPlanWeekEntity>()
                        .eq(RoutineMeetingPlanWeekEntity::getPlanId, id)
                        .eq(RoutineMeetingPlanWeekEntity::getWeekStart, weekStart));
        if (week == null || week.getFirstFeedbackAt() == null) {
            throw new ServiceException("该周还没有反馈记录，不能校正");
        }
        LocalDate actualDate = request.getActualDate();
        if (actualDate.isBefore(week.getWeekStart())) {
            throw new ServiceException("实际反馈日期不能早于该周开始日期");
        }
        if (actualDate.isAfter(today())) {
            throw new ServiceException("实际反馈日期不能晚于今天");
        }
        if (week.getProgress() != null && week.getProgress() == 100) {
            assertNoEarlierMissingWeeks(id, weekStart);
        }
        Date newFeedbackAt = combineDate(week.getFirstFeedbackAt(), actualDate);
        week.setFirstFeedbackAt(newFeedbackAt);
        weekMapper.updateById(week);

        if (plan.getCompletedAt() != null
                && week.getProgress() != null
                && week.getProgress() == 100
                && isLatestFeedbackWeek(id, weekStart)) {
            plan.setCompletedAt(combineDate(plan.getCompletedAt(), actualDate));
            baseMapper.updateById(plan);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void correctCompletedDate(Long id, RoutineMeetingPlanCompletedDateRequest request) {
        ensureSuperAdmin();
        RoutineMeetingPlanEntity plan = getPlan(id);
        if (plan.getCompletedAt() == null) {
            throw new ServiceException("只有已完成计划可以校正完成日期");
        }
        LocalDate completedDate = request.getCompletedDate();
        if (completedDate.isAfter(today())) {
            throw new ServiceException("完成日期不能晚于今天");
        }
        Date lastFeedback = latestFeedbackDate(id);
        if (lastFeedback == null) {
            throw new ServiceException("计划没有反馈记录，不能校正完成日期");
        }
        if (completedDate.isBefore(toLocalDate(lastFeedback))) {
            throw new ServiceException("完成日期不能早于最后反馈日期");
        }
        plan.setCompletedAt(combineDate(plan.getCompletedAt(), completedDate));
        baseMapper.updateById(plan);
    }

    private LambdaQueryWrapper<RoutineMeetingPlanEntity> buildQueryWrapper(RoutineMeetingPlanQuery query) {
        LambdaQueryWrapper<RoutineMeetingPlanEntity> wrapper = new LambdaQueryWrapper<>();
        if (query == null) {
            return wrapper;
        }
        wrapper.like(StringUtils.hasText(query.getName()), RoutineMeetingPlanEntity::getName, query.getName());
        if (query.getStartDate() != null) {
            wrapper.ge(RoutineMeetingPlanEntity::getEndDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(RoutineMeetingPlanEntity::getStartDate, query.getEndDate());
        }
        if (query.getYear() != null) {
            LocalDate yearStart = LocalDate.of(query.getYear(), 1, 1);
            LocalDate yearEnd = LocalDate.of(query.getYear(), 12, 31);
            wrapper.le(RoutineMeetingPlanEntity::getStartDate, yearEnd)
                    .ge(RoutineMeetingPlanEntity::getEndDate, yearStart);
        }
        wrapper.orderByDesc(RoutineMeetingPlanEntity::getStartDate)
                .orderByDesc(RoutineMeetingPlanEntity::getCreateTime);
        return wrapper;
    }

    private void applyVisibility(LambdaQueryWrapper<RoutineMeetingPlanEntity> wrapper) {
    }

    private void applyLeaderFilter(LambdaQueryWrapper<RoutineMeetingPlanEntity> wrapper) {
        String userId = currentUserId();
        String userCode = currentUserCode();
        wrapper.and(w -> {
            w.eq(RoutineMeetingPlanEntity::getLeaderId, userId);
            if (StringUtils.hasText(userCode) && !userCode.equals(userId)) {
                w.or().eq(RoutineMeetingPlanEntity::getLeaderId, userCode);
            }
        });
    }

    private PageData<RoutineMeetingPlanVO> queryPage(RoutineMeetingPlanQuery query,
                                                     LambdaQueryWrapper<RoutineMeetingPlanEntity> wrapper,
                                                     boolean pendingOnly) {
        int pageNo = query != null && query.getPageNo() != null && query.getPageNo() > 0
                ? query.getPageNo() : 1;
        int pageSize = query != null && query.getPageSize() != null && query.getPageSize() > 0
                ? query.getPageSize() : 10;
        String status = query != null && StringUtils.hasText(query.getStatus())
                ? normalizeStatus(query.getStatus()) : null;

        if (status == null && !pendingOnly && pageSize <= 100) {
            Page<RoutineMeetingPlanEntity> resultPage = baseMapper.selectPage(
                    new Page<>(pageNo, pageSize), wrapper);
            return new PageData<>(buildVOs(resultPage.getRecords()), resultPage.getTotal());
        }

        List<RoutineMeetingPlanEntity> plans = baseMapper.selectList(wrapper);
        if (status != null) {
            plans = plans.stream()
                    .filter(plan -> status.equals(statusOf(plan)))
                    .collect(Collectors.toList());
        }

        if (pendingOnly) {
            List<RoutineMeetingPlanVO> rows = buildVOs(plans);
            rows = rows.stream()
                    .filter(v -> v.getPendingWeekCount() != null && v.getPendingWeekCount() > 0)
                    .collect(Collectors.toList());
            long total = rows.size();
            int from = Math.min((pageNo - 1) * pageSize, rows.size());
            int to = Math.min(from + pageSize, rows.size());
            return new PageData<>(new ArrayList<>(rows.subList(from, to)), total);
        }

        long total = plans.size();
        int from = Math.min((pageNo - 1) * pageSize, plans.size());
        int to = Math.min(from + pageSize, plans.size());
        return new PageData<>(buildVOs(new ArrayList<>(plans.subList(from, to))), total);
    }

    private List<RoutineMeetingPlanVO> buildVOs(List<RoutineMeetingPlanEntity> plans) {
        if (plans == null || plans.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = plans.stream().map(RoutineMeetingPlanEntity::getId).collect(Collectors.toList());
        List<RoutineMeetingPlanWeekEntity> allWeeks = weekMapper.selectList(
                new LambdaQueryWrapper<RoutineMeetingPlanWeekEntity>()
                        .in(RoutineMeetingPlanWeekEntity::getPlanId, ids)
                        .orderByAsc(RoutineMeetingPlanWeekEntity::getWeekStart));
        Map<Long, List<RoutineMeetingPlanWeekEntity>> weekMap = allWeeks.stream()
                .collect(Collectors.groupingBy(RoutineMeetingPlanWeekEntity::getPlanId));

        List<RoutineMeetingPlanVO> result = new ArrayList<>();
        for (RoutineMeetingPlanEntity plan : plans) {
            result.add(toVO(plan, weekMap.getOrDefault(plan.getId(), Collections.emptyList())));
        }
        return result;
    }

    private RoutineMeetingPlanVO toVO(RoutineMeetingPlanEntity plan,
                                      List<RoutineMeetingPlanWeekEntity> weeks) {
        RoutineMeetingPlanVO vo = new RoutineMeetingPlanVO();
        vo.setId(plan.getId());
        vo.setName(StringUtils.hasText(plan.getName()) ? plan.getName() : "未命名草稿");
        vo.setGoal(plan.getGoal());
        vo.setLeaderId(plan.getLeaderId());
        vo.setLeaderName(plan.getLeaderName());
        vo.setCreatorId(plan.getCreatorId());
        vo.setCreatorName(plan.getCreatorName());
        vo.setStartDate(plan.getStartDate());
        vo.setEndDate(plan.getEndDate());
        vo.setOriginalEndDate(plan.getOriginalEndDate());
        vo.setIssuedAt(plan.getIssuedAt());
        vo.setProgress(plan.getProgress() == null ? 0 : plan.getProgress());
        vo.setCompletedAt(plan.getCompletedAt());
        vo.setDraft(Integer.valueOf(1).equals(plan.getDraft()));
        String status = statusOf(plan);
        vo.setStatus(status);
        vo.setStatusName(statusName(status));
        vo.setDurationDays(durationDays(plan));
        vo.setRemainDays(remainDays(plan));
        vo.setOverdueDays(overdueDays(plan, status));
        vo.setCurrentWeekStart(currentMonday());

        int pendingCount = 0;
        List<RoutineMeetingPlanWeekVO> weekVOs = new ArrayList<>();
        boolean feedbackOpen = isLeader(plan)
                && !Integer.valueOf(1).equals(plan.getDraft())
                && plan.getCompletedAt() == null
                && (plan.getStartDate() == null || !today().isBefore(plan.getStartDate()));
        for (RoutineMeetingPlanWeekEntity week : weeks) {
            RoutineMeetingPlanWeekVO wv = toWeekVO(plan, week, feedbackOpen);
            weekVOs.add(wv);
            if (week.getFirstFeedbackAt() == null && Boolean.TRUE.equals(wv.getCanFeedback())) {
                pendingCount++;
            }
        }
        vo.setWeeks(weekVOs);
        vo.setPendingWeekCount(pendingCount);
        vo.setCanEdit(canEdit(plan));
        vo.setCanDelete(canDelete(plan, status));
        vo.setCanExtend(canExtend(plan, status));
        vo.setCanFeedback(feedbackOpen);
        return vo;
    }

    private RoutineMeetingPlanWeekVO toWeekVO(RoutineMeetingPlanEntity plan,
                                               RoutineMeetingPlanWeekEntity week,
                                               boolean feedbackOpen) {
        RoutineMeetingPlanWeekVO vo = new RoutineMeetingPlanWeekVO();
        vo.setId(week.getId());
        vo.setPlanId(plan.getId());
        vo.setWeekStart(week.getWeekStart());
        vo.setWeekEnd(week.getWeekEnd());
        vo.setWeekLabel(formatWeek(week.getWeekStart(), week.getWeekEnd()));
        vo.setLeaderId(week.getLeaderId());
        vo.setLeaderName(week.getLeaderName());
        vo.setProgress(week.getProgress() == null ? 0 : week.getProgress());
        vo.setContent(week.getContent());
        vo.setFirstFeedbackAt(week.getFirstFeedbackAt());

        String weekStatus;
        if (week.getFirstFeedbackAt() != null) {
            LocalDate feedbackDate = toLocalDate(week.getFirstFeedbackAt());
            weekStatus = feedbackDate.isAfter(week.getWeekEnd()) ? "OVERDUE" : "ON_TIME";
        } else if (week.getWeekStart().isAfter(currentMonday())) {
            weekStatus = "NOT_DUE";
        } else {
            weekStatus = "NOT_FEEDBACK";
        }
        vo.setStatus(weekStatus);
        vo.setStatusName(weekStatusName(weekStatus));

        boolean canFeedback = feedbackOpen
                && !week.getWeekStart().isAfter(currentMonday());
        vo.setCanFeedback(canFeedback);
        return vo;
    }

    private void generateWeeks(RoutineMeetingPlanEntity plan) {
        rebuildWeeks(plan);
    }

    private void rebuildWeeks(RoutineMeetingPlanEntity plan) {
        if (plan.getStartDate() == null || plan.getEndDate() == null) {
            return;
        }
        List<LocalDate> desired = weekStarts(plan.getStartDate(), plan.getEndDate());
        List<RoutineMeetingPlanWeekEntity> existing = weekMapper.selectList(
                new LambdaQueryWrapper<RoutineMeetingPlanWeekEntity>()
                        .eq(RoutineMeetingPlanWeekEntity::getPlanId, plan.getId()));
        Map<LocalDate, RoutineMeetingPlanWeekEntity> existingMap = existing.stream()
                .collect(Collectors.toMap(RoutineMeetingPlanWeekEntity::getWeekStart, v -> v, (a, b) -> a));

        if (desired.isEmpty()) {
            weekMapper.delete(new LambdaQueryWrapper<RoutineMeetingPlanWeekEntity>()
                    .eq(RoutineMeetingPlanWeekEntity::getPlanId, plan.getId()));
            return;
        }

        weekMapper.delete(new LambdaQueryWrapper<RoutineMeetingPlanWeekEntity>()
                .eq(RoutineMeetingPlanWeekEntity::getPlanId, plan.getId())
                .notIn(RoutineMeetingPlanWeekEntity::getWeekStart, desired));

        for (LocalDate weekStart : desired) {
            if (!existingMap.containsKey(weekStart)) {
                RoutineMeetingPlanWeekEntity week = new RoutineMeetingPlanWeekEntity();
                week.setPlanId(plan.getId());
                week.setWeekStart(weekStart);
                week.setWeekEnd(weekStart.plusDays(6));
                week.setLeaderId(plan.getLeaderId());
                week.setLeaderName(plan.getLeaderName());
                week.setProgress(0);
                weekMapper.insert(week);
            }
        }
    }

    private void updatePendingWeekLeader(RoutineMeetingPlanEntity plan) {
        List<RoutineMeetingPlanWeekEntity> weeks = weekMapper.selectList(
                new LambdaQueryWrapper<RoutineMeetingPlanWeekEntity>()
                        .eq(RoutineMeetingPlanWeekEntity::getPlanId, plan.getId())
                        .isNull(RoutineMeetingPlanWeekEntity::getFirstFeedbackAt));
        for (RoutineMeetingPlanWeekEntity week : weeks) {
            week.setLeaderId(plan.getLeaderId());
            week.setLeaderName(plan.getLeaderName());
            weekMapper.updateById(week);
        }
    }

    private void recalcPlan(RoutineMeetingPlanEntity plan) {
        List<RoutineMeetingPlanWeekEntity> weeks = weekMapper.selectList(
                new LambdaQueryWrapper<RoutineMeetingPlanWeekEntity>()
                        .eq(RoutineMeetingPlanWeekEntity::getPlanId, plan.getId())
                        .orderByAsc(RoutineMeetingPlanWeekEntity::getWeekStart));
        RoutineMeetingPlanWeekEntity latest = null;
        for (RoutineMeetingPlanWeekEntity week : weeks) {
            if (week.getFirstFeedbackAt() != null
                    && (latest == null || week.getWeekStart().isAfter(latest.getWeekStart()))) {
                latest = week;
            }
        }

        int progress = latest == null ? 0 : (latest.getProgress() == null ? 0 : latest.getProgress());
        boolean reopen = false;
        if (plan.getCompletedAt() != null && (latest == null || progress < 100)) {
            plan.setCompletedAt(null);
            reopen = true;
        }

        if (plan.getCompletedAt() == null && latest != null && progress >= 100) {
            plan.setProgress(100);
            plan.setCompletedAt(latest.getFirstFeedbackAt());
            baseMapper.updateById(plan);
            weekMapper.delete(new LambdaQueryWrapper<RoutineMeetingPlanWeekEntity>()
                    .eq(RoutineMeetingPlanWeekEntity::getPlanId, plan.getId())
                    .gt(RoutineMeetingPlanWeekEntity::getWeekStart, latest.getWeekStart()));
            return;
        }

        plan.setProgress(progress);
        if (reopen) {
            baseMapper.update(null, new LambdaUpdateWrapper<RoutineMeetingPlanEntity>()
                    .eq(RoutineMeetingPlanEntity::getId, plan.getId())
                    .set(RoutineMeetingPlanEntity::getCompletedAt, null)
                    .set(RoutineMeetingPlanEntity::getProgress, progress));
        } else {
            baseMapper.updateById(plan);
        }

        if (reopen) {
            rebuildWeeks(plan);
        }
    }

    private void copyPlanFields(RoutineMeetingPlanEntity plan, RoutineMeetingPlanSaveRequest request) {
        plan.setName(request.getName());
        plan.setGoal(request.getGoal());
        plan.setLeaderId(request.getLeaderId());
        plan.setLeaderName(request.getLeaderName());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
    }

    private void validatePlan(RoutineMeetingPlanSaveRequest request, boolean requireAll) {
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new ServiceException("结束日期不能早于开始日期");
        }
        if (request.getName() != null && request.getName().length() > 60) {
            throw new ServiceException("计划名称不能超过60字");
        }
        if (!requireAll) {
            return;
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new ServiceException("请输入计划名称");
        }
        if (!StringUtils.hasText(request.getGoal())) {
            throw new ServiceException("请输入目标");
        }
        if (!StringUtils.hasText(request.getLeaderId())) {
            throw new ServiceException("请选择责任领导");
        }
        if (request.getStartDate() == null) {
            throw new ServiceException("请选择开始日期");
        }
        if (request.getEndDate() == null) {
            throw new ServiceException("请选择结束日期");
        }
    }

    private RoutineMeetingPlanEntity getPlan(Long id) {
        RoutineMeetingPlanEntity plan = baseMapper.selectById(id);
        if (plan == null) {
            throw new ServiceException("计划不存在");
        }
        return plan;
    }

    private void assertVisible(RoutineMeetingPlanEntity plan) {
    }

    private boolean canEdit(RoutineMeetingPlanEntity plan) {
        if (plan.getCompletedAt() != null) {
            return false;
        }
        if (Integer.valueOf(1).equals(plan.getDraft())) {
            return isAdmin() || isCreator(plan) || isLeader(plan);
        }
        return isAdmin() || isLeader(plan);
    }

    private boolean canDelete(RoutineMeetingPlanEntity plan, String status) {
        if (!Integer.valueOf(1).equals(plan.getDraft()) && !NOT_STARTED.equals(status)) {
            return false;
        }
        if (isAdmin() || isLeader(plan)) {
            return true;
        }
        return Integer.valueOf(1).equals(plan.getDraft()) && isCreator(plan);
    }

    private boolean canExtend(RoutineMeetingPlanEntity plan, String status) {
        return !Integer.valueOf(1).equals(plan.getDraft())
                && plan.getCompletedAt() == null
                && IN_PROGRESS.equals(status)
                && (isAdmin() || isLeader(plan));
    }

    private String statusOf(RoutineMeetingPlanEntity plan) {
        if (Integer.valueOf(1).equals(plan.getDraft())) {
            return DRAFT;
        }
        if (plan.getCompletedAt() != null) {
            LocalDate originalEnd = plan.getOriginalEndDate() == null ? plan.getEndDate() : plan.getOriginalEndDate();
            LocalDate completedDate = toLocalDate(plan.getCompletedAt());
            if (originalEnd != null && completedDate.isAfter(originalEnd)) {
                return COMPLETED_OVERDUE;
            }
            return COMPLETED_ON_TIME;
        }
        if (plan.getStartDate() != null && today().isBefore(plan.getStartDate())) {
            return NOT_STARTED;
        }
        return IN_PROGRESS;
    }

    private String statusName(String status) {
        switch (status) {
            case DRAFT:
                return "草稿";
            case NOT_STARTED:
                return "未开始";
            case COMPLETED_ON_TIME:
                return "如期完成";
            case COMPLETED_OVERDUE:
                return "超期完成";
            default:
                return "执行中";
        }
    }

    private String weekStatusName(String status) {
        switch (status) {
            case "ON_TIME":
                return "如期反馈";
            case "OVERDUE":
                return "超期反馈";
            case "NOT_DUE":
                return "未到期";
            default:
                return "未反馈";
        }
    }

    private String normalizeStatus(String status) {
        if (DRAFT.equals(status) || NOT_STARTED.equals(status) || IN_PROGRESS.equals(status)
                || COMPLETED_ON_TIME.equals(status) || COMPLETED_OVERDUE.equals(status)) {
            return status;
        }
        if ("草稿".equals(status)) return DRAFT;
        if ("未开始".equals(status)) return NOT_STARTED;
        if ("执行中".equals(status)) return IN_PROGRESS;
        if ("如期完成".equals(status)) return COMPLETED_ON_TIME;
        if ("超期完成".equals(status)) return COMPLETED_OVERDUE;
        return status;
    }

    private Long durationDays(RoutineMeetingPlanEntity plan) {
        if (plan.getStartDate() == null || plan.getEndDate() == null) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate()) + 1;
    }

    private Long remainDays(RoutineMeetingPlanEntity plan) {
        if (plan.getEndDate() == null || plan.getCompletedAt() != null) {
            return 0L;
        }
        long days = ChronoUnit.DAYS.between(today(), plan.getEndDate()) + 1;
        return Math.max(days, 0);
    }

    private Long overdueDays(RoutineMeetingPlanEntity plan, String status) {
        if (COMPLETED_OVERDUE.equals(status)) {
            LocalDate originalEnd = plan.getOriginalEndDate() == null ? plan.getEndDate() : plan.getOriginalEndDate();
            if (originalEnd != null && plan.getCompletedAt() != null) {
                return Math.max(0L, ChronoUnit.DAYS.between(originalEnd, toLocalDate(plan.getCompletedAt())));
            }
        }
        if (plan.getEndDate() != null && plan.getCompletedAt() == null
                && today().isAfter(plan.getEndDate())) {
            return Math.max(0L, ChronoUnit.DAYS.between(plan.getEndDate(), today()));
        }
        return 0L;
    }

    private List<LocalDate> weekStarts(LocalDate start, LocalDate end) {
        List<LocalDate> result = new ArrayList<>();
        if (start == null || end == null || end.isBefore(start)) {
            return result;
        }
        LocalDate cursor = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate last = end.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        while (!cursor.isAfter(last)) {
            result.add(cursor);
            cursor = cursor.plusWeeks(1);
        }
        return result;
    }

    private LocalDate normalizeMonday(LocalDate date) {
        return date == null ? null : date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate currentMonday() {
        return today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate today() {
        return LocalDate.now();
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private String formatWeek(LocalDate start, LocalDate end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        return start.format(formatter) + " 至 " + end.format(formatter);
    }

    private String currentUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId != null) {
            return String.valueOf(userId);
        }
        return currentUserCode();
    }

    private String currentUserCode() {
        String code = SecurityUtils.getUserCode();
        return code == null ? "" : code;
    }

    private String currentUserName() {
        String name = SecurityUtils.getUserName();
        if (StringUtils.hasText(name)) {
            return name;
        }
        return currentUserId();
    }

    private boolean isAdmin() {
        try {
            if (SecurityUtils.isAdmin()) {
                return true;
            }
            UserDetailsImpl details = SecurityUtils.getUserDetails();
            return details != null && details.getRoleIds() != null
                    && details.getRoleIds().stream().anyMatch(ADMIN_ROLE_IDS::contains);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isSuperAdmin() {
        try {
            UserDetailsImpl details = SecurityUtils.getUserDetails();
            if (details != null) {
                if (details.getRoleIds() != null
                        && details.getRoleIds().contains(SUPER_ADMIN_ROLE)) {
                    return true;
                }
                if (details.getAuthorities() != null
                        && details.getAuthorities().stream().anyMatch(authority ->
                        SUPER_ADMIN_ROLE.equals(authority.getAuthority())
                                || ("ROLE_" + SUPER_ADMIN_ROLE).equals(authority.getAuthority()))) {
                    return true;
                }
            }
            String token = SecurityUtils.obtainBearerToken();
            if (!StringUtils.hasText(token)) {
                return false;
            }
            long now = System.currentTimeMillis();
            SuperAdminCache cache = SUPER_ADMIN_CACHE.get(token);
            if (cache != null && cache.expiresAt > now) {
                return cache.superAdmin;
            }
            boolean result = fetchSuperAdmin(token);
            if (SUPER_ADMIN_CACHE.size() > 500) {
                SUPER_ADMIN_CACHE.clear();
            }
            SUPER_ADMIN_CACHE.put(token, new SuperAdminCache(result, now + 60000));
            return result;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean fetchSuperAdmin(String token) {
        if (!StringUtils.hasText(tokenInfoUri)) {
            return false;
        }
        String base = tokenInfoUri;
        int index = base.indexOf("/oauth/check_token");
        if (index > 0) {
            base = base.substring(0, index);
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(base + "/api/system/permission/info");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestProperty("Authorization", "Bearer " + token);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }
            try (InputStream inputStream = connection.getInputStream()) {
                JsonNode roles = OBJECT_MAPPER.readTree(inputStream).path("data").path("roles");
                if (roles.isArray()) {
                    for (JsonNode role : roles) {
                        if (SUPER_ADMIN_ROLE.equals(role.asText())) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return false;
    }

    private void ensureSuperAdmin() {
        if (!isSuperAdmin()) {
            throw new ServiceException("只有超级管理员可以校正历史数据");
        }
    }

    private void assertNoEarlierMissingWeeks(Long planId, LocalDate weekStart) {
        List<RoutineMeetingPlanWeekEntity> missingWeeks = weekMapper.selectList(
                new LambdaQueryWrapper<RoutineMeetingPlanWeekEntity>()
                        .eq(RoutineMeetingPlanWeekEntity::getPlanId, planId)
                        .lt(RoutineMeetingPlanWeekEntity::getWeekStart, weekStart)
                        .isNull(RoutineMeetingPlanWeekEntity::getFirstFeedbackAt)
                        .orderByAsc(RoutineMeetingPlanWeekEntity::getWeekStart));
        if (!missingWeeks.isEmpty()) {
            String labels = missingWeeks.stream()
                    .map(item -> formatWeek(item.getWeekStart(), item.getWeekEnd()))
                    .collect(Collectors.joining("、"));
            throw new ServiceException("前面还有未反馈的周：" + labels
                    + "，请先补填后再提交100%");
        }
    }

    private Date combineDate(Date existing, LocalDate date) {
        Calendar calendar = Calendar.getInstance();
        if (existing != null) {
            calendar.setTime(existing);
        }
        calendar.set(Calendar.YEAR, date.getYear());
        calendar.set(Calendar.MONTH, date.getMonthValue() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, date.getDayOfMonth());
        return calendar.getTime();
    }

    private boolean isLatestFeedbackWeek(Long planId, LocalDate weekStart) {
        List<RoutineMeetingPlanWeekEntity> weeks = weekMapper.selectList(
                new LambdaQueryWrapper<RoutineMeetingPlanWeekEntity>()
                        .eq(RoutineMeetingPlanWeekEntity::getPlanId, planId)
                        .isNotNull(RoutineMeetingPlanWeekEntity::getFirstFeedbackAt)
                        .orderByDesc(RoutineMeetingPlanWeekEntity::getWeekStart));
        return !weeks.isEmpty() && weeks.get(0).getWeekStart().equals(weekStart);
    }

    private Date latestFeedbackDate(Long planId) {
        List<RoutineMeetingPlanWeekEntity> weeks = weekMapper.selectList(
                new LambdaQueryWrapper<RoutineMeetingPlanWeekEntity>()
                        .eq(RoutineMeetingPlanWeekEntity::getPlanId, planId)
                        .isNotNull(RoutineMeetingPlanWeekEntity::getFirstFeedbackAt)
                        .orderByDesc(RoutineMeetingPlanWeekEntity::getWeekStart));
        return weeks.isEmpty() ? null : weeks.get(0).getFirstFeedbackAt();
    }

    private boolean isLeader(RoutineMeetingPlanEntity plan) {
        return equalsString(plan.getLeaderId(), currentUserId())
                || equalsString(plan.getLeaderId(), currentUserCode());
    }

    private boolean isCreator(RoutineMeetingPlanEntity plan) {
        return equalsString(plan.getCreatorId(), currentUserId());
    }

    private boolean equalsString(String a, String b) {
        return a != null && a.equals(b);
    }

    private static class SuperAdminCache {
        private final boolean superAdmin;
        private final long expiresAt;

        private SuperAdminCache(boolean superAdmin, long expiresAt) {
            this.superAdmin = superAdmin;
            this.expiresAt = expiresAt;
        }
    }
}
