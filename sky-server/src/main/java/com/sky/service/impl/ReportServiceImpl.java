package com.sky.service.impl;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.DateAmount;
import com.sky.entity.OrderCount;
import com.sky.entity.Orders;
import com.sky.entity.UserAmount;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;
    @Override
    public TurnoverReportVO TurnoverStatistics(LocalDate begin, LocalDate end) {
        //-----------------------日期列表---------------------------//
        //构造日期列表
        List<LocalDate> dateList =new ArrayList<>();
        //每次累加1天
        LocalDate TempTime=begin;
        while (!TempTime.isAfter(end)){
            dateList.add(TempTime);
            TempTime= TempTime.plusDays(1);

        }
        //使用StringUtils构造String
        String dateString = StringUtils.join(dateList, ",");
        //--------------------每日营业额----------------------//
        //构造开始时间与结束时间用于sql查询
        LocalDateTime beginTime=LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime=LocalDateTime.of(end,LocalTime.MAX);
        Map map=new HashMap();
        map.put("begin",beginTime);
        map.put("end",endTime);
        map.put("status",Orders.COMPLETED);

        List<DateAmount>  dateStatistics=orderMapper.TurnoverStatistics(map);
        log.info("dateStatistics:{}",dateStatistics);
        List<Double> DateStatistics=new ArrayList<>();
        //处理dateStatistics为空的情况
        if(dateStatistics.isEmpty()){
            for (int i = 0; i < dateList.size(); i++) {
                DateStatistics.add(0.0);
            }
        }else {
            //先将第一天插入
            DateStatistics.add(dateStatistics.get(0).getDateAmount());
            //从第二天开始遍历，如果当天与上一天并不是相差一天，则在中间补上0.0
            for (int i = 1; i < dateStatistics.size(); i++) {
                DateAmount current = dateStatistics.get(i);
                DateAmount pervious = dateStatistics.get(i - 1);
                LocalDate today=current.getOrderTime().toLocalDate();
                LocalDate perviousDay=pervious.getOrderTime().toLocalDate();
                //具体在这里实现
                long daysBetween = ChronoUnit.DAYS.between(perviousDay, today) - 1;
                // 若有间隔（间隔天数 > 0），填充0.0
                for (long j = 0; j < daysBetween; j++) {
                    DateStatistics.add(0.0);
                }
                DateStatistics.add(current.getDateAmount());
            }
            // 如果数据库查出来的第一天并非begin 需要补上前面的0.0 判断dateStatisticsList长度是否小于dateList，在前面补0.0
            int diff = dateList.size() - DateStatistics.size();
            if (diff > 0) {
                List<Double> prefix = new ArrayList<>();
                // 先添加需要补充的0.0到前缀列表
                for (int i = 0; i<diff; i++) {
                    prefix.add(0.0);
                }
                // 将前缀列表与原列表合并（前缀在前，原列表在后）
                prefix.addAll(DateStatistics);
                DateStatistics = prefix;
            }
        }

        String dateStatisticsList=StringUtils.join(DateStatistics,",");
        TurnoverReportVO turnoverReportVO=TurnoverReportVO
                .builder()
                .dateList(dateString)
                .turnoverList(dateStatisticsList)
                .build();
        return turnoverReportVO;
    }

    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        //-----------------------日期列表---------------------------//
        List<LocalDate> dateList=new ArrayList<>();
        LocalDate tempDate=begin;
        while (!tempDate.isAfter(end)){
            dateList.add(tempDate);
            tempDate=tempDate.plusDays(1);
        }
        //------------------------处理新增用户-----------------------//
        List<Long> userAmount=new ArrayList<>();
        Map map=new HashMap();
        LocalDateTime beginTime=LocalDateTime.of(begin,LocalTime.MIN);
        LocalDateTime endTime=LocalDateTime.of(end,LocalTime.MAX);
        map.put("beginTime",beginTime);
        map.put("endTime",endTime);
        List<UserAmount> userDateAmount=userMapper.countByDate(map);
        if(userDateAmount.isEmpty()){
            for (int i = 0; i < dateList.size(); i++) {
                userAmount.add(0L);
            }
        }else {
            userAmount.add(userDateAmount.get(0).getDateAmount());
            for (int i = 1; i < userDateAmount.size(); i++) {
                UserAmount current=userDateAmount.get(i);
                UserAmount previous=userDateAmount.get(i-1);
                LocalDate today=current.getCreateTime().toLocalDate();
                LocalDate perviousDay=previous.getCreateTime().toLocalDate();
                Long between= ChronoUnit.DAYS.between(perviousDay, today) - 1;
                for (int j = 0; j < between; j++) {
                    userAmount.add(0L);
                }
                userAmount.add(current.getDateAmount());
            }
            int diff=dateList.size()-userAmount.size();
            if(diff>0){
                List<Long> prefix=new ArrayList<>();
                for (int i = 0; i < diff; i++) {
                    prefix.add(0L);
                }
                prefix.addAll(userAmount);
                userAmount=prefix;
            }
        }

        List<Long> userTotalAmount=new ArrayList<>();
        //处理累计用户
        Long previoesAmount= userMapper.countUser(beginTime);
        Long currentSum=0L;
        for (Long todayAmount : userAmount) {
            currentSum+=todayAmount;
            userTotalAmount.add(currentSum+previoesAmount);
        }
        UserReportVO userReportVO=UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .newUserList(StringUtils.join(userAmount,","))
                .totalUserList(StringUtils.join(userTotalAmount,","))
                .build();
        return userReportVO;
    }

    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {
        //-----------------------日期列表---------------------------//
        List<LocalDate> dateList=new ArrayList<>();
        LocalDate tempDate=begin;
        while (!tempDate.isAfter(end)){
            dateList.add(tempDate);
            tempDate=tempDate.plusDays(1);
        }
        //-----------------------共用参数--------------------------//
        LocalDateTime beginTime=LocalDateTime.of(begin,LocalTime.MIN);
        LocalDateTime endTime=LocalDateTime.of(end,LocalTime.MAX);
        Map map=new HashMap();
        map.put("begin",beginTime);
        map.put("end",endTime);
        //-----------------------每日的订单------------------------//
        List<Long> EveryDayOrderAmount=new ArrayList<>();
        List<OrderCount> OrderAmountList=orderMapper.ordersAStatistics(map);
        EveryDayOrderAmount=getOrderAmount(dateList,OrderAmountList,EveryDayOrderAmount);
        //-----------------------总订单数-------------------//
        Integer totalOrderAmount=orderMapper.countByStatusAndTime(map);
        //----------------------每日完成的订单---------------------------//
        //查询完成的订单
        map.put("status",Orders.COMPLETED);
        List<Long> completedEveryDayOrderAmount=new ArrayList<>();
        List<OrderCount> completedOrderAmountList=orderMapper.ordersAStatistics(map);
        completedEveryDayOrderAmount=getOrderAmount(dateList,completedOrderAmountList,completedEveryDayOrderAmount);
        //-----------------------有效订单数---------------//
        Integer completedOrderAmount=orderMapper.countByStatusAndTime(map);
        //----------------------完成率-------------------//
        double orderCompletionRate = totalOrderAmount == 0
                ? 0.0  // 无订单时，完成率为0
                : (double) completedOrderAmount / totalOrderAmount;
        //----------------------构造返回数据--------------//
        OrderReportVO orderReportVO=OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .validOrderCountList(StringUtils.join(completedEveryDayOrderAmount,","))
                .orderCountList(StringUtils.join(EveryDayOrderAmount,","))
                .validOrderCount(completedOrderAmount)
                .totalOrderCount(totalOrderAmount)
                .orderCompletionRate(orderCompletionRate)
                .build();
        return orderReportVO;
    }

    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {

        Map map=new HashMap();
        LocalDateTime beginTime=LocalDateTime.of(begin,LocalTime.MIN);
        LocalDateTime endTime=LocalDateTime.of(end, LocalTime.MAX);
        map.put("begin",beginTime);
        map.put("end",endTime);
        map.put("status",Orders.COMPLETED);
        List<GoodsSalesDTO> top10List=orderMapper.top10(map);
        //------------------使用stream处理List-------------//
        List<String> nameList = top10List.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = top10List.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        //------------------构造返回数据-------------------//
        SalesTop10ReportVO salesTop10=SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList,","))
                .numberList(StringUtils.join(numberList,","))
                .build();
        return salesTop10;
    }

    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //-----------查询数据库，获取数据---------------------//
        LocalDate beginDay = LocalDate.now().minusDays(30);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime start=LocalDateTime.of(beginDay,LocalTime.MIN);
        LocalDateTime end=LocalDateTime.of(yesterday,LocalTime.MAX);
        BusinessDataVO businessData = workspaceService.getBusinessData(start,end);
        //----------通过POI将数据写入到excel------------------//
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            XSSFWorkbook excel = new XSSFWorkbook(inputStream);
            //-------------------填充数据----------------//
            XSSFSheet sheet = excel.getSheet("Sheet1");
            //---时间--//
            sheet.getRow(1).getCell(1).setCellValue("时间: "+beginDay+"至"+yesterday);

            //--营业额--//
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessData.getTurnover());
            //--订单完成率--//
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            //--新增用户数--//
            row.getCell(6).setCellValue(businessData.getNewUsers());

            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());

            //--订单数据--//
            //营业额列表//
            TurnoverReportVO turnoverReportVO = TurnoverStatistics(beginDay, yesterday);
            String turnoverList = turnoverReportVO.getTurnoverList();
            String[] split = StringUtils.split(turnoverList, ",");
            List<String> turnover = Arrays.asList(split);
            //日期列表//
            String dateList = turnoverReportVO.getDateList();
            split = StringUtils.split(dateList, ",");
            List<String>  date=Arrays.asList(split);
            //用户列表//
            UserReportVO userReportVO = userStatistics(beginDay, yesterday);
            String newUserList = userReportVO.getNewUserList();
            split = StringUtils.split(newUserList, ",");
            List<String> newUser=Arrays.asList(split);

            //总订单//
            OrderReportVO orderReportVO = ordersStatistics(beginDay, yesterday);
            String orderCountList = orderReportVO.getOrderCountList();
            split = StringUtils.split(orderCountList, ",");
            List<String> orderCount=Arrays.asList(split);
            //有效订单
            String validOrderCountList = orderReportVO.getValidOrderCountList();
            split = StringUtils.split(validOrderCountList, ",");
            List<String> validOrderCount=Arrays.asList(split);
            

            //向excel插入数据
            for (int i = 0; i < 30; i++) {
                XSSFRow sheetRow = sheet.getRow(7+i);
                //日期
                sheetRow.getCell(1).setCellValue(date.get(i));
                //营业额
                sheetRow.getCell(2).setCellValue(turnover.get(i));
                //有效订单
                sheetRow.getCell(3).setCellValue(validOrderCount.get(i));
                //订单完成率
                String currentValidOrder= validOrderCount.get(i);
                String currentOrderCount=orderCount.get(i);
                Double orderCompletionRate;
                if(currentOrderCount.equals("0")){
                    orderCompletionRate=0.0;
                }else {
                    orderCompletionRate=Double.parseDouble(currentValidOrder)/Double.parseDouble(currentOrderCount);
                }
                sheetRow.getCell(4).setCellValue(orderCompletionRate);
                //平均客单价
                Double unitPrice;
                if(validOrderCount.get(i).equals("0")){
                    unitPrice=0.0;
                }else {
                    unitPrice= Double.parseDouble(turnover.get(i))/Double.parseDouble(validOrderCount.get(i));
                }
                sheetRow.getCell(5).setCellValue(unitPrice);
                //新增用户数
                sheetRow.getCell(6).setCellValue(newUser.get(i));
            }
            //

            //----------通过流将Excel文件下载到客户端浏览器------//
            ServletOutputStream outputStream = response.getOutputStream();
            excel.write(outputStream);
            //关闭资源
            outputStream.close();
            excel.close();

        } catch (IOException e) {
            log.error("导出数据导出失败", e);
            throw new RuntimeException("导出数据导出失败", e);
        }
    }

    public List<Long> getOrderAmount(List<LocalDate> dateList, List<OrderCount> OrderAmountList, List<Long> everyDayOrderAmount){
        if(OrderAmountList.isEmpty()){
            for (int i = 0; i < dateList.size(); i++) {
                everyDayOrderAmount.add(0L);
            }
        }else {
            everyDayOrderAmount.add(OrderAmountList.get(0).getOrderCount());
            for (int i = 1; i <OrderAmountList.size() ; i++) {
                OrderCount current=OrderAmountList.get(i);
                OrderCount previous=OrderAmountList.get(i-1);
                LocalDate perviousDay=previous.getOrderTime().toLocalDate();
                LocalDate today=current.getOrderTime().toLocalDate();
                Long betweenDays=ChronoUnit.DAYS.between(perviousDay, today) - 1;
                for (int j = 0; j < betweenDays; j++) {
                    everyDayOrderAmount.add(0L);
                }
                everyDayOrderAmount.add(current.getOrderCount());
            }
            int diff=dateList.size()-everyDayOrderAmount.size();
            if(diff>0){
                List<Long> prefix=new ArrayList<>();
                for (int i = 0; i < diff; i++) {
                    prefix.add(0L);
                }
                prefix.addAll(everyDayOrderAmount);
                everyDayOrderAmount=prefix;
            }
        }
        return everyDayOrderAmount;
    }
}
